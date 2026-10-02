package com.medilink.controller;

import com.medilink.model.support.ReportedIssue;
import com.medilink.repository.ReportedIssueRepository;
import com.medilink.service.AuditLogService;
import com.medilink.service.ReportedIssueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SupportIssueControllerTest {

    @Mock
    private ReportedIssueRepository issueRepository;

    @Mock
    private AuditLogService auditLogService;

    private ReportedIssueService reportedIssueService;
    private SupportIssueController supportIssueController;
    private AdminController adminController;

    @BeforeEach
    public void setUp() {
        reportedIssueService = new ReportedIssueService(issueRepository, auditLogService);
        supportIssueController = new SupportIssueController(reportedIssueService);

        // Minimal AdminController instance
        adminController = new AdminController(
                null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, auditLogService,
                null, null, null
        );
        adminController.setReportedIssueService(reportedIssueService);
    }

    private MockHttpServletRequest createAdminRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("adminId", "ADM_TEST_01");
        request.setAttribute("adminName", "Test Admin");
        request.setRemoteAddr("127.0.0.1");
        return request;
    }

    @Test
    @DisplayName("Patient can report an issue and ticket is persisted and returned")
    public void testPatientSubmitIssue() {
        when(issueRepository.save(any(ReportedIssue.class))).thenAnswer(invocation -> {
            ReportedIssue issue = invocation.getArgument(0);
            return issue;
        });

        Map<String, String> payload = new HashMap<>();
        payload.put("userId", "PT-1001");
        payload.put("userName", "Rahim Ahmed");
        payload.put("userEmail", "rahim@example.com");
        payload.put("userRole", "PATIENT");
        payload.put("category", "Order Issues");
        payload.put("severity", "High/Urgent");
        payload.put("subject", "Medicine delayed for 3 hours");
        payload.put("description", "My emergency asthma inhaler delivery is overdue.");
        payload.put("attachmentName", "prescription_snap.png");

        ResponseEntity<Map<String, Object>> response = supportIssueController.submitIssue(payload);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertNotNull(response.getBody().get("ticketId"));

        ArgumentCaptor<ReportedIssue> captor = ArgumentCaptor.forClass(ReportedIssue.class);
        verify(issueRepository, times(1)).save(captor.capture());
        ReportedIssue saved = captor.getValue();
        assertEquals("PT-1001", saved.getUserId());
        assertEquals("Rahim Ahmed", saved.getUserName());
        assertEquals("Order Issues", saved.getCategory());
        assertEquals("High/Urgent", saved.getSeverity());
        assertEquals("OPEN", saved.getStatus());
        assertEquals("prescription_snap.png", saved.getAttachmentName());
    }

    @Test
    @DisplayName("Pharmacist can report an inventory / DGDA license issue")
    public void testPharmacistSubmitIssue() {
        when(issueRepository.save(any(ReportedIssue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, String> payload = new HashMap<>();
        payload.put("userId", "PH-5002");
        payload.put("userName", "Lazz Pharma Dhanmondi");
        payload.put("userEmail", "pharmacist@lazzpharma.com");
        payload.put("userRole", "PHARMACIST");
        payload.put("category", "Medicine / Stock");
        payload.put("severity", "Medium");
        payload.put("subject", "Stock sync discrepancy for Napa Extra");
        payload.put("description", "Inventory shows 50 units but local ERP has 120 units.");

        ResponseEntity<Map<String, Object>> response = supportIssueController.submitIssue(payload);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(issueRepository, times(1)).save(any(ReportedIssue.class));
    }

    @Test
    @DisplayName("Submission fails validation if required subject or description is missing")
    public void testValidationFailure() {
        Map<String, String> payload = new HashMap<>();
        payload.put("userId", "PT-1001");
        payload.put("category", "Order Issues");
        // Missing subject and description

        ResponseEntity<Map<String, Object>> response = supportIssueController.submitIssue(payload);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse((Boolean) response.getBody().get("success"));
        verify(issueRepository, never()).save(any());
    }

    @Test
    @DisplayName("User can retrieve their own support ticket history")
    public void testGetMyIssues() {
        ReportedIssue issue1 = new ReportedIssue();
        issue1.setId("SR-1001");
        issue1.setUserId("PT-1001");
        issue1.setSubject("Late delivery");
        issue1.setStatus("RESOLVED");

        when(issueRepository.findByUserIdOrderByCreatedAtDesc("PT-1001"))
                .thenReturn(Collections.singletonList(issue1));

        ResponseEntity<Map<String, Object>> response = supportIssueController.getMyIssues("PT-1001");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<?> issues = (List<?>) response.getBody().get("issues");
        assertEquals(1, issues.size());
    }

    @Test
    @DisplayName("Admin can retrieve all reported issues with filtering")
    public void testAdminGetSupportIssues() {
        ReportedIssue openIssue = new ReportedIssue();
        openIssue.setId("SR-2001");
        openIssue.setUserId("PT-1001");
        openIssue.setSubject("Urgent order issue");
        openIssue.setCategory("Order Issues");
        openIssue.setSeverity("High/Urgent");
        openIssue.setStatus("OPEN");

        ReportedIssue resolvedIssue = new ReportedIssue();
        resolvedIssue.setId("SR-2002");
        resolvedIssue.setUserId("PH-5002");
        resolvedIssue.setSubject("Stock question");
        resolvedIssue.setCategory("Medicine / Stock");
        resolvedIssue.setSeverity("Low");
        resolvedIssue.setStatus("RESOLVED");

        when(issueRepository.findAllByOrderByCreatedAtDesc()).thenReturn(Arrays.asList(openIssue, resolvedIssue));

        // Filter by status=OPEN
        ResponseEntity<Map<String, Object>> response = adminController.getAllSupportIssues("OPEN", null, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<?> list = (List<?>) response.getBody().get("issues");
        assertEquals(1, list.size());
    }

    @Test
    @DisplayName("Admin can update ticket status and record admin notes")
    public void testAdminResolveIssue() {
        ReportedIssue issue = new ReportedIssue();
        issue.setId("SR-3001");
        issue.setUserId("PT-1001");
        issue.setSubject("Payment discrepancy");
        issue.setStatus("OPEN");

        when(issueRepository.findById("SR-3001")).thenReturn(Optional.of(issue));
        when(issueRepository.save(any(ReportedIssue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, String> body = new HashMap<>();
        body.put("status", "RESOLVED");
        body.put("adminNotes", "Refund of 150 BDT issued via bKash.");

        ResponseEntity<Map<String, Object>> response = adminController.updateSupportIssueStatus(
                "SR-3001", body, createAdminRequest()
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ReportedIssue updated = (ReportedIssue) response.getBody().get("issue");
        assertEquals("RESOLVED", updated.getStatus());
        assertEquals("Refund of 150 BDT issued via bKash.", updated.getAdminNotes());
        assertEquals("ADM_TEST_01", updated.getResolvedByAdminId());
        assertNotNull(updated.getResolvedAt());

        // Verify audit log was recorded
        verify(auditLogService, times(1)).logAction(
                eq("ADM_TEST_01"), eq("Test Admin"), eq("UPDATE_SUPPORT_ISSUE"), eq("SUPPORT_TICKET"),
                eq("SR-3001"), contains("RESOLVED"), eq("SUCCESS"), eq("127.0.0.1")
        );
    }
}
