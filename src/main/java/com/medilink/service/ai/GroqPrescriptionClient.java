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
import java.util.*;

/**
 * Secondary Independent Vision Engine utilizing Groq's Multimodal API
 * (e.g. Llama 3.2 11B/90B Vision) for cross-verification of prescription data.
 */
@Service
public class GroqPrescriptionClient {

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private final AiVisionConfig config;
    private final ObjectMapper objectMapper;

    @Autowired
    public GroqPrescriptionClient(AiVisionConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    private final ThreadLocal<String> prescriptionTextContext = new ThreadLocal<String>();

    public void setPrescriptionTextContext(String text) {
        this.prescriptionTextContext.set(text);
    }

    public void clearPrescriptionTextContext() {
        this.prescriptionTextContext.remove();
    }

    public RawExtractedPrescription extract(byte[] imageBytes, String mimeType) throws Exception {
        return extract(imageBytes, mimeType, this.prescriptionTextContext.get());
    }

    public RawExtractedPrescription extract(byte[] imageBytes, String mimeType, String extractedTextFallback) throws Exception {
        if (!config.isGroqConfigured()) {
            throw new IllegalStateException("Groq API is not configured (missing GROQ_API_KEY).");
        }

        String apiKey = config.getGroqApiKey();
        String model = config.getGroqModel();
        boolean isKnownTextModel = model != null && (
            model.contains("oss") ||
            model.contains("qwen") ||
            model.contains("allam") ||
            model.contains("orpheus") ||
            model.contains("versatile") ||
            model.contains("llama-3.3") ||
            model.contains("llama-3.1")
        );

        if (isKnownTextModel) {
            if (extractedTextFallback != null && !extractedTextFallback.trim().isEmpty()) {
                System.out.println("[AI Vision] Groq model (" + model + ") is a language model. Running independent text prescription verification...");
                return extractFromText(extractedTextFallback, apiKey, model);
            } else {
                throw new UnsupportedOperationException("Groq model '" + model + "' is a text language model and requires a multimodal vision model to process raw images without extracted text.");
            }
        }

        try {
            return extractFromImage(imageBytes, mimeType, apiKey, model);
        } catch (Exception e) {
            if (extractedTextFallback != null && !extractedTextFallback.trim().isEmpty() &&
                e.getMessage() != null && (e.getMessage().contains("content must be a string") || e.getMessage().contains("400"))) {
                System.out.println("[AI Vision] Groq vision unsupported for model " + model + ", falling back to independent text verification.");
                return extractFromText(extractedTextFallback, apiKey, model);
            }
            throw e;
        }
    }

    private RawExtractedPrescription extractFromImage(byte[] imageBytes, String mimeType, String apiKey, String model) throws Exception {
        String safeMime = (mimeType != null && !mimeType.trim().isEmpty()) ? mimeType.trim() : "image/jpeg";
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        String requestPayload = buildGroqPayload(base64Image, safeMime, model);

        System.out.println("[AI Vision] Prescription scan started: Groq (" + model + ")");
        return sendGroqRequest(requestPayload, apiKey);
    }

    private RawExtractedPrescription extractFromText(String rawText, String apiKey, String model) throws Exception {
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", PrescriptionVisionPrompt.INSTRUCTION + "\n\nPRESCRIPTION TEXT CONTENT:\n" + rawText);

        List<Object> messages = new ArrayList<>();
        messages.add(userMessage);

        Map<String, Object> responseFormat = new HashMap<>();
        responseFormat.put("type", "json_object");

        Map<String, Object> root = new HashMap<>();
        root.put("model", model);
        root.put("messages", messages);
        root.put("response_format", responseFormat);
        root.put("temperature", 0.1);

        String requestPayload = objectMapper.writeValueAsString(root);
        System.out.println("[AI Vision] Prescription cross-check started: Groq text (" + model + ")");
        return sendGroqRequest(requestPayload, apiKey);
    }

    private RawExtractedPrescription sendGroqRequest(String requestPayload, String apiKey) throws Exception {
        URL url = new URL(GROQ_API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(45000);
        conn.setDoOutput(true);

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
            System.err.println("[AI Vision] Groq error (" + responseCode + "): " + errorMsg);
            throw new RuntimeException("Groq API Error (" + responseCode + "): " + errorMsg);
        }

        RawExtractedPrescription result = parseGroqResponse(rawResponse);
        System.out.println("[AI Vision] Groq verification completed: " +
            (result.getMedicines() != null ? result.getMedicines().size() : 0) + " medicine(s) detected.");
        return result;
    }

    private String buildGroqPayload(String base64Image, String mimeType, String model) throws Exception {
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("type", "text");
        textPart.put("text", PrescriptionVisionPrompt.INSTRUCTION);

        Map<String, Object> imageUrlObj = new HashMap<>();
        imageUrlObj.put("url", "data:" + mimeType + ";base64," + base64Image);

        Map<String, Object> imagePart = new HashMap<>();
        imagePart.put("type", "image_url");
        imagePart.put("image_url", imageUrlObj);

        List<Object> contentList = new ArrayList<>();
        contentList.add(textPart);
        contentList.add(imagePart);

        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", contentList);

        List<Object> messages = new ArrayList<>();
        messages.add(userMessage);

        Map<String, Object> responseFormat = new HashMap<>();
        responseFormat.put("type", "json_object");

        Map<String, Object> root = new HashMap<>();
        root.put("model", model);
        root.put("messages", messages);
        root.put("response_format", responseFormat);
        root.put("temperature", 0.1);

        return objectMapper.writeValueAsString(root);
    }

    private RawExtractedPrescription parseGroqResponse(String responseJson) throws Exception {
        JsonNode root = objectMapper.readTree(responseJson);
        JsonNode choices = root.path("choices");
        if (choices.isMissingNode() || choices.size() == 0) {
            throw new RuntimeException("No choices returned from Groq API");
        }

        JsonNode message = choices.get(0).path("message");
        String content = message.path("content").asText("");
        if (content.trim().isEmpty()) {
            throw new RuntimeException("Empty response content from Groq API");
        }

        String cleanJson = stripMarkdown(content);
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
}
