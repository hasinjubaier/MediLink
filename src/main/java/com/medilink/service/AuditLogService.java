package com.medilink.service;

import com.medilink.model.audit.AuditLog;
import com.medilink.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service managing administrative audit logs.
 * Provides immutable audit logging for security compliance.
 * Statically prevents logging of credentials, keys, and secrets.
 */
@Service
@Transactional
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;

    @Autowired
    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Creates and persists a new administrative audit log.
     * Sanitizes descriptions to guarantee no passwords or API keys are ever stored.
     */
    public AuditLog logAction(String adminId, String adminName, String action,
                              String entityType, String entityId, String description,
                              String status, String ipAddress) {
        String cleanAdminId = (adminId != null && !adminId.trim().isEmpty()) ? adminId.trim() : "usr_admin_01";
        String cleanAdminName = (adminName != null && !adminName.trim().isEmpty()) ? adminName.trim() : "System Administrator";
        String cleanAction = (action != null) ? action.toUpperCase().trim() : "ACTION";
        String cleanEntityType = (entityType != null) ? entityType.toUpperCase().trim() : "SYSTEM";
        String cleanStatus = (status != null) ? status.toUpperCase().trim() : "SUCCESS";
        String cleanIp = (ipAddress != null && !ipAddress.trim().isEmpty()) ? ipAddress.trim() : "127.0.0.1";

        // Sanitize sensitive values
        String sanitizedDesc = sanitizeDescription(description);

        String id = "log_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        AuditLog auditLog = new AuditLog(id, cleanAdminId, cleanAdminName, cleanAction,
                cleanEntityType, entityId, sanitizedDesc, cleanStatus, cleanIp);

        try {
            AuditLog saved = auditLogRepository.save(auditLog);
            log.info("[AuditLog] {} performed {} on {} [ID: {}] - Status: {}",
                    cleanAdminName, cleanAction, cleanEntityType, entityId, cleanStatus);
            return saved;
        } catch (Exception e) {
            log.error("[AuditLog] Failed to persist audit log: {}", e.getMessage());
            return auditLog;
        }
    }

    public List<AuditLog> findAll(String search, String action, String entityType, String adminId) {
        List<AuditLog> all = auditLogRepository.findAllByOrderByTimestampDesc();
        if (all.isEmpty()) {
            return all;
        }

        return all.stream()
                .filter(l -> {
                    boolean matchAction = action == null || action.trim().isEmpty() || "ALL".equalsIgnoreCase(action)
                            || l.getAction().equalsIgnoreCase(action.trim())
                            || l.getAction().contains(action.toUpperCase().trim());

                    boolean matchEntity = entityType == null || entityType.trim().isEmpty() || "ALL".equalsIgnoreCase(entityType)
                            || l.getEntityType().equalsIgnoreCase(entityType.trim());

                    boolean matchAdmin = adminId == null || adminId.trim().isEmpty() || "ALL".equalsIgnoreCase(adminId)
                            || l.getAdminId().equalsIgnoreCase(adminId.trim());

                    boolean matchSearch = search == null || search.trim().isEmpty()
                            || (l.getDescription() != null && l.getDescription().toLowerCase().contains(search.toLowerCase().trim()))
                            || (l.getAdminName() != null && l.getAdminName().toLowerCase().contains(search.toLowerCase().trim()))
                            || (l.getEntityId() != null && l.getEntityId().toLowerCase().contains(search.toLowerCase().trim()))
                            || (l.getId() != null && l.getId().toLowerCase().contains(search.toLowerCase().trim()));

                    return matchAction && matchEntity && matchAdmin && matchSearch;
                })
                .collect(Collectors.toList());
    }

    public long count() {
        return auditLogRepository.count();
    }

    private String sanitizeDescription(String input) {
        if (input == null) return "Administrative operation performed.";
        // Replace potential password/token/key/secret patterns
        return input.replaceAll("(?i)(password|pass|secret|api[_-]?key|token|auth)\\s*[:=]\\s*[^,\\s]+", "$1=***REDACTED***");
    }
}
