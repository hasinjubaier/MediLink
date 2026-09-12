package com.medilink.service.market;

import com.medilink.model.market.MarketPriceItem;
import com.medilink.model.market.MedicinePriceHistory;
import com.medilink.model.medicine.Medicine;
import com.medilink.model.pharmacy.PharmacyStock;
import com.medilink.repository.MedicinePriceHistoryRepository;
import com.medilink.repository.MedicineRepository;
import com.medilink.repository.PharmacyStockRepository;
import com.medilink.service.StockObserverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Enterprise Service for automatic Bangladesh medicine market price synchronization.
 * Synchronizes PostgreSQL medicine records and pharmacy stocks, records audit logs,
 * and broadcasts price changes over real-time SSE.
 */
@Service
public class MarketPriceSyncService {

    private final MedicineRepository medicineRepository;
    private final PharmacyStockRepository pharmacyStockRepository;
    private final MedicinePriceHistoryRepository priceHistoryRepository;
    private final BangladeshDgdaMedexProvider marketPriceProvider;

    @Autowired
    public MarketPriceSyncService(MedicineRepository medicineRepository,
                                  PharmacyStockRepository pharmacyStockRepository,
                                  MedicinePriceHistoryRepository priceHistoryRepository,
                                  BangladeshDgdaMedexProvider marketPriceProvider) {
        this.medicineRepository = medicineRepository;
        this.pharmacyStockRepository = pharmacyStockRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.marketPriceProvider = marketPriceProvider;
    }

    /**
     * Automatic background polling scheduled every 5 minutes (300,000 ms).
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 15000)
    public void scheduledMarketSync() {
        System.out.println("[MarketPriceSyncService] Starting scheduled BD market price synchronization...");
        SyncResult result = syncAllMedicines("Scheduled Auto-Sync (Cron)");
        System.out.println("[MarketPriceSyncService] Scheduled sync completed: " + result.getUpdatedCount() + " price(s) updated.");
    }

    /**
     * Synchronizes all local medicine prices against the Bangladesh market registry.
     */
    @Transactional
    public SyncResult syncAllMedicines(String triggerSource) {
        List<Medicine> allMeds = medicineRepository.findAll();
        List<MedicinePriceHistory> changes = new ArrayList<>();
        int checkedCount = 0;
        int updatedCount = 0;

        for (Medicine med : allMeds) {
            checkedCount++;
            Optional<MarketPriceItem> quoteOpt = marketPriceProvider.fetchPriceByBrand(med.getBrandName());
            if (quoteOpt.isPresent()) {
                MarketPriceItem quote = quoteOpt.get();
                double marketPrice = quote.getMrp();
                double currentPrice = med.getUnitPrice();

                // Check if price changed by more than 0.01 BDT
                if (Math.abs(currentPrice - marketPrice) >= 0.01) {
                    double oldPrice = currentPrice;
                    med.setUnitPrice(marketPrice);
                    medicineRepository.save(med);

                    // Update corresponding pharmacy stocks
                    List<PharmacyStock> stocks = pharmacyStockRepository.findByMedicineId(med.getId());
                    for (PharmacyStock stock : stocks) {
                        stock.setUnitPrice(marketPrice);
                        stock.setLastUpdated(LocalDateTime.now());
                        pharmacyStockRepository.save(stock);
                    }

                    // Create Price History audit record
                    String histId = "prc_hist_" + UUID.randomUUID().toString().substring(0, 8);
                    MedicinePriceHistory history = new MedicinePriceHistory(
                            histId,
                            med.getId(),
                            med.getBrandName(),
                            med.getGenericName(),
                            med.getCompany(),
                            oldPrice,
                            marketPrice,
                            triggerSource != null ? triggerSource : quote.getSourceRegistry()
                    );
                    priceHistoryRepository.save(history);
                    changes.add(history);
                    updatedCount++;

                    // Broadcast real-time SSE event to all connected patients & pharmacists
                    broadcastPriceUpdate(med, oldPrice, marketPrice, history.getDirection(), history.getPercentageChange());
                }
            }
        }

        return new SyncResult(checkedCount, updatedCount, changes, LocalDateTime.now());
    }

    /**
     * Direct update from Webhook or manual Admin trigger.
     */
    @Transactional
    public boolean updatePriceDirectly(String brandName, double newPrice, String source) {
        if (brandName == null || newPrice <= 0) return false;
        marketPriceProvider.updateRegistryPrice(brandName, newPrice);

        Optional<Medicine> medOpt = medicineRepository.findAll().stream()
                .filter(m -> m.getBrandName().equalsIgnoreCase(brandName.trim()) ||
                             m.getBrandName().toLowerCase().contains(brandName.toLowerCase().trim()))
                .findFirst();

        if (medOpt.isPresent()) {
            Medicine med = medOpt.get();
            double oldPrice = med.getUnitPrice();
            med.setUnitPrice(newPrice);
            medicineRepository.save(med);

            List<PharmacyStock> stocks = pharmacyStockRepository.findByMedicineId(med.getId());
            for (PharmacyStock s : stocks) {
                s.setUnitPrice(newPrice);
                s.setLastUpdated(LocalDateTime.now());
                pharmacyStockRepository.save(s);
            }

            String histId = "prc_hist_" + UUID.randomUUID().toString().substring(0, 8);
            MedicinePriceHistory history = new MedicinePriceHistory(
                    histId,
                    med.getId(),
                    med.getBrandName(),
                    med.getGenericName(),
                    med.getCompany(),
                    oldPrice,
                    newPrice,
                    source != null ? source : "Live Webhook Push"
            );
            priceHistoryRepository.save(history);

            broadcastPriceUpdate(med, oldPrice, newPrice, history.getDirection(), history.getPercentageChange());
            return true;
        }
        return false;
    }

    /**
     * Simulates a realistic DGDA market price adjustment for interactive demonstrations.
     */
    @Transactional
    public MedicinePriceHistory simulateDgdaFluctuation(String medicineId, Double targetPrice) {
        Medicine targetMed = null;
        if (medicineId != null && !medicineId.trim().isEmpty()) {
            targetMed = medicineRepository.findById(medicineId.trim()).orElse(null);
        }
        if (targetMed == null) {
            List<Medicine> meds = medicineRepository.findAll();
            if (meds.isEmpty()) return null;
            // Pick Napa Extra or first available
            targetMed = meds.stream()
                    .filter(m -> m.getBrandName().contains("Napa"))
                    .findFirst()
                    .orElse(meds.get(0));
        }

        double oldPrice = targetMed.getUnitPrice();
        double newPrice;

        if (targetPrice != null && targetPrice > 0) {
            newPrice = targetPrice;
        } else {
            // Fluctuate by +৳0.50 or -৳0.50 realistically
            if (oldPrice <= 3.00) {
                newPrice = (oldPrice == 3.50) ? 3.00 : 3.50;
            } else if (oldPrice <= 10.00) {
                newPrice = (oldPrice >= 6.50) ? 6.00 : 6.80;
            } else {
                newPrice = Math.round((oldPrice * 1.10) * 100.0) / 100.0;
            }
        }

        updatePriceDirectly(targetMed.getBrandName(), newPrice, "DGDA Official Market Circular");
        return priceHistoryRepository.findTop20ByOrderByTimestampDesc().stream().findFirst().orElse(null);
    }

    private void broadcastPriceUpdate(Medicine med, double oldPrice, double newPrice, String direction, double pct) {
        String sign = pct >= 0 ? "+" : "";
        String payload = String.format("{\"type\":\"PRICE_UPDATE\",\"medicineId\":\"%s\",\"brandName\":\"%s\",\"genericName\":\"%s\",\"oldPrice\":%.2f,\"newPrice\":%.2f,\"direction\":\"%s\",\"changePercent\":\"%s%.1f%%\",\"timestamp\":\"%s\"}",
                med.getId(),
                med.getBrandName(),
                med.getGenericName(),
                oldPrice,
                newPrice,
                direction,
                sign,
                pct,
                LocalDateTime.now()
        );

        // Notify client stream over SSE
        StockObserverService.getInstance().onNotification("PRICE_UPDATE", payload);
        System.out.println("[MarketPriceSyncService] Real-time Price Update Broadcasted: " + med.getBrandName() +
                " " + oldPrice + " -> " + newPrice + " BDT (" + sign + pct + "%)");
    }

    public List<MedicinePriceHistory> getRecentPriceHistories() {
        return priceHistoryRepository.findTop20ByOrderByTimestampDesc();
    }

    public List<MedicinePriceHistory> getAllPriceHistories() {
        return priceHistoryRepository.findAllByOrderByTimestampDesc();
    }

    public Medicine findMedicineByBrand(String brandName) {
        if (brandName == null || brandName.trim().isEmpty()) return null;
        String clean = brandName.trim().toLowerCase();
        return medicineRepository.findAll().stream()
                .filter(m -> m.getBrandName().equalsIgnoreCase(clean) ||
                             m.getBrandName().toLowerCase().contains(clean) ||
                             clean.contains(m.getBrandName().toLowerCase()))
                .findFirst()
                .orElse(null);
    }

    public Medicine findDefaultMedicine() {
        List<Medicine> meds = medicineRepository.findAll();
        if (meds.isEmpty()) return null;
        return meds.stream()
                .filter(m -> m.getBrandName().toLowerCase().contains("napa"))
                .findFirst()
                .orElse(meds.get(0));
    }

    public BangladeshDgdaMedexProvider getMarketPriceProvider() {
        return marketPriceProvider;
    }

    public static class SyncResult {
        private final int checkedCount;
        private final int updatedCount;
        private final List<MedicinePriceHistory> changes;
        private final LocalDateTime timestamp;

        public SyncResult(int checkedCount, int updatedCount, List<MedicinePriceHistory> changes, LocalDateTime timestamp) {
            this.checkedCount = checkedCount;
            this.updatedCount = updatedCount;
            this.changes = changes;
            this.timestamp = timestamp;
        }

        public int getCheckedCount() { return checkedCount; }
        public int getUpdatedCount() { return updatedCount; }
        public List<MedicinePriceHistory> getChanges() { return changes; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}
