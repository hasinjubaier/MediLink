package com.medilink.dto.market;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Data Transfer Object representing a medicine price quote received
 * from the Apify MedEx Scraper or an approved external medicine REST API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalMedicinePriceDto {

    @JsonProperty("brand_name")
    @JsonAlias({"brandName", "medicineName", "name"})
    private String brandName;

    @JsonProperty("generic_name")
    @JsonAlias({"genericName", "generic"})
    private String genericName;

    @JsonProperty("manufacturer")
    @JsonAlias({"company", "manufacturerName"})
    private String manufacturer;

    @JsonProperty("strength")
    private String strength;

    @JsonProperty("dosage_form")
    @JsonAlias({"dosageForm", "form"})
    private String dosageForm;

    @JsonProperty("brand_id")
    @JsonAlias({"brandId", "id"})
    private String brandId;

    @JsonProperty("unit_price")
    @JsonAlias({"unitPrice", "price", "mrp", "pack_price"})
    private BigDecimal unitPrice;

    @JsonProperty("currency")
    private String currency = "BDT";

    @JsonProperty("source")
    private String source = "MedEx via Apify";

    @JsonProperty("url")
    @JsonAlias({"sourceUrl", "source_url", "productUrl"})
    private String sourceUrl;

    @JsonProperty("observedAt")
    @JsonAlias({"fetchedAt", "timestamp"})
    private Instant observedAt = Instant.now();

    public ExternalMedicinePriceDto() {}

    public ExternalMedicinePriceDto(String brandName, String genericName, String manufacturer,
                                    String strength, String dosageForm, BigDecimal unitPrice,
                                    String currency, String source, Instant observedAt) {
        this.brandName = brandName;
        this.genericName = genericName;
        this.manufacturer = manufacturer;
        this.strength = strength;
        this.dosageForm = dosageForm;
        this.unitPrice = unitPrice;
        this.currency = currency != null ? currency : "BDT";
        this.source = source != null ? source : "MedEx via Apify";
        this.observedAt = observedAt != null ? observedAt : Instant.now();
    }

    /**
     * Tolerant setter for unit price accommodating numeric or string values (e.g. "৳ 3.50", "3.50").
     */
    @JsonSetter("unit_price")
    public void setUnitPriceFromSnake(Object val) {
        this.unitPrice = parseBigDecimalSafe(val);
    }

    @JsonSetter("price")
    public void setPriceFromField(Object val) {
        if (this.unitPrice == null) {
            this.unitPrice = parseBigDecimalSafe(val);
        }
    }

    @JsonSetter("mrp")
    public void setMrpFromField(Object val) {
        if (this.unitPrice == null) {
            this.unitPrice = parseBigDecimalSafe(val);
        }
    }

    public static BigDecimal parseBigDecimalSafe(Object val) {
        if (val == null) return null;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        if (val instanceof Number) return BigDecimal.valueOf(((Number) val).doubleValue());
        String str = String.valueOf(val).trim().replaceAll("[^0-9.]", "");
        if (str.isEmpty()) return null;
        try {
            return new BigDecimal(str);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Validates that essential pricing attributes are present, finite, and strictly positive.
     */
    public boolean isValid() {
        return brandName != null && !brandName.trim().isEmpty()
                && unitPrice != null && unitPrice.compareTo(BigDecimal.ZERO) > 0;
    }

    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }

    public String getMedicineName() { return brandName; }
    public void setMedicineName(String medicineName) { this.brandName = medicineName; }

    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public String getCompany() { return manufacturer; }
    public void setCompany(String company) { this.manufacturer = company; }

    public String getStrength() { return strength; }
    public void setStrength(String strength) { this.strength = strength; }

    public String getDosageForm() { return dosageForm; }
    public void setDosageForm(String dosageForm) { this.dosageForm = dosageForm; }

    public String getBrandId() { return brandId; }
    public void setBrandId(String brandId) { this.brandId = brandId; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public Instant getObservedAt() { return observedAt; }
    public void setObservedAt(Instant observedAt) { this.observedAt = observedAt; }

    public Instant getFetchedAt() { return observedAt; }
    public void setFetchedAt(Instant fetchedAt) { this.observedAt = fetchedAt; }

    @Override
    public String toString() {
        return "ExternalMedicinePriceDto{" +
                "brandName='" + brandName + '\'' +
                ", genericName='" + genericName + '\'' +
                ", strength='" + strength + '\'' +
                ", dosageForm='" + dosageForm + '\'' +
                ", unitPrice=" + unitPrice + " " + currency +
                ", source='" + source + '\'' +
                '}';
    }
}
