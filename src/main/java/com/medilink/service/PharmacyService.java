package com.medilink.service;

import com.medilink.model.pharmacy.Pharmacy;
import com.medilink.model.pharmacy.PharmacyStock;
import com.medilink.repository.PharmacyRepository;
import com.medilink.repository.PharmacyStockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class PharmacyService {

    private final PharmacyRepository pharmacyRepository;
    private final PharmacyStockRepository pharmacyStockRepository;
    private final StockObserverService stockObserverService;

    @Autowired
    public PharmacyService(PharmacyRepository pharmacyRepository,
                           PharmacyStockRepository pharmacyStockRepository) {
        this.pharmacyRepository = pharmacyRepository;
        this.pharmacyStockRepository = pharmacyStockRepository;
        this.stockObserverService = StockObserverService.getInstance();
    }

    public List<Pharmacy> findAllPharmacies() {
        return pharmacyRepository.findAll();
    }

    public Optional<Pharmacy> findPharmacyById(String id) {
        return pharmacyRepository.findById(id);
    }

    public Pharmacy savePharmacy(Pharmacy pharmacy) {
        return pharmacyRepository.save(pharmacy);
    }

    public List<Map<String, Object>> findEmergencyPharmacies(Double userLat, Double userLng) {
        List<Pharmacy> list = pharmacyRepository.findEmergencyPharmacies();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Pharmacy p : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("name", p.getName());
            map.put("address", p.getAddress());
            map.put("area", p.getArea());
            map.put("phone", p.getPhone());
            map.put("is24Hours", p.is24Hours());
            map.put("hasEmergencyDelivery", p.hasEmergencyDelivery());

            double dist = (userLat != null && userLng != null) ? p.calculateDistanceKm(userLat, userLng) : 0.0;
            map.put("distanceKm", Math.round(dist * 10.0) / 10.0);
            result.add(map);
        }

        if (userLat != null && userLng != null) {
            result.sort(Comparator.comparingDouble(m -> (Double) m.get("distanceKm")));
        }
        return result;
    }

    public List<PharmacyStock> findAllStocks() {
        return pharmacyStockRepository.findAll();
    }

    public Optional<PharmacyStock> findStockById(String id) {
        return pharmacyStockRepository.findById(id);
    }

    public List<PharmacyStock> findStocksByMedicine(String medicineId) {
        return pharmacyStockRepository.findByMedicineId(medicineId);
    }

    public List<PharmacyStock> findStocksByPharmacy(String pharmacyId) {
        return pharmacyStockRepository.findByPharmacyId(pharmacyId);
    }

    public PharmacyStock saveStock(PharmacyStock stock) {
        return pharmacyStockRepository.save(stock);
    }

    public Optional<PharmacyStock> updateStockQuantity(String stockId, int newQuantity) {
        Optional<PharmacyStock> stockOpt = pharmacyStockRepository.findById(stockId);
        if (!stockOpt.isPresent()) return Optional.empty();

        PharmacyStock stock = stockOpt.get();
        int oldQuantity = stock.getQuantity();
        stock.updateQuantity(newQuantity);
        PharmacyStock saved = pharmacyStockRepository.save(stock);

        // Observer pattern dispatch to SSE and listeners
        String summary = "Stock updated for " + saved.getMedicineBrandName() + " at " +
                         saved.getPharmacyName() + ": " + oldQuantity + " -> " + newQuantity + " units.";
        stockObserverService.onNotification("STOCK_UPDATE", summary);

        return Optional.of(saved);
    }
}
