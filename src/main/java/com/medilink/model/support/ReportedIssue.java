package com.medilink.model.support;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing an issue or technical problem reported by a Patient or Pharmacist.
 * Directly monitored and resolved by MediLink Platform Administrators.
 */
@Entity
@Table(name = "reported_issues")
public class ReportedIssue {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "user_id", length = 50)
    private String userId;

    @Column(name = "user_name", length = 100)
    private String userName;

    @Column(name = "user_email", length = 100)
    private String userEmail;

    @Column(name = "user_role", length = 30)
    private String userRole; // PATIENT or PHARMACIST

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "severity", length = 30)
    private String severity; // Low, Medium, High/Urgent

    @Column(name = "subject", length = 255)
    private String subject;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "attachment_name", length = 255)
    private String attachmentName;

    @Column(name = "status", length = 30)
    private String status = "OPEN"; // OPEN, IN_PROGRESS, RESOLVED

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "resolved_by_admin_id", length = 50)
    private String resolvedByAdminId;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ReportedIssue() {
    }

    public ReportedIssue(String id, String userId, String userName, String userEmail, String userRole,
                         String category, String severity, String subject, String description, String attachmentName) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.userRole = userRole;
        this.category = category;
        this.severity = severity != null ? severity : "Medium";
        this.subject = subject;
        this.description = description;
        this.attachmentName = attachmentName;
        this.status = "OPEN";
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAttachmentName() { return attachmentName; }
    public void setAttachmentName(String attachmentName) { this.attachmentName = attachmentName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }

    public String getResolvedByAdminId() { return resolvedByAdminId; }
    public void setResolvedByAdminId(String resolvedByAdminId) { this.resolvedByAdminId = resolvedByAdminId; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
