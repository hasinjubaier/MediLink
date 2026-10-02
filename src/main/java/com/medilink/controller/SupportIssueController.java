package com.medilink.controller;

import com.medilink.model.support.ReportedIssue;
import com.medilink.service.ReportedIssueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for Patient and Pharmacist Support & Issue Reporting.
 * Allows users to submit technical issues, report anomalies, and check ticket history.
 */
@RestController
@RequestMapping("/api/support")
@CrossOrigin(origins = "*")
public class SupportIssueController {

    private final ReportedIssueService reportedIssueService;

    @Autowired
    public SupportIssueController(ReportedIssueService reportedIssueService) {
        this.reportedIssueService = reportedIssueService;
    }

    @PostMapping("/issues")
    public ResponseEntity<Map<String, Object>> submitIssue(@RequestBody Map<String, String> body) {
        String category = body.get("category");
        String severity = body.getOrDefault("severity", "Medium");
        String subject = body.get("subject");
        String description = body.get("description");
        String userId = body.get("userId");
        String userName = body.get("userName");
        String userEmail = body.get("userEmail");
        String userRole = body.getOrDefault("userRole", "PATIENT");
        String attachmentName = body.get("attachmentName");

        if (subject == null || subject.trim().isEmpty() || description == null || description.trim().isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("success", false);
            err.put("message", "Subject and detailed description are required.");
            return ResponseEntity.badRequest().body(err);
        }

        try {
            ReportedIssue issue = reportedIssueService.createIssue(
                    userId, userName, userEmail, userRole, category, severity, subject, description, attachmentName
            );

            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "SUCCESS");
            resp.put("success", true);
            resp.put("message", "Support report submitted successfully. Ticket ID: " + issue.getId());
            resp.put("ticketId", issue.getId());
            resp.put("issue", issue);
            return ResponseEntity.status(HttpStatus.CREATED).body(resp);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("success", false);
            err.put("message", "Failed to submit support report: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    @GetMapping("/my-issues")
    public ResponseEntity<Map<String, Object>> getMyIssues(@RequestParam(required = false) String userId) {
        List<ReportedIssue> issues = reportedIssueService.findByUserId(userId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("totalCount", issues.size());
        resp.put("issues", issues);
        return ResponseEntity.ok(resp);
    }
}
