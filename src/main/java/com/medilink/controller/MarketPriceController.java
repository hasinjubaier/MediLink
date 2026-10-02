package com.medilink.controller;

import com.medilink.config.MarketApiProperties;
import com.medilink.model.market.MarketPriceItem;
import com.medilink.model.market.MedicinePriceHistory;
import com.medilink.service.market.CompositeMarketPriceProvider;
import com.medilink.service.market.MarketPriceSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

/**
 * REST API for Bangladesh Medicine Market Price Synchronization & Real-time Auto-Updates.
 * Exposes sanitized metadata, manual triggers, secure webhook ingestion, simulation controls,
 * and high-fidelity diagnostic status reporting.
 */
@RestController
@RequestMapping("/api/market")
@CrossOrigin(origins = "*")
public class MarketPriceController {

    private final MarketPriceSyncService syncService;
    private final MarketApiProperties properties;

    @Autowired
    public MarketPriceController(MarketPriceSyncService syncService, MarketApiProperties properties) {
        this.syncService = syncService;
        this.properties = properties != null ? properties : new MarketApiProperties();
    }

    /**
     * Backward-compatible constructor for testing.
     */
    public MarketPriceController(MarketPriceSyncService syncService) {
        this(syncService, new MarketApiProperties());
    }

    /**
     * Diagnostic endpoint for system health, provider connectivity, and sync metrics.
     * Complies with Section 4: externalApiReachable is true ONLY upon genuine HTTP success.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getMarketStatus() {
        return buildMarketStatusResponse(false);
    }

    /**
     * Retrieve the current market price index and sync provider status.
     * Never reveals internal API keys or secrets.
     */
    @GetMapping("/prices")
    public ResponseEntity<Map<String, Object>> getMarketPrices() {
        return buildMarketStatusResponse(true);
    }

    private ResponseEntity<Map<String, Object>> buildMarketStatusResponse(boolean includePrices) {
        List<MarketPriceItem> quotes = syncService.getMarketPriceProvider().fetchLatestMarketPrices();

        String providerName = syncService.getMarketPriceProvider().getProviderName();
        boolean isLive = syncService.getMarketPriceProvider().isLiveApiConnected();

        boolean fallbackActive = false;
        String providerMode = "BENCHMARK_REGISTRY";

        if (syncService.getMarketPriceProvider() instanceof CompositeMarketPriceProvider) {
            CompositeMarketPriceProvider cmp = (CompositeMarketPriceProvider) syncService.getMarketPriceProvider();
            providerMode = cmp.getProviderMode();
            fallbackActive = cmp.isFallbackActive();
        } else if (isLive) {
            providerMode = "LIVE_EXTERNAL_API";
        }

        boolean externalApiConfigured = properties.isEnabled()
                && properties.getBaseUrl() != null && !properties.getBaseUrl().trim().isEmpty()
                && properties.getApiKey() != null && !properties.getApiKey().trim().isEmpty();

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("enabled", properties.isEnabled());
        resp.put("provider", properties.getProvider());
        resp.put("configuredProvider", properties.getProvider());
        resp.put("providerName", providerName);
        resp.put("providerMode", providerMode);
        resp.put("externalApiConfigured", externalApiConfigured);
        resp.put("externalApiReachable", isLive);
        resp.put("isLiveApiConnected", isLive);
        resp.put("liveApiConnected", isLive);
        resp.put("fallbackEnabled", properties.isFallbackEnabled());
        resp.put("fallbackActive", fallbackActive);
        resp.put("lastSyncStatus", syncService.getLastSyncStatus());
        resp.put("lastSyncAt", syncService.getLastSyncTime() != null ? syncService.getLastSyncTime().toString() : null);
        resp.put("lastSuccessfulSync", syncService.getLastSyncTime() != null ? syncService.getLastSyncTime().toString() : null);
        resp.put("lastError", syncService.getLastSyncError());
        resp.put("lastSyncStatistics", syncService.getLastSyncStatistics());
        resp.put("marketCount", quotes.size());

        if (includePrices) {
            resp.put("prices", quotes);
            resp.put("marketItems", quotes);
        }

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
        } else if (body != null && body.containsKey("triggerSource")) {
            triggerSource = body.get("triggerSource");
        }

        MarketPriceSyncService.SyncResult result = syncService.syncAllMedicines(triggerSource);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", "COMPLETED");
        resp.put("message", "Market price synchronization completed successfully.");
        resp.put("triggerSource", triggerSource);
        resp.put("provider", syncService.getMarketPriceProvider().getProviderName());
        resp.put("checkedMedicines", result.getCheckedCount());
        resp.put("checkedCount", result.getCheckedCount());
        resp.put("updatedMedicines", result.getUpdatedCount());
        resp.put("updatedCount", result.getUpdatedCount());
        resp.put("changes", result.getChanges());
        resp.put("statistics", result.getStatistics());
        resp.put("lastSyncStatistics", result.getStatistics());
        resp.put("timestamp", result.getTimestamp().toString());
        return ResponseEntity.ok(resp);
    }

    /**
     * Retrieve the historical price fluctuation audit log.
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getPriceHistory(
            @RequestParam(value = "medicineId", required = false) String medicineId,
            @RequestParam(value = "brandName", required = false) String brandName,
            @RequestParam(value = "limit", required = false, defaultValue = "50") int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 200));
        List<MedicinePriceHistory> history = syncService.getAllPriceHistories();

        if (medicineId != null && !medicineId.trim().isEmpty()) {
            String cleanMedId = medicineId.trim();
            history.removeIf(h -> !cleanMedId.equalsIgnoreCase(h.getMedicineId()));
        }

        if (brandName != null && !brandName.trim().isEmpty()) {
            String cleanBrand = brandName.trim();
            history.removeIf(h -> !cleanBrand.equalsIgnoreCase(h.getBrandName()));
        }

        if (history.size() > boundedLimit) {
            history = history.subList(0, boundedLimit);
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("totalRecords", history.size());
        resp.put("history", history);
        return ResponseEntity.ok(resp);
    }

    public ResponseEntity<Map<String, Object>> getPriceHistory(String medicineId, int limit) {
        return getPriceHistory(medicineId, null, limit);
    }

    public ResponseEntity<Map<String, Object>> receivePriceWebhook(String headerSecret, Map<String, Object> payload) {
        return receivePriceWebhook(headerSecret, headerSecret, payload);
    }

    /**
     * Webhook receiver for external distributor APIs or DGDA push notifications.
     * Enforces webhook authentication when secret is configured using constant-time comparison.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> receivePriceWebhook(
            @RequestHeader(value = "X-Market-Webhook-Secret", required = false) String marketHeaderSecret,
            @RequestHeader(value = "X-Webhook-Secret", required = false) String genericHeaderSecret,
            @RequestBody Map<String, Object> payload) {

        String expectedSecret = properties.getWebhookSecret();
        if (expectedSecret != null && !expectedSecret.trim().isEmpty()) {
            String incomingSecret = marketHeaderSecret != null ? marketHeaderSecret : genericHeaderSecret;
            if (incomingSecret == null && payload != null) {
                incomingSecret = (String) payload.get("secret");
                if (incomingSecret == null) {
                    incomingSecret = (String) payload.get("webhookSecret");
                }
            }

            if (incomingSecret == null || !MessageDigest.isEqual(
                    expectedSecret.trim().getBytes(StandardCharsets.UTF_8),
                    incomingSecret.trim().getBytes(StandardCharsets.UTF_8))) {
                Map<String, Object> authErr = new LinkedHashMap<>();
                authErr.put("status", "ERROR");
                authErr.put("message", "Unauthorized: Invalid or missing webhook secret.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(authErr);
            }
        }

        String brandName = (String) payload.get("brandName");
        Object priceObj = payload.get("newPrice");
        String source = (String) payload.getOrDefault("source", "External BD Market Webhook");

        if (brandName == null || priceObj == null) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("status", "ERROR");
            err.put("message", "Missing required fields: brandName and newPrice.");
            return ResponseEntity.badRequest().body(err);
        }

        double newPrice;
        try {
            newPrice = Double.parseDouble(String.valueOf(priceObj));
            if (newPrice <= 0 || Double.isInfinite(newPrice) || Double.isNaN(newPrice)) {
                throw new IllegalArgumentException("Price must be strictly positive");
            }
        } catch (Exception e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("status", "ERROR");
            err.put("message", "Invalid numeric value for newPrice. Must be greater than 0.");
            return ResponseEntity.badRequest().body(err);
        }

        boolean updated = syncService.updatePriceDirectly(brandName, newPrice, source);
        Map<String, Object> resp = new LinkedHashMap<>();
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
     * Simulates a realistic market price adjustment for live presentation/testing.
     * Controlled by configuration property medilink.market.simulation-enabled.
     */
    @PostMapping("/simulate-fluctuation")
    public ResponseEntity<Map<String, Object>> simulateFluctuation(
            @RequestParam(value = "brandName", required = false) String brandParam,
            @RequestParam(value = "percentChange", required = false) Double pctParam,
            @RequestParam(value = "targetPrice", required = false) Double targetPriceParam,
            @RequestBody(required = false) Map<String, Object> body) {

        if (!properties.isSimulationEnabled()) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("status", "ERROR");
            err.put("message", "Market fluctuation simulation is disabled in production mode.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err);
        }

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
            Map<String, Object> err = new LinkedHashMap<>();
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
            newPrice = (oldPrice <= 3.0) ? (oldPrice >= 3.0 ? 2.5 : 3.0) : Math.round((oldPrice * 1.10) * 100.0) / 100.0;
        }

        syncService.updatePriceDirectly(targetMed.getBrandName(), newPrice, "SIMULATION");
        MedicinePriceHistory record = syncService.getRecentPriceHistories().stream().findFirst().orElse(null);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("message", String.format("Simulation price adjustment for %s: ৳%.2f -> ৳%.2f", targetMed.getBrandName(), oldPrice, newPrice));
        resp.put("record", record);
        return ResponseEntity.ok(resp);
    }
}
