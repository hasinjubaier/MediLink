package com.medilink.dto.prescription;

import java.util.*;

/**
 * Normalized outcome of dual-engine AI prescription vision scan and reconciliation.
 */
public class PrescriptionScanResult {

    private String patientName;
    private String doctorName;
    private String hospitalName;
    private String rawText;
    private double overallConfidence = 0.95;
    private boolean needsVerification = false;
    private String verificationStatus; // VERIFIED_BY_BOTH, GROQ_UNAVAILABLE, GEMINI_UNAVAILABLE, CONFLICTS_DETECTED, UNVERIFIED
    private String statusMessage;
    private List<ScannedMedicineItem> medicines = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private Map<String, Object> sourceAgreement = new HashMap<>();
    private Map<String, List<String>> fieldAlternatives = new HashMap<>();

    // Logged-in user context
    private String loggedInPatientName;
    private String loggedInPatientId;

    public PrescriptionScanResult() {}

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

    public double getOverallConfidence() {
        return overallConfidence;
    }

    public void setOverallConfidence(double overallConfidence) {
        this.overallConfidence = overallConfidence;
    }

    public boolean isNeedsVerification() {
        return needsVerification;
    }

    public void setNeedsVerification(boolean needsVerification) {
        this.needsVerification = needsVerification;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public List<ScannedMedicineItem> getMedicines() {
        return medicines;
    }

    public void setMedicines(List<ScannedMedicineItem> medicines) {
        this.medicines = medicines != null ? medicines : new ArrayList<ScannedMedicineItem>();
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings != null ? warnings : new ArrayList<String>();
    }

    public Map<String, Object> getSourceAgreement() {
        return sourceAgreement;
    }

    public void setSourceAgreement(Map<String, Object> sourceAgreement) {
        this.sourceAgreement = sourceAgreement != null ? sourceAgreement : new HashMap<String, Object>();
    }

    public Map<String, List<String>> getFieldAlternatives() {
        return fieldAlternatives;
    }

    public void setFieldAlternatives(Map<String, List<String>> fieldAlternatives) {
        this.fieldAlternatives = fieldAlternatives != null ? fieldAlternatives : new HashMap<String, List<String>>();
    }

    public String getLoggedInPatientName() {
        return loggedInPatientName;
    }

    public void setLoggedInPatientName(String loggedInPatientName) {
        this.loggedInPatientName = loggedInPatientName;
    }

    public String getLoggedInPatientId() {
        return loggedInPatientId;
    }

    public void setLoggedInPatientId(String loggedInPatientId) {
        this.loggedInPatientId = loggedInPatientId;
    }
}
