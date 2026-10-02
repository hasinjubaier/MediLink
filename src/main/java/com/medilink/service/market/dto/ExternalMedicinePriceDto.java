package com.medilink.service.market.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Backward-compatible alias extending com.medilink.dto.market.ExternalMedicinePriceDto.
 */
public class ExternalMedicinePriceDto extends com.medilink.dto.market.ExternalMedicinePriceDto {

    public ExternalMedicinePriceDto() {
        super();
    }

    public ExternalMedicinePriceDto(String brandName, String genericName, String manufacturer,
                                    String strength, String dosageForm, BigDecimal unitPrice,
                                    String currency, String source, Instant observedAt) {
        super(brandName, genericName, manufacturer, strength, dosageForm, unitPrice, currency, source, observedAt);
    }
}
