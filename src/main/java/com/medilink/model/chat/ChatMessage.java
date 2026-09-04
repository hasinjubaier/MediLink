package com.medilink.model.chat;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Domain model representing a chat message in patient-pharmacist communication.
 * Mapped as a JPA @Entity for PostgreSQL persistence.
 */
@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_id", length = 50, nullable = false)
    private String senderId;

    @Column(name = "sender_name", length = 100, nullable = false)
    private String senderName;

    @Column(name = "sender_role", length = 30, nullable = false)
    private String senderRole;

    @Column(name = "receiver_id", length = 50, nullable = false)
    private String receiverId;

    @Column(name = "message_text", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "message_type", length = 30)
    private String messageType = "TEXT"; // TEXT, PRESCRIPTION_REF, EMERGENCY_ALERT

    @Column(name = "sent_at")
    private LocalDateTime timestamp;

    public ChatMessage() {
        this.timestamp = LocalDateTime.now();
        this.messageType = "TEXT";
    }

    public ChatMessage(String senderId, String senderName, String senderRole,
                       String receiverId, String content, String messageType) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderRole = senderRole;
        this.receiverId = receiverId;
        this.content = content;
        this.messageType = messageType != null ? messageType : "TEXT";
        this.timestamp = LocalDateTime.now();
    }

    public ChatMessage(String legacyId, String senderId, String senderName, String senderRole,
                       String receiverId, String content, String messageType) {
        this(senderId, senderName, senderRole, receiverId, content, messageType);
        try {
            if (legacyId != null && legacyId.matches("\\d+")) {
                this.id = Long.parseLong(legacyId);
            }
        } catch (Exception ignored) {}
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderRole() { return senderRole; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
