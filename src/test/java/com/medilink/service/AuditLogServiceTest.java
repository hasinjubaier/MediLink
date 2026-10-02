package com.medilink.service;

import com.medilink.model.audit.AuditLog;
import com.medilink.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditLogService auditLogService;

    @BeforeEach
    public void setUp() {
        auditLogService = new AuditLogService(auditLogRepository);
    }

    @Test
    @DisplayName("Should successfully record an administrative audit log with sanitized inputs")
    public void testLogActionCreatesAuditEntry() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditLog log = auditLogService.logAction(
                "ADM_01",
                "Hasin Admin",
                "CREATE_MEDICINE",
                "MEDICINE",
                "med_napa",
                "Added new medicine Napa Extra 500mg",
                "SUCCESS",
                "192.168.1.10"
        );

        assertNotNull(log);
        assertNotNull(log.getId());
        assertEquals("ADM_01", log.getAdminId());
        assertEquals("Hasin Admin", log.getAdminName());
        assertEquals("CREATE_MEDICINE", log.getAction());
        assertEquals("MEDICINE", log.getEntityType());
        assertEquals("med_napa", log.getEntityId());
        assertEquals("SUCCESS", log.getStatus());
        assertEquals("192.168.1.10", log.getIpAddress());
        assertNotNull(log.getTimestamp());

        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Should scrub and redact sensitive credentials like passwords and secret keys from logs")
    public void testLogActionScrubsSensitiveCredentials() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        when(auditLogRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        String rawDescription = "Created user with password=SecretPassword123! and token:AIzaSyB123456789 and secret=topsecret";

        auditLogService.logAction(
                "ADM_01",
                "Admin",
                "CREATE_USER",
                "USER",
                "USR_999",
                rawDescription,
                "SUCCESS",
                "127.0.0.1"
        );

        AuditLog captured = captor.getValue();
        assertNotNull(captured);
        assertFalse(captured.getDescription().contains("SecretPassword123!"));
        assertFalse(captured.getDescription().contains("AIzaSyB123456789"));
        assertFalse(captured.getDescription().contains("topsecret"));
        assertTrue(captured.getDescription().contains("***REDACTED***"));
    }

    @Test
    @DisplayName("Should filter audit logs by action, entity type, and search term")
    public void testFindAllWithFilters() {
        AuditLog l1 = new AuditLog("log_1", "ADM_01", "Admin One", "CREATE_USER", "USER", "u1", "Created patient Rahim", "SUCCESS", "127.0.0.1");
        AuditLog l2 = new AuditLog("log_2", "ADM_01", "Admin One", "DELETE_MEDICINE", "MEDICINE", "m1", "Deleted expired drug", "SUCCESS", "127.0.0.1");
        AuditLog l3 = new AuditLog("log_3", "ADM_02", "Admin Two", "BROADCAST", "SYSTEM", "s1", "Dispatched storm alert", "SUCCESS", "127.0.0.1");

        when(auditLogRepository.findAllByOrderByTimestampDesc()).thenReturn(Arrays.asList(l1, l2, l3));

        // Filter by action
        List<AuditLog> filteredByAction = auditLogService.findAll(null, "CREATE_USER", null, null);
        assertEquals(1, filteredByAction.size());
        assertEquals("log_1", filteredByAction.get(0).getId());

        // Filter by entity type
        List<AuditLog> filteredByEntity = auditLogService.findAll(null, null, "MEDICINE", null);
        assertEquals(1, filteredByEntity.size());
        assertEquals("log_2", filteredByEntity.get(0).getId());

        // Filter by admin ID
        List<AuditLog> filteredByAdmin = auditLogService.findAll(null, null, null, "ADM_02");
        assertEquals(1, filteredByAdmin.size());
        assertEquals("log_3", filteredByAdmin.get(0).getId());

        // Search text
        List<AuditLog> searched = auditLogService.findAll("Rahim", null, null, null);
        assertEquals(1, searched.size());
        assertEquals("log_1", searched.get(0).getId());
    }

    @Test
    @DisplayName("Should return count of logs")
    public void testCount() {
        when(auditLogRepository.count()).thenReturn(42L);
        assertEquals(42L, auditLogService.count());
    }
}
