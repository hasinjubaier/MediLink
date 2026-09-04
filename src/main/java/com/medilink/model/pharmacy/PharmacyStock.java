package com.medilink.model.pharmacy;

import com.medilink.model.observer.Notifiable;
import com.medilink.model.observer.Subject;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * PharmacyStock model implementing Subject in the Observer Design Pattern.
 * Mapped as a JPA @Entity for persistence while keeping Observer capabilities.
 */
@Entity
@Table(name = "pharmacy_stocks")
public class PharmacyStock implements Subject {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "pharmacy_id", length = 50)
    private String pharmacyId;

    @Column(name = "pharmacy_name", length = 100)
    private String pharmacyName;

    @Column(name = "medicine_id", length = 50)
    private String medicineId;

    @Column(name = "medicine_brand_name", length = 100)
    private String medicineBrandName;

    @Column(name = "generic_name", length = 100)
    private String genericName;

    @Column(name = "quantity")
    private Integer quantity = 0;

    @Column(name = "unit_price")
    private Double unitPrice = 0.0;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    // Observers subscribed to stock updates (Transient - not persisted in DB)
    @Transient
    private final Set<Notifiable> observers = Collections.synchronizedSet(new HashSet<>());

    public PharmacyStock() {
        this.lastUpdated = LocalDateTime.now();
    }

    public PharmacyStock(String id, String pharmacyId, String pharmacyName, String medicineId,
                         String medicineBrandName, String genericName, Integer quantity, Double unitPrice) {
        this.id = id;
        this.pharmacyId = pharmacyId;
        this.pharmacyName = pharmacyName;
        this.medicineId = medicineId;
        this.medicineBrandName = medicineBrandName;
        this.genericName = genericName;
        this.quantity = quantity != null ? quantity : 0;
        this.unitPrice = unitPrice != null ? unitPrice : 0.0;
        this.lastUpdated = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPharmacyId() { return pharmacyId; }
    public void setPharmacyId(String pharmacyId) { this.pharmacyId = pharmacyId; }

    public String getPharmacyName() { return pharmacyName; }
    public void setPharmacyName(String pharmacyName) { this.pharmacyName = pharmacyName; }

    public String getMedicineId() { return medicineId; }
    public void setMedicineId(String medicineId) { this.medicineId = medicineId; }

    public String getMedicineBrandName() { return medicineBrandName; }
    public void setMedicineBrandName(String medicineBrandName) { this.medicineBrandName = medicineBrandName; }

    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }

    public int getQuantity() { return quantity != null ? quantity : 0; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public double getUnitPrice() { return unitPrice != null ? unitPrice : 0.0; }
    public void setUnitPrice(Double unitPrice) { this.unitPrice = unitPrice; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    public synchronized void updateQuantity(int newQuantity) {
        int oldQuantity = getQuantity();
        this.quantity = newQuantity;
        this.lastUpdated = LocalDateTime.now();

        // Notify observers via Observer pattern
        String updateSummary = "Stock updated for " + medicineBrandName + " at " + pharmacyName +
                               ": " + oldQuantity + " -> " + newQuantity + " units.";
        notifyObservers("STOCK_UPDATE", updateSummary);
    }

    @Override
    public void registerObserver(Notifiable observer) {
        if (observer != null) {
            observers.add(observer);
        }
    }

    @Override
    public void removeObserver(Notifiable observer) {
        if (observer != null) {
            observers.remove(observer);
        }
    }

    @Override
    public void notifyObservers(String eventType, Object payload) {
        synchronized (observers) {
            for (Notifiable observer : observers) {
                try {
                    observer.onNotification(eventType, payload);
                } catch (Exception e) {
                    System.err.println("Error notifying observer " + observer.getObserverId() + ": " + e.getMessage());
                }
            }
        }
    }
}
