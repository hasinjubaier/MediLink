package com.medilink.controller;

import com.medilink.model.pharmacy.Pharmacy;
import com.medilink.model.pharmacy.PharmacyStock;
import com.medilink.service.PharmacyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/pharmacies")
@CrossOrigin(origins = "*")
public class PharmacyController {

    private final PharmacyService pharmacyService;

    @Autowired
    public PharmacyController(PharmacyService pharmacyService) {
        this.pharmacyService = pharmacyService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllPharmacies() {
        List<Pharmacy> list = pharmacyService.findAllPharmacies();
        List<Map<String, Object>> pharmacies = new ArrayList<>();
        for (Pharmacy p : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("name", p.getName());
            map.put("address", p.getAddress());
            map.put("area", p.getArea());
            map.put("phone", p.getPhone());
            map.put("is24Hours", p.is24Hours());
            pharmacies.add(map);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("pharmacies", pharmacies);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/emergency")
    public ResponseEntity<Map<String, Object>> getEmergencyPharmacies(
            @RequestParam(name = "lat", defaultValue = "23.7465") Double lat,
            @RequestParam(name = "lng", defaultValue = "90.3760") Double lng) {

        List<Map<String, Object>> list = pharmacyService.findEmergencyPharmacies(lat, lng);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("emergencyPharmacies", list);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = {"/stocks", "/stock"})
    public ResponseEntity<Map<String, Object>> getAllStocks() {
        List<PharmacyStock> stocks = pharmacyService.findAllStocks();
        List<Map<String, Object>> list = new ArrayList<>();
        for (PharmacyStock s : stocks) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", s.getId());
            map.put("pharmacyId", s.getPharmacyId());
            map.put("pharmacyName", s.getPharmacyName());
            map.put("medicineId", s.getMedicineId());
            map.put("medicineBrandName", s.getMedicineBrandName());
            map.put("genericName", s.getGenericName());
            map.put("quantity", s.getQuantity());
            map.put("unitPrice", s.getUnitPrice());
            map.put("lastUpdated", s.getLastUpdated() != null ? s.getLastUpdated().toString() : "");
            list.add(map);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("stocks", list);
        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = {"/stocks", "/stock"}, method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<Map<String, Object>> updateStock(@RequestBody Map<String, Object> body) {
        String stockId = (String) body.get("stockId");
        int quantity = 10;
        if (body.containsKey("quantity")) {
            Object q = body.get("quantity");
            if (q instanceof Number) {
                quantity = ((Number) q).intValue();
            } else {
                try {
                    quantity = Integer.parseInt(q.toString());
                } catch (Exception ignored) {}
            }
        }

        Optional<PharmacyStock> updated = pharmacyService.updateStockQuantity(stockId, quantity);
        Map<String, Object> response = new HashMap<>();
        if (updated.isPresent()) {
            response.put("status", "SUCCESS");
            response.put("message", "Stock updated to " + quantity + " units and broadcast to observers.");
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "ERROR");
            response.put("message", "Stock record not found");
            return ResponseEntity.status(404).body(response);
        }
    }
}
