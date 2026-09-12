package com.medilink.controller;

import com.medilink.model.market.MarketPriceItem;
import com.medilink.model.market.MedicinePriceHistory;
import com.medilink.service.market.MarketPriceSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for Bangladesh Medicine Market Price Synchronization & Real-time Auto-Updates.
 */
@RestController
@RequestMapping("/api/market")
@CrossOrigin(origins = "*")
public class MarketPriceController {

    private final MarketPriceSyncService syncService;

    @Autowired
    public MarketPriceController(MarketPriceSyncService syncService) {
        this.syncService = syncService;
    }

    /**
     * Retrieve the current Bangladesh market price index and sync provider status.
     */
    @GetMapping("/prices")
    public ResponseEntity<Map<String, Object>> getMarketPrices() {
        List<MarketPriceItem> quotes = syncService.getMarketPriceProvider().fetchLatestMarketPrices();
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("providerName", syncService.getMarketPriceProvider().getProviderName());
        resp.put("isLiveApiConnected", syncService.getMarketPriceProvider().isLiveApiConnected());
        resp.put("marketCount", quotes.size());
        resp.put("prices", quotes);
        resp.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(resp);
    }

    /**
     * Trigger an immediate on-demand market price synchronization.
     */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncNow(
            @RequestParam(value = "source", required = false) String sourceParam,
            @RequestBody(required = false) Map<String, String> body) {
        String triggerSource = "Manual User/Admin Trigger";
        if (sourceParam != null && !sourceParam.trim().isEmpty()) {
            triggerSource = sourceParam.trim();
        } else if (body != null && body.containsKey("source")) {
            triggerSource = body.get("source");
        }

        MarketPriceSyncService.SyncResult result = syncService.syncAllMedicines(triggerSource);
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("message", "Market price synchronization completed successfully.");
        resp.put("checkedMedicines", result.getCheckedCount());
        resp.put("updatedMedicines", result.getUpdatedCount());
        resp.put("changes", result.getChanges());
        resp.put("timestamp", result.getTimestamp().toString());
        return ResponseEntity.ok(resp);
    }

    /**
     * Retrieve the historical price fluctuation audit log.
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getPriceHistory() {
        List<MedicinePriceHistory> history = syncService.getRecentPriceHistories();
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("totalRecords", history.size());
        resp.put("history", history);
        return ResponseEntity.ok(resp);
    }

    /**
     * Webhook receiver for external distributor APIs or DGDA push notifications.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> receivePriceWebhook(@RequestBody Map<String, Object> payload) {
        String brandName = (String) payload.get("brandName");
        Object priceObj = payload.get("newPrice");
        String source = (String) payload.getOrDefault("source", "External BD Market Webhook");

        if (brandName == null || priceObj == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", "Missing required fields: brandName and newPrice.");
            return ResponseEntity.badRequest().body(err);
        }

        double newPrice;
        try {
            newPrice = Double.parseDouble(String.valueOf(priceObj));
        } catch (NumberFormatException e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", "Invalid numeric value for newPrice.");
            return ResponseEntity.badRequest().body(err);
        }

        boolean updated = syncService.updatePriceDirectly(brandName, newPrice, source);
        Map<String, Object> resp = new HashMap<>();
        if (updated) {
            resp.put("status", "SUCCESS");
            resp.put("message", "Price for '" + brandName + "' updated to ৳" + newPrice + " and broadcasted via SSE.");
            resp.put("brandName", brandName);
            resp.put("newPrice", newPrice);
        } else {
            resp.put("status", "WARNING");
            resp.put("message", "Market registry updated, but medicine was not currently in active catalog: " + brandName);
            resp.put("brandName", brandName);
            resp.put("newPrice", newPrice);
        }
        return ResponseEntity.ok(resp);
    }

    /**
     * Simulates a realistic DGDA market price adjustment for live presentation/testing.
     */
    @PostMapping("/simulate-fluctuation")
    public ResponseEntity<Map<String, Object>> simulateFluctuation(
            @RequestParam(value = "brandName", required = false) String brandParam,
            @RequestParam(value = "percentChange", required = false) Double pctParam,
            @RequestParam(value = "targetPrice", required = false) Double targetPriceParam,
            @RequestBody(required = false) Map<String, Object> body) {
        String brand = brandParam;
        Double targetPrice = targetPriceParam;
        Double pct = pctParam;

        if (body != null) {
            if (brand == null && body.containsKey("brandName")) brand = String.valueOf(body.get("brandName"));
            if (targetPrice == null && body.containsKey("targetPrice")) {
                try {
                    targetPrice = Double.parseDouble(String.valueOf(body.get("targetPrice")));
                } catch (Exception ignored) {}
            }
            if (pct == null && body.containsKey("percentChange")) {
                try {
                    pct = Double.parseDouble(String.valueOf(body.get("percentChange")));
                } catch (Exception ignored) {}
            }
        }

        com.medilink.model.medicine.Medicine targetMed = null;
        if (brand != null && !brand.trim().isEmpty()) {
            targetMed = syncService.findMedicineByBrand(brand);
        }
        if (targetMed == null) {
            targetMed = syncService.findDefaultMedicine();
        }

        if (targetMed == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", "No medicines found in catalog to fluctuate.");
            return ResponseEntity.ok(err);
        }

        double oldPrice = targetMed.getUnitPrice();
        double newPrice;
        if (targetPrice != null && targetPrice > 0) {
            newPrice = targetPrice;
        } else if (pct != null && pct != 0) {
            newPrice = Math.round((oldPrice * (1.0 + (pct / 100.0))) * 100.0) / 100.0;
            if (newPrice <= 0.5) newPrice = 1.0;
        } else {
            // Default realistic fluctuation
            newPrice = (oldPrice <= 3.0) ? (oldPrice >= 3.0 ? 2.5 : 3.0) : Math.round((oldPrice * 1.10) * 100.0) / 100.0;
        }

        syncService.updatePriceDirectly(targetMed.getBrandName(), newPrice, "DGDA Bangladesh Official Price Circular");
        MedicinePriceHistory record = syncService.getRecentPriceHistories().stream().findFirst().orElse(null);

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("message", String.format("DGDA price adjustment for %s: ৳%.2f -> ৳%.2f", targetMed.getBrandName(), oldPrice, newPrice));
        resp.put("record", record);
        return ResponseEntity.ok(resp);
    }
}
