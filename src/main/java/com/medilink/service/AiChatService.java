package com.medilink.service;

import com.medilink.model.prescription.Prescription;
import com.medilink.model.prescription.PrescriptionItem;
import com.medilink.model.strategy.AiChatStrategy;
import com.medilink.model.strategy.GeminiAiStrategy;
import com.medilink.model.strategy.LocalClinicalFallbackStrategy;
import com.medilink.model.user.Patient;
import com.medilink.repository.PatientRepository;
import com.medilink.repository.PrescriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service managing AI Chatbot conversations and routing between strategies.
 * Demonstrates the Strategy Pattern.
 */
@Service
public class AiChatService {
    private static AiChatService instance;

    private final AiChatStrategy geminiStrategy;
    private final AiChatStrategy fallbackStrategy;
    private String serverApiKey;

    @Autowired(required = false)
    private PatientRepository patientRepository;

    @Autowired(required = false)
    private PrescriptionRepository prescriptionRepository;

    public AiChatService() {
        this.geminiStrategy = new GeminiAiStrategy();
        this.fallbackStrategy = new LocalClinicalFallbackStrategy();

        // Check environment variable or system property for Gemini API Key
        String envKey = System.getenv("GEMINI_API_KEY");
        if (envKey == null || envKey.trim().isEmpty()) {
            envKey = System.getProperty("gemini.api.key", "");
        }
        this.serverApiKey = envKey != null ? envKey.trim() : "";
        instance = this;
    }

    public static synchronized AiChatService getInstance() {
        if (instance == null) {
            instance = new AiChatService();
        }
        return instance;
    }

    public synchronized void setServerApiKey(String apiKey) {
        if (apiKey != null) {
            this.serverApiKey = apiKey.trim();
        }
    }

    public synchronized String getServerApiKey() {
        return this.serverApiKey;
    }

    public boolean hasValidServerKey() {
        return geminiStrategy.isConfigured(this.serverApiKey);
    }

    /**
     * Process a patient chat query, resolving patient clinical history and delegating to the proper strategy.
     */
    public Map<String, Object> processChatQuery(String userMessage, List<Map<String, String>> history, String clientApiKey, String patientId) {
        Map<String, Object> result = new HashMap<>();

        // Resolve which API key to use (Client-provided takes precedence, then server-configured)
        String effectiveKey = (clientApiKey != null && !clientApiKey.trim().isEmpty()) ? clientApiKey.trim() : this.serverApiKey;

        // Build clinical patient context
        String patientContext = buildPatientContext(patientId);

        // Attempt Gemini AI Strategy if configured
        if (geminiStrategy.isConfigured(effectiveKey)) {
            try {
                String reply = geminiStrategy.ask(userMessage, history, patientContext, effectiveKey);
                result.put("status", "SUCCESS");
                result.put("reply", reply);
                result.put("provider", "GEMINI_AI");
                result.put("model", "gemini-1.5-flash");
                return result;
            } catch (Exception e) {
                System.err.println("[AI Service] Gemini strategy exception: " + e.getMessage());
                try {
                    String fallbackReply = fallbackStrategy.ask(userMessage, history, patientContext, null);
                    result.put("status", "SUCCESS");
                    result.put("reply", fallbackReply + "\n\n*(Note: Gemini API reported: " + e.getMessage() + ". Delivered via Clinical Fallback Engine.)*");
                    result.put("provider", "CLINICAL_FALLBACK");
                    result.put("errorNotice", e.getMessage());
                    return result;
                } catch (Exception ignored) {}
            }
        }

        // Fallback strategy when no Gemini key is configured
        try {
            String fallbackReply = fallbackStrategy.ask(userMessage, history, patientContext, null);
            result.put("status", "SUCCESS");
            result.put("reply", fallbackReply);
            result.put("provider", "CLINICAL_FALLBACK");
            result.put("hasGeminiKey", false);
            return result;
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("message", "Unable to process query: " + e.getMessage());
            return result;
        }
    }

    private String buildPatientContext(String patientId) {
        if (patientId == null || patientId.trim().isEmpty()) return "";
        StringBuilder sb = new StringBuilder();

        if (patientRepository != null) {
            Optional<Patient> pOpt = patientRepository.findById(patientId);
            if (pOpt.isPresent()) {
                Patient patient = pOpt.get();
                sb.append("Patient Name: ").append(patient.getName()).append("\n");
                sb.append("Patient ID: ").append(patient.getId()).append("\n");
            }
        }

        if (prescriptionRepository != null) {
            List<Prescription> prescriptions = prescriptionRepository.findByPatientIdOrderByUploadedAtDesc(patientId);
            if (prescriptions != null && !prescriptions.isEmpty()) {
                sb.append("Active Prescriptions:\n");
                for (Prescription p : prescriptions) {
                    for (PrescriptionItem item : p.getItems()) {
                        sb.append("- ").append(item.getMedicineName())
                          .append(" (").append(item.getDosage()).append(", ")
                          .append(item.getFrequency()).append(", Duration: ")
                          .append(item.getDuration()).append(")\n");
                    }
                }
            }
        }

        return sb.toString();
    }
}
