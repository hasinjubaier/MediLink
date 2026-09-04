package com.medilink.controller;

import com.medilink.model.chat.ChatMessage;
import com.medilink.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;

    @Autowired
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getChatHistory(
            @RequestParam(name = "user1", defaultValue = "usr_patient_01") String user1,
            @RequestParam(name = "user2", defaultValue = "usr_pharma_01") String user2) {

        List<ChatMessage> list = chatService.getHistory(user1, user2);
        List<Map<String, Object>> messages = new ArrayList<>();
        for (ChatMessage m : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId() != null ? String.valueOf(m.getId()) : "");
            map.put("senderName", m.getSenderName());
            map.put("senderRole", m.getSenderRole());
            map.put("content", m.getContent());
            map.put("timestamp", m.getTimestamp() != null ? m.getTimestamp().toString() : "");
            messages.add(map);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("messages", messages);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody Map<String, String> data) {
        String senderId = data.getOrDefault("senderId", "usr_patient_01");
        String senderName = data.getOrDefault("senderName", "Rahim Ahmed");
        String senderRole = data.getOrDefault("senderRole", "PATIENT");
        String receiverId = data.getOrDefault("receiverId", "usr_pharma_01");
        String content = data.getOrDefault("content", "");
        String type = data.getOrDefault("type", "TEXT");

        ChatMessage saved = chatService.saveMessage(senderId, senderName, senderRole, receiverId, content, type);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("messageId", saved.getId() != null ? String.valueOf(saved.getId()) : "msg_saved");
        return ResponseEntity.ok(response);
    }
}
