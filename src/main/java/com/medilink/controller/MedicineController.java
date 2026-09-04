package com.medilink.controller;

import com.medilink.model.medicine.LowStockBadgeDecorator;
import com.medilink.model.medicine.Medicine;
import com.medilink.model.medicine.VerifiedBadgeDecorator;
import com.medilink.model.strategy.MedicineVerificationStrategy.VerificationResult;
import com.medilink.service.MedicineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/medicines")
@CrossOrigin(origins = "*")
public class MedicineController {

    private final MedicineService medicineService;

    @Autowired
    public MedicineController(MedicineService medicineService) {
        this.medicineService = medicineService;
    }

    @GetMapping(value = {"", "/search"})
    public ResponseEntity<Map<String, Object>> searchMedicines(
            @RequestParam(name = "query", defaultValue = "") String query,
            @RequestParam(name = "strategy", defaultValue = "BRAND_SEARCH") String strategyName) {

        String stratKey = "BRAND_SEARCH";
        if ("GENERIC_SEARCH".equalsIgnoreCase(strategyName)) {
            stratKey = "GENERIC";
        } else if ("BEST_PRICE_STRATEGY".equalsIgnoreCase(strategyName)) {
            stratKey = "PRICE";
        } else {
            stratKey = "BRAND";
        }

        List<Medicine> filtered = medicineService.search(query, stratKey);

        List<Map<String, Object>> results = new ArrayList<>();
        for (Medicine m : filtered) {
            // Apply Decorator Design Pattern
            Medicine decorated = m;
            if (m.getBrandName().startsWith("Napa") || m.getBrandName().startsWith("Ace") || m.getBrandName().startsWith("Seclo")) {
                decorated = new VerifiedBadgeDecorator(decorated, "DGDA-BD-VERIFIED");
            }
            if (m.getUnitPrice() < 5.0) {
                decorated = new LowStockBadgeDecorator(decorated, 8);
            }

            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("brandName", m.getBrandName());
            map.put("genericName", m.getGenericName());
            map.put("company", m.getCompany());
            map.put("strength", m.getStrength());
            map.put("formulation", m.getFormulation());
            map.put("unitPrice", m.getUnitPrice());
            map.put("category", m.getCategory());
            map.put("sideEffects", m.getSideEffects());
            map.put("displayBadge", decorated.getDisplayBadge());
            results.add(map);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("strategy", strategyName);
        response.put("results", results);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/alternatives")
    public ResponseEntity<Map<String, Object>> getAlternatives(
            @RequestParam(name = "generic", defaultValue = "") String generic) {

        List<Medicine> all = medicineService.findAll();
        List<Medicine> matching = new ArrayList<>();
        for (Medicine m : all) {
            if (generic != null && !generic.isEmpty() && m.getGenericName().toLowerCase().contains(generic.toLowerCase())) {
                matching.add(m);
            }
        }
        matching.sort(Comparator.comparingDouble(Medicine::getUnitPrice));

        List<Map<String, Object>> alts = new ArrayList<>();
        for (Medicine m : matching) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("brandName", m.getBrandName());
            map.put("company", m.getCompany());
            map.put("strength", m.getStrength());
            map.put("price", m.getUnitPrice());
            alts.add(map);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("generic", generic);
        response.put("alternatives", alts);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pharmacy-prices")
    public ResponseEntity<Map<String, Object>> getPharmacyPrices(
            @RequestParam(name = "medicineId", required = false) String medicineId,
            @RequestParam(name = "medicine", required = false) String medicine,
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "lat", required = false) Double lat,
            @RequestParam(name = "lng", required = false) Double lng) {

        String target = medicineId;
        if (target == null || target.isEmpty()) target = medicine;
        if (target == null || target.isEmpty()) target = query;
        if (target == null) target = "";

        Optional<Medicine> medOpt = medicineService.findById(target);
        if (!medOpt.isPresent()) {
            List<Medicine> all = medicineService.findAll();
            for (Medicine m : all) {
                if (m.getBrandName().equalsIgnoreCase(target) || m.getBrandName().toLowerCase().contains(target.toLowerCase())) {
                    medOpt = Optional.of(m);
                    break;
                }
            }
        }

        Medicine med = medOpt.orElse(null);
        String resolvedId = med != null ? med.getId() : target;
        List<Map<String, Object>> pharmacyPrices = medicineService.getPharmacyPrices(resolvedId, lat, lng);

        double minPrice = pharmacyPrices.isEmpty() ? (med != null ? med.getUnitPrice() : 0.0)
                : ((Number) pharmacyPrices.get(0).get("unitPrice")).doubleValue();
        double maxPrice = pharmacyPrices.isEmpty() ? minPrice
                : ((Number) pharmacyPrices.get(pharmacyPrices.size() - 1).get("unitPrice")).doubleValue();
        double savingsPct = maxPrice > 0 ? ((maxPrice - minPrice) / maxPrice) * 100.0 : 0.0;

        List<Map<String, Object>> formattedPrices = new ArrayList<>();
        for (int i = 0; i < pharmacyPrices.size(); i++) {
            Map<String, Object> p = new HashMap<>(pharmacyPrices.get(i));
            double uPrice = ((Number) p.get("unitPrice")).doubleValue();
            int qty = ((Number) p.get("quantity")).intValue();
            p.put("inStock", qty > 0);
            p.put("isBestPrice", i == 0 || uPrice <= minPrice);
            p.put("stockId", "stk_" + p.get("pharmacyId"));
            formattedPrices.add(p);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("medicineId", med != null ? med.getId() : target);
        response.put("brandName", med != null ? med.getBrandName() : target);
        response.put("genericName", med != null ? med.getGenericName() : "");
        response.put("strength", med != null ? med.getStrength() : "");
        response.put("company", med != null ? med.getCompany() : "");
        response.put("basePrice", med != null ? med.getUnitPrice() : minPrice);
        response.put("bestPrice", minPrice);
        response.put("maxPrice", maxPrice);
        response.put("savingsPercent", String.format(Locale.US, "%.1f", savingsPct));
        response.put("pharmacyPrices", formattedPrices);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/interaction-check")
    public ResponseEntity<Map<String, Object>> checkInteractions(@RequestBody Map<String, String> body) {
        String medsStr = body.getOrDefault("medicines", "");
        String mode = body.getOrDefault("mode", "STANDARD");

        List<String> list = Arrays.asList(medsStr.split(","));
        Map<String, Object> result = medicineService.checkInteractions(list, mode);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("mode", result.get("strategy"));
        response.put("analysis", result.get("analysis"));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyMedicine(@RequestBody Map<String, String> body) {
        String medId = body.getOrDefault("medicineId", "");
        String code = body.getOrDefault("code", "");

        VerificationResult res = medicineService.verifyBatch(medId, code);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("isAuthentic", res.isAuthentic());
        response.put("verificationStatus", res.getStatus());
        response.put("details", res.getDetails());
        response.put("manufacturer", res.getManufacturer());
        return ResponseEntity.ok(response);
    }
}
