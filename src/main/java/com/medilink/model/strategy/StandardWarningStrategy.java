package com.medilink.model.strategy;

import java.util.List;

/**
 * Concrete Strategy: Standard Patient-Friendly Interaction Warnings.
 */
public class StandardWarningStrategy implements InteractionCheckStrategy {

    @Override
    public String evaluateRisk(List<String> genericNames) {
        if (genericNames == null || genericNames.size() < 2) {
            return "SAFE: Single medicine or no combinations detected.";
        }

        boolean hasParacetamol = false;
        boolean hasNSAID = false; // Aspirin / Ibuprofen / Naproxen
        boolean hasCipro = false;
        boolean hasAntacid = false;
        boolean hasWarfarin = false;

        for (String name : genericNames) {
            String lower = name.toLowerCase();
            if (lower.contains("paracetamol")) hasParacetamol = true;
            if (lower.contains("aspirin") || lower.contains("naproxen") || lower.contains("ibuprofen")) hasNSAID = true;
            if (lower.contains("ciprofloxacin")) hasCipro = true;
            if (lower.contains("antacid") || lower.contains("magnesium") || lower.contains("aluminum")) hasAntacid = true;
            if (lower.contains("warfarin")) hasWarfarin = true;
        }

        if (hasWarfarin && (hasNSAID || hasParacetamol)) {
            return "CRITICAL WARNING: Blood-thinner interaction! Warfarin combined with NSAIDs/Paracetamol elevates severe internal bleeding risk. Consult doctor immediately.";
        }
        if (hasCipro && hasAntacid) {
            return "MODERATE WARNING: Antacids significantly decrease absorption of Ciprofloxacin. Space doses by at least 2 hours.";
        }
        if (hasParacetamol && hasNSAID) {
            return "MILD CAUTION: Multiple pain relievers combined. Monitor dosage carefully to prevent liver/kidney strain.";
        }

        return "LOW RISK: No severe known cross-reactions found in standard database.";
    }

    @Override
    public String getStrategyLevel() {
        return "STANDARD_PATIENT_MODE";
    }
}
