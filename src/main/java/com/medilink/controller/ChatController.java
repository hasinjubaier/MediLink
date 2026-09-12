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

    @GetMapping({"", "/", "/messages"})
    public ResponseEntity<Map<String, Object>> getChatHistory(
            @RequestParam(name = "user1", defaultValue = "ML-9824-A") String user1,
            @RequestParam(name = "user2", defaultValue = "usr_pharma_01") String user2) {

        List<ChatMessage> list = chatService.getHistory(user1, user2);
        List<Map<String, Object>> messages = new ArrayList<>();
        for (ChatMessage m : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId() != null ? String.valueOf(m.getId()) : "");
            map.put("senderId", m.getSenderId());
            map.put("senderName", m.getSenderName());
            map.put("senderRole", m.getSenderRole());
            map.put("senderEmail", m.getSenderEmail());
            map.put("receiverId", m.getReceiverId());
            map.put("receiverEmail", m.getReceiverEmail());
            map.put("content", m.getContent());
            map.put("messageType", m.getMessageType());
            map.put("prescriptionId", m.getPrescriptionId());
            map.put("prescriptionSummary", m.getPrescriptionSummary());
            map.put("isRead", m.getIsRead());
            map.put("timestamp", m.getTimestamp() != null ? m.getTimestamp().toString() : "");
            messages.add(map);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("messages", messages);
        response.put("total", messages.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pharmacists")
    public ResponseEntity<Map<String, Object>> getPharmacists() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("pharmacists", chatService.getAvailablePharmacists());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/conversations")
    public ResponseEntity<Map<String, Object>> getConversations(
            @RequestParam(name = "userId", defaultValue = "ML-9824-A") String userId,
            @RequestParam(name = "role", required = false, defaultValue = "PATIENT") String role) {
        List<Map<String, Object>> conversations = chatService.getConversations(userId, role);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("conversations", conversations);
        response.put("total", conversations.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(
            @RequestParam(name = "userId", defaultValue = "ML-9824-A") String userId) {
        long unread = chatService.getUnreadCount(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("unreadCount", unread);
        return ResponseEntity.ok(response);
    }

    @PostMapping({"", "/", "/send"})
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody Map<String, Object> data) {
        String senderId = String.valueOf(data.getOrDefault("senderId", "ML-9824-A"));
        String senderName = String.valueOf(data.getOrDefault("senderName", "Rahim Ahmed"));
        String senderRole = String.valueOf(data.getOrDefault("senderRole", "PATIENT"));
        String senderEmail = data.containsKey("senderEmail") ? String.valueOf(data.get("senderEmail")) : null;

        String receiverId = String.valueOf(data.getOrDefault("receiverId", "usr_pharma_01"));
        String receiverEmail = data.containsKey("receiverEmail") ? String.valueOf(data.get("receiverEmail")) : null;

        String content = String.valueOf(data.getOrDefault("content", ""));
        String type = String.valueOf(data.getOrDefault("type", "TEXT"));
        String prescriptionId = data.containsKey("prescriptionId") && data.get("prescriptionId") != null
                ? String.valueOf(data.get("prescriptionId")) : null;
        String prescriptionSummary = data.containsKey("prescriptionSummary") && data.get("prescriptionSummary") != null
                ? String.valueOf(data.get("prescriptionSummary")) : null;

        boolean autoReply = data.containsKey("autoReply") && Boolean.parseBoolean(String.valueOf(data.get("autoReply")));

        ChatMessage saved = chatService.saveMessage(
                senderId, senderName, senderRole, senderEmail,
                receiverId, receiverEmail, content, type,
                prescriptionId, prescriptionSummary, autoReply
        );

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("messageId", saved.getId() != null ? String.valueOf(saved.getId()) : "msg_saved");
        response.put("message", "Message delivered to pharmacist consultation channel.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@RequestBody Map<String, String> body) {
        String user1 = body.getOrDefault("user1", "ML-9824-A");
        String user2 = body.getOrDefault("user2", "usr_pharma_01");
        chatService.markMessagesAsRead(user1, user2);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Conversation marked as read.");
        return ResponseEntity.ok(response);
    }
}
