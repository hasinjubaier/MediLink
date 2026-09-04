package com.medilink.model.medicine;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain entity representing a medicine product.
 * Mapped as a JPA @Entity for PostgreSQL persistence.
 */
@Entity
@Table(name = "medicines")
public class Medicine {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "brand_name", nullable = false, length = 100)
    private String brandName;

    @Column(name = "generic_name", nullable = false, length = 100)
    private String genericName;

    @Column(name = "manufacturer", length = 100)
    private String company;

    @Column(name = "strength", length = 50)
    private String strength;

    @Column(name = "dosage_form", length = 50)
    private String formulation;

    @Column(name = "unit_price", nullable = false)
    private double unitPrice;

    @Column(name = "is_prescription_required")
    private boolean prescriptionRequired;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "side_effects", columnDefinition = "TEXT")
    private String sideEffects;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "medicine_batches", joinColumns = @JoinColumn(name = "medicine_id"))
    @Column(name = "batch_code")
    private List<String> verifiedBatches = new ArrayList<>();

    public Medicine() {}

    public Medicine(String id, String brandName, String genericName, String company, String strength,
                    String formulation, double unitPrice, boolean prescriptionRequired,
                    String category, String sideEffects) {
        this.id = id;
        this.brandName = brandName;
        this.genericName = genericName;
        this.company = company;
        this.strength = strength;
        this.formulation = formulation;
        this.unitPrice = unitPrice;
        this.prescriptionRequired = prescriptionRequired;
        this.category = category;
        this.sideEffects = sideEffects;
        this.verifiedBatches = new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }

    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getStrength() { return strength; }
    public void setStrength(String strength) { this.strength = strength; }

    public String getFormulation() { return formulation; }
    public void setFormulation(String formulation) { this.formulation = formulation; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public boolean isPrescriptionRequired() { return prescriptionRequired; }
    public void setPrescriptionRequired(boolean prescriptionRequired) { this.prescriptionRequired = prescriptionRequired; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSideEffects() { return sideEffects; }
    public void setSideEffects(String sideEffects) { this.sideEffects = sideEffects; }

    public List<String> getVerifiedBatches() { return verifiedBatches; }
    public void setVerifiedBatches(List<String> verifiedBatches) { this.verifiedBatches = verifiedBatches; }

    public void addVerifiedBatch(String batchCode) {
        if (this.verifiedBatches == null) {
            this.verifiedBatches = new ArrayList<>();
        }
        this.verifiedBatches.add(batchCode);
    }

    public boolean isBatchValid(String batchCode) {
        if (batchCode == null || verifiedBatches == null) return false;
        return verifiedBatches.contains(batchCode.trim().toUpperCase());
    }

    /**
     * Method that can be decorated dynamically by decorators.
     */
    public String getDisplayBadge() {
        return "STANDARD";
    }

    public String getFullDescription() {
        return brandName + " (" + genericName + ") " + strength + " - " + company + " [BDT " + unitPrice + "]";
    }
}
