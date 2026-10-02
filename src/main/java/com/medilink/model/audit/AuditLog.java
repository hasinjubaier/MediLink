package com.medilink.model.audit;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Domain entity for administrative audit logs.
 * Permanently logs administrative actions in the platform.
 * NOTE: Never logs passwords, API keys, or raw secrets.
 */
@Entity
@Table(name = "admin_audit_logs")
public class AuditLog {

    @Id
    @Column(name = "id", length = 60)
    private String id;

    @Column(name = "admin_id", length = 60, nullable = false)
    private String adminId;

    @Column(name = "admin_name", length = 120)
    private String adminName;

    @Column(name = "action", length = 60, nullable = false)
    private String action; // CREATE, UPDATE, DELETE, DEACTIVATE, VERIFY, SUSPEND, BROADCAST, SYNC

    @Column(name = "entity_type", length = 60, nullable = false)
    private String entityType; // USER, MEDICINE, PHARMACY, PHARMACIST, PRESCRIPTION, SYSTEM, MARKET

    @Column(name = "entity_id", length = 60)
    private String entityId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "status", length = 30)
    private String status = "SUCCESS"; // SUCCESS, FAILED, WARNING

    @Column(name = "ip_address", length = 60)
    private String ipAddress;

    public AuditLog() {
        this.timestamp = LocalDateTime.now();
    }

    public AuditLog(String id, String adminId, String adminName, String action,
                    String entityType, String entityId, String description,
                    String status, String ipAddress) {
        this.id = id;
        this.adminId = adminId;
        this.adminName = adminName;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.description = description;
        this.timestamp = LocalDateTime.now();
        this.status = status != null ? status : "SUCCESS";
        this.ipAddress = ipAddress;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAdminId() { return adminId; }
    public void setAdminId(String adminId) { this.adminId = adminId; }

    public String getAdminName() { return adminName; }
    public void setAdminName(String adminName) { this.adminName = adminName; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
}
