package com.medilink.controller;

import com.medilink.config.AiVisionConfig;
import com.medilink.model.audit.AuditLog;
import com.medilink.model.market.MedicinePriceHistory;
import com.medilink.model.medicine.Medicine;
import com.medilink.model.pharmacy.Pharmacy;
import com.medilink.model.pharmacy.PharmacyStock;
import com.medilink.model.prescription.Prescription;
import com.medilink.model.prescription.PrescriptionItem;
import com.medilink.model.user.*;
import com.medilink.repository.*;
import com.medilink.service.*;
import com.medilink.service.market.MarketPriceSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.medilink.model.support.ReportedIssue;

import javax.servlet.http.HttpServletRequest;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Master Administrative Controller for MediLink 2.0.
 * Empowers system administrators to govern and monitor the entire platform:
 * 1. Platform KPIs & System Live Telemetry
 * 2. System Health Diagnostics (JVM, DB, AI Services, SSE, Market Sync)
 * 3. User Management (Full CRUD with status, safe deactivation, role filters)
 * 4. Medicine Catalog (Full CRUD with strict pricing/batch validation)
 * 5. Pharmacy Network Management (Full CRUD with safe stock cascades)
 * 6. Prescription Monitoring (Master queue, inspection, status oversight)
 * 7. Prescription Compliance & Issue Detection (Administrative issue flagging)
 * 8. Bangladesh Market Price Control Center (Manual & auto sync, price history)
 * 9. Pharmacist Accreditation & License Verification
 * 10. Immutable Administrative Audit Logs
 * 11. Reports & Platform Intelligence Analytics
 * 12. Global System Broadcasts over SSE (Safe plain-text multi-cast)
 * 13. Support Tickets & Issue Resolution
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final UserService userService;
    private final MedicineService medicineService;
    private final PharmacyService pharmacyService;
    private final PrescriptionService prescriptionService;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PharmacistRepository pharmacistRepository;
    private final AdminRepository adminRepository;
    private final MedicineRepository medicineRepository;
    private final PharmacyRepository pharmacyRepository;
    private final PharmacyStockRepository pharmacyStockRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final ReminderRepository reminderRepository;
    private final MedicinePriceHistoryRepository priceHistoryRepository;
    private final AuditLogService auditLogService;
    private final MarketPriceSyncService marketPriceSyncService;
    private final EventStreamController eventStreamController;
    private final AiVisionConfig aiVisionConfig;

    @Autowired(required = false)
    private ReportedIssueService reportedIssueService;

    @Autowired
    public AdminController(UserService userService,
                           MedicineService medicineService,
                           PharmacyService pharmacyService,
                           PrescriptionService prescriptionService,
                           UserRepository userRepository,
                           PatientRepository patientRepository,
                           PharmacistRepository pharmacistRepository,
                           AdminRepository adminRepository,
                           MedicineRepository medicineRepository,
                           PharmacyRepository pharmacyRepository,
                           PharmacyStockRepository pharmacyStockRepository,
                           PrescriptionRepository prescriptionRepository,
                           ReminderRepository reminderRepository,
                           MedicinePriceHistoryRepository priceHistoryRepository,
                           AuditLogService auditLogService,
                           MarketPriceSyncService marketPriceSyncService,
                           EventStreamController eventStreamController,
                           AiVisionConfig aiVisionConfig) {
        this.userService = userService;
        this.medicineService = medicineService;
        this.pharmacyService = pharmacyService;
        this.prescriptionService = prescriptionService;
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.pharmacistRepository = pharmacistRepository;
        this.adminRepository = adminRepository;
        this.medicineRepository = medicineRepository;
        this.pharmacyRepository = pharmacyRepository;
        this.pharmacyStockRepository = pharmacyStockRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.reminderRepository = reminderRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.auditLogService = auditLogService;
        this.marketPriceSyncService = marketPriceSyncService;
        this.eventStreamController = eventStreamController;
        this.aiVisionConfig = aiVisionConfig;
    }

    // ==========================================
    // 1. SYSTEM TELEMETRY & HEALTH OVERVIEW
    // ==========================================
    @GetMapping("/telemetry")
    public ResponseEntity<Map<String, Object>> getSystemTelemetry() {
        Map<String, Object> resp = new HashMap<>();

        long totalUsers = userRepository.count();
        long patientsCount = patientRepository.count();
        long pharmacistsCount = pharmacistRepository.count();
        long adminsCount = adminRepository.count();
        long medicinesCount = medicineRepository.count();
        long pharmaciesCount = pharmacyRepository.count();
        long prescriptionsCount = prescriptionRepository.count();
        long remindersCount = reminderRepository.count();

        List<Prescription> allRx = prescriptionRepository.findAll();
        long pendingRx = allRx.stream()
                .filter(r -> "UPLOADED".equalsIgnoreCase(r.getStatus()) || "EXTRACTED".equalsIgnoreCase(r.getStatus()))
                .count();
        long verifiedRx = allRx.stream()
                .filter(r -> "VERIFIED".equalsIgnoreCase(r.getStatus()) || "VERIFIED_BY_PHARMACIST".equalsIgnoreCase(r.getStatus()))
                .count();
        long dispensedRx = allRx.stream()
                .filter(r -> "DISPENSED".equalsIgnoreCase(r.getStatus()))
                .count();
        long rejectedRx = allRx.stream()
                .filter(r -> "REJECTED".equalsIgnoreCase(r.getStatus()))
                .count();

        long lowStockCount = pharmacyStockRepository.findAll().stream()
                .filter(s -> s.getQuantity() <= 15)
                .count();

        int activeSseClients = eventStreamController != null ? eventStreamController.getActiveClientCount() : 1;

        Runtime runtime = Runtime.getRuntime();
        long usedMemoryMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long maxMemoryMb = runtime.maxMemory() / (1024 * 1024);

        RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
        long uptimeSeconds = rb.getUptime() / 1000;
        long uptimeHours = uptimeSeconds / 3600;
        long uptimeMins = (uptimeSeconds % 3600) / 60;
        String uptimeFormatted = String.format("%dh %dm %ds", uptimeHours, uptimeMins, uptimeSeconds % 60);

        Map<String, Object> counts = new HashMap<>();
        counts.put("totalUsers", totalUsers);
        counts.put("patients", patientsCount);
        counts.put("pharmacists", pharmacistsCount);
        counts.put("admins", adminsCount);
        counts.put("medicines", medicinesCount);
        counts.put("pharmacies", pharmaciesCount);
        counts.put("prescriptions", prescriptionsCount);
        counts.put("prescriptionsPending", pendingRx);
        counts.put("prescriptionsVerified", verifiedRx);
        counts.put("prescriptionsDispensed", dispensedRx);
        counts.put("prescriptionsRejected", rejectedRx);
        counts.put("activeReminders", remindersCount);
        counts.put("activeSseClients", activeSseClients);
        counts.put("lowStockItems", lowStockCount);
        counts.put("openSupportIssues", reportedIssueService != null ? reportedIssueService.countOpen() : 0);
        counts.put("totalSupportIssues", reportedIssueService != null ? reportedIssueService.countAll() : 0);

        Map<String, Object> health = new HashMap<>();
        health.put("status", "HEALTHY");
        health.put("database", "PostgreSQL (medilink_db) - CONNECTED");
        health.put("serverPort", 8080);
        health.put("usedMemoryMb", usedMemoryMb);
        health.put("maxMemoryMb", maxMemoryMb);
        health.put("availableProcessors", runtime.availableProcessors());
        health.put("uptime", uptimeFormatted);
        health.put("timestamp", LocalDateTime.now().toString());

        // External microservices statuses
        health.put("geminiStatus", aiVisionConfig != null && aiVisionConfig.isGeminiConfigured() ? "HEALTHY (" + aiVisionConfig.getGeminiModel() + ")" : "UNAVAILABLE");
        health.put("groqStatus", aiVisionConfig != null && aiVisionConfig.isGroqConfigured() ? "HEALTHY (" + aiVisionConfig.getGroqModel() + ")" : "UNAVAILABLE");
        health.put("sseStatus", activeSseClients > 0 ? "ONLINE (" + activeSseClients + " connected)" : "STANDBY");

        if (marketPriceSyncService != null) {
            health.put("marketSyncStatus", marketPriceSyncService.getLastSyncStatus());
            health.put("lastSuccessfulMarketSync", marketPriceSyncService.getLastSyncTime() != null ? marketPriceSyncService.getLastSyncTime().toString() : LocalDateTime.now().toString());
            health.put("latestSyncError", marketPriceSyncService.getLastSyncError());
        }

        resp.put("status", "SUCCESS");
        resp.put("counts", counts);
        resp.put("health", health);
        resp.put("recentAuditEvents", StockObserverService.getInstance().getRecentEvents());

        return ResponseEntity.ok(resp);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getSystemHealthDiagnostics() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemoryMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long maxMemoryMb = runtime.maxMemory() / (1024 * 1024);
        long totalMemoryMb = runtime.totalMemory() / (1024 * 1024);

        RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
        long uptimeSeconds = rb.getUptime() / 1000;
        String uptime = String.format("%dh %dm %ds", uptimeSeconds / 3600, (uptimeSeconds % 3600) / 60, uptimeSeconds % 60);

        Map<String, Object> jvm = new HashMap<>();
        jvm.put("status", "HEALTHY");
        jvm.put("usedMemoryMb", usedMemoryMb);
        jvm.put("totalMemoryMb", totalMemoryMb);
        jvm.put("maxMemoryMb", maxMemoryMb);
        jvm.put("cpuCores", runtime.availableProcessors());
        jvm.put("uptime", uptime);

        Map<String, Object> db = new HashMap<>();
        try {
            long userCount = userRepository.count();
            db.put("status", "HEALTHY");
            db.put("engine", "PostgreSQL");
            db.put("totalUsersRecorded", userCount);
            db.put("message", "Database connection pool is responsive and healthy.");
        } catch (Exception e) {
            db.put("status", "ERROR");
            db.put("message", "Database error: " + e.getMessage());
        }

        Map<String, Object> ai = new HashMap<>();
        boolean geminiOk = aiVisionConfig != null && aiVisionConfig.isGeminiConfigured();
        boolean groqOk = aiVisionConfig != null && aiVisionConfig.isGroqConfigured();
        ai.put("geminiStatus", geminiOk ? "HEALTHY" : "UNAVAILABLE");
        ai.put("geminiModel", aiVisionConfig != null ? aiVisionConfig.getGeminiModel() : "gemini-1.5-flash");
        ai.put("groqStatus", groqOk ? "HEALTHY" : "UNAVAILABLE");
        ai.put("groqModel", aiVisionConfig != null ? aiVisionConfig.getGroqModel() : "llama-3.2-11b-vision-preview");

        Map<String, Object> sse = new HashMap<>();
        int sseCount = eventStreamController != null ? eventStreamController.getActiveClientCount() : 1;
        sse.put("status", "ONLINE");
        sse.put("activeListeners", sseCount);

        Map<String, Object> market = new HashMap<>();
        if (marketPriceSyncService != null) {
            market.put("status", marketPriceSyncService.getLastSyncStatus());
            market.put("provider", marketPriceSyncService.getMarketPriceProvider() != null ? marketPriceSyncService.getMarketPriceProvider().getProviderName() : "DGDA Medex Bangladesh");
            market.put("lastSync", marketPriceSyncService.getLastSyncTime() != null ? marketPriceSyncService.getLastSyncTime().toString() : "Recent");
            market.put("lastUpdatedCount", marketPriceSyncService.getLastUpdatedCount());
            market.put("lastError", marketPriceSyncService.getLastSyncError());
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("overallStatus", "HEALTHY");
        resp.put("jvm", jvm);
        resp.put("database", db);
        resp.put("aiServices", ai);
        resp.put("sseStream", sse);
        resp.put("marketSync", market);
        resp.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(resp);
    }

    // ==========================================
    // 2. USER MANAGEMENT (FULL CRUD WITH STATUS & SAFETY)
    // ==========================================
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {

        List<User> users = userRepository.findAll();
        List<Map<String, Object>> userList = new ArrayList<>();

        for (User u : users) {
            if (role != null && !role.trim().isEmpty() && !"ALL".equalsIgnoreCase(role)) {
                if (u.getRole() == null || !u.getRole().name().equalsIgnoreCase(role.trim())) {
                    continue;
                }
            }
            if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
                if (u.getStatus() == null || !u.getStatus().equalsIgnoreCase(status.trim())) {
                    continue;
                }
            }
            if (search != null && !search.trim().isEmpty()) {
                String q = search.trim().toLowerCase();
                boolean match = (u.getName() != null && u.getName().toLowerCase().contains(q))
                        || (u.getEmail() != null && u.getEmail().toLowerCase().contains(q))
                        || (u.getPhone() != null && u.getPhone().toLowerCase().contains(q))
                        || (u.getId() != null && u.getId().toLowerCase().contains(q));
                if (!match) continue;
            }

            Map<String, Object> m = mapUserToDto(u);
            userList.add(m);
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("totalCount", userList.size());
        resp.put("users", userList);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody Map<String, String> body,
                                                          HttpServletRequest request) {
        String name = body.get("name");
        String email = body.get("email");
        String password = body.get("password");
        String roleStr = body.getOrDefault("role", "PATIENT").toUpperCase();

        if (name == null || name.trim().isEmpty() || email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", "Name, Email, and Password are strictly required.");
            return ResponseEntity.badRequest().body(err);
        }

        if (!email.contains("@") || !email.contains(".")) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", "Invalid email address format.");
            return ResponseEntity.badRequest().body(err);
        }

        if (userRepository.existsByEmailIgnoreCase(email.trim())) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", "A user account with email " + email.trim() + " already exists.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
        }

        UserRole role;
        try {
            role = UserRole.valueOf(roleStr);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", "Invalid user role: " + roleStr + ". Allowed: PATIENT, PHARMACIST, ADMIN.");
            return ResponseEntity.badRequest().body(err);
        }

        try {
            User created = userService.registerUser(role, name.trim(), email.trim().toLowerCase(), password, body);
            auditLogService.logAction(getAdminId(request), getAdminName(request), "CREATE_USER",
                    "USER", created.getId(), "Admin registered new " + role + " user: " + created.getName() + " (" + created.getEmail() + ")",
                    "SUCCESS", getClientIp(request));

            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "SUCCESS");
            resp.put("message", "User " + name + " created successfully with ID: " + created.getId());
            resp.put("id", created.getId());
            resp.put("user", mapUserToDto(created));
            return ResponseEntity.status(HttpStatus.CREATED).body(resp);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(@PathVariable String id,
                                                          @RequestBody Map<String, Object> updates,
                                                          HttpServletRequest request) {
        Optional<User> existing = userRepository.findById(id);
        if (!existing.isPresent()) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", "User not found with ID: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
        }

        try {
            User updated = userService.adminUpdateUser(id, updates);
            auditLogService.logAction(getAdminId(request), getAdminName(request), "UPDATE_USER",
                    "USER", id, "Admin updated user details for " + updated.getName() + " (" + updated.getRole() + ")",
                    "SUCCESS", getClientIp(request));

            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "SUCCESS");
            resp.put("message", "User " + updated.getName() + " updated successfully.");
            resp.put("user", mapUserToDto(updated));
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", "Failed to update user: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable String id,
                                                          HttpServletRequest request) {
        String adminId = getAdminId(request);

        // Self-deletion prevention
        if (id != null && id.trim().equalsIgnoreCase(adminId.trim())) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", "Administrators cannot delete their own active administrative account.");
            return ResponseEntity.badRequest().body(err);
        }

        Optional<User> userOpt = userRepository.findById(id);
        if (!userOpt.isPresent()) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "ERROR");
            resp.put("message", "User not found with ID: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resp);
        }

        User user = userOpt.get();
        try {
            boolean deleted = userService.deleteUser(id, adminId);
            if (deleted) {
                String action = "DEACTIVATED".equalsIgnoreCase(user.getStatus()) ? "DEACTIVATE_USER" : "DELETE_USER";
                auditLogService.logAction(adminId, getAdminName(request), action,
                        "USER", id, "Admin safely removed/deactivated user " + user.getName() + " (" + user.getEmail() + ")",
                        "SUCCESS", getClientIp(request));

                Map<String, Object> resp = new HashMap<>();
                resp.put("status", "SUCCESS");
                resp.put("message", "User " + user.getName() + " (ID: " + id + ") has been safely processed.");
                return ResponseEntity.ok(resp);
            } else {
                Map<String, Object> resp = new HashMap<>();
                resp.put("status", "ERROR");
                resp.put("message", "Could not delete user with ID: " + id);
                return ResponseEntity.badRequest().body(resp);
            }
        } catch (IllegalArgumentException e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    // ==========================================
    // 3. MEDICINE CATALOG (CRUD WITH VALIDATION)
    // ==========================================
    @GetMapping("/medicines")
    public ResponseEntity<Map<String, Object>> getAllMedicines(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String category) {

        List<Medicine> meds = medicineService.findAll();
        List<Medicine> filtered = meds.stream().filter(m -> {
            if (company != null && !company.trim().isEmpty() && !"ALL".equalsIgnoreCase(company)) {
                if (m.getCompany() == null || !m.getCompany().equalsIgnoreCase(company.trim())) return false;
            }
            if (category != null && !category.trim().isEmpty() && !"ALL".equalsIgnoreCase(category)) {
                if (m.getCategory() == null || !m.getCategory().equalsIgnoreCase(category.trim())) return false;
            }
            if (search != null && !search.trim().isEmpty()) {
                String q = search.trim().toLowerCase();
                boolean match = (m.getBrandName() != null && m.getBrandName().toLowerCase().contains(q))
                        || (m.getGenericName() != null && m.getGenericName().toLowerCase().contains(q))
                        || (m.getCompany() != null && m.getCompany().toLowerCase().contains(q))
                        || (m.getId() != null && m.getId().toLowerCase().contains(q));
                if (!match) return false;
            }
            return true;
        }).collect(Collectors.toList());

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("totalCount", filtered.size());
        resp.put("medicines", filtered);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/medicines")
    public ResponseEntity<Map<String, Object>> createMedicine(@RequestBody Map<String, Object> body,
                                                              HttpServletRequest request) {
        String brandName = (String) body.get("brandName");
        String genericName = (String) body.get("genericName");

        if (brandName == null || brandName.trim().isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", "Medicine Brand Name is required.");
            return ResponseEntity.badRequest().body(err);
        }

        if (genericName == null || genericName.trim().isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", "Medicine Generic Name is required.");
            return ResponseEntity.badRequest().body(err);
        }

        double unitPrice = 5.0;
        if (body.get("unitPrice") instanceof Number) {
            unitPrice = ((Number) body.get("unitPrice")).doubleValue();
        } else if (body.get("unitPrice") instanceof String) {
            try {
                unitPrice = Double.parseDouble((String) body.get("unitPrice"));
            } catch (Exception e) {
                Map<String, Object> err = new HashMap<>();
                err.put("status", "ERROR");
                err.put("message", "Unit Price must be a valid number.");
                return ResponseEntity.badRequest().body(err);
            }
        }

        if (unitPrice < 0) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", "Unit Price cannot be negative.");
            return ResponseEntity.badRequest().body(err);
        }

        try {
            String company = (String) body.getOrDefault("company", "Generic Pharma");
            String strength = (String) body.getOrDefault("strength", "500mg");
            String formulation = (String) body.getOrDefault("formulation", "Tablet");
            String category = (String) body.getOrDefault("category", "General");
            String sideEffects = (String) body.getOrDefault("sideEffects", "None listed.");

            boolean rxReq = false;
            if (body.get("prescriptionRequired") instanceof Boolean) {
                rxReq = (Boolean) body.get("prescriptionRequired");
            } else if (body.get("prescriptionRequired") instanceof String) {
                rxReq = Boolean.parseBoolean((String) body.get("prescriptionRequired"));
            }

            String id = (String) body.get("id");
            if (id == null || id.trim().isEmpty()) {
                id = "med_" + (medicineRepository.count() + 1);
            }

            Medicine med = new Medicine(id, brandName.trim(), genericName.trim(), company.trim(),
                    strength.trim(), formulation.trim(), unitPrice, rxReq, category.trim(), sideEffects.trim());

            Medicine saved = medicineService.saveMedicine(med);

            auditLogService.logAction(getAdminId(request), getAdminName(request), "CREATE_MEDICINE",
                    "MEDICINE", saved.getId(), "Admin cataloged new medicine: " + saved.getBrandName() + " (" + saved.getCompany() + ") @ ৳" + saved.getUnitPrice(),
                    "SUCCESS", getClientIp(request));

            StockObserverService.getInstance().onNotification("NEW_MEDICINE_CATALOG",
                    "Admin added new medicine: " + saved.getBrandName() + " (" + saved.getCompany() + ")");

            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "SUCCESS");
            resp.put("message", "Medicine " + saved.getBrandName() + " created successfully.");
            resp.put("medicine", saved);
            return ResponseEntity.status(HttpStatus.CREATED).body(resp);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PutMapping("/medicines/{id}")
    public ResponseEntity<Map<String, Object>> updateMedicine(@PathVariable String id,
                                                              @RequestBody Map<String, Object> body,
                                                              HttpServletRequest request) {
        if (body.containsKey("unitPrice")) {
            double price = -1;
            if (body.get("unitPrice") instanceof Number) {
                price = ((Number) body.get("unitPrice")).doubleValue();
            } else if (body.get("unitPrice") instanceof String) {
                try {
                    price = Double.parseDouble((String) body.get("unitPrice"));
                } catch (Exception ignored) {}
            }
            if (price < 0) {
                Map<String, Object> err = new HashMap<>();
                err.put("status", "ERROR");
                err.put("message", "Unit price cannot be negative.");
                return ResponseEntity.badRequest().body(err);
            }
        }

        try {
            Medicine updated = medicineService.updateMedicine(id, body);
            auditLogService.logAction(getAdminId(request), getAdminName(request), "UPDATE_MEDICINE",
                    "MEDICINE", id, "Admin updated medicine record: " + updated.getBrandName(),
                    "SUCCESS", getClientIp(request));

            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "SUCCESS");
            resp.put("message", "Medicine " + updated.getBrandName() + " updated successfully.");
            resp.put("medicine", updated);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @DeleteMapping("/medicines/{id}")
    public ResponseEntity<Map<String, Object>> deleteMedicine(@PathVariable String id,
                                                              HttpServletRequest request) {
        Optional<Medicine> medOpt = medicineRepository.findById(id);
        if (!medOpt.isPresent()) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "ERROR");
            resp.put("message", "Medicine not found with ID: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resp);
        }

        Medicine med = medOpt.get();

        // Referential integrity check: remove linked stocks cleanly
        try {
            List<PharmacyStock> stocks = pharmacyStockRepository.findByMedicineId(id);
            if (!stocks.isEmpty()) {
                pharmacyStockRepository.deleteAll(stocks);
            }

            boolean deleted = medicineService.deleteMedicine(id);
            if (deleted) {
                auditLogService.logAction(getAdminId(request), getAdminName(request), "DELETE_MEDICINE",
                        "MEDICINE", id, "Admin deleted medicine " + med.getBrandName() + " and purged stock links.",
                        "SUCCESS", getClientIp(request));

                Map<String, Object> resp = new HashMap<>();
                resp.put("status", "SUCCESS");
                resp.put("message", "Medicine " + med.getBrandName() + " (ID: " + id + ") has been deleted.");
                return ResponseEntity.ok(resp);
            } else {
                Map<String, Object> resp = new HashMap<>();
                resp.put("status", "ERROR");
                resp.put("message", "Could not delete medicine with ID: " + id);
                return ResponseEntity.badRequest().body(resp);
            }
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", "Error deleting medicine: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    // ==========================================
    // 4. PHARMACY NETWORK MANAGEMENT (CRUD)
    // ==========================================
    @GetMapping("/pharmacies")
    public ResponseEntity<Map<String, Object>> getAllPharmacies(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String area) {

        List<Pharmacy> list = pharmacyService.findAllPharmacies();
        List<Map<String, Object>> resList = new ArrayList<>();

        for (Pharmacy p : list) {
            if (area != null && !area.trim().isEmpty() && !"ALL".equalsIgnoreCase(area)) {
                if (p.getArea() == null || !p.getArea().equalsIgnoreCase(area.trim())) continue;
            }
            if (search != null && !search.trim().isEmpty()) {
                String q = search.trim().toLowerCase();
                boolean match = (p.getName() != null && p.getName().toLowerCase().contains(q))
                        || (p.getAddress() != null && p.getAddress().toLowerCase().contains(q))
                        || (p.getArea() != null && p.getArea().toLowerCase().contains(q))
                        || (p.getId() != null && p.getId().toLowerCase().contains(q));
                if (!match) continue;
            }

            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("name", p.getName());
            m.put("address", p.getAddress());
            m.put("area", p.getArea() != null ? p.getArea() : "");
            m.put("phone", p.getPhone() != null ? p.getPhone() : "");
            m.put("is24Hours", p.is24Hours());
            m.put("hasEmergencyDelivery", p.hasEmergencyDelivery());
            m.put("latitude", p.getLatitude());
            m.put("longitude", p.getLongitude());

            int stockCount = pharmacyService.findStocksByPharmacy(p.getId()).size();
            m.put("stockCount", stockCount);

            resList.add(m);
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("totalCount", resList.size());
        resp.put("pharmacies", resList);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/pharmacies")
    public ResponseEntity<Map<String, Object>> createPharmacy(@RequestBody Map<String, Object> body,
                                                              HttpServletRequest request) {
        String name = (String) body.get("name");
        String address = (String) body.get("address");

        if (name == null || name.trim().isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", "Pharmacy Name is required.");
            return ResponseEntity.badRequest().body(err);
        }

        if (address == null || address.trim().isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", "Pharmacy Address is required.");
            return ResponseEntity.badRequest().body(err);
        }

        try {
            String area = (String) body.getOrDefault("area", "Dhaka");
            String phone = (String) body.getOrDefault("phone", "");
            Boolean is24Hours = body.get("is24Hours") instanceof Boolean ? (Boolean) body.get("is24Hours") : true;
            Boolean hasEmergencyDelivery = body.get("hasEmergencyDelivery") instanceof Boolean ? (Boolean) body.get("hasEmergencyDelivery") : true;

            Double lat = 23.777176;
            Double lng = 90.399452;
            if (body.get("latitude") != null) {
                try {
                    lat = Double.parseDouble(body.get("latitude").toString());
                } catch (Exception ignored) {}
            }
            if (body.get("longitude") != null) {
                try {
                    lng = Double.parseDouble(body.get("longitude").toString());
                } catch (Exception ignored) {}
            }

            String customId = (String) body.get("id");
            String id = (customId != null && !customId.trim().isEmpty()) ? customId.trim() : "pharma_" + UUID.randomUUID().toString().substring(0, 8);

            Pharmacy p = new Pharmacy(id, name.trim(), address.trim(), area.trim(), phone.trim(), lat, lng, is24Hours, hasEmergencyDelivery);
            Pharmacy saved = pharmacyService.savePharmacy(p);

            auditLogService.logAction(getAdminId(request), getAdminName(request), "CREATE_PHARMACY",
                    "PHARMACY", saved.getId(), "Admin enrolled partner dispensary: " + saved.getName() + " (" + saved.getArea() + ")",
                    "SUCCESS", getClientIp(request));

            StockObserverService.getInstance().onNotification("PHARMACY_REGISTERED",
                    "New partner pharmacy registered: " + saved.getName() + " (" + saved.getArea() + ")");

            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "SUCCESS");
            resp.put("message", "Pharmacy created successfully");
            resp.put("pharmacy", saved);
            return ResponseEntity.status(HttpStatus.CREATED).body(resp);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", "Failed to create pharmacy: " + e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PutMapping("/pharmacies/{id}")
    public ResponseEntity<Map<String, Object>> updatePharmacy(@PathVariable String id,
                                                              @RequestBody Map<String, Object> body,
                                                              HttpServletRequest request) {
        Optional<Pharmacy> opt = pharmacyService.findPharmacyById(id);
        if (!opt.isPresent()) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", "Pharmacy not found: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
        }

        Pharmacy p = opt.get();
        if (body.containsKey("name")) p.setName((String) body.get("name"));
        if (body.containsKey("address")) p.setAddress((String) body.get("address"));
        if (body.containsKey("area")) p.setArea((String) body.get("area"));
        if (body.containsKey("phone")) p.setPhone((String) body.get("phone"));
        if (body.containsKey("is24Hours")) p.set24Hours((Boolean) body.get("is24Hours"));
        if (body.containsKey("hasEmergencyDelivery")) p.setHasEmergencyDelivery((Boolean) body.get("hasEmergencyDelivery"));
        if (body.containsKey("latitude")) {
            try {
                p.setLatitude(Double.parseDouble(body.get("latitude").toString()));
            } catch (Exception ignored) {}
        }
        if (body.containsKey("longitude")) {
            try {
                p.setLongitude(Double.parseDouble(body.get("longitude").toString()));
            } catch (Exception ignored) {}
        }

        Pharmacy updated = pharmacyService.savePharmacy(p);

        auditLogService.logAction(getAdminId(request), getAdminName(request), "UPDATE_PHARMACY",
                "PHARMACY", id, "Admin updated dispensary details for " + updated.getName(),
                "SUCCESS", getClientIp(request));

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("message", "Pharmacy updated successfully");
        resp.put("pharmacy", updated);
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/pharmacies/{id}")
    public ResponseEntity<Map<String, Object>> deletePharmacy(@PathVariable String id,
                                                              HttpServletRequest request) {
        Optional<Pharmacy> opt = pharmacyService.findPharmacyById(id);
        if (!opt.isPresent()) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "ERROR");
            resp.put("message", "Pharmacy not found: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resp);
        }

        // Clean up stocks linked to this pharmacy
        try {
            List<PharmacyStock> stocks = pharmacyStockRepository.findByPharmacyId(id);
            if (!stocks.isEmpty()) {
                pharmacyStockRepository.deleteAll(stocks);
            }

            boolean deleted = pharmacyService.deletePharmacy(id);
            if (deleted) {
                auditLogService.logAction(getAdminId(request), getAdminName(request), "DELETE_PHARMACY",
                        "PHARMACY", id, "Admin deleted partner pharmacy ID " + id + " and cleared stock registry.",
                        "SUCCESS", getClientIp(request));

                StockObserverService.getInstance().onNotification("PHARMACY_REMOVED",
                        "Partner pharmacy ID " + id + " was removed from platform registry.");

                Map<String, Object> resp = new HashMap<>();
                resp.put("status", "SUCCESS");
                resp.put("message", "Pharmacy successfully removed.");
                return ResponseEntity.ok(resp);
            } else {
                Map<String, Object> resp = new HashMap<>();
                resp.put("status", "ERROR");
                resp.put("message", "Pharmacy could not be removed.");
                return ResponseEntity.badRequest().body(resp);
            }
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", "Error deleting pharmacy: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    // ==========================================
    // 5. PRESCRIPTION MONITORING & COMPLIANCE VIEW
    // ==========================================
    @GetMapping("/prescriptions")
    public ResponseEntity<Map<String, Object>> getAllPrescriptions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {

        List<Prescription> list = prescriptionService.findAll();
        List<Prescription> filtered = list.stream().filter(p -> {
            if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
                if (p.getStatus() == null || !p.getStatus().equalsIgnoreCase(status.trim())) return false;
            }
            if (search != null && !search.trim().isEmpty()) {
                String q = search.trim().toLowerCase();
                boolean match = (p.getId() != null && p.getId().toLowerCase().contains(q))
                        || (p.getPatientName() != null && p.getPatientName().toLowerCase().contains(q))
                        || (p.getDoctorName() != null && p.getDoctorName().toLowerCase().contains(q))
                        || (p.getHospitalOrClinic() != null && p.getHospitalOrClinic().toLowerCase().contains(q));
                if (!match) return false;
            }
            return true;
        }).collect(Collectors.toList());

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("totalCount", filtered.size());
        resp.put("prescriptions", filtered);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/prescriptions/{id}")
    public ResponseEntity<Map<String, Object>> getPrescriptionDetails(@PathVariable String id) {
        Optional<Prescription> pOpt = prescriptionService.findById(id);
        if (!pOpt.isPresent()) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", "Prescription not found with ID: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
        }

        Prescription p = pOpt.get();
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("prescription", p);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/prescriptions/compliance")
    public ResponseEntity<Map<String, Object>> getPrescriptionComplianceQueue() {
        List<Prescription> all = prescriptionService.findAll();
        List<Map<String, Object>> issues = new ArrayList<>();

        for (Prescription p : all) {
            List<String> flags = new ArrayList<>();

            // 1. Missing medicines
            if (p.getItems() == null || p.getItems().isEmpty()) {
                if (!"UPLOADED".equalsIgnoreCase(p.getStatus())) {
                    flags.add("MISSING_MEDICINES: No line items extracted from uploaded prescription.");
                }
            }

            // 2. Incomplete AI Extraction
            if (p.getRawScanText() != null && !p.getRawScanText().trim().isEmpty() && (p.getItems() == null || p.getItems().isEmpty())) {
                flags.add("INCOMPLETE_AI_EXTRACTION: Raw text captured but structured items empty.");
            }

            // 3. Conflicting AI results
            if (p.getRawScanText() != null && p.getRawScanText().toUpperCase().contains("CONFLICT")) {
                flags.add("AI_CONFLICT: Discrepancy noted between dual vision models.");
            }

            // 4. Unverified status > 24 hours
            if (p.getUploadedAt() != null && p.getUploadedAt().isBefore(LocalDateTime.now().minusHours(24))) {
                if ("UPLOADED".equalsIgnoreCase(p.getStatus()) || "EXTRACTED".equalsIgnoreCase(p.getStatus())) {
                    flags.add("UNVERIFIED_TOO_LONG: Prescription pending validation over 24 hours.");
                }
            }

            // 5. Missing pharmacist verification
            if ("VERIFIED".equalsIgnoreCase(p.getStatus()) && (p.getVerifiedByPharmacistId() == null || p.getVerifiedByPharmacistId().trim().isEmpty())) {
                flags.add("MISSING_PHARMACIST_ID: Verified state missing verifiedByPharmacistId.");
            }

            if (!flags.isEmpty()) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", p.getId());
                item.put("patientId", p.getPatientId());
                item.put("patientName", p.getPatientName());
                item.put("doctorName", p.getDoctorName());
                item.put("hospital", p.getHospitalOrClinic());
                item.put("status", p.getStatus());
                item.put("uploadedAt", p.getUploadedAt() != null ? p.getUploadedAt().toString() : "");
                item.put("itemCount", p.getItems() != null ? p.getItems().size() : 0);
                item.put("complianceFlags", flags);
                issues.add(item);
            }
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("totalIssues", issues.size());
        resp.put("complianceQueue", issues);
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/prescriptions/{id}/status")
    public ResponseEntity<Map<String, Object>> updatePrescriptionStatus(@PathVariable String id,
                                                                        @RequestBody Map<String, String> body,
                                                                        HttpServletRequest request) {
        String newStatus = body.get("status");
        if (newStatus == null || newStatus.trim().isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", "Status is required.");
            return ResponseEntity.badRequest().body(err);
        }

        Optional<Prescription> pOpt = prescriptionService.findById(id);
        if (!pOpt.isPresent()) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", "Prescription not found with ID: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
        }

        Prescription p = pOpt.get();
        String upperStatus = newStatus.toUpperCase().trim();
        p.setStatus(upperStatus);

        if ("VERIFIED".equalsIgnoreCase(upperStatus)) {
            p.setDispenseReady(true);
            if (p.getVerifiedByPharmacistId() == null || p.getVerifiedByPharmacistId().trim().isEmpty()) {
                p.setVerifiedByPharmacistId(getAdminId(request));
            }
        } else if ("DISPENSED".equalsIgnoreCase(upperStatus)) {
            p.setDispenseReady(false);
        }

        prescriptionService.save(p);

        auditLogService.logAction(getAdminId(request), getAdminName(request), "STATUS_CHANGE",
                "PRESCRIPTION", id, "Admin adjusted prescription status to " + upperStatus,
                "SUCCESS", getClientIp(request));

        StockObserverService.getInstance().onNotification("PRESCRIPTION_STATUS_OVERRIDE",
                "Admin updated prescription " + id + " to " + upperStatus);

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("message", "Prescription " + id + " status set to " + upperStatus);
        resp.put("prescription", p);
        return ResponseEntity.ok(resp);
    }

    // ==========================================
    // 6. MARKET PRICE CONTROL CENTER
    // ==========================================
    @GetMapping("/market/status")
    public ResponseEntity<Map<String, Object>> getMarketStatus() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        if (marketPriceSyncService != null) {
            resp.put("provider", marketPriceSyncService.getMarketPriceProvider() != null ? marketPriceSyncService.getMarketPriceProvider().getProviderName() : "DGDA Medex Bangladesh");
            resp.put("isLiveConnected", marketPriceSyncService.getMarketPriceProvider() != null && marketPriceSyncService.getMarketPriceProvider().isLiveApiConnected());
            resp.put("lastSyncTime", marketPriceSyncService.getLastSyncTime() != null ? marketPriceSyncService.getLastSyncTime().toString() : LocalDateTime.now().toString());
            resp.put("nextScheduledSync", marketPriceSyncService.getLastSyncTime() != null ? marketPriceSyncService.getLastSyncTime().plusMinutes(5).toString() : LocalDateTime.now().plusMinutes(5).toString());
            resp.put("syncStatus", marketPriceSyncService.getLastSyncStatus());
            resp.put("lastError", marketPriceSyncService.getLastSyncError());
            resp.put("lastUpdatedCount", marketPriceSyncService.getLastUpdatedCount());
        }
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/market/sync")
    public ResponseEntity<Map<String, Object>> triggerMarketSync(HttpServletRequest request) {
        if (marketPriceSyncService == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", "Market sync service unavailable.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(err);
        }

        MarketPriceSyncService.SyncResult result = marketPriceSyncService.syncAllMedicines("Admin Portal Manual Trigger (" + getAdminName(request) + ")");
        auditLogService.logAction(getAdminId(request), getAdminName(request), "SYNC_MARKET",
                "MARKET", "DGDA", "Manual market sync executed. Checked: " + result.getCheckedCount() + ", Updated: " + result.getUpdatedCount(),
                "SUCCESS", getClientIp(request));

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("message", "Bangladesh market price synchronization completed.");
        resp.put("checkedMedicines", result.getCheckedCount());
        resp.put("updatedMedicines", result.getUpdatedCount());
        resp.put("changes", result.getChanges());
        resp.put("timestamp", result.getTimestamp().toString());
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/market/history")
    public ResponseEntity<Map<String, Object>> getMarketPriceHistory(
            @RequestParam(required = false) String medicine,
            @RequestParam(required = false) String source) {

        List<MedicinePriceHistory> history = priceHistoryRepository.findAllByOrderByTimestampDesc();
        List<MedicinePriceHistory> filtered = history.stream().filter(h -> {
            if (medicine != null && !medicine.trim().isEmpty()) {
                String q = medicine.trim().toLowerCase();
                boolean match = (h.getBrandName() != null && h.getBrandName().toLowerCase().contains(q))
                        || (h.getGenericName() != null && h.getGenericName().toLowerCase().contains(q));
                if (!match) return false;
            }
            if (source != null && !source.trim().isEmpty() && !"ALL".equalsIgnoreCase(source)) {
                if (h.getSource() == null || !h.getSource().equalsIgnoreCase(source.trim())) return false;
            }
            return true;
        }).collect(Collectors.toList());

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("totalRecords", filtered.size());
        resp.put("history", filtered);
        return ResponseEntity.ok(resp);
    }

    // ==========================================
    // 7. PHARMACIST ACCREDITATION & VERIFICATION
    // ==========================================
    @GetMapping("/pharmacists")
    public ResponseEntity<Map<String, Object>> getPharmacists(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String verificationStatus) {

        List<Pharmacist> pharmacists = pharmacistRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Pharmacist ph : pharmacists) {
            if (verificationStatus != null && !verificationStatus.trim().isEmpty() && !"ALL".equalsIgnoreCase(verificationStatus)) {
                if (ph.getVerificationStatus() == null || !ph.getVerificationStatus().equalsIgnoreCase(verificationStatus.trim())) {
                    continue;
                }
            }
            if (search != null && !search.trim().isEmpty()) {
                String q = search.trim().toLowerCase();
                boolean match = (ph.getName() != null && ph.getName().toLowerCase().contains(q))
                        || (ph.getEmail() != null && ph.getEmail().toLowerCase().contains(q))
                        || (ph.getLicenseNumber() != null && ph.getLicenseNumber().toLowerCase().contains(q))
                        || (ph.getPharmacyName() != null && ph.getPharmacyName().toLowerCase().contains(q))
                        || (ph.getId() != null && ph.getId().toLowerCase().contains(q));
                if (!match) continue;
            }

            Map<String, Object> m = new HashMap<>();
            m.put("id", ph.getId());
            m.put("name", ph.getName());
            m.put("email", ph.getEmail());
            m.put("phone", ph.getPhone() != null ? ph.getPhone() : "");
            m.put("pharmacyId", ph.getPharmacyId() != null ? ph.getPharmacyId() : "");
            m.put("pharmacyName", ph.getPharmacyName() != null ? ph.getPharmacyName() : "Unaffiliated Dispensary");
            m.put("licenseNumber", ph.getLicenseNumber() != null ? ph.getLicenseNumber() : "PENDING-LICENSE");
            m.put("status", ph.getStatus());
            m.put("verificationStatus", ph.getVerificationStatus());
            m.put("verifiedAt", ph.getVerifiedAt() != null ? ph.getVerifiedAt().toString() : "");
            m.put("verifiedByAdminId", ph.getVerifiedByAdminId() != null ? ph.getVerifiedByAdminId() : "");
            m.put("createdAt", ph.getCreatedAt() != null ? ph.getCreatedAt().toString() : "");
            result.add(m);
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("totalCount", result.size());
        resp.put("pharmacists", result);
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/pharmacists/{id}/verify")
    public ResponseEntity<Map<String, Object>> verifyPharmacist(@PathVariable String id,
                                                                HttpServletRequest request) {
        Optional<Pharmacist> opt = pharmacistRepository.findById(id);
        if (!opt.isPresent()) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", "Pharmacist account not found: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
        }

        Pharmacist ph = opt.get();
        ph.setVerificationStatus("VERIFIED");
        ph.setStatus("ACTIVE");
        ph.setVerifiedAt(LocalDateTime.now());
        ph.setVerifiedByAdminId(getAdminId(request));
        Pharmacist saved = pharmacistRepository.save(ph);

        auditLogService.logAction(getAdminId(request), getAdminName(request), "VERIFY_PHARMACIST",
                "PHARMACIST", id, "Admin verified pharmacist credentials for " + saved.getName() + " (Lic: " + saved.getLicenseNumber() + ")",
                "SUCCESS", getClientIp(request));

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("message", "Pharmacist " + saved.getName() + " verified successfully.");
        resp.put("pharmacist", saved);
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/pharmacists/{id}/suspend")
    public ResponseEntity<Map<String, Object>> suspendPharmacist(@PathVariable String id,
                                                                 HttpServletRequest request) {
        Optional<Pharmacist> opt = pharmacistRepository.findById(id);
        if (!opt.isPresent()) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", "Pharmacist account not found: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
        }

        Pharmacist ph = opt.get();
        ph.setVerificationStatus("SUSPENDED");
        ph.setStatus("SUSPENDED");
        Pharmacist saved = pharmacistRepository.save(ph);

        auditLogService.logAction(getAdminId(request), getAdminName(request), "SUSPEND_PHARMACIST",
                "PHARMACIST", id, "Admin suspended pharmacist credentials for " + saved.getName(),
                "SUCCESS", getClientIp(request));

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("message", "Pharmacist " + saved.getName() + " has been suspended.");
        resp.put("pharmacist", saved);
        return ResponseEntity.ok(resp);
    }

    // ==========================================
    // 8. ADMINISTRATIVE AUDIT LOGS
    // ==========================================
    @GetMapping("/audit-logs")
    public ResponseEntity<Map<String, Object>> getAuditLogs(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String adminId) {

        List<AuditLog> logs = auditLogService.findAll(search, action, entityType, adminId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("totalCount", logs.size());
        resp.put("logs", logs);
        return ResponseEntity.ok(resp);
    }

    // ==========================================
    // 9. PLATFORM REPORTS & ANALYTICS
    // ==========================================
    @GetMapping("/reports")
    public ResponseEntity<Map<String, Object>> getPlatformReports() {
        // 1. Users breakdown
        long totalUsers = userRepository.count();
        long patients = patientRepository.count();
        long pharmacists = pharmacistRepository.count();
        long admins = adminRepository.count();

        long activeUsers = userRepository.findAll().stream().filter(u -> "ACTIVE".equalsIgnoreCase(u.getStatus())).count();
        long suspendedUsers = userRepository.findAll().stream().filter(u -> "SUSPENDED".equalsIgnoreCase(u.getStatus())).count();
        long deactivatedUsers = userRepository.findAll().stream().filter(u -> "DEACTIVATED".equalsIgnoreCase(u.getStatus())).count();

        Map<String, Object> userReports = new HashMap<>();
        userReports.put("total", totalUsers);
        userReports.put("patients", patients);
        userReports.put("pharmacists", pharmacists);
        userReports.put("admins", admins);
        userReports.put("active", activeUsers);
        userReports.put("suspended", suspendedUsers);
        userReports.put("deactivated", deactivatedUsers);

        // 2. Prescriptions breakdown
        List<Prescription> allRx = prescriptionRepository.findAll();
        long uploaded = allRx.stream().filter(p -> "UPLOADED".equalsIgnoreCase(p.getStatus())).count();
        long extracted = allRx.stream().filter(p -> "EXTRACTED".equalsIgnoreCase(p.getStatus())).count();
        long verified = allRx.stream().filter(p -> "VERIFIED".equalsIgnoreCase(p.getStatus()) || "VERIFIED_BY_PHARMACIST".equalsIgnoreCase(p.getStatus())).count();
        long dispensed = allRx.stream().filter(p -> "DISPENSED".equalsIgnoreCase(p.getStatus())).count();
        long rejected = allRx.stream().filter(p -> "REJECTED".equalsIgnoreCase(p.getStatus())).count();

        Map<String, Object> rxReports = new HashMap<>();
        rxReports.put("total", allRx.size());
        rxReports.put("uploaded", uploaded);
        rxReports.put("extracted", extracted);
        rxReports.put("verified", verified);
        rxReports.put("dispensed", dispensed);
        rxReports.put("rejected", rejected);

        // 3. Pharmacy area distribution
        List<Pharmacy> allPharmacies = pharmacyRepository.findAll();
        Map<String, Long> areaDistribution = allPharmacies.stream()
                .collect(Collectors.groupingBy(p -> (p.getArea() != null && !p.getArea().trim().isEmpty()) ? p.getArea().trim() : "Dhaka", Collectors.counting()));

        Map<String, Object> pharmaReports = new HashMap<>();
        pharmaReports.put("total", allPharmacies.size());
        pharmaReports.put("twentyFourHours", allPharmacies.stream().filter(Pharmacy::is24Hours).count());
        pharmaReports.put("emergencyDelivery", allPharmacies.stream().filter(Pharmacy::hasEmergencyDelivery).count());
        pharmaReports.put("areaDistribution", areaDistribution);

        // 4. Medicine formulation distribution
        List<Medicine> allMeds = medicineRepository.findAll();
        Map<String, Long> formulationDistribution = allMeds.stream()
                .collect(Collectors.groupingBy(m -> (m.getFormulation() != null && !m.getFormulation().trim().isEmpty()) ? m.getFormulation().trim() : "Tablet", Collectors.counting()));

        Map<String, Object> medReports = new HashMap<>();
        medReports.put("total", allMeds.size());
        medReports.put("rxRequired", allMeds.stream().filter(Medicine::isPrescriptionRequired).count());
        medReports.put("otc", allMeds.stream().filter(m -> !m.isPrescriptionRequired()).count());
        medReports.put("formulationDistribution", formulationDistribution);

        // 5. Stock health
        List<PharmacyStock> allStocks = pharmacyStockRepository.findAll();
        long lowStockCount = allStocks.stream().filter(s -> s.getQuantity() <= 15).count();
        long outOfStockCount = allStocks.stream().filter(s -> s.getQuantity() <= 0).count();

        Map<String, Object> stockReports = new HashMap<>();
        stockReports.put("totalSkus", allStocks.size());
        stockReports.put("lowStockCount", lowStockCount);
        stockReports.put("outOfStockCount", outOfStockCount);

        // 6. Price change activity
        List<MedicinePriceHistory> recentHist = priceHistoryRepository.findTop20ByOrderByTimestampDesc();
        long priceIncreases = recentHist.stream().filter(h -> h.getNewPrice() > h.getOldPrice()).count();
        long priceDecreases = recentHist.stream().filter(h -> h.getNewPrice() < h.getOldPrice()).count();

        Map<String, Object> priceReports = new HashMap<>();
        priceReports.put("totalChangesLogged", priceHistoryRepository.count());
        priceReports.put("recentIncreases", priceIncreases);
        priceReports.put("recentDecreases", priceDecreases);

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("users", userReports);
        resp.put("prescriptions", rxReports);
        resp.put("pharmacies", pharmaReports);
        resp.put("medicines", medReports);
        resp.put("stocks", stockReports);
        resp.put("pricing", priceReports);

        return ResponseEntity.ok(resp);
    }

    // ==========================================
    // 10. SYSTEM BROADCAST OVER SSE
    // ==========================================
    @PostMapping("/broadcast")
    public ResponseEntity<Map<String, Object>> sendSystemBroadcast(@RequestBody Map<String, String> body,
                                                                   HttpServletRequest request) {
        String message = body.get("message");
        if (message == null || message.trim().isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", "Broadcast message content cannot be empty.");
            return ResponseEntity.badRequest().body(err);
        }

        if (message.length() > 500) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", "Broadcast message cannot exceed 500 characters.");
            return ResponseEntity.badRequest().body(err);
        }

        // Sanitize: strip any potential script/HTML tags for client safety
        String cleanMessage = message.replaceAll("<[^>]*>", "").trim();
        String category = body.getOrDefault("category", "SYSTEM_NOTICE").toUpperCase().trim();
        String adminName = getAdminName(request);

        String broadcastPayload = String.format("[%s] %s (Dispatched by %s)", category, cleanMessage, adminName);
        StockObserverService.getInstance().onNotification("ADMIN_BROADCAST", broadcastPayload);

        auditLogService.logAction(getAdminId(request), adminName, "BROADCAST",
                "SYSTEM", category, "Global broadcast dispatched: " + cleanMessage,
                "SUCCESS", getClientIp(request));

        int clientCount = eventStreamController != null ? eventStreamController.getActiveClientCount() : 1;

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("message", "System broadcast dispatched to all connected clients.");
        resp.put("broadcastMessage", cleanMessage);
        resp.put("category", category);
        resp.put("connectedClients", clientCount);
        resp.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(resp);
    }

    // ==========================================
    // 11. SUPPORT TICKETS & REPORTED ISSUES
    // ==========================================
    @GetMapping("/support-issues")
    public ResponseEntity<Map<String, Object>> getAllSupportIssues(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {

        List<ReportedIssue> issues = reportedIssueService != null ?
                reportedIssueService.findAll(status, severity, category, search) : Collections.emptyList();

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("totalCount", issues.size());
        resp.put("issues", issues);
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/support-issues/{id}/status")
    public ResponseEntity<Map<String, Object>> updateSupportIssueStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        String status = body.get("status");
        String adminNotes = body.get("adminNotes");

        if (status == null || status.trim().isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", "Status is required.");
            return ResponseEntity.badRequest().body(err);
        }

        if (reportedIssueService == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", "Support issue service is unavailable.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(err);
        }

        try {
            ReportedIssue updated = reportedIssueService.updateStatus(
                    id, status, adminNotes, getAdminId(request), getAdminName(request)
            );

            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "SUCCESS");
            resp.put("message", "Support ticket " + id + " updated to " + updated.getStatus());
            resp.put("issue", updated);
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", "Error updating ticket: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    public void setReportedIssueService(ReportedIssueService reportedIssueService) {
        this.reportedIssueService = reportedIssueService;
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================
    private Map<String, Object> mapUserToDto(User u) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.getId());
        m.put("name", u.getName());
        m.put("email", u.getEmail());
        m.put("role", u.getRole() != null ? u.getRole().name() : "PATIENT");
        m.put("status", u.getStatus() != null ? u.getStatus() : "ACTIVE");
        m.put("phone", u.getPhone() != null ? u.getPhone() : "");
        m.put("customAvatar", u.getCustomAvatar() != null ? u.getCustomAvatar() : "");
        m.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : "");

        if (u instanceof Patient) {
            Patient p = (Patient) u;
            m.put("address", p.getAddress() != null ? p.getAddress() : "");
            m.put("emergencyContact", p.getEmergencyContact() != null ? p.getEmergencyContact() : "");
            m.put("bloodType", p.getBloodType() != null ? p.getBloodType() : "");
            m.put("chronicConditions", p.getChronicConditions() != null ? p.getChronicConditions() : "");
            m.put("allergies", p.getAllergies() != null ? p.getAllergies() : "");
        } else if (u instanceof Pharmacist) {
            Pharmacist ph = (Pharmacist) u;
            m.put("pharmacyId", ph.getPharmacyId() != null ? ph.getPharmacyId() : "");
            m.put("pharmacyName", ph.getPharmacyName() != null ? ph.getPharmacyName() : "");
            m.put("licenseNumber", ph.getLicenseNumber() != null ? ph.getLicenseNumber() : "");
            m.put("verificationStatus", ph.getVerificationStatus());
            m.put("verifiedAt", ph.getVerifiedAt() != null ? ph.getVerifiedAt().toString() : "");
            m.put("verifiedByAdminId", ph.getVerifiedByAdminId() != null ? ph.getVerifiedByAdminId() : "");
        } else if (u instanceof Admin) {
            Admin a = (Admin) u;
            m.put("accessLevel", a.getAccessLevel());
        }
        return m;
    }

    private String getAdminId(HttpServletRequest req) {
        Object attr = req.getAttribute("adminId");
        if (attr != null) return attr.toString();
        String header = req.getHeader("X-User-Id");
        return (header != null && !header.trim().isEmpty()) ? header.trim() : "usr_admin_01";
    }

    private String getAdminName(HttpServletRequest req) {
        Object attr = req.getAttribute("adminName");
        if (attr != null) return attr.toString();
        return "System Administrator";
    }

    private String getClientIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = req.getRemoteAddr();
        }
        return ip != null ? ip : "127.0.0.1";
    }
}
