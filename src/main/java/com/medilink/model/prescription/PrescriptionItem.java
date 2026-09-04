package com.medilink.model.prescription;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.*;

/**
 * Single medicine item line extracted from a prescription.
 * Mapped as a JPA @Entity.
 */
@Entity
@Table(name = "prescription_items")
public class PrescriptionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id")
    private Prescription prescription;

    @Column(name = "medicine_id", length = 50)
    private String medicineId;

    @Column(name = "medicine_name", length = 100, nullable = false)
    private String medicineName;

    @Column(name = "generic_name", length = 100)
    private String genericName;

    @Column(name = "dosage", length = 50)
    private String dosage;

    @Column(name = "frequency", length = 50)
    private String frequency;

    @Column(name = "duration", length = 50)
    private String duration;

    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;

    public PrescriptionItem() {}

    public PrescriptionItem(String medicineId, String medicineName, String genericName,
                            String dosage, String frequency, String duration, String instructions) {
        this.medicineId = medicineId;
        this.medicineName = medicineName;
        this.genericName = genericName;
        this.dosage = dosage;
        this.frequency = frequency;
        this.duration = duration;
        this.instructions = instructions;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Prescription getPrescription() { return prescription; }
    public void setPrescription(Prescription prescription) { this.prescription = prescription; }

    public String getMedicineId() { return medicineId; }
    public void setMedicineId(String medicineId) { this.medicineId = medicineId; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
}
