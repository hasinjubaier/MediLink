package com.medilink.model.strategy;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Concrete Strategy connecting to Google Gemini AI's Free Tier REST API.
 * Uses gemini-1.5-flash with clinical safety instructions.
 */
public class GeminiAiStrategy implements AiChatStrategy {
    private static final String DEFAULT_MODEL = "gemini-1.5-flash";
    private static final String API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    @Override
    public String getProviderName() {
        return "GEMINI_AI";
    }

    @Override
    public boolean isConfigured(String apiKey) {
        return apiKey != null && !apiKey.trim().isEmpty() && apiKey.trim().length() > 10;
    }

    /**
     * Returns true if the key is an OAuth2 Bearer token (AQ. or ya29. prefix)
     * rather than a standard AIzaSy API key.
     */
    private boolean isOAuthToken(String apiKey) {
        if (apiKey == null) return false;
        String k = apiKey.trim();
        return k.startsWith("AQ.") || k.startsWith("ya29.") || k.startsWith("AQ") && k.length() > 20 && !k.startsWith("AIza");
    }

    @Override
    public String ask(String userMessage, List<Map<String, String>> conversationHistory, String patientContext, String apiKey) throws Exception {
        if (!isConfigured(apiKey)) {
            throw new IllegalArgumentException("Google Gemini API Key is missing or invalid. Please configure your API key.");
        }

        String trimmedKey = apiKey.trim();
        boolean useOAuth  = isOAuthToken(trimmedKey);

        // For standard API keys: use ?key= query param
        // For OAuth tokens (AQ./ya29.): use Authorization: Bearer header
        String endpointUrl = useOAuth
            ? API_BASE_URL + DEFAULT_MODEL + ":generateContent"
            : API_BASE_URL + DEFAULT_MODEL + ":generateContent?key=" + trimmedKey;

        String systemInstruction = buildSystemPrompt(patientContext);
        String requestPayload    = buildJsonPayload(userMessage, conversationHistory, systemInstruction);

        URL url = new URL(endpointUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        conn.setDoOutput(true);

        if (useOAuth) {
            conn.setRequestProperty("Authorization", "Bearer " + trimmedKey);
        }

        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestPayload.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        int responseCode = conn.getResponseCode();
        InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder responseBuilder = new StringBuilder();

        if (is != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }
            }
        }

        String rawResponse = responseBuilder.toString();
        if (responseCode != 200) {
            String errorMsg = parseErrorMessage(rawResponse);
            throw new RuntimeException("Gemini API Error (" + responseCode + "): " + errorMsg);
        }

        return parseCandidateText(rawResponse);
    }

    private String buildSystemPrompt(String patientContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are MediLink, an expert, empathetic, and clinical healthcare assistant for patients on the MediLink 2.0 platform. ");
        sb.append("You provide medically accurate, accessible information on prescription medications, OTC drugs, dosage instructions, possible drug interactions, side effects, and daily health habits. ");
        sb.append("CRITICAL LANGUAGE RULE: Automatically detect the language of the user's message and ALWAYS respond in that exact same language. ");
        sb.append("If the user writes in Bengali (Bangla), respond fully in Bengali. ");
        sb.append("If the user writes in Arabic, respond in Arabic. ");
        sb.append("If the user writes in Spanish, respond in Spanish. ");
        sb.append("If the user writes in French, respond in French. ");
        sb.append("If the user writes in Hindi/Urdu, respond in Hindi/Urdu. ");
        sb.append("If the user writes in any other language, respond naturally in that same language. ");
        sb.append("Never switch to English unless the user writes in English first. ");
        sb.append("Guidelines:\\n");
        sb.append("1. Communicate clearly using clean bullet points and short paragraphs in the user's own language.\\n");
        sb.append("2. Always emphasize medication safety, compliance, and proper storage.\\n");
        sb.append("3. If discussing emergency symptoms (e.g. acute chest pain, severe shortness of breath, anaphylaxis, sudden numbness), immediately urge calling emergency services (999 or 911).\\n");
        sb.append("4. Remind patients that MediLink provides clinical guidance, but formal diagnostic decisions require their physician or pharmacist.\\n");
        sb.append("5. Keep greetings and conversational responses warm and natural in the user's language.\\n");

        if (patientContext != null && !patientContext.trim().isEmpty()) {
            sb.append("\\n[PATIENT HEALTH CONTEXT]\\n");
            sb.append(patientContext.replace("\"", "\\\"").replace("\n", "\\n"));
        }

        return sb.toString();
    }

    private String buildJsonPayload(String userMessage, List<Map<String, String>> history, String systemInstruction) {
        StringBuilder sb = new StringBuilder("{");

        // System Instruction
        sb.append("\"system_instruction\":{\"parts\":[{\"text\":\"").append(escapeJson(systemInstruction)).append("\"}]},");

        // Contents array
        sb.append("\"contents\":[");
        boolean first = true;

        if (history != null) {
            for (Map<String, String> turn : history) {
                String role = turn.getOrDefault("role", "user");
                String content = turn.getOrDefault("content", "");
                if (content.trim().isEmpty()) continue;

                if (!first) sb.append(",");
                first = false;

                String geminiRole = "assistant".equalsIgnoreCase(role) || "model".equalsIgnoreCase(role) ? "model" : "user";
                sb.append("{\"role\":\"").append(geminiRole).append("\",\"parts\":[{\"text\":\"").append(escapeJson(content)).append("\"}]}");
            }
        }

        // Current message
        if (!first) sb.append(",");
        sb.append("{\"role\":\"user\",\"parts\":[{\"text\":\"").append(escapeJson(userMessage)).append("\"}]}");
        sb.append("],");

        // Generation Config
        sb.append("\"generationConfig\":{");
        sb.append("\"temperature\":0.4,");
        sb.append("\"maxOutputTokens\":1200");
        sb.append("}");

        sb.append("}");
        return sb.toString();
    }

    private String parseCandidateText(String json) {
        // Find "text": "..." inside candidates
        int textIdx = json.indexOf("\"text\":");
        if (textIdx == -1) {
            return "I received your message, but no textual response was generated by the model.";
        }

        int startQuote = json.indexOf("\"", textIdx + 7);
        if (startQuote == -1) return "Could not parse AI response.";

        StringBuilder result = new StringBuilder();
        boolean escaped = false;
        for (int i = startQuote + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                if (c == 'n') result.append('\n');
                else if (c == 'r') result.append('\r');
                else if (c == 't') result.append('\t');
                else if (c == '\"') result.append('\"');
                else if (c == '\\') result.append('\\');
                else result.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '\"') {
                break;
            } else {
                result.append(c);
            }
        }

        return result.toString().trim();
    }

    private String parseErrorMessage(String rawResponse) {
        if (rawResponse == null || rawResponse.isEmpty()) return "Unknown error";
        int messageIdx = rawResponse.indexOf("\"message\":");
        if (messageIdx != -1) {
            int start = rawResponse.indexOf("\"", messageIdx + 10);
            int end = rawResponse.indexOf("\"", start + 1);
            if (start != -1 && end != -1) {
                return rawResponse.substring(start + 1, end);
            }
        }
        return rawResponse.length() > 180 ? rawResponse.substring(0, 180) + "..." : rawResponse;
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '\"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        String t = "000" + Integer.toHexString(c);
                        sb.append("\\u").append(t.substring(t.length() - 4));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
