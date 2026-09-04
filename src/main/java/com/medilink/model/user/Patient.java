package com.medilink.model.user;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Concrete class representing a Patient user.
 * Demonstrates Inheritance, Polymorphism, and JPA JOINED subclass mapping.
 */
@Entity
@Table(name = "patients")
public class Patient extends User {

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "emergency_contact", length = 100)
    private String emergencyContact;

    @Column(name = "date_of_birth", length = 50)
    private String dateOfBirth;

    @Column(name = "gender", length = 30)
    private String gender;

    @Column(name = "blood_type", length = 30)
    private String bloodType;

    @Column(name = "allergies", length = 255)
    private String allergies;

    @Column(name = "chronic_conditions", length = 255)
    private String chronicConditions;

    @Column(name = "emergency_contacts_json", columnDefinition = "TEXT")
    private String emergencyContactsJson;

    public Patient() {
        super();
        this.role = UserRole.PATIENT;
    }

    public Patient(String id, String name, String email, String passwordHash, String phone, String address, String emergencyContact) {
        super(id, name, email, passwordHash, UserRole.PATIENT, phone);
        this.address = address;
        this.emergencyContact = emergencyContact != null ? emergencyContact : phone;
    }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getEmergencyContact() { return emergencyContact; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }

    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getBloodType() { return bloodType; }
    public void setBloodType(String bloodType) { this.bloodType = bloodType; }

    public String getAllergies() { return allergies; }
    public void setAllergies(String allergies) { this.allergies = allergies; }

    public String getChronicConditions() { return chronicConditions; }
    public void setChronicConditions(String chronicConditions) { this.chronicConditions = chronicConditions; }

    public String getEmergencyContactsJson() { return emergencyContactsJson; }
    public void setEmergencyContactsJson(String emergencyContactsJson) { this.emergencyContactsJson = emergencyContactsJson; }

    @Override
    public String getDashboardInfo() {
        return "Patient Dashboard: Prescriptions, Medicine Finder, Stock Alerts, Emergency Support.";
    }

    @Override
    public Set<String> getPermissions() {
        return new HashSet<>(Arrays.asList(
            "SEARCH_MEDICINE",
            "FIND_ALTERNATIVES",
            "SCAN_PRESCRIPTION",
            "LOCATE_PHARMACY",
            "CHAT_PHARMACIST",
            "SET_REMINDERS",
            "VERIFY_FAKE_MEDICINE"
        ));
    }
}
