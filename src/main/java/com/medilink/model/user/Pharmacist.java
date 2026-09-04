package com.medilink.model.user;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Concrete class representing a Pharmacist user.
 * Demonstrates Inheritance, Polymorphism, and JPA JOINED subclass mapping.
 */
@Entity
@Table(name = "pharmacists")
public class Pharmacist extends User {

    @Column(name = "pharmacy_id", length = 50)
    private String pharmacyId;

    @Column(name = "pharmacy_name", length = 100)
    private String pharmacyName;

    @Column(name = "license_number", length = 50)
    private String licenseNumber;

    public Pharmacist() {
        super();
        this.role = UserRole.PHARMACIST;
    }

    public Pharmacist(String id, String name, String email, String passwordHash, String pharmacyId, String pharmacyName, String licenseNumber) {
        super(id, name, email, passwordHash, UserRole.PHARMACIST);
        this.pharmacyId = pharmacyId;
        this.pharmacyName = pharmacyName;
        this.licenseNumber = licenseNumber;
    }

    public String getPharmacyId() { return pharmacyId; }
    public void setPharmacyId(String pharmacyId) { this.pharmacyId = pharmacyId; }

    public String getPharmacyName() { return pharmacyName; }
    public void setPharmacyName(String pharmacyName) { this.pharmacyName = pharmacyName; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    @Override
    public String getDashboardInfo() {
        return "Pharmacist Dashboard: Inventory Management, Real-time Stock Broadcaster, Live Patient Chat, Prescription Validation.";
    }

    @Override
    public Set<String> getPermissions() {
        return new HashSet<>(Arrays.asList(
            "UPDATE_STOCK",
            "BROADCAST_STOCK_CHANGE",
            "CHAT_PATIENT",
            "VERIFY_PRESCRIPTION",
            "VERIFY_FAKE_MEDICINE"
        ));
    }
}
