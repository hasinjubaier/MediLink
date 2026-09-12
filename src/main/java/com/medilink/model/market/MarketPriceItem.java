package com.medilink.model.market;

/**
 * Standardized data transfer representation of a medicine price quote
 * from Bangladesh pharmaceutical market registries (DGDA, Medex, Distributors).
 */
public class MarketPriceItem {

    private String brandName;
    private String genericName;
    private String manufacturer;
    private String strength;
    private String dosageForm;
    private double mrp; // Maximum Retail Price in BDT (৳)
    private String currency = "BDT";
    private String effectiveDate;
    private String sourceRegistry;

    public MarketPriceItem() {}

    public MarketPriceItem(String brandName, String genericName, String manufacturer,
                           String strength, String dosageForm, double mrp,
                           String effectiveDate, String sourceRegistry) {
        this.brandName = brandName;
        this.genericName = genericName;
        this.manufacturer = manufacturer;
        this.strength = strength;
        this.dosageForm = dosageForm;
        this.mrp = mrp;
        this.currency = "BDT";
        this.effectiveDate = effectiveDate;
        this.sourceRegistry = sourceRegistry;
    }

    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }

    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public String getStrength() { return strength; }
    public void setStrength(String strength) { this.strength = strength; }

    public String getDosageForm() { return dosageForm; }
    public void setDosageForm(String dosageForm) { this.dosageForm = dosageForm; }

    public double getMrp() { return mrp; }
    public void setMrp(double mrp) { this.mrp = mrp; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(String effectiveDate) { this.effectiveDate = effectiveDate; }

    public String getSourceRegistry() { return sourceRegistry; }
    public void setSourceRegistry(String sourceRegistry) { this.sourceRegistry = sourceRegistry; }

    @Override
    public String toString() {
        return "MarketPriceItem{" +
                "brandName='" + brandName + '\'' +
                ", genericName='" + genericName + '\'' +
                ", manufacturer='" + manufacturer + '\'' +
                ", mrp=" + mrp + " BDT" +
                ", sourceRegistry='" + sourceRegistry + '\'' +
                '}';
    }
}
