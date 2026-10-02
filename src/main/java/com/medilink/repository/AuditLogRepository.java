package com.medilink.repository;

import com.medilink.model.audit.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    List<AuditLog> findAllByOrderByTimestampDesc();

    List<AuditLog> findTop200ByOrderByTimestampDesc();

    List<AuditLog> findByEntityTypeOrderByTimestampDesc(String entityType);

    List<AuditLog> findByActionOrderByTimestampDesc(String action);

    List<AuditLog> findByAdminIdOrderByTimestampDesc(String adminId);

    List<AuditLog> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);

    long countByAction(String action);
}
