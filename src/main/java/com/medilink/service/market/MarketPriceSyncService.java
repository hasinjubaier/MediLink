package com.medilink.service.market;

import com.medilink.config.MarketApiProperties;
import com.medilink.model.market.MarketPriceItem;
import com.medilink.model.market.MedicinePriceHistory;
import com.medilink.model.medicine.Medicine;
import com.medilink.model.pharmacy.PharmacyStock;
import com.medilink.repository.MedicinePriceHistoryRepository;
import com.medilink.repository.MedicineRepository;
import com.medilink.repository.PharmacyStockRepository;
import com.medilink.service.StockObserverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Enterprise Service for automatic Bangladesh medicine market price synchronization.
 * Synchronizes PostgreSQL medicine records and pharmacy stocks against the active market provider,
 * computes high-fidelity audit statistics, enforces runaway price protection, and broadcasts SSE updates.
 */
@Service
public class MarketPriceSyncService {

    private static final Logger log = LoggerFactory.getLogger(MarketPriceSyncService.class);
    private static final int PRICE_SCALE = 2;
    private static final RoundingMode PRICE_ROUNDING = RoundingMode.HALF_UP;

    private final MedicineRepository medicineRepository;
    private final PharmacyStockRepository pharmacyStockRepository;
    private final MedicinePriceHistoryRepository priceHistoryRepository;
    private final MarketPriceProvider marketPriceProvider;
    private final MedicinePriceMatchingService priceMatchingService;
    private final MarketApiProperties properties;

    private final AtomicBoolean isSyncRunning = new AtomicBoolean(false);

    private LocalDateTime lastSyncTime = LocalDateTime.now();
    private String lastSyncStatus = "HEALTHY";
    private String lastSyncError = null;
    private int lastUpdatedCount = 0;
    private int lastCheckedCount = 0;

    private MarketSyncStatistics lastSyncStatistics = new MarketSyncStatistics(
            0, 0, 0, 0, 0, 0, 0, 0, "0.0%", "HEALTHY", null, LocalDateTime.now()
    );

    @Autowired
    public MarketPriceSyncService(MedicineRepository medicineRepository,
                                  PharmacyStockRepository pharmacyStockRepository,
                                  MedicinePriceHistoryRepository priceHistoryRepository,
                                  MarketPriceProvider marketPriceProvider,
                                  MedicinePriceMatchingService priceMatchingService,
                                  MarketApiProperties properties) {
        this.medicineRepository = medicineRepository;
        this.pharmacyStockRepository = pharmacyStockRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.marketPriceProvider = marketPriceProvider;
        this.priceMatchingService = priceMatchingService;
        this.properties = properties != null ? properties : new MarketApiProperties();
    }

    /**
     * Backward-compatible constructor for testing and manual wiring.
     */
    public MarketPriceSyncService(MedicineRepository medicineRepository,
                                  PharmacyStockRepository pharmacyStockRepository,
                                  MedicinePriceHistoryRepository priceHistoryRepository,
                                  MarketPriceProvider marketPriceProvider,
                                  MedicinePriceMatchingService priceMatchingService) {
        this(medicineRepository, pharmacyStockRepository, priceHistoryRepository, marketPriceProvider, priceMatchingService, new MarketApiProperties());
    }

    public MarketPriceSyncService(MedicineRepository medicineRepository,
                                  PharmacyStockRepository pharmacyStockRepository,
                                  MedicinePriceHistoryRepository priceHistoryRepository,
                                  MarketPriceProvider marketPriceProvider) {
        this(medicineRepository, pharmacyStockRepository, priceHistoryRepository, marketPriceProvider, new MedicinePriceMatchingService(), new MarketApiProperties());
    }

    /**
     * Automatic background polling scheduled every 5 minutes by default (300,000 ms),
     * protected by an AtomicBoolean lock to prevent overlapping runs.
     */
    @Scheduled(fixedDelayString = "${medilink.market.sync-delay-ms:300000}", initialDelayString = "${medilink.market.initial-delay-ms:15000}")
    public void scheduledMarketSync() {
        if (!isSyncRunning.compareAndSet(false, true)) {
            log.warn("[MarketPriceSyncService] Previous synchronization is still running. Skipping overlapping execution.");
            return;
        }

        try {
            log.info("[MarketPriceSyncService] Starting scheduled external market synchronization...");
            SyncResult result = syncAllMedicines("Scheduled Auto-Sync (Cron)");
            log.info("[MarketPriceSyncService] Scheduled sync completed: {} price(s) updated, {} matched out of {} local medicines.",
                    result.getUpdatedCount(), result.getStatistics().getMatchedMedicines(), result.getCheckedCount());
        } finally {
            isSyncRunning.set(false);
        }
    }

    /**
     * Synchronizes all local PostgreSQL medicines against external market data.
     * Computes explicit synchronization metrics and guarantees database safety.
     */
    @Transactional
    public SyncResult syncAllMedicines(String triggerSource) {
        List<Medicine> allLocalMeds = medicineRepository.findAll();
        int localConsidered = allLocalMeds.size();

        log.info("[MarketPriceSyncService] Synchronizing {} local medicines against active provider ({})",
                localConsidered, marketPriceProvider.getProviderName());

        List<MarketPriceItem> externalQuotes = null;
        int failedExternalRequests = 0;
        String syncError = null;

        try {
            externalQuotes = marketPriceProvider.fetchLatestMarketPrices();
        } catch (Exception e) {
            log.error("[MarketPriceSyncService] Failed to retrieve external market quotes: {}", e.getMessage());
            failedExternalRequests++;
            syncError = e.getMessage();
        }

        int externalRetrieved = (externalQuotes != null) ? externalQuotes.size() : 0;
        log.info("[MarketPriceSyncService] Retrieved {} external market quotes for synchronization.", externalRetrieved);

        List<MedicinePriceHistory> changes = new ArrayList<>();
        Set<String> processedMedicineIds = new HashSet<>();

        int matchedCount = 0;
        int updatedCount = 0;
        int unchangedCount = 0;
        int unmatchedCount = 0;
        int invalidCount = 0;

        for (Medicine med : allLocalMeds) {
            if (med == null || med.getId() == null) continue;

            // Prevent duplicate updates if multiple external records represent the same medicine
            if (processedMedicineIds.contains(med.getId())) {
                continue;
            }

            Optional<MarketPriceItem> quoteOpt = Optional.empty();

            if (externalQuotes != null && !externalQuotes.isEmpty()) {
                // Find safe match in external quotes list
                quoteOpt = findMatchingQuoteForMedicine(med, externalQuotes);
            }

            // Fallback to fetchPriceByBrand if direct quote was not in batch list and external call did not fail
            if (!quoteOpt.isPresent() && failedExternalRequests == 0) {
                try {
                    quoteOpt = marketPriceProvider.fetchPriceByBrand(med.getBrandName());
                } catch (Exception ignored) {}
            }

            if (!quoteOpt.isPresent()) {
                unmatchedCount++;
                // Local medicine price remains intact
                continue;
            }

            MarketPriceItem quote = quoteOpt.get();
            processedMedicineIds.add(med.getId());
            matchedCount++;

            BigDecimal marketPrice = BigDecimal.valueOf(quote.getMrp()).setScale(PRICE_SCALE, PRICE_ROUNDING);
            BigDecimal currentPrice = BigDecimal.valueOf(med.getUnitPrice()).setScale(PRICE_SCALE, PRICE_ROUNDING);

            // Reject negative, zero, or non-finite invalid quotes
            if (marketPrice.compareTo(BigDecimal.ZERO) <= 0
                    || Double.isInfinite(marketPrice.doubleValue())
                    || Double.isNaN(marketPrice.doubleValue())) {
                log.warn("[MarketPriceSyncService] Rejected invalid market price {} for '{}'. Skipping.", marketPrice, med.getBrandName());
                invalidCount++;
                continue;
            }

            // Check against excessive runaway price changes if configured
            double maxAllowedPct = properties.getMaxPriceChangePercent();
            if (maxAllowedPct > 0 && currentPrice.compareTo(BigDecimal.ZERO) > 0) {
                double pct = Math.abs((marketPrice.doubleValue() - currentPrice.doubleValue()) / currentPrice.doubleValue()) * 100.0;
                if (pct > maxAllowedPct) {
                    log.warn("[MarketPriceSyncService] Rejected excessive price fluctuation for '{}': {:.1f}% exceeds max threshold of {:.1f}%. Preserving price ৳{:.2f}.",
                            med.getBrandName(), pct, maxAllowedPct, currentPrice.doubleValue());
                    invalidCount++;
                    continue;
                }
            }

            // Compare prices (at least ৳0.01 change)
            if (currentPrice.compareTo(marketPrice) != 0) {
                double oldVal = currentPrice.doubleValue();
                double newVal = marketPrice.doubleValue();

                med.setUnitPrice(newVal);
                medicineRepository.save(med);

                // Update corresponding pharmacy stocks in PostgreSQL
                List<PharmacyStock> stocks = pharmacyStockRepository.findByMedicineId(med.getId());
                for (PharmacyStock stock : stocks) {
                    stock.setUnitPrice(newVal);
                    stock.setLastUpdated(LocalDateTime.now());
                    pharmacyStockRepository.save(stock);
                }

                // Create auditable Price History record
                String histId = "prc_hist_" + UUID.randomUUID().toString().substring(0, 8);
                MedicinePriceHistory history = new MedicinePriceHistory(
                        histId,
                        med.getId(),
                        med.getBrandName(),
                        med.getGenericName(),
                        med.getCompany(),
                        oldVal,
                        newVal,
                        triggerSource != null ? triggerSource : quote.getSourceRegistry()
                );
                priceHistoryRepository.save(history);
                changes.add(history);
                updatedCount++;

                // Broadcast real-time SSE event to all connected clients
                broadcastPriceUpdate(med, oldVal, newVal, history.getDirection(), history.getPercentageChange());
            } else {
                unchangedCount++;
            }
        }

        // Calculate sync coverage
        double coveragePct = localConsidered > 0 ? (matchedCount * 100.0) / localConsidered : 0.0;
        String coverageStr = String.format(Locale.US, "%.1f%%", coveragePct);

        String computedStatus = "COMPLETED";
        if (failedExternalRequests > 0) {
            boolean isTimeout = syncError != null && (syncError.toLowerCase().contains("timeout") || syncError.toLowerCase().contains("timed out"));
            computedStatus = isTimeout ? "TIMED_OUT" : "DEGRADED";
        } else if (unmatchedCount > 0) {
            computedStatus = "PARTIAL_SUCCESS";
        }

        this.lastSyncTime = LocalDateTime.now();
        this.lastSyncStatus = computedStatus;
        this.lastSyncError = syncError;
        this.lastUpdatedCount = updatedCount;
        this.lastCheckedCount = localConsidered;

        this.lastSyncStatistics = new MarketSyncStatistics(
                localConsidered,
                externalRetrieved,
                matchedCount,
                updatedCount,
                unchangedCount,
                unmatchedCount,
                invalidCount,
                failedExternalRequests,
                coverageStr,
                computedStatus,
                syncError,
                this.lastSyncTime
        );

        log.info("[MarketPriceSyncService] Sync summary: Considered={}, External={}, Matched={}, Updated={}, Unchanged={}, Unmatched={}, Coverage={}",
                localConsidered, externalRetrieved, matchedCount, updatedCount, unchangedCount, unmatchedCount, coverageStr);

        return new SyncResult(localConsidered, updatedCount, changes, this.lastSyncTime, this.lastSyncStatistics);
    }

    /**
     * Matches a local medicine against a list of external market quotes using the 5-tier strategy.
     */
    private Optional<MarketPriceItem> findMatchingQuoteForMedicine(Medicine med, List<MarketPriceItem> quotes) {
        String localBrandNorm = priceMatchingService.normalize(med.getBrandName());
        String localStrengthNorm = med.getStrength() != null ? priceMatchingService.normalize(med.getStrength()) : "";
        String localFormNorm = med.getFormulation() != null ? priceMatchingService.normalize(med.getFormulation()) : "";

        // Tier 1: Exact brand match
        List<MarketPriceItem> exactBrandMatches = new ArrayList<>();
        for (MarketPriceItem q : quotes) {
            if (priceMatchingService.normalize(q.getBrandName()).equals(localBrandNorm)) {
                exactBrandMatches.add(q);
            }
        }

        if (exactBrandMatches.size() == 1) {
            return Optional.of(exactBrandMatches.get(0));
        } else if (exactBrandMatches.size() > 1) {
            // Disambiguate by strength if present
            if (!localStrengthNorm.isEmpty()) {
                for (MarketPriceItem q : exactBrandMatches) {
                    if (q.getStrength() != null && priceMatchingService.normalize(q.getStrength()).equals(localStrengthNorm)) {
                        return Optional.of(q);
                    }
                }
            }
            // Disambiguate by dosage form if present
            if (!localFormNorm.isEmpty()) {
                for (MarketPriceItem q : exactBrandMatches) {
                    if (q.getDosageForm() != null && priceMatchingService.normalize(q.getDosageForm()).equals(localFormNorm)) {
                        return Optional.of(q);
                    }
                }
            }
            return Optional.of(exactBrandMatches.get(0));
        }

        // Tier 2: Check canonical alias
        for (MarketPriceItem q : quotes) {
            Optional<Medicine> match = priceMatchingService.findSafeMedicineMatch(
                    q.getBrandName(), q.getStrength(), q.getDosageForm(), q.getGenericName(), q.getManufacturer(), Collections.singletonList(med)
            );
            if (match.isPresent() && match.get().getId().equals(med.getId())) {
                return Optional.of(q);
            }
        }

        return Optional.empty();
    }

    /**
     * Direct update from Webhook or manual Admin trigger.
     */
    @Transactional
    public boolean updatePriceDirectly(String brandName, double newPrice, String source) {
        if (brandName == null || newPrice <= 0 || Double.isInfinite(newPrice) || Double.isNaN(newPrice)) {
            return false;
        }

        BigDecimal normalizedNewPrice = BigDecimal.valueOf(newPrice).setScale(PRICE_SCALE, PRICE_ROUNDING);

        // Update in-memory / composite registry if applicable
        if (marketPriceProvider instanceof CompositeMarketPriceProvider) {
            ((CompositeMarketPriceProvider) marketPriceProvider).updateRegistryPrice(brandName, normalizedNewPrice.doubleValue());
        } else if (marketPriceProvider instanceof BangladeshDgdaMedexProvider) {
            ((BangladeshDgdaMedexProvider) marketPriceProvider).updateRegistryPrice(brandName, normalizedNewPrice.doubleValue());
        }

        List<Medicine> allMeds = medicineRepository.findAll();
        Optional<Medicine> medOpt = priceMatchingService.findSafeMedicineMatch(brandName, allMeds);

        if (medOpt.isPresent()) {
            Medicine med = medOpt.get();
            BigDecimal currentPrice = BigDecimal.valueOf(med.getUnitPrice()).setScale(PRICE_SCALE, PRICE_ROUNDING);

            // Avoid redundant audit record if price is identical
            if (currentPrice.compareTo(normalizedNewPrice) == 0) {
                return true;
            }

            double oldVal = currentPrice.doubleValue();
            double newVal = normalizedNewPrice.doubleValue();

            med.setUnitPrice(newVal);
            medicineRepository.save(med);

            List<PharmacyStock> stocks = pharmacyStockRepository.findByMedicineId(med.getId());
            for (PharmacyStock s : stocks) {
                s.setUnitPrice(newVal);
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
                    oldVal,
                    newVal,
                    source != null ? source : "Live Webhook Push"
            );
            priceHistoryRepository.save(history);

            broadcastPriceUpdate(med, oldVal, newVal, history.getDirection(), history.getPercentageChange());
            return true;
        }

        return false;
    }

    /**
     * Simulates a realistic market price adjustment for interactive demonstrations.
     */
    @Transactional
    public MedicinePriceHistory simulateDgdaFluctuation(String medicineId, Double targetPrice) {
        Medicine targetMed = null;
        if (medicineId != null && !medicineId.trim().isEmpty()) {
            targetMed = medicineRepository.findById(medicineId.trim()).orElse(null);
        }
        if (targetMed == null) {
            List<Medicine> meds = medicineRepository.findAll();
            if (meds.isEmpty()) {
                return null;
            }
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
            if (oldPrice <= 3.00) {
                newPrice = (oldPrice == 3.50) ? 3.00 : 3.50;
            } else if (oldPrice <= 10.00) {
                newPrice = (oldPrice >= 6.50) ? 6.00 : 6.80;
            } else {
                newPrice = Math.round((oldPrice * 1.10) * 100.0) / 100.0;
            }
        }

        updatePriceDirectly(targetMed.getBrandName(), newPrice, "SIMULATION");
        return priceHistoryRepository.findTop20ByOrderByTimestampDesc().stream().findFirst().orElse(null);
    }

    private void broadcastPriceUpdate(Medicine med, double oldPrice, double newPrice, String direction, double pct) {
        String sign = pct >= 0 ? "+" : "";
        String payload = String.format(
                "{\"type\":\"PRICE_UPDATE\",\"medicineId\":\"%s\",\"brandName\":\"%s\",\"genericName\":\"%s\",\"oldPrice\":%.2f,\"newPrice\":%.2f,\"direction\":\"%s\",\"changePercent\":\"%s%.1f%%\",\"timestamp\":\"%s\"}",
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

        StockObserverService.getInstance().onNotification("PRICE_UPDATE", payload);
        log.info("[MarketPriceSyncService] Real-time Price Update Broadcasted: {} ৳{:.2f} -> ৳{:.2f} ({}{:.1f}%)",
                med.getBrandName(), oldPrice, newPrice, sign, pct);
    }

    public List<MedicinePriceHistory> getRecentPriceHistories() {
        return priceHistoryRepository.findTop20ByOrderByTimestampDesc();
    }

    public List<MedicinePriceHistory> getAllPriceHistories() {
        return priceHistoryRepository.findAllByOrderByTimestampDesc();
    }

    public Medicine findMedicineByBrand(String brandName) {
        if (brandName == null || brandName.trim().isEmpty()) {
            return null;
        }
        return priceMatchingService.findSafeMedicineMatch(brandName, medicineRepository.findAll()).orElse(null);
    }

    public Medicine findDefaultMedicine() {
        List<Medicine> meds = medicineRepository.findAll();
        if (meds.isEmpty()) {
            return null;
        }
        return meds.stream()
                .filter(m -> m.getBrandName().toLowerCase().contains("napa"))
                .findFirst()
                .orElse(meds.get(0));
    }

    public MarketPriceProvider getMarketPriceProvider() {
        return marketPriceProvider;
    }

    public LocalDateTime getLastSyncTime() { return lastSyncTime; }
    public String getLastSyncStatus() { return lastSyncStatus; }
    public String getLastSyncError() { return lastSyncError; }
    public int getLastUpdatedCount() { return lastUpdatedCount; }
    public int getLastCheckedCount() { return lastCheckedCount; }
    public MarketSyncStatistics getLastSyncStatistics() { return lastSyncStatistics; }

    /**
     * Detailed audit and synchronization statistics for observability.
     */
    public static class MarketSyncStatistics {
        private final int localMedicinesConsidered;
        private final int externalRecordsRetrieved;
        private final int matchedMedicines;
        private final int updatedMedicines;
        private final int unchangedMedicines;
        private final int unmatchedMedicines;
        private final int invalidPriceRecords;
        private final int failedExternalRequests;
        private final String syncCoveragePercent;
        private final String status;
        private final String errorSummary;
        private final LocalDateTime timestamp;

        public MarketSyncStatistics(int localMedicinesConsidered, int externalRecordsRetrieved,
                                    int matchedMedicines, int updatedMedicines, int unchangedMedicines,
                                    int unmatchedMedicines, int invalidPriceRecords, int failedExternalRequests,
                                    String syncCoveragePercent, String status, String errorSummary,
                                    LocalDateTime timestamp) {
            this.localMedicinesConsidered = localMedicinesConsidered;
            this.externalRecordsRetrieved = externalRecordsRetrieved;
            this.matchedMedicines = matchedMedicines;
            this.updatedMedicines = updatedMedicines;
            this.unchangedMedicines = unchangedMedicines;
            this.unmatchedMedicines = unmatchedMedicines;
            this.invalidPriceRecords = invalidPriceRecords;
            this.failedExternalRequests = failedExternalRequests;
            this.syncCoveragePercent = syncCoveragePercent;
            this.status = status;
            this.errorSummary = errorSummary;
            this.timestamp = timestamp;
        }

        public int getLocalMedicinesConsidered() { return localMedicinesConsidered; }
        public int getExternalRecordsRetrieved() { return externalRecordsRetrieved; }
        public int getMatchedMedicines() { return matchedMedicines; }
        public int getUpdatedMedicines() { return updatedMedicines; }
        public int getUnchangedMedicines() { return unchangedMedicines; }
        public int getUnmatchedMedicines() { return unmatchedMedicines; }
        public int getInvalidPriceRecords() { return invalidPriceRecords; }
        public int getFailedExternalRequests() { return failedExternalRequests; }
        public String getSyncCoveragePercent() { return syncCoveragePercent; }
        public String getStatus() { return status; }
        public String getErrorSummary() { return errorSummary; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    public static class SyncResult {
        private final int checkedCount;
        private final int updatedCount;
        private final List<MedicinePriceHistory> changes;
        private final LocalDateTime timestamp;
        private final MarketSyncStatistics statistics;

        public SyncResult(int checkedCount, int updatedCount, List<MedicinePriceHistory> changes, LocalDateTime timestamp, MarketSyncStatistics statistics) {
            this.checkedCount = checkedCount;
            this.updatedCount = updatedCount;
            this.changes = changes;
            this.timestamp = timestamp;
            this.statistics = statistics;
        }

        public SyncResult(int checkedCount, int updatedCount, List<MedicinePriceHistory> changes, LocalDateTime timestamp) {
            this(checkedCount, updatedCount, changes, timestamp, new MarketSyncStatistics(
                    checkedCount, 0, checkedCount - updatedCount, updatedCount, checkedCount - updatedCount, 0, 0, 0, "100.0%", "COMPLETED", null, timestamp
            ));
        }

        public int getCheckedCount() { return checkedCount; }
        public int getUpdatedCount() { return updatedCount; }
        public List<MedicinePriceHistory> getChanges() { return changes; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public MarketSyncStatistics getStatistics() { return statistics; }
    }
}
