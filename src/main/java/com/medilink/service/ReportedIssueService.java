package com.medilink.service;

import com.medilink.model.support.ReportedIssue;
import com.medilink.repository.ReportedIssueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReportedIssueService {

    private static final Logger log = LoggerFactory.getLogger(ReportedIssueService.class);

    private final ReportedIssueRepository issueRepository;
    private final AuditLogService auditLogService;

    @Autowired
    public ReportedIssueService(ReportedIssueRepository issueRepository, AuditLogService auditLogService) {
        this.issueRepository = issueRepository;
        this.auditLogService = auditLogService;
    }

    public ReportedIssue createIssue(String userId, String userName, String userEmail, String userRole,
                                     String category, String severity, String subject, String description,
                                     String attachmentName) {
        String cleanSubject = subject != null ? subject.trim() : "Untitled Issue";
        String cleanDesc = description != null ? description.trim() : "";
        String cleanCat = category != null ? category.trim() : "Other";
        String cleanSev = severity != null ? severity.trim() : "Medium";
        String cleanName = userName != null && !userName.trim().isEmpty() ? userName.trim() : "MediLink User";
        String cleanRole = userRole != null ? userRole.trim().toUpperCase() : "PATIENT";

        String id = "SR-" + (10000 + (int)(Math.random() * 90000));

        ReportedIssue issue = new ReportedIssue(
                id,
                userId,
                cleanName,
                userEmail,
                cleanRole,
                cleanCat,
                cleanSev,
                cleanSubject,
                cleanDesc,
                attachmentName
        );

        ReportedIssue saved = issueRepository.save(issue);
        log.info("[ReportedIssue] New support ticket {} submitted by {} ({}) - Category: {}, Severity: {}",
                saved.getId(), saved.getUserName(), saved.getUserRole(), saved.getCategory(), saved.getSeverity());

        // Dispatch real-time SSE notification to connected Admins
        try {
            String broadcastMsg = String.format("[SUPPORT_ALERT] New %s priority issue '%s' reported by %s (%s). Ticket ID: %s",
                    cleanSev, cleanSubject, cleanName, cleanRole, saved.getId());
            StockObserverService.getInstance().onNotification("NEW_SUPPORT_ISSUE", broadcastMsg);
        } catch (Exception e) {
            log.warn("[ReportedIssue] Could not broadcast SSE alert: {}", e.getMessage());
        }

        // Record audit trail
        try {
            auditLogService.logAction(
                    userId != null ? userId : "USER",
                    cleanName,
                    "REPORT_ISSUE",
                    "SUPPORT_TICKET",
                    saved.getId(),
                    "User submitted support ticket: " + cleanSubject + " [" + cleanCat + " / " + cleanSev + "]",
                    "SUCCESS",
                    "127.0.0.1"
            );
        } catch (Exception ignored) {}

        return saved;
    }

    public List<ReportedIssue> findAll(String status, String severity, String category, String search) {
        List<ReportedIssue> all = issueRepository.findAllByOrderByCreatedAtDesc();

        return all.stream().filter(issue -> {
            if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
                if (issue.getStatus() == null || !issue.getStatus().equalsIgnoreCase(status.trim())) return false;
            }
            if (severity != null && !severity.trim().isEmpty() && !"ALL".equalsIgnoreCase(severity)) {
                if (issue.getSeverity() == null || !issue.getSeverity().equalsIgnoreCase(severity.trim())) return false;
            }
            if (category != null && !category.trim().isEmpty() && !"ALL".equalsIgnoreCase(category)) {
                if (issue.getCategory() == null || !issue.getCategory().equalsIgnoreCase(category.trim())) return false;
            }
            if (search != null && !search.trim().isEmpty()) {
                String q = search.trim().toLowerCase();
                boolean match = (issue.getSubject() != null && issue.getSubject().toLowerCase().contains(q))
                        || (issue.getDescription() != null && issue.getDescription().toLowerCase().contains(q))
                        || (issue.getUserName() != null && issue.getUserName().toLowerCase().contains(q))
                        || (issue.getUserEmail() != null && issue.getUserEmail().toLowerCase().contains(q))
                        || (issue.getId() != null && issue.getId().toLowerCase().contains(q));
                if (!match) return false;
            }
            return true;
        }).collect(Collectors.toList());
    }

    public List<ReportedIssue> findByUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return issueRepository.findAllByOrderByCreatedAtDesc();
        }
        return issueRepository.findByUserIdOrderByCreatedAtDesc(userId.trim());
    }

    public Optional<ReportedIssue> findById(String id) {
        if (id == null) return Optional.empty();
        return issueRepository.findById(id);
    }

    public ReportedIssue updateStatus(String id, String newStatus, String adminNotes, String adminId, String adminName) {
        Optional<ReportedIssue> opt = issueRepository.findById(id);
        if (!opt.isPresent()) {
            throw new IllegalArgumentException("Support ticket not found with ID: " + id);
        }

        ReportedIssue issue = opt.get();
        String cleanStatus = newStatus != null ? newStatus.toUpperCase().trim() : "OPEN";
        issue.setStatus(cleanStatus);

        if (adminNotes != null && !adminNotes.trim().isEmpty()) {
            issue.setAdminNotes(adminNotes.trim());
        }

        if ("RESOLVED".equalsIgnoreCase(cleanStatus)) {
            issue.setResolvedAt(LocalDateTime.now());
            issue.setResolvedByAdminId(adminId != null ? adminId : "ADMIN");
        }

        ReportedIssue updated = issueRepository.save(issue);

        // Record administrative action in audit log
        try {
            auditLogService.logAction(
                    adminId,
                    adminName,
                    "UPDATE_SUPPORT_ISSUE",
                    "SUPPORT_TICKET",
                    id,
                    "Admin updated ticket " + id + " to " + cleanStatus + (adminNotes != null ? " (Notes: " + adminNotes + ")" : ""),
                    "SUCCESS",
                    "127.0.0.1"
            );
        } catch (Exception ignored) {}

        return updated;
    }

    public long countOpen() {
        return issueRepository.countByStatus("OPEN");
    }

    public long countAll() {
        return issueRepository.count();
    }
}
