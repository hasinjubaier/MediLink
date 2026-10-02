package com.medilink.service.market;

import com.medilink.config.MarketApiProperties;
import com.medilink.model.market.MarketPriceItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Composite Strategy Provider: Coordinates Apify MedEx Scraper with safe automatic
 * fallback to the local benchmark registry. Truthfully reports live connection status
 * and active provider mode.
 */
@Component
@Primary
public class CompositeMarketPriceProvider implements MarketPriceProvider {

    private static final Logger log = LoggerFactory.getLogger(CompositeMarketPriceProvider.class);

    private final ApifyMedicinePriceProvider apifyProvider;
    private final BangladeshDgdaMedexProvider fallbackProvider;
    private final MarketApiProperties properties;

    @Autowired
    public CompositeMarketPriceProvider(ApifyMedicinePriceProvider apifyProvider,
                                        BangladeshDgdaMedexProvider fallbackProvider,
                                        MarketApiProperties properties) {
        this.apifyProvider = apifyProvider;
        this.fallbackProvider = fallbackProvider;
        this.properties = properties;
    }

    public boolean isExternalModeActive() {
        return properties.isEnabled() && "external".equalsIgnoreCase(properties.getProvider());
    }

    @Override
    public List<MarketPriceItem> fetchLatestMarketPrices() {
        if (isExternalModeActive()) {
            log.info("[CompositeMarketPriceProvider] Attempting external Apify market sync...");
            List<MarketPriceItem> externalItems = apifyProvider.fetchLatestMarketPrices();
            if (externalItems != null && !externalItems.isEmpty()) {
                return externalItems;
            }

            if (properties.isFallbackEnabled()) {
                log.warn("[CompositeMarketPriceProvider] External provider failed or returned empty; fallback enabled. Using local benchmark registry.");
                return fallbackProvider.fetchLatestMarketPrices();
            }
            return Collections.emptyList();
        }

        log.info("[CompositeMarketPriceProvider] Using local database/benchmark provider (MARKET_PROVIDER={}).", properties.getProvider());
        return fallbackProvider.fetchLatestMarketPrices();
    }

    @Override
    public Optional<MarketPriceItem> fetchPriceByBrand(String brandName) {
        if (isExternalModeActive()) {
            Optional<MarketPriceItem> extQuote = apifyProvider.fetchPriceByBrand(brandName);
            if (extQuote.isPresent()) {
                return extQuote;
            }
            if (properties.isFallbackEnabled()) {
                return fallbackProvider.fetchPriceByBrand(brandName);
            }
            return Optional.empty();
        }

        return fallbackProvider.fetchPriceByBrand(brandName);
    }

    @Override
    public String getProviderName() {
        if (isLiveApiConnected()) {
            return apifyProvider.getProviderName();
        } else if (isExternalModeActive() && properties.isFallbackEnabled()) {
            return apifyProvider.getProviderName() + " (Fallback: " + fallbackProvider.getProviderName() + ")";
        }
        return fallbackProvider.getProviderName();
    }

    @Override
    public boolean isLiveApiConnected() {
        // Live status is true ONLY if external mode is configured and Apify HTTP call succeeded
        return isExternalModeActive() && apifyProvider.isLiveApiConnected();
    }

    public boolean isFallbackActive() {
        return !isLiveApiConnected() && properties.isFallbackEnabled();
    }

    public String getProviderMode() {
        if (isLiveApiConnected()) {
            return "LIVE_EXTERNAL_API";
        } else if (isExternalModeActive() && properties.isFallbackEnabled()) {
            return "DATABASE_FALLBACK";
        } else if ("database".equalsIgnoreCase(properties.getProvider())) {
            return "DATABASE_BENCHMARK";
        }
        return "DISABLED";
    }

    public void updateRegistryPrice(String brandName, double newMrp) {
        fallbackProvider.updateRegistryPrice(brandName, newMrp);
    }

    public ApifyMedicinePriceProvider getApifyProvider() {
        return apifyProvider;
    }

    public BangladeshDgdaMedexProvider getFallbackProvider() {
        return fallbackProvider;
    }
}
