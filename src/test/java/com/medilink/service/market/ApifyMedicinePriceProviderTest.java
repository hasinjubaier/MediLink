package com.medilink.service.market;

import com.medilink.config.MarketApiProperties;
import com.medilink.dto.market.ExternalMedicinePriceDto;
import com.medilink.model.market.MarketPriceItem;
import com.medilink.service.market.client.ExternalMedicineApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ApifyMedicinePriceProviderTest {

    @Mock
    private ExternalMedicineApiClient apiClient;

    private MarketApiProperties properties;
    private ApifyMedicinePriceProvider apifyProvider;
    private BangladeshDgdaMedexProvider fallbackProvider;
    private CompositeMarketPriceProvider compositeProvider;

    @BeforeEach
    public void setUp() {
        properties = new MarketApiProperties();
        properties.setEnabled(true);
        properties.setProvider("external");
        properties.setFallbackEnabled(true);

        apifyProvider = new ApifyMedicinePriceProvider(apiClient, properties);
        fallbackProvider = new BangladeshDgdaMedexProvider();
        compositeProvider = new CompositeMarketPriceProvider(apifyProvider, fallbackProvider, properties);
    }

    @Test
    @DisplayName("Provider name is MedEx via Apify")
    public void testProviderName() {
        assertEquals("MedEx via Apify", apifyProvider.getProviderName());
    }

    @Test
    @DisplayName("Live connection reports false when client has not succeeded")
    public void testLiveConnectionStatusFalseWhenNotRun() {
        when(apiClient.isConfiguredAndValid()).thenReturn(true);
        when(apiClient.isLastCallSuccessful()).thenReturn(false);

        assertFalse(apifyProvider.isLiveApiConnected());
        assertFalse(compositeProvider.isLiveApiConnected());
    }

    @Test
    @DisplayName("Live connection reports true when external call succeeds")
    public void testLiveConnectionStatusTrueWhenSucceeded() {
        when(apiClient.isConfiguredAndValid()).thenReturn(true);
        when(apiClient.isLastCallSuccessful()).thenReturn(true);

        assertTrue(apifyProvider.isLiveApiConnected());
        assertTrue(compositeProvider.isLiveApiConnected());
        assertEquals("LIVE_EXTERNAL_API", compositeProvider.getProviderMode());
    }

    @Test
    @DisplayName("Composite provider falls back to local benchmark when Apify returns empty")
    public void testCompositeFallbackOnEmptyExternalResponse() {
        when(apiClient.isConfiguredAndValid()).thenReturn(true);
        when(apiClient.fetchPrices()).thenReturn(Collections.emptyList());

        List<MarketPriceItem> items = compositeProvider.fetchLatestMarketPrices();

        assertNotNull(items);
        assertFalse(items.isEmpty());
        // Comes from local benchmark registry
        assertTrue(items.stream().anyMatch(i -> i.getBrandName().equalsIgnoreCase("Napa Extra")));
        assertEquals("DATABASE_FALLBACK", compositeProvider.getProviderMode());
    }

    @Test
    @DisplayName("Composite provider returns external data when Apify succeeds with valid quotes")
    public void testCompositeReturnsExternalWhenSuccess() {
        when(apiClient.isConfiguredAndValid()).thenReturn(true);
        when(apiClient.isLastCallSuccessful()).thenReturn(true);

        ExternalMedicinePriceDto dto = new ExternalMedicinePriceDto();
        dto.setBrandName("Napa Extra");
        dto.setUnitPrice(new BigDecimal("3.40"));
        dto.setSource("MedEx via Apify");

        when(apiClient.fetchPrices()).thenReturn(Collections.singletonList(dto));

        List<MarketPriceItem> items = compositeProvider.fetchLatestMarketPrices();

        assertNotNull(items);
        assertEquals(1, items.size());
        assertEquals("Napa Extra", items.get(0).getBrandName());
        assertEquals(3.40, items.get(0).getMrp(), 0.001);
        assertEquals("MedEx via Apify", items.get(0).getSourceRegistry());
        assertEquals("LIVE_EXTERNAL_API", compositeProvider.getProviderMode());
    }

    @Test
    @DisplayName("Single medicine lookup delegates to Apify and falls back")
    public void testFetchPriceByBrandFallback() {
        when(apiClient.isConfiguredAndValid()).thenReturn(true);
        when(apiClient.fetchPriceByBrand("Napa Extra")).thenReturn(Optional.empty());

        Optional<MarketPriceItem> quote = compositeProvider.fetchPriceByBrand("Napa Extra");

        assertTrue(quote.isPresent());
        assertEquals("Napa Extra", quote.get().getBrandName());
        assertEquals(3.00, quote.get().getMrp(), 0.001);
    }
}
