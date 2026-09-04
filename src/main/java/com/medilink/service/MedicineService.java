package com.medilink.service;

import com.medilink.model.medicine.Medicine;
import com.medilink.model.pharmacy.Pharmacy;
import com.medilink.model.pharmacy.PharmacyStock;
import com.medilink.model.strategy.*;
import com.medilink.model.strategy.MedicineVerificationStrategy.VerificationResult;
import com.medilink.repository.MedicineRepository;
import com.medilink.repository.PharmacyRepository;
import com.medilink.repository.PharmacyStockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class MedicineService {

    private final MedicineRepository medicineRepository;
    private final PharmacyStockRepository pharmacyStockRepository;
    private final PharmacyRepository pharmacyRepository;

    private final Map<String, MedicineSearchStrategy> searchStrategies = new HashMap<>();

    @Autowired
    public MedicineService(MedicineRepository medicineRepository,
                           PharmacyStockRepository pharmacyStockRepository,
                           PharmacyRepository pharmacyRepository) {
        this.medicineRepository = medicineRepository;
        this.pharmacyStockRepository = pharmacyStockRepository;
        this.pharmacyRepository = pharmacyRepository;

        // Initialize Strategy Pattern search implementations
        searchStrategies.put("BRAND", new BrandSearchStrategy());
        searchStrategies.put("GENERIC", new GenericSearchStrategy());
        searchStrategies.put("PRICE", new PriceSearchStrategy());
    }

    public List<Medicine> findAll() {
        return medicineRepository.findAll();
    }

    public Optional<Medicine> findById(String id) {
        return medicineRepository.findById(id);
    }

    @Transactional
    public Medicine save(Medicine medicine) {
        return medicineRepository.save(medicine);
    }

    public List<Medicine> search(String query, String strategyName) {
        List<Medicine> all = medicineRepository.findAll();
        String stratKey = strategyName != null ? strategyName.trim().toUpperCase() : "BRAND";
        MedicineSearchStrategy strategy = searchStrategies.getOrDefault(stratKey, searchStrategies.get("BRAND"));
        return strategy.search(all, query);
    }

    public List<Medicine> findAlternatives(String medicineId) {
        Optional<Medicine> medOpt = medicineRepository.findById(medicineId);
        if (!medOpt.isPresent()) return Collections.emptyList();

        Medicine target = medOpt.get();
        List<Medicine> candidates = medicineRepository.findByGenericNameIgnoreCase(target.getGenericName());
        List<Medicine> alternatives = new ArrayList<>();
        for (Medicine m : candidates) {
            if (!m.getId().equalsIgnoreCase(target.getId())) {
                alternatives.add(m);
            }
        }
        return alternatives;
    }

    public List<Map<String, Object>> getPharmacyPrices(String medicineId, Double userLat, Double userLng) {
        Optional<Medicine> medOpt = medicineRepository.findById(medicineId);
        String targetMedId = medOpt.isPresent() ? medOpt.get().getId() : medicineId;
        String targetBrand = medOpt.isPresent() ? medOpt.get().getBrandName() : "";

        List<PharmacyStock> stocks = pharmacyStockRepository.searchStocksByMedicine(targetMedId);
        if (stocks.isEmpty() && !targetBrand.isEmpty()) {
            stocks = pharmacyStockRepository.searchStocksByMedicine(targetBrand);
        }

        stocks.sort(Comparator.comparingDouble(PharmacyStock::getUnitPrice));

        List<Map<String, Object>> results = new ArrayList<>();
        for (PharmacyStock s : stocks) {
            Map<String, Object> map = new HashMap<>();
            map.put("pharmacyId", s.getPharmacyId());
            map.put("pharmacyName", s.getPharmacyName());
            map.put("unitPrice", s.getUnitPrice());
            map.put("quantity", s.getQuantity());

            double distance = 0.0;
            Optional<Pharmacy> pOpt = pharmacyRepository.findById(s.getPharmacyId());
            if (pOpt.isPresent()) {
                Pharmacy p = pOpt.get();
                map.put("address", p.getAddress());
                map.put("phone", p.getPhone());
                map.put("is24Hours", p.is24Hours());
                if (userLat != null && userLng != null) {
                    distance = p.calculateDistanceKm(userLat, userLng);
                }
            }
            map.put("distanceKm", Math.round(distance * 10.0) / 10.0);
            results.add(map);
        }
        return results;
    }

    public Map<String, Object> checkInteractions(List<String> genericNames, String strategyLevel) {
        InteractionCheckStrategy strategy;
        if ("STRICT".equalsIgnoreCase(strategyLevel)) {
            strategy = new ClinicalStrictStrategy();
        } else {
            strategy = new StandardWarningStrategy();
        }

        String warning = strategy.evaluateRisk(genericNames);
        Map<String, Object> response = new HashMap<>();
        response.put("strategy", strategy.getStrategyLevel());
        response.put("analysis", warning);
        response.put("hasHighRisk", warning != null && warning.toLowerCase().contains("severe"));
        return response;
    }

    public VerificationResult verifyBatch(String medicineId, String batchCode) {
        if (batchCode == null || batchCode.trim().isEmpty()) {
            return new VerificationResult(false, "INVALID_CODE", "Please enter or scan a valid batch / QR code.", "Unknown");
        }

        String cleanCode = batchCode.trim().toUpperCase();
        if (medicineId != null && !medicineId.trim().isEmpty()) {
            Optional<Medicine> mOpt = medicineRepository.findById(medicineId.trim());
            if (mOpt.isPresent()) {
                Medicine m = mOpt.get();
                if (m.isBatchValid(cleanCode)) {
                    return new VerificationResult(
                        true,
                        "AUTHENTIC_GENUINE",
                        "Official DGDA regulatory compliance verified. Batch " + cleanCode + " belongs to genuine production line of " + m.getCompany() + ".",
                        m.getCompany()
                    );
                } else {
                    return new VerificationResult(
                        false,
                        "SUSPICIOUS_UNVERIFIED",
                        "WARNING: Batch code " + cleanCode + " was not recognized in official manufacturer records for " + m.getBrandName() + ". Potential counterfeit risk!",
                        m.getCompany()
                    );
                }
            }
        }

        // Global search across all medicines
        List<Medicine> all = medicineRepository.findAll();
        for (Medicine m : all) {
            if (m.isBatchValid(cleanCode)) {
                return new VerificationResult(
                    true,
                    "AUTHENTIC_GENUINE",
                    "Verified genuine batch for " + m.getBrandName() + " (" + m.getGenericName() + "). Manufactured by " + m.getCompany() + ".",
                    m.getCompany()
                );
            }
        }

        return new VerificationResult(
            false,
            "SUSPECT_OR_FAKE",
            "CRITICAL: Scanned code " + cleanCode + " has NO matching manufacturer record in the Directorate General of Drug Administration (DGDA) database.",
            "Unidentified / Unauthorized Source"
        );
    }
}
