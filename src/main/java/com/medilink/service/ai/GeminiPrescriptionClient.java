package com.medilink.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medilink.config.AiVisionConfig;
import com.medilink.dto.prescription.RawExtractedPrescription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Primary Vision Engine utilizing Google Gemini Multimodal REST API
 * for structured prescription transcription and understanding.
 */
@Service
public class GeminiPrescriptionClient {

    private static final String API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private final AiVisionConfig config;
    private final ObjectMapper objectMapper;

    @Autowired
    public GeminiPrescriptionClient(AiVisionConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    public RawExtractedPrescription extract(byte[] imageBytes, String mimeType) throws Exception {
        if (!config.isGeminiConfigured()) {
            throw new IllegalStateException("Google Gemini API is not configured (missing GEMINI_API_KEY).");
        }

        String apiKey = config.getGeminiApiKey();
        String rawModel = config.getGeminiModel();
        String primaryModel = (rawModel != null && rawModel.startsWith("models/"))
            ? rawModel.substring(7).trim()
            : (rawModel != null ? rawModel.trim() : "gemini-3.6-flash");
        boolean isOAuth = isOAuthToken(apiKey);

        String safeMime = (mimeType != null && !mimeType.trim().isEmpty()) ? mimeType.trim() : "image/jpeg";
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String requestPayload = buildGeminiPayload(base64Image, safeMime);

        List<String> candidateModels = new ArrayList<String>();
        candidateModels.add(primaryModel);
        if (!"gemini-3.6-flash".equals(primaryModel)) candidateModels.add("gemini-3.6-flash");
        if (!"gemini-3-flash-preview".equals(primaryModel)) candidateModels.add("gemini-3-flash-preview");
        if (!"gemini-3.7-flash".equals(primaryModel)) candidateModels.add("gemini-3.7-flash");
        if (!"gemini-flash-latest".equals(primaryModel)) candidateModels.add("gemini-flash-latest");

        Exception lastException = null;

        for (String modelToUse : candidateModels) {
            int maxAttempts = 2;
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    System.out.println("[AI Vision] Prescription scan started: Gemini (" + modelToUse + ")" +
                        (attempt > 1 ? " [Retry " + attempt + "]" : ""));
                    String rawResponse = executeGeminiHttpCall(requestPayload, apiKey, modelToUse, isOAuth);
                    RawExtractedPrescription result = parseGeminiResponse(rawResponse);
                    System.out.println("[AI Vision] Gemini extraction completed: " +
                        (result.getMedicines() != null ? result.getMedicines().size() : 0) + " medicine(s) detected using model " + modelToUse);
                    return result;
                } catch (GeminiTransientException e) {
                    lastException = e;
                    System.err.println("[AI Vision] Gemini model " + modelToUse + " transient issue (" + e.getMessage() + ").");
                    if (attempt < maxAttempts) {
                        try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
                    }
                } catch (GeminiModelNotFoundException e) {
                    lastException = e;
                    System.err.println("[AI Vision] Gemini model " + modelToUse + " unavailable/retired. Trying next candidate...");
                    break;
                } catch (Exception e) {
                    // Non-transient errors (e.g. invalid payload) fail immediately
                    throw e;
                }
            }
        }

        throw lastException != null ? lastException : new RuntimeException("All Gemini candidate models failed to respond.");
    }

    private String executeGeminiHttpCall(String requestPayload, String apiKey, String model, boolean isOAuth) throws Exception {
        String endpointUrl = isOAuth
            ? API_BASE_URL + model + ":generateContent"
            : API_BASE_URL + model + ":generateContent?key=" + apiKey;

        URL url = new URL(endpointUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(45000);
        conn.setDoOutput(true);

        if (isOAuth) {
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        } else {
            conn.setRequestProperty("x-goog-api-key", apiKey);
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
            System.err.println("[AI Vision] Gemini error (" + responseCode + "): " + errorMsg);
            if (responseCode == 503 || responseCode == 429 || errorMsg.contains("high demand") || errorMsg.contains("RESOURCE_EXHAUSTED")) {
                throw new GeminiTransientException("Gemini API Error (" + responseCode + "): " + errorMsg);
            }
            if (responseCode == 404 || errorMsg.contains("NOT_FOUND") || errorMsg.contains("not found")) {
                throw new GeminiModelNotFoundException("Gemini API Error (" + responseCode + "): " + errorMsg);
            }
            throw new RuntimeException("Gemini API Error (" + responseCode + "): " + errorMsg);
        }

        return rawResponse;
    }

    private static class GeminiTransientException extends RuntimeException {
        public GeminiTransientException(String message) { super(message); }
    }

    private static class GeminiModelNotFoundException extends RuntimeException {
        public GeminiModelNotFoundException(String message) { super(message); }
    }

    private String buildGeminiPayload(String base64Image, String mimeType) throws Exception {
        Map<String, Object> inlineData = new HashMap<>();
        inlineData.put("mimeType", mimeType);
        inlineData.put("data", base64Image);

        Map<String, Object> part1 = new HashMap<>();
        part1.put("inlineData", inlineData);

        Map<String, Object> part2 = new HashMap<>();
        part2.put("text", PrescriptionVisionPrompt.INSTRUCTION);

        Map<String, Object> content = new HashMap<>();
        content.put("role", "user");
        content.put("parts", new Object[]{part1, part2});

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.1);
        generationConfig.put("responseMimeType", "application/json");

        Map<String, Object> root = new HashMap<>();
        root.put("contents", new Object[]{content});
        root.put("generationConfig", generationConfig);

        return objectMapper.writeValueAsString(root);
    }

    private RawExtractedPrescription parseGeminiResponse(String responseJson) throws Exception {
        JsonNode root = objectMapper.readTree(responseJson);
        JsonNode candidates = root.path("candidates");
        if (candidates.isMissingNode() || candidates.size() == 0) {
            throw new RuntimeException("No candidates returned from Gemini API");
        }

        JsonNode parts = candidates.get(0).path("content").path("parts");
        if (parts.isMissingNode() || parts.size() == 0) {
            throw new RuntimeException("No content parts returned in Gemini candidate");
        }

        String textContent = parts.get(0).path("text").asText("");
        if (textContent.trim().isEmpty()) {
            throw new RuntimeException("Empty text content generated by Gemini");
        }

        String cleanJson = stripMarkdown(textContent);
        return objectMapper.readValue(cleanJson, RawExtractedPrescription.class);
    }

    private String stripMarkdown(String text) {
        String t = text.trim();
        if (t.startsWith("```json")) {
            t = t.substring(7);
        } else if (t.startsWith("```")) {
            t = t.substring(3);
        }
        if (t.endsWith("```")) {
            t = t.substring(0, t.length() - 3);
        }
        return t.trim();
    }

    private String parseErrorMessage(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode errorNode = root.path("error");
            if (!errorNode.isMissingNode()) {
                String message = errorNode.path("message").asText();
                if (!message.isEmpty()) return message;
            }
        } catch (Exception ignored) {}
        return rawResponse.length() > 180 ? rawResponse.substring(0, 180) + "..." : rawResponse;
    }

    private boolean isOAuthToken(String apiKey) {
        if (apiKey == null) return false;
        String k = apiKey.trim();
        return k.startsWith("ya29.");
    }
}
