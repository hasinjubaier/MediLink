package com.medilink.controller;

import com.medilink.service.AiChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AiController {

    private final AiChatService aiChatService = AiChatService.getInstance();

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> processChat(@RequestBody Map<String, Object> body) {
        String message = (String) body.getOrDefault("message", "");
        String apiKey = (String) body.getOrDefault("apiKey", "");
        String patientId = (String) body.getOrDefault("patientId", "ML-9824-A");

        if (message == null || message.trim().isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", "Message query cannot be empty.");
            return ResponseEntity.badRequest().body(err);
        }

        List<Map<String, String>> history = new ArrayList<>();
        Object histObj = body.get("history");
        if (histObj instanceof List) {
            for (Object item : (List<?>) histObj) {
                if (item instanceof Map) {
                    Map<?, ?> m = (Map<?, ?>) item;
                    Map<String, String> turn = new HashMap<>();
                    turn.put("role", String.valueOf(m.get("role")));
                    turn.put("content", String.valueOf(m.get("content")));
                    history.add(turn);
                }
            }
        }

        Map<String, Object> result = aiChatService.processChatQuery(
                message.trim(), history, (apiKey != null ? apiKey.trim() : ""), (patientId != null ? patientId.trim() : "")
        );

        return ResponseEntity.ok(result);
    }

    @RequestMapping(value = "/status", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> getOrSetAiStatus(@RequestBody(required = false) Map<String, String> body) {
        if (body != null && body.containsKey("apiKey")) {
            String key = body.get("apiKey");
            if (key != null && !key.trim().isEmpty()) {
                aiChatService.setServerApiKey(key.trim());
            }
        }

        boolean hasKey = aiChatService.hasValidServerKey();
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("hasServerKey", hasKey);
        response.put("provider", hasKey ? "GEMINI_AI" : "CLINICAL_FALLBACK");
        return ResponseEntity.ok(response);
    }
}
