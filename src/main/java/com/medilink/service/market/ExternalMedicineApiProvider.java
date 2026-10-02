package com.medilink.service.market;

import com.medilink.config.MarketApiProperties;
import com.medilink.model.market.MarketPriceItem;
import com.medilink.service.market.client.ExternalMedicineApiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adapter provider wrapping ApifyMedicinePriceProvider to preserve full backward compatibility
 * across existing tests and consumers.
 */
@Component
public class ExternalMedicineApiProvider implements MarketPriceProvider {

    private final ApifyMedicinePriceProvider apifyProvider;

    @Autowired
    public ExternalMedicineApiProvider(ApifyMedicinePriceProvider apifyProvider) {
        this.apifyProvider = apifyProvider;
    }

    public ExternalMedicineApiProvider(ExternalMedicineApiClient apiClient, MarketApiProperties properties) {
        this.apifyProvider = new ApifyMedicinePriceProvider(apiClient, properties);
    }

    @Override
    public List<MarketPriceItem> fetchLatestMarketPrices() {
        return apifyProvider.fetchLatestMarketPrices();
    }

    @Override
    public Optional<MarketPriceItem> fetchPriceByBrand(String brandName) {
        return apifyProvider.fetchPriceByBrand(brandName);
    }

    @Override
    public String getProviderName() {
        return apifyProvider.getProviderName();
    }

    @Override
    public boolean isLiveApiConnected() {
        return apifyProvider.isLiveApiConnected();
    }
}
