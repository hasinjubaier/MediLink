package com.medilink.model.strategy;

import java.util.List;

/**
 * Concrete Strategy: Clinical strict multi-parameter interaction analysis.
 */
public class ClinicalStrictStrategy implements InteractionCheckStrategy {

    @Override
    public String evaluateRisk(List<String> genericNames) {
        if (genericNames == null || genericNames.size() < 2) {
            return "CLINICAL AUDIT: Mono-therapy verified. No active contraindications.";
        }

        StringBuilder report = new StringBuilder("CLINICAL AUDIT REPORT (STRICT):\n");
        boolean hasConflict = false;

        for (int i = 0; i < genericNames.size(); i++) {
            for (int j = i + 1; j < genericNames.size(); j++) {
                String medA = genericNames.get(i).toLowerCase();
                String medB = genericNames.get(j).toLowerCase();

                if ((medA.contains("omeprazole") || medA.contains("esomeprazole")) && medB.contains("clopidogrel")) {
                    report.append("- [CYP2C19 INHIBITION] PPI (Omeprazole) attenuates antiplatelet efficacy of Clopidogrel.\n");
                    hasConflict = true;
                }
                if (medA.contains("azithromycin") && (medB.contains("ciprofloxacin") || medB.contains("erythromycin"))) {
                    report.append("- [QT PROLONGATION RISK] Co-administration of macrolides & fluoroquinolones elevates cardiac arrhythmia risk.\n");
                    hasConflict = true;
                }
            }
        }

        if (!hasConflict) {
            report.append("- No high-tier clinical enzyme conflicts or pharmacokinetic contraindications detected.");
        }
        return report.toString();
    }

    @Override
    public String getStrategyLevel() {
        return "CLINICAL_STRICT_MODE";
    }
}
