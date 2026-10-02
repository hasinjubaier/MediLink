package com.medilink.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MarketApiPropertiesTest {

    @Test
    @DisplayName("Verify default configuration values for MarketApiProperties")
    public void testDefaultProperties() {
        MarketApiProperties props = new MarketApiProperties();

        assertFalse(props.isEnabled());
        assertEquals("database", props.getProvider());
        assertEquals("https://api.apify.com/v2", props.getBaseUrl());
        assertEquals("Authorization", props.getApiKeyHeader());
        assertEquals("Bearer", props.getApiKeyPrefix());
        assertEquals("riad_h~medex-medicine-scraper", props.getApifyActorId());
        assertEquals(10000, props.getConnectTimeoutMs());
        assertEquals(120000, props.getReadTimeoutMs());
        assertEquals(2, props.getMaxRetries());
        assertTrue(props.isFallbackEnabled());
        assertEquals(300000L, props.getSyncDelayMs());
        assertEquals(15000L, props.getInitialDelayMs());
        assertFalse(props.isSimulationEnabled());
        assertEquals(50.0, props.getMaxPriceChangePercent(), 0.001);
        assertEquals(1, props.getSearchDepth());
        assertEquals(100, props.getMaxResults());
        assertEquals(200, props.getRequestDelayMs());
    }

    @Test
    @DisplayName("Verify custom setters and getters")
    public void testCustomProperties() {
        MarketApiProperties props = new MarketApiProperties();
        props.setEnabled(true);
        props.setProvider("external");
        props.setApiKey("test_token_xyz");
        props.setMaxPriceChangePercent(75.0);
        props.setSimulationEnabled(true);
        props.setWebhookSecret("my_secret");
        props.setSearchDepth(2);
        props.setMaxResults(500);

        assertTrue(props.isEnabled());
        assertEquals("external", props.getProvider());
        assertEquals("test_token_xyz", props.getApiKey());
        assertEquals(75.0, props.getMaxPriceChangePercent(), 0.001);
        assertTrue(props.isSimulationEnabled());
        assertEquals("my_secret", props.getWebhookSecret());
        assertEquals(2, props.getSearchDepth());
        assertEquals(500, props.getMaxResults());
    }
}
