package com.medilink.controller;

import com.medilink.config.MarketApiProperties;
import com.medilink.model.market.MarketPriceItem;
import com.medilink.model.market.MedicinePriceHistory;
import com.medilink.model.medicine.Medicine;
import com.medilink.service.market.MarketPriceProvider;
import com.medilink.service.market.MarketPriceSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MarketPriceControllerTest {

    @Mock
    private MarketPriceSyncService syncService;

    @Mock
    private MarketPriceProvider marketPriceProvider;

    private MarketApiProperties properties;
    private MarketPriceController controller;

    @BeforeEach
    public void setUp() {
        properties = new MarketApiProperties();
        properties.setEnabled(true);
        properties.setSimulationEnabled(true);
        properties.setWebhookSecret("test_webhook_sec_123");

        lenient().when(syncService.getMarketPriceProvider()).thenReturn(marketPriceProvider);

        controller = new MarketPriceController(syncService, properties);
    }

    @Test
    @DisplayName("GET /api/market/prices returns safe metadata without leaking API keys")
    public void testGetMarketPricesMetadata() {
        MarketPriceItem item = new MarketPriceItem(
                "Napa Extra", "Paracetamol + Caffeine", "Beximco", "500mg+65mg", "Tablet", 3.00, "2026-09-26", "Test Source"
        );
        when(marketPriceProvider.fetchLatestMarketPrices()).thenReturn(Collections.singletonList(item));
        when(marketPriceProvider.getProviderName()).thenReturn("Bangladesh DGDA & Medex Benchmark Index");
        when(marketPriceProvider.isLiveApiConnected()).thenReturn(false);
        when(syncService.getLastSyncTime()).thenReturn(LocalDateTime.now());

        ResponseEntity<Map<String, Object>> response = controller.getMarketPrices();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("SUCCESS", response.getBody().get("status"));
        assertEquals("Bangladesh DGDA & Medex Benchmark Index", response.getBody().get("providerName"));
        assertEquals(false, response.getBody().get("isLiveApiConnected"));
        assertEquals(1, response.getBody().get("marketCount"));

        // Verify that sensitive credentials are never exposed
        assertFalse(response.getBody().containsKey("apiKey"));
        assertFalse(response.getBody().containsKey("authorization"));
        assertFalse(response.getBody().containsKey("secret"));
    }

    @Test
    @DisplayName("GET /api/market/status returns diagnostic metadata and statistics without prices")
    public void testGetMarketStatusDiagnostic() {
        properties.setApiKey("test_key_abc");
        properties.setBaseUrl("https://api.apify.com/v2");
        when(marketPriceProvider.fetchLatestMarketPrices()).thenReturn(Collections.emptyList());
        when(marketPriceProvider.getProviderName()).thenReturn("MedEx via Apify");
        when(marketPriceProvider.isLiveApiConnected()).thenReturn(true);
        when(syncService.getLastSyncStatus()).thenReturn("COMPLETED");

        MarketPriceSyncService.MarketSyncStatistics stats = new MarketPriceSyncService.MarketSyncStatistics(
                100, 100, 85, 10, 75, 15, 0, 0, "85.0%", "PARTIAL_SUCCESS", null, LocalDateTime.now()
        );
        when(syncService.getLastSyncStatistics()).thenReturn(stats);

        ResponseEntity<Map<String, Object>> response = controller.getMarketStatus();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("SUCCESS", response.getBody().get("status"));
        assertEquals(true, response.getBody().get("enabled"));
        assertEquals(true, response.getBody().get("externalApiConfigured"));
        assertEquals(true, response.getBody().get("externalApiReachable"));
        assertEquals(true, response.getBody().get("isLiveApiConnected"));
        assertEquals(true, response.getBody().get("fallbackEnabled"));
        assertEquals("COMPLETED", response.getBody().get("lastSyncStatus"));
        assertNotNull(response.getBody().get("lastSyncStatistics"));
        // Does not include prices list in status-only endpoint
        assertFalse(response.getBody().containsKey("prices"));

        // Guarantee sensitive secrets are NOT exposed
        assertFalse(response.getBody().containsKey("apiKey"));
        assertFalse(response.getBody().containsKey("apiKeyHeader"));
        assertFalse(response.getBody().containsKey("webhookSecret"));
    }

    @Test
    @DisplayName("POST /api/market/sync triggers on-demand synchronization")
    public void testSyncNow() {
        MarketPriceSyncService.SyncResult syncResult = new MarketPriceSyncService.SyncResult(
                12, 3, Collections.emptyList(), LocalDateTime.now()
        );
        when(syncService.syncAllMedicines(anyString())).thenReturn(syncResult);

        ResponseEntity<Map<String, Object>> response = controller.syncNow("Admin Manual Sync", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("COMPLETED", response.getBody().get("status"));
        assertEquals(12, response.getBody().get("checkedMedicines"));
        assertEquals(3, response.getBody().get("updatedMedicines"));
    }

    @Test
    @DisplayName("POST /api/market/webhook rejects request when webhook secret is missing or incorrect")
    public void testWebhookRejectsInvalidSecret() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("brandName", "Napa Extra");
        payload.put("newPrice", 3.20);

        // Header has wrong secret
        ResponseEntity<Map<String, Object>> response = controller.receivePriceWebhook(
                "wrong_secret", payload
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(syncService, never()).updatePriceDirectly(anyString(), anyDouble(), anyString());
    }

    @Test
    @DisplayName("POST /api/market/webhook accepts request with valid secret and updates price")
    public void testWebhookAcceptsValidSecret() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("brandName", "Napa Extra");
        payload.put("newPrice", 3.20);
        payload.put("source", "Distributor API");

        when(syncService.updatePriceDirectly("Napa Extra", 3.20, "Distributor API")).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = controller.receivePriceWebhook(
                "test_webhook_sec_123", payload
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("SUCCESS", response.getBody().get("status"));
        verify(syncService, times(1)).updatePriceDirectly("Napa Extra", 3.20, "Distributor API");
    }

    @Test
    @DisplayName("POST /api/market/simulate-fluctuation is blocked when simulation is disabled in production")
    public void testSimulationDisabledInProduction() {
        properties.setSimulationEnabled(false);

        ResponseEntity<Map<String, Object>> response = controller.simulateFluctuation("Napa Extra", 10.0, null, null);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(syncService, never()).updatePriceDirectly(anyString(), anyDouble(), anyString());
    }

    @Test
    @DisplayName("POST /api/market/simulate-fluctuation succeeds when enabled")
    public void testSimulationSucceedsWhenEnabled() {
        Medicine med = new Medicine();
        med.setBrandName("Napa Extra");
        med.setUnitPrice(2.50);

        when(syncService.findMedicineByBrand("Napa Extra")).thenReturn(med);
        when(syncService.getRecentPriceHistories()).thenReturn(Collections.emptyList());

        ResponseEntity<Map<String, Object>> response = controller.simulateFluctuation("Napa Extra", 20.0, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("SUCCESS", response.getBody().get("status"));
        verify(syncService, times(1)).updatePriceDirectly("Napa Extra", 3.00, "SIMULATION");
    }

    @Test
    @DisplayName("GET /api/market/history returns filtered price audit records")
    public void testGetPriceHistoryFiltered() {
        MedicinePriceHistory h1 = new MedicinePriceHistory("h1", "med_01", "Napa Extra", "Paracetamol", "Beximco", 2.50, 3.00, "DGDA");
        MedicinePriceHistory h2 = new MedicinePriceHistory("h2", "med_02", "Seclo 20", "Omeprazole", "Square", 6.00, 6.50, "DGDA");

        when(syncService.getAllPriceHistories()).thenReturn(new ArrayList<>(Arrays.asList(h1, h2)));

        ResponseEntity<Map<String, Object>> response = controller.getPriceHistory("med_01", 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<?> list = (List<?>) response.getBody().get("history");
        assertEquals(1, list.size());
    }
}
