package com.medilink.model.prescription;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain entity for Prescription using the State Design Pattern for its lifecycle.
 * Mapped as a JPA @Entity for PostgreSQL persistence.
 */
@Entity
@Table(name = "prescriptions")
public class Prescription {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "patient_id", length = 50)
    private String patientId;

    @Column(name = "patient_name", length = 100, nullable = false)
    private String patientName;

    @Column(name = "doctor_name", length = 100, nullable = false)
    private String doctorName;

    @Column(name = "hospital", length = 150)
    private String hospitalOrClinic;

    @Column(name = "raw_scan_text", columnDefinition = "TEXT")
    private String rawScanText;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Column(name = "status", length = 50, nullable = false)
    private String status = "UPLOADED";

    @Column(name = "is_dispense_ready")
    private boolean isDispenseReady = false;

    @Column(name = "verified_by_pharmacist_id", length = 50)
    private String verifiedByPharmacistId;

    @Column(name = "voice_note_audio", columnDefinition = "TEXT")
    private String voiceNoteAudio;

    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PrescriptionItem> items = new ArrayList<>();

    @Transient
    @JsonIgnore
    private PrescriptionState state;

    public Prescription() {
        this.uploadedAt = LocalDateTime.now();
        this.state = new UploadedState();
        this.status = "UPLOADED";
    }

    public Prescription(String id, String patientId, String patientName, String doctorName,
                        String hospitalOrClinic, String rawScanText) {
        this.id = id;
        this.patientId = patientId;
        this.patientName = patientName;
        this.doctorName = doctorName;
        this.hospitalOrClinic = hospitalOrClinic;
        this.rawScanText = rawScanText;
        this.uploadedAt = LocalDateTime.now();
        this.state = new UploadedState();
        this.status = "UPLOADED";
        this.items = new ArrayList<>();
    }

    @PostLoad
    public void initTransientState() {
        if ("EXTRACTED".equalsIgnoreCase(status)) {
            this.state = new ExtractedState();
        } else if ("VERIFIED".equalsIgnoreCase(status)) {
            this.state = new VerifiedState();
        } else {
            this.state = new UploadedState();
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getHospitalOrClinic() { return hospitalOrClinic; }
    public void setHospitalOrClinic(String hospitalOrClinic) { this.hospitalOrClinic = hospitalOrClinic; }

    public String getRawScanText() { return rawScanText; }
    public void setRawScanText(String rawScanText) { this.rawScanText = rawScanText; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public PrescriptionState getState() {
        if (state == null) {
            initTransientState();
        }
        return state;
    }

    public void setState(PrescriptionState state) {
        this.state = state;
        if (state != null) {
            this.status = state.getStatusName();
            this.isDispenseReady = state.canDispense();
        }
    }

    public List<PrescriptionItem> getItems() { return items; }
    public void setItems(List<PrescriptionItem> items) {
        this.items.clear();
        if (items != null) {
            for (PrescriptionItem item : items) {
                addItem(item);
            }
        }
    }

    public String getVerifiedByPharmacistId() { return verifiedByPharmacistId; }
    public void setVerifiedByPharmacistId(String verifiedByPharmacistId) { this.verifiedByPharmacistId = verifiedByPharmacistId; }

    public String getVoiceNoteAudio() { return voiceNoteAudio; }
    public void setVoiceNoteAudio(String voiceNoteAudio) { this.voiceNoteAudio = voiceNoteAudio; }

    public void addItem(PrescriptionItem item) {
        if (item != null) {
            item.setPrescription(this);
            this.items.add(item);
        }
    }

    // State Pattern Transition delegations
    public void advanceWorkflow() {
        if (state == null) initTransientState();
        this.state.next(this);
    }

    public void revertWorkflow() {
        if (state == null) initTransientState();
        this.state.prev(this);
    }

    public String getStatus() {
        if (this.status != null) return this.status;
        return (this.state != null) ? this.state.getStatusName() : "UPLOADED";
    }

    public void setStatus(String status) {
        this.status = status;
        initTransientState();
    }

    public boolean isDispenseReady() {
        return this.isDispenseReady;
    }

    public void setDispenseReady(boolean dispenseReady) {
        this.isDispenseReady = dispenseReady;
    }
}
