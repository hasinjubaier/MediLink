package com.medilink.dto.prescription;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

/**
 * Raw prescription payload as extracted directly from an individual vision model.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RawExtractedPrescription {

    private String patientName;
    private String doctorName;
    private String hospitalName;
    private String rawText = "";
    private List<RawMedicineItem> medicines = new ArrayList<>();
    private Boolean isPrescription = true;
    private String confidenceNotes;

    public RawExtractedPrescription() {}

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public List<RawMedicineItem> getMedicines() {
        return medicines;
    }

    public void setMedicines(List<RawMedicineItem> medicines) {
        this.medicines = medicines != null ? medicines : new ArrayList<RawMedicineItem>();
    }

    public Boolean getIsPrescription() {
        return isPrescription != null ? isPrescription : true;
    }

    public void setIsPrescription(Boolean isPrescription) {
        this.isPrescription = isPrescription;
    }

    public String getConfidenceNotes() {
        return confidenceNotes;
    }

    public void setConfidenceNotes(String confidenceNotes) {
        this.confidenceNotes = confidenceNotes;
    }
}
