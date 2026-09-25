package com.medilink.dto.prescription;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

/**
 * Raw medicine item parsed directly from AI engine response before reconciliation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RawMedicineItem {

    private String medicineName;
    private String strength;
    private String dosageAmount;
    private String dosageUnit;
    private String frequency;
    private List<String> timing = new ArrayList<>();
    private List<String> exactTimes = new ArrayList<>();
    private String mealRelation;
    private String duration;
    private String totalQuantity;
    private String instructions;
    private Boolean isUncertain;

    private Double confidence;

    public RawMedicineItem() {}

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

    @com.fasterxml.jackson.annotation.JsonSetter("dosageAmount")
    public void setDosageAmountFromNode(com.fasterxml.jackson.databind.JsonNode node) {
        if (node == null || node.isNull()) {
            this.dosageAmount = null;
        } else {
            this.dosageAmount = node.isTextual() ? node.asText() : node.toString();
        }
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

    @com.fasterxml.jackson.annotation.JsonSetter("timing")
    public void setTimingFromNode(com.fasterxml.jackson.databind.JsonNode node) {
        this.timing = new ArrayList<String>();
        if (node == null || node.isNull()) return;
        if (node.isArray()) {
            for (com.fasterxml.jackson.databind.JsonNode item : node) {
                if (!item.isNull() && !item.asText().trim().isEmpty()) {
                    this.timing.add(item.asText().trim());
                }
            }
        } else if (node.isTextual()) {
            String txt = node.asText().trim();
            if (!txt.isEmpty()) {
                String[] parts = txt.split("[,;+]");
                for (String p : parts) {
                    if (!p.trim().isEmpty()) this.timing.add(p.trim());
                }
            }
        }
    }

    public void setTiming(List<String> timing) {
        this.timing = timing != null ? timing : new ArrayList<String>();
    }

    public List<String> getExactTimes() {
        return exactTimes;
    }

    @com.fasterxml.jackson.annotation.JsonSetter("exactTimes")
    public void setExactTimesFromNode(com.fasterxml.jackson.databind.JsonNode node) {
        this.exactTimes = new ArrayList<String>();
        if (node == null || node.isNull()) return;
        if (node.isArray()) {
            for (com.fasterxml.jackson.databind.JsonNode item : node) {
                if (!item.isNull() && !item.asText().trim().isEmpty()) {
                    this.exactTimes.add(item.asText().trim());
                }
            }
        } else if (node.isTextual()) {
            String txt = node.asText().trim();
            if (!txt.isEmpty()) {
                String[] parts = txt.split("[,;+]");
                for (String p : parts) {
                    if (!p.trim().isEmpty()) this.exactTimes.add(p.trim());
                }
            }
        }
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

    @com.fasterxml.jackson.annotation.JsonSetter("totalQuantity")
    public void setTotalQuantityFromNode(com.fasterxml.jackson.databind.JsonNode node) {
        if (node == null || node.isNull()) {
            this.totalQuantity = null;
        } else {
            this.totalQuantity = node.isTextual() ? node.asText() : node.toString();
        }
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

    public Boolean getIsUncertain() {
        return isUncertain;
    }

    public void setIsUncertain(Boolean isUncertain) {
        this.isUncertain = isUncertain;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
}
