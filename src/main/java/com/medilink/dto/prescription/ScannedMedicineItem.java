package com.medilink.dto.prescription;

import java.util.*;

/**
 * Normalized medicine item extracted from a prescription with dosage,
 * frequency, timing, instructions, and confidence/uncertainty tracking.
 */
public class ScannedMedicineItem {

    private String medicineName;
    private String strength;
    private String dosageAmount; // e.g. "1 tablet"
    private String dosageUnit;   // e.g. "tablet"
    private String frequency;    // e.g. "1+0+1", "2 times daily"
    private List<String> timing = new ArrayList<>(); // e.g. ["Morning", "Night"]
    private List<String> exactTimes = new ArrayList<>(); // e.g. ["08:00 AM", "08:30 PM"]
    private String mealRelation; // "Before meal", "After meal", "With meal"
    private String duration;     // e.g. "7 days"
    private String totalQuantity;// e.g. "14 tablets"
    private String instructions; // e.g. "Take with warm water"
    private double confidence = 0.95;
    private List<String> uncertainFields = new ArrayList<>();
    private Map<String, List<String>> alternatives = new HashMap<>();

    public ScannedMedicineItem() {}

    public ScannedMedicineItem(String medicineName, String strength, String dosageAmount,
                               String frequency, String mealRelation, String duration, String instructions) {
        this.medicineName = medicineName;
        this.strength = strength;
        this.dosageAmount = dosageAmount;
        this.frequency = frequency;
        this.mealRelation = mealRelation;
        this.duration = duration;
        this.instructions = instructions;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public void setMedicineName(String medicineName) {
        this.medicineName = medicineName;
    }

    public String getStrength() {
        return strength;
    }

    public void setStrength(String strength) {
        this.strength = strength;
    }

    public String getDosageAmount() {
        return dosageAmount;
    }

    public void setDosageAmount(String dosageAmount) {
        this.dosageAmount = dosageAmount;
    }

    public String getDosageUnit() {
        return dosageUnit;
    }

    public void setDosageUnit(String dosageUnit) {
        this.dosageUnit = dosageUnit;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public List<String> getTiming() {
        return timing;
    }

    public void setTiming(List<String> timing) {
        this.timing = timing != null ? timing : new ArrayList<String>();
    }

    public List<String> getExactTimes() {
        return exactTimes;
    }

    public void setExactTimes(List<String> exactTimes) {
        this.exactTimes = exactTimes != null ? exactTimes : new ArrayList<String>();
    }

    public String getMealRelation() {
        return mealRelation;
    }

    public void setMealRelation(String mealRelation) {
        this.mealRelation = mealRelation;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(String totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public List<String> getUncertainFields() {
        return uncertainFields;
    }

    public void setUncertainFields(List<String> uncertainFields) {
        this.uncertainFields = uncertainFields != null ? uncertainFields : new ArrayList<String>();
    }

    public Map<String, List<String>> getAlternatives() {
        return alternatives;
    }

    public void setAlternatives(Map<String, List<String>> alternatives) {
        this.alternatives = alternatives != null ? alternatives : new HashMap<String, List<String>>();
    }
}
