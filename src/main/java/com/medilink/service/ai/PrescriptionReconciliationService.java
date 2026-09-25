package com.medilink.service.ai;

import com.medilink.dto.prescription.PrescriptionScanResult;
import com.medilink.dto.prescription.RawExtractedPrescription;
import com.medilink.dto.prescription.RawMedicineItem;
import com.medilink.dto.prescription.ScannedMedicineItem;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Reconciles independent extraction outputs from Google Gemini and Groq AI engines.
 * Resolves agreements, flags conflicting dosages/potencies/timings, and records alternatives.
 */
@Service
public class PrescriptionReconciliationService {

    public PrescriptionScanResult reconcile(RawExtractedPrescription gemini,
                                            RawExtractedPrescription groq,
                                            boolean geminiAvailable,
                                            boolean groqAvailable,
                                            String loggedInPatientId,
                                            String loggedInPatientName) {

        PrescriptionScanResult result = new PrescriptionScanResult();
        result.setLoggedInPatientId(loggedInPatientId);
        result.setLoggedInPatientName(loggedInPatientName);

        Map<String, Object> sourceAgreement = new HashMap<>();
        sourceAgreement.put("geminiAvailable", geminiAvailable);
        sourceAgreement.put("groqAvailable", groqAvailable);
        result.setSourceAgreement(sourceAgreement);

        // Case A: Both engines available and succeeded
        if (geminiAvailable && groqAvailable && gemini != null && groq != null) {
            return reconcileBoth(gemini, groq, result, sourceAgreement);
        }

        // Case B: Only Gemini available
        if (geminiAvailable && gemini != null) {
            return populateSingleSource(gemini, result, "GROQ_UNAVAILABLE",
                "Extracted by Gemini (Groq cross-verification was unavailable).", sourceAgreement);
        }

        // Case C: Only Groq available
        if (groqAvailable && groq != null) {
            return populateSingleSource(groq, result, "GEMINI_UNAVAILABLE",
                "Extracted by Groq (Gemini primary engine was unavailable).", sourceAgreement);
        }

        // Case D: Neither available
        result.setVerificationStatus("UNVERIFIED");
        result.setNeedsVerification(true);
        result.setOverallConfidence(0.0);
        result.setStatusMessage("Unable to extract prescription data from AI vision services.");
        return result;
    }

    private PrescriptionScanResult reconcileBoth(RawExtractedPrescription gemini,
                                                 RawExtractedPrescription groq,
                                                 PrescriptionScanResult result,
                                                 Map<String, Object> sourceAgreement) {

        boolean conflictsDetected = false;

        // 1. Reconcile Patient Name
        String reconciledPatient = reconcileField("Patient Name", gemini.getPatientName(), groq.getPatientName(), result);
        result.setPatientName(reconciledPatient);
        if ("Uncertain".equalsIgnoreCase(reconciledPatient)) {
            conflictsDetected = true;
        }

        // 2. Reconcile Doctor Name
        String reconciledDoctor = reconcileField("Doctor Name", gemini.getDoctorName(), groq.getDoctorName(), result);
        result.setDoctorName(reconciledDoctor);
        if ("Uncertain".equalsIgnoreCase(reconciledDoctor)) {
            conflictsDetected = true;
        }

        // 3. Reconcile Hospital / Clinic
        String reconciledHospital = reconcileField("Hospital Name", gemini.getHospitalName(), groq.getHospitalName(), result);
        result.setHospitalName(reconciledHospital);
        if ("Uncertain".equalsIgnoreCase(reconciledHospital)) {
            conflictsDetected = true;
        }

        // 4. Reconcile Raw Text (prioritize Gemini, supplement if needed)
        String rawText = gemini.getRawText();
        if (rawText == null || rawText.trim().isEmpty()) {
            rawText = groq.getRawText();
        }
        result.setRawText(rawText != null ? rawText.trim() : "");

        // 5. Reconcile Medicine Items
        List<RawMedicineItem> geminiMeds = gemini.getMedicines() != null ? gemini.getMedicines() : Collections.<RawMedicineItem>emptyList();
        List<RawMedicineItem> groqMeds = groq.getMedicines() != null ? groq.getMedicines() : Collections.<RawMedicineItem>emptyList();

        List<ScannedMedicineItem> reconciledMedicines = new ArrayList<>();
        Set<Integer> matchedGroqIndices = new HashSet<>();

        for (RawMedicineItem gMed : geminiMeds) {
            int bestMatchIdx = findBestMedicineMatch(gMed, groqMeds, matchedGroqIndices);
            if (bestMatchIdx != -1) {
                matchedGroqIndices.add(bestMatchIdx);
                RawMedicineItem qMed = groqMeds.get(bestMatchIdx);

                ScannedMedicineItem item = reconcileMedicinePair(gMed, qMed);
                if (item.getUncertainFields() != null && !item.getUncertainFields().isEmpty()) {
                    conflictsDetected = true;
                }
                reconciledMedicines.add(item);
            } else {
                // Detected by Gemini only
                ScannedMedicineItem item = convertRawToScanned(gMed);
                item.setConfidence(0.80);
                item.getUncertainFields().add("unverifiedByGroq");
                Map<String, List<String>> alt = item.getAlternatives();
                alt.put("source", Collections.singletonList("Detected by Gemini only"));
                item.setAlternatives(alt);
                reconciledMedicines.add(item);
            }
        }

        // Add remaining Groq medicines not matched to Gemini
        for (int i = 0; i < groqMeds.size(); i++) {
            if (!matchedGroqIndices.contains(i)) {
                RawMedicineItem qMed = groqMeds.get(i);
                ScannedMedicineItem item = convertRawToScanned(qMed);
                item.setConfidence(0.80);
                item.getUncertainFields().add("unverifiedByGemini");
                Map<String, List<String>> alt = item.getAlternatives();
                alt.put("source", Collections.singletonList("Detected by Groq only"));
                item.setAlternatives(alt);
                reconciledMedicines.add(item);
            }
        }

        result.setMedicines(reconciledMedicines);
        sourceAgreement.put("conflictsDetected", conflictsDetected);

        if (conflictsDetected) {
            result.setVerificationStatus("CONFLICTS_DETECTED");
            result.setNeedsVerification(true);
            result.setOverallConfidence(0.70);
            result.setStatusMessage("Discrepancies detected between Gemini and Groq extractions. Please review highlighted uncertain fields.");
            result.getWarnings().add("One or more medication dosages or details differ between vision models. Marked as Uncertain.");
        } else {
            result.setVerificationStatus("VERIFIED_BY_BOTH");
            result.setNeedsVerification(false);
            result.setOverallConfidence(0.96);
            result.setStatusMessage("All extracted prescription items verified and agreed by both Gemini and Groq.");
        }

        System.out.println("[AI Vision] Prescription reconciliation completed: " +
            reconciledMedicines.size() + " medication(s). Status=" + result.getVerificationStatus());

        return result;
    }

    private ScannedMedicineItem reconcileMedicinePair(RawMedicineItem g, RawMedicineItem q) {
        ScannedMedicineItem item = new ScannedMedicineItem();

        // 1. Medicine Name
        String name = choosePreferredString(g.getMedicineName(), q.getMedicineName());
        item.setMedicineName(name);

        // 2. Strength
        String strength = reconcileMedField("strength", g.getStrength(), q.getStrength(), item);
        item.setStrength(strength);

        // 3. Dosage Amount
        String dose = reconcileMedField("dosageAmount", g.getDosageAmount(), q.getDosageAmount(), item);
        item.setDosageAmount(dose);

        // 4. Dosage Unit
        item.setDosageUnit(choosePreferredString(g.getDosageUnit(), q.getDosageUnit()));

        // 5. Frequency
        String freq = reconcileMedField("frequency", g.getFrequency(), q.getFrequency(), item);
        item.setFrequency(freq);

        // 6. Timing & Exact Times
        Set<String> timings = new LinkedHashSet<>();
        if (g.getTiming() != null) {
            for (String t : g.getTiming()) {
                if (t != null && !t.trim().isEmpty() && !t.trim().equalsIgnoreCase("Uncertain")) {
                    timings.add(t.trim());
                }
            }
        }
        if (q.getTiming() != null) {
            for (String t : q.getTiming()) {
                if (t != null && !t.trim().isEmpty() && !t.trim().equalsIgnoreCase("Uncertain")) {
                    timings.add(t.trim());
                }
            }
        }
        item.setTiming(new ArrayList<>(timings));

        Set<String> times = new LinkedHashSet<>();
        if (g.getExactTimes() != null) {
            for (String ex : g.getExactTimes()) {
                if (ex != null && !ex.trim().isEmpty() && !ex.trim().equalsIgnoreCase("Uncertain")) {
                    times.add(ex.trim());
                }
            }
        }
        if (q.getExactTimes() != null) {
            for (String ex : q.getExactTimes()) {
                if (ex != null && !ex.trim().isEmpty() && !ex.trim().equalsIgnoreCase("Uncertain")) {
                    times.add(ex.trim());
                }
            }
        }
        item.setExactTimes(new ArrayList<>(times));

        // 7. Meal Relation
        String meal = reconcileMedField("mealRelation", g.getMealRelation(), q.getMealRelation(), item);
        item.setMealRelation(meal);

        // 8. Duration
        String dur = reconcileMedField("duration", g.getDuration(), q.getDuration(), item);
        item.setDuration(dur);

        // 9. Total Quantity
        String qty = reconcileMedField("totalQuantity", g.getTotalQuantity(), q.getTotalQuantity(), item);
        item.setTotalQuantity(qty);

        // 10. Instructions
        item.setInstructions(choosePreferredString(g.getInstructions(), q.getInstructions()));

        // Confidence calculation
        int uncertainties = item.getUncertainFields().size();
        if (uncertainties == 0) {
            item.setConfidence(0.96);
        } else if (uncertainties == 1) {
            item.setConfidence(0.72);
        } else {
            item.setConfidence(0.50);
        }

        return item;
    }

    private String reconcileMedField(String fieldName, String valG, String valQ, ScannedMedicineItem item) {
        String cleanG = clean(valG);
        String cleanQ = clean(valQ);

        if ("uncertain".equalsIgnoreCase(cleanG)) cleanG = "";
        if ("uncertain".equalsIgnoreCase(cleanQ)) cleanQ = "";

        if (cleanG.isEmpty() && cleanQ.isEmpty()) {
            return null;
        }
        if (!cleanG.isEmpty() && cleanQ.isEmpty()) {
            return valG;
        }
        if (cleanG.isEmpty() && !cleanQ.isEmpty()) {
            return valQ;
        }

        // Both provided: compare normalized
        if (areValuesEquivalent(cleanG, cleanQ)) {
            return valG.length() >= valQ.length() ? valG : valQ;
        }

        // Conflict detected!
        item.getUncertainFields().add(fieldName);
        List<String> alts = Arrays.asList(valG + " (Gemini)", valQ + " (Groq)");
        item.getAlternatives().put(fieldName, alts);
        return "Uncertain";
    }

    private String reconcileField(String fieldLabel, String valG, String valQ, PrescriptionScanResult result) {
        if ("Patient Name".equals(fieldLabel)) {
            String pG = cleanDemographics(valG);
            String pQ = cleanDemographics(valQ);
            if (pG.isEmpty() && pQ.isEmpty()) return null;
            if (!pG.isEmpty() && pQ.isEmpty()) return pG;
            if (pG.isEmpty() && !pQ.isEmpty()) return pQ;
            if (areValuesEquivalent(pG, pQ)) {
                return pG.length() >= pQ.length() ? pG : pQ;
            }
            String normPG = pG.toLowerCase().replaceAll("[^a-z0-9]", "");
            String normPQ = pQ.toLowerCase().replaceAll("[^a-z0-9]", "");
            if (normPG.contains(normPQ) || normPQ.contains(normPG)) {
                return normPG.length() <= normPQ.length() ? pG : pQ;
            }
            result.getFieldAlternatives().put(fieldLabel, Arrays.asList(pG + " (Gemini)", pQ + " (Groq)"));
            result.getWarnings().add("Note: Alternate Patient Name detected: '" + pG + "' vs '" + pQ + "'");
            return pG;
        }

        if ("Doctor Name".equals(fieldLabel)) {
            String dG = clean(valG);
            String dQ = clean(valQ);
            if (dG.isEmpty() && dQ.isEmpty()) return null;
            if (!dG.isEmpty() && dQ.isEmpty()) return valG;
            if (dG.isEmpty() && !dQ.isEmpty()) return valQ;
            if (areValuesEquivalent(dG, dQ)) {
                return dG.length() >= dQ.length() ? valG : valQ;
            }
            String normDG = dG.toLowerCase().replaceAll("[^a-z0-9]", "");
            String normDQ = dQ.toLowerCase().replaceAll("[^a-z0-9]", "");
            if (normDG.contains(normDQ) || normDQ.contains(normDG)) {
                return valG.toLowerCase().contains("dr.") ? valG : (valQ.toLowerCase().contains("dr.") ? valQ : valG);
            }
            result.getFieldAlternatives().put(fieldLabel, Arrays.asList(valG + " (Gemini)", valQ + " (Groq)"));
            result.getWarnings().add("Note: Alternate Doctor Name detected: '" + valG + "' vs '" + valQ + "'");
            if (valG.toLowerCase().contains("dr.") || valG.toLowerCase().contains("doctor")) return valG;
            if (valQ.toLowerCase().contains("dr.") || valQ.toLowerCase().contains("doctor")) return valQ;
            return valG;
        }

        if ("Hospital Name".equals(fieldLabel)) {
            String hG = clean(valG);
            String hQ = clean(valQ);
            if (hG.isEmpty() && hQ.isEmpty()) return null;
            if (!hG.isEmpty() && hQ.isEmpty()) return valG;
            if (hG.isEmpty() && !hQ.isEmpty()) return valQ;
            if (areValuesEquivalent(hG, hQ)) {
                return hG.length() >= hQ.length() ? valG : valQ;
            }
            String normHG = hG.toLowerCase().replaceAll("[^a-z0-9]", "");
            String normHQ = hQ.toLowerCase().replaceAll("[^a-z0-9]", "");
            if (normHG.contains(normHQ) || normHQ.contains(normHG)) {
                return hG.length() >= hQ.length() ? valG : valQ;
            }
            result.getFieldAlternatives().put(fieldLabel, Arrays.asList(valG + " (Gemini)", valQ + " (Groq)"));
            return valG;
        }

        // Conflict detected in top-level field
        result.getFieldAlternatives().put(fieldLabel, Arrays.asList(valG + " (Gemini)", valQ + " (Groq)"));
        result.getWarnings().add("Conflicting " + fieldLabel + " detected between models: '" + valG + "' vs '" + valQ + "'");
        return "Uncertain";
    }

    private String cleanDemographics(String name) {
        if (name == null) return "";
        return name.replaceAll("(?i)\\s*\\(\\s*\\d+\\s*/?\\s*[MFmf]?\\s*\\)", "")
                   .replaceAll("(?i)\\s*\\(\\s*[MFmf]\\s*/?\\s*\\d*\\s*\\)", "")
                   .replaceAll("(?i)\\s*\\(\\s*Age\\s*:\\s*\\d+.*\\)", "")
                   .replaceAll("(?i)\\s*\\bAge\\s*:\\s*\\d+.*", "")
                   .trim();
    }

    private boolean areValuesEquivalent(String a, String b) {
        String normA = a.toLowerCase().replaceAll("[^a-z0-9]", "");
        String normB = b.toLowerCase().replaceAll("[^a-z0-9]", "");

        if (normA.equals(normB)) return true;

        // Compare digits: if numeric digits differ, dosages/quantities are conflicting!
        String digitsA = a.replaceAll("[^0-9]", "");
        String digitsB = b.replaceAll("[^0-9]", "");
        if (!digitsA.isEmpty() || !digitsB.isEmpty()) {
            if (!digitsA.equals(digitsB)) {
                return false;
            }
        }

        // Substring containment only if lengths are very close
        if ((normA.contains(normB) || normB.contains(normA)) && Math.abs(normA.length() - normB.length()) <= 2) {
            return true;
        }

        // Only allow single character OCR typo on longer strings (> 5 chars)
        if (normA.length() > 5 && normB.length() > 5) {
            return calculateLevenshteinDistance(normA, normB) <= 1;
        }

        return false;
    }

    private int findBestMedicineMatch(RawMedicineItem target, List<RawMedicineItem> candidates, Set<Integer> excluded) {
        if (target == null || target.getMedicineName() == null) return -1;
        String targetName = clean(target.getMedicineName()).toLowerCase().replaceAll("[^a-z0-9]", "");
        if (targetName.isEmpty()) return -1;

        int bestIdx = -1;
        int minDistance = Integer.MAX_VALUE;

        for (int i = 0; i < candidates.size(); i++) {
            if (excluded.contains(i)) continue;
            RawMedicineItem candidate = candidates.get(i);
            if (candidate == null || candidate.getMedicineName() == null) continue;

            String candidateName = clean(candidate.getMedicineName()).toLowerCase().replaceAll("[^a-z0-9]", "");
            if (candidateName.isEmpty()) continue;

            if (targetName.equals(candidateName) || targetName.startsWith(candidateName) || candidateName.startsWith(targetName)) {
                return i;
            }

            int dist = calculateLevenshteinDistance(targetName, candidateName);
            if (dist <= 3 && dist < minDistance) {
                minDistance = dist;
                bestIdx = i;
            }
        }

        return bestIdx;
    }

    private PrescriptionScanResult populateSingleSource(RawExtractedPrescription source,
                                                        PrescriptionScanResult result,
                                                        String status,
                                                        String message,
                                                        Map<String, Object> sourceAgreement) {
        result.setPatientName(cleanDemographics(source.getPatientName()));
        result.setDoctorName(source.getDoctorName());
        result.setHospitalName(source.getHospitalName());
        result.setRawText(source.getRawText() != null ? source.getRawText().trim() : "");
        result.setVerificationStatus(status);
        result.setStatusMessage(message);
        result.setNeedsVerification(true);
        result.setOverallConfidence(0.85);

        List<ScannedMedicineItem> scanned = new ArrayList<>();
        if (source.getMedicines() != null) {
            for (RawMedicineItem r : source.getMedicines()) {
                ScannedMedicineItem s = convertRawToScanned(r);
                s.setConfidence(0.85);
                scanned.add(s);
            }
        }
        result.setMedicines(scanned);
        sourceAgreement.put("conflictsDetected", false);

        return result;
    }

    private ScannedMedicineItem convertRawToScanned(RawMedicineItem raw) {
        ScannedMedicineItem item = new ScannedMedicineItem();
        item.setMedicineName(raw.getMedicineName());
        item.setStrength(raw.getStrength());
        item.setDosageAmount(raw.getDosageAmount());
        item.setDosageUnit(raw.getDosageUnit());
        item.setFrequency(raw.getFrequency());
        List<String> cleanTimings = new ArrayList<>();
        if (raw.getTiming() != null) {
            for (String t : raw.getTiming()) {
                if (t != null && !t.trim().isEmpty() && !t.trim().equalsIgnoreCase("Uncertain")) {
                    cleanTimings.add(t.trim());
                }
            }
        }
        item.setTiming(cleanTimings);

        List<String> cleanExact = new ArrayList<>();
        if (raw.getExactTimes() != null) {
            for (String ex : raw.getExactTimes()) {
                if (ex != null && !ex.trim().isEmpty() && !ex.trim().equalsIgnoreCase("Uncertain")) {
                    cleanExact.add(ex.trim());
                }
            }
        }
        item.setExactTimes(cleanExact);
        item.setMealRelation(raw.getMealRelation());
        item.setDuration(raw.getDuration());
        item.setTotalQuantity(raw.getTotalQuantity());
        item.setInstructions(raw.getInstructions());
        item.setConfidence(0.85);
        if (Boolean.TRUE.equals(raw.getIsUncertain())) {
            item.getUncertainFields().add("markedUncertainByModel");
        }
        return item;
    }

    private String choosePreferredString(String a, String b) {
        String cleanA = clean(a);
        String cleanB = clean(b);
        if (!cleanA.isEmpty() && !cleanB.isEmpty()) {
            return cleanA.length() >= cleanB.length() ? a : b;
        }
        return !cleanA.isEmpty() ? a : (!cleanB.isEmpty() ? b : null);
    }

    private int calculateLevenshteinDistance(String s1, String s2) {
        int[] prev = new int[s2.length() + 1];
        int[] curr = new int[s2.length() + 1];

        for (int j = 0; j <= s2.length(); j++) prev[j] = j;

        for (int i = 1; i <= s1.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            System.arraycopy(curr, 0, prev, 0, curr.length);
        }
        return prev[s2.length()];
    }

    private String clean(String str) {
        return str != null ? str.trim() : "";
    }
}
