package com.medilink.service.market;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medilink.config.MarketApiProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Root service package alias for ExternalMedicineApiClient.
 */
public class ExternalMedicineApiClient extends com.medilink.service.market.client.ExternalMedicineApiClient {

    public ExternalMedicineApiClient(MarketApiProperties properties) {
        super(properties);
    }

    public ExternalMedicineApiClient(MarketApiProperties properties, RestTemplate restTemplate) {
        super(properties, restTemplate);
    }

    public ExternalMedicineApiClient(MarketApiProperties properties, RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(properties, restTemplate, objectMapper);
    }
}
