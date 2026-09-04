package com.medilink.model.strategy;

import java.util.List;
import java.util.Map;

/**
 * Strategy Pattern Interface for AI Chat providers in MediLink.
 * Enables interchangeable AI engines (Google Gemini AI, Clinical Fallback, etc.).
 */
public interface AiChatStrategy {
    /**
     * Name identifier for this strategy (e.g. "GEMINI_AI", "CLINICAL_FALLBACK").
     */
    String getProviderName();

    /**
     * Check if the strategy has the necessary credentials to execute queries.
     */
    boolean isConfigured(String apiKey);

    /**
     * Send a query to the AI engine with conversational history and patient clinical context.
     *
     * @param userMessage User's current question or symptom query.
     * @param conversationHistory Prior turns formatted as list of maps with "role" and "content".
     * @param patientContext Relevant patient data (allergies, active medications, chronic conditions).
     * @param apiKey Optional user-supplied or server-configured API key.
     * @return Formatted AI response string.
     * @throws Exception If an error occurs during generation.
     */
    String ask(String userMessage, List<Map<String, String>> conversationHistory, String patientContext, String apiKey) throws Exception;
}
