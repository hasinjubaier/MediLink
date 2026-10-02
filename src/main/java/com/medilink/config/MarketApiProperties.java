package com.medilink.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Type-safe configuration properties for external Medicine Data
 * & Market Price Synchronization (Apify MedEx Actor & Fallback Benchmark).
 */
@Component
@ConfigurationProperties(prefix = "medilink.market")
public class MarketApiProperties {

    /**
     * Whether external market price API integration is active. Default is false (uses database benchmark).
     */
    private boolean enabled = false;

    /**
     * Selected provider mode: "external", "database".
     */
    private String provider = "database";

    /**
     * Base URL for the Apify or external medicine market REST API.
     */
    private String baseUrl = "https://api.apify.com/v2";

    /**
     * API key / Token for authenticating with Apify or external market provider.
     */
    private String apiKey = "";

    /**
     * Header name used for transmitting the API key (e.g. Authorization or X-API-Key).
     */
    private String apiKeyHeader = "Authorization";

    /**
     * Prefix prepended to the API key in the header (e.g. "Bearer" or empty).
     */
    private String apiKeyPrefix = "Bearer";

    /**
     * Apify Actor identifier (e.g. riad_h~medex-medicine-scraper).
     */
    private String apifyActorId = "riad_h~medex-medicine-scraper";

    /**
     * HTTP connect timeout in milliseconds.
     */
    private int connectTimeoutMs = 10000;

    /**
     * HTTP socket read timeout in milliseconds.
     */
    private int readTimeoutMs = 120000;

    /**
     * Maximum retry attempts for transient network failures.
     */
    private int maxRetries = 2;

    /**
     * Whether to safely fall back to the internal database benchmark if the external API fails.
     */
    private boolean fallbackEnabled = true;

    /**
     * Automatic synchronization delay between executions in milliseconds (default: 300,000 ms = 5 minutes).
     */
    private long syncDelayMs = 300000L;

    /**
     * Initial startup delay before first synchronization in milliseconds (default: 15,000 ms = 15s).
     */
    private long initialDelayMs = 15000L;

    /**
     * Whether regulatory price fluctuation simulation endpoint is enabled (disabled in strict production).
     */
    private boolean simulationEnabled = false;

    /**
     * Webhook secret key required to authenticate inbound price notifications.
     */
    private String webhookSecret = "";

    /**
     * Maximum allowable percentage price change before triggering manual review/rejection.
     */
    private double maxPriceChangePercent = 50.0;

    /**
     * Apify MedEx Scraper search depth (1 = single letter ~280 brands, 2 = two letter combos ~2900 brands).
     */
    private int searchDepth = 1;

    /**
     * Apify MedEx Scraper max brands to scrape (0 = all, default: 100 for sync).
     */
    private int maxResults = 100;

    /**
     * Apify MedEx Scraper request delay in ms between MedEx search calls.
     */
    private int requestDelayMs = 200;

    public MarketApiProperties() {}

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getApiKeyHeader() { return apiKeyHeader; }
    public void setApiKeyHeader(String apiKeyHeader) { this.apiKeyHeader = apiKeyHeader; }

    public String getApiKeyPrefix() { return apiKeyPrefix; }
    public void setApiKeyPrefix(String apiKeyPrefix) { this.apiKeyPrefix = apiKeyPrefix; }

    public String getApifyActorId() { return apifyActorId; }
    public void setApifyActorId(String apifyActorId) { this.apifyActorId = apifyActorId; }

    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }

    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

    public boolean isFallbackEnabled() { return fallbackEnabled; }
    public void setFallbackEnabled(boolean fallbackEnabled) { this.fallbackEnabled = fallbackEnabled; }

    public long getSyncDelayMs() { return syncDelayMs; }
    public void setSyncDelayMs(long syncDelayMs) { this.syncDelayMs = syncDelayMs; }

    public long getInitialDelayMs() { return initialDelayMs; }
    public void setInitialDelayMs(long initialDelayMs) { this.initialDelayMs = initialDelayMs; }

    public boolean isSimulationEnabled() { return simulationEnabled; }
    public void setSimulationEnabled(boolean simulationEnabled) { this.simulationEnabled = simulationEnabled; }

    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }

    public double getMaxPriceChangePercent() { return maxPriceChangePercent; }
    public void setMaxPriceChangePercent(double maxPriceChangePercent) { this.maxPriceChangePercent = maxPriceChangePercent; }

    public int getSearchDepth() { return searchDepth; }
    public void setSearchDepth(int searchDepth) { this.searchDepth = searchDepth; }

    public int getMaxResults() { return maxResults; }
    public void setMaxResults(int maxResults) { this.maxResults = maxResults; }

    public int getRequestDelayMs() { return requestDelayMs; }
    public void setRequestDelayMs(int requestDelayMs) { this.requestDelayMs = requestDelayMs; }
}
