package com.medilink.service.market;

import com.medilink.config.MarketApiProperties;
import com.medilink.dto.market.ExternalMedicinePriceDto;
import com.medilink.model.market.MarketPriceItem;
import com.medilink.service.market.client.ExternalMedicineApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Concrete MarketPriceProvider implementation connecting to MedEx via the Apify Actor.
 * Adheres strictly to the Strategy Pattern and reports live connectivity only upon successful network execution.
 */
@Component("apifyMedicinePriceProvider")
public class ApifyMedicinePriceProvider implements MarketPriceProvider {

    private static final Logger log = LoggerFactory.getLogger(ApifyMedicinePriceProvider.class);

    private final ExternalMedicineApiClient apiClient;
    private final MarketApiProperties properties;
    private final Map<String, MarketPriceItem> cache = new ConcurrentHashMap<>();

    @Autowired
    public ApifyMedicinePriceProvider(ExternalMedicineApiClient apiClient, MarketApiProperties properties) {
        this.apiClient = apiClient;
        this.properties = properties;
    }

    @Override
    public List<MarketPriceItem> fetchLatestMarketPrices() {
        if (!properties.isEnabled() || !apiClient.isConfiguredAndValid()) {
            return Collections.emptyList();
        }

        log.info("[ApifyMedicinePriceProvider] Fetching MedEx data from Apify Actor: {}", properties.getApifyActorId());
        List<ExternalMedicinePriceDto> dtos = apiClient.fetchPrices();
        if (dtos == null || dtos.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("[ApifyMedicinePriceProvider] Received {} records from Apify.", dtos.size());

        List<MarketPriceItem> items = new ArrayList<>();
        for (ExternalMedicinePriceDto dto : dtos) {
            if (dto.isValid()) {
                MarketPriceItem item = toMarketPriceItem(dto);
                items.add(item);
                cache.put(normalize(item.getBrandName()), item);
            }
        }
        return items;
    }

    @Override
    public Optional<MarketPriceItem> fetchPriceByBrand(String brandName) {
        if (brandName == null || brandName.trim().isEmpty()) {
            return Optional.empty();
        }

        String norm = normalize(brandName);
        MarketPriceItem cached = cache.get(norm);
        if (cached != null) {
            return Optional.of(cached);
        }

        if (!properties.isEnabled() || !apiClient.isConfiguredAndValid()) {
            return Optional.empty();
        }

        Optional<ExternalMedicinePriceDto> dtoOpt = apiClient.fetchPriceByBrand(brandName);
        if (dtoOpt.isPresent() && dtoOpt.get().isValid()) {
            MarketPriceItem item = toMarketPriceItem(dtoOpt.get());
            cache.put(norm, item);
            return Optional.of(item);
        }

        return Optional.empty();
    }

    @Override
    public String getProviderName() {
        return "MedEx via Apify";
    }

    @Override
    public boolean isLiveApiConnected() {
        // Strictly true ONLY when an actual HTTP call has succeeded and response parsed
        return properties.isEnabled() && apiClient.isConfiguredAndValid() && apiClient.isLastCallSuccessful();
    }

    public MarketPriceItem toMarketPriceItem(ExternalMedicinePriceDto dto) {
        double mrp = dto.getUnitPrice() != null ? dto.getUnitPrice().doubleValue() : 0.0;
        String dateStr = dto.getObservedAt() != null
                ? dto.getObservedAt().toString().substring(0, 10)
                : LocalDate.now().toString();

        return new MarketPriceItem(
                dto.getBrandName(),
                dto.getGenericName() != null ? dto.getGenericName() : "",
                dto.getManufacturer() != null ? dto.getManufacturer() : "Bangladesh Pharma",
                dto.getStrength() != null ? dto.getStrength() : "",
                dto.getDosageForm() != null ? dto.getDosageForm() : "Unit",
                mrp,
                dateStr,
                "MedEx via Apify"
        );
    }

    private String normalize(String s) {
        return s != null ? s.trim().toLowerCase().replaceAll("\\s+", " ") : "";
    }
}
