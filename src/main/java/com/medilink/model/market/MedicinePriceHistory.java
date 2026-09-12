package com.medilink.model.market;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA Entity maintaining a persistent, auditable history of all medicine price fluctuations
 * across the Bangladesh pharmaceutical market.
 */
@Entity
@Table(name = "medicine_price_history")
public class MedicinePriceHistory {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "medicine_id", nullable = false, length = 50)
    private String medicineId;

    @Column(name = "brand_name", nullable = false, length = 100)
    private String brandName;

    @Column(name = "generic_name", length = 100)
    private String genericName;

    @Column(name = "manufacturer", length = 100)
    private String manufacturer;

    @Column(name = "old_price", nullable = false)
    private double oldPrice;

    @Column(name = "new_price", nullable = false)
    private double newPrice;

    @Column(name = "price_change", nullable = false)
    private double priceChange;

    @Column(name = "percentage_change", nullable = false)
    private double percentageChange;

    @Column(name = "direction", length = 20)
    private String direction; // INCREASED, DECREASED, UNCHANGED

    @Column(name = "source", length = 100)
    private String source;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    public MedicinePriceHistory() {
        this.timestamp = LocalDateTime.now();
    }

    public MedicinePriceHistory(String id, String medicineId, String brandName, String genericName,
                                String manufacturer, double oldPrice, double newPrice,
                                String source) {
        this.id = id;
        this.medicineId = medicineId;
        this.brandName = brandName;
        this.genericName = genericName;
        this.manufacturer = manufacturer;
        this.oldPrice = oldPrice;
        this.newPrice = newPrice;
        this.priceChange = Math.round((newPrice - oldPrice) * 100.0) / 100.0;
        if (oldPrice > 0) {
            this.percentageChange = Math.round(((newPrice - oldPrice) / oldPrice * 100.0) * 10.0) / 10.0;
        } else {
            this.percentageChange = 0.0;
        }
        if (newPrice > oldPrice) {
            this.direction = "INCREASED";
        } else if (newPrice < oldPrice) {
            this.direction = "DECREASED";
        } else {
            this.direction = "UNCHANGED";
        }
        this.source = source != null ? source : "DGDA Market Index";
        this.timestamp = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMedicineId() { return medicineId; }
    public void setMedicineId(String medicineId) { this.medicineId = medicineId; }

    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }

    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public double getOldPrice() { return oldPrice; }
    public void setOldPrice(double oldPrice) { this.oldPrice = oldPrice; }

    public double getNewPrice() { return newPrice; }
    public void setNewPrice(double newPrice) { this.newPrice = newPrice; }

    public double getPriceChange() { return priceChange; }
    public void setPriceChange(double priceChange) { this.priceChange = priceChange; }

    public double getPercentageChange() { return percentageChange; }
    public void setPercentageChange(double percentageChange) { this.percentageChange = percentageChange; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
