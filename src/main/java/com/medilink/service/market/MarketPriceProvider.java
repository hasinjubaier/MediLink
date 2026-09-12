package com.medilink.service.market;

import com.medilink.model.market.MarketPriceItem;
import java.util.List;
import java.util.Optional;

/**
 * Adapter Pattern Interface: Decouples the MediLink pricing engine from external data sources.
 * Supports official DGDA feeds, Medex BD market registries, e-pharmacy distributor APIs,
 * and live webhook providers.
 */
public interface MarketPriceProvider {

    /**
     * Fetches the latest published market price index for all monitored medicines in Bangladesh.
     */
    List<MarketPriceItem> fetchLatestMarketPrices();

    /**
     * Fetches the market price quote for a specific medicine by brand name.
     */
    Optional<MarketPriceItem> fetchPriceByBrand(String brandName);

    /**
     * Returns human-readable name of this market data source.
     */
    String getProviderName();

    /**
     * Indicates whether an external live REST API / network endpoint is actively connected.
     */
    boolean isLiveApiConnected();
}
