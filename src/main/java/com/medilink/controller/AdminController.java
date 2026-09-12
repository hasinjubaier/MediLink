package com.medilink.controller;

import com.medilink.model.medicine.Medicine;
import com.medilink.model.pharmacy.Pharmacy;
import com.medilink.model.prescription.Prescription;
import com.medilink.model.user.*;
import com.medilink.repository.*;
import com.medilink.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.*;

/**
 * Master Administrative Controller for MediLink 2.0.
 * Empowers system administrators to handle the entire platform:
 * - User Management (Full CRUD for Patients, Pharmacists, Admins)
 * - Medicine Catalog & Pricing CRUD
 * - Prescriptions Master Oversight
 * - System Live Telemetry & Health Monitoring
 * - Global Platform SSE Broadcast Alerts
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
    private final PrescriptionRepository prescriptionRepository;

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
                           PrescriptionRepository prescriptionRepository) {
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
        this.prescriptionRepository = prescriptionRepository;
    }

    // ==========================================
    // 1. SYSTEM TELEMETRY & HEALTH
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

        List<Prescription> allRx = prescriptionService.findAll();
        long pendingRx = allRx.stream().filter(r -> "UPLOADED".equalsIgnoreCase(r.getStatus()) || "EXTRACTED".equalsIgnoreCase(r.getStatus())).count();
        long verifiedRx = allRx.stream().filter(r -> "VERIFIED".equalsIgnoreCase(r.getStatus()) || "VERIFIED_BY_PHARMACIST".equalsIgnoreCase(r.getStatus())).count();
        long dispensedRx = allRx.stream().filter(r -> "DISPENSED".equalsIgnoreCase(r.getStatus())).count();

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

        Map<String, Object> health = new HashMap<>();
        health.put("status", "HEALTHY");
        health.put("database", "PostgreSQL 18 (medilink_db) - CONNECTED");
        health.put("serverPort", 8080);
        health.put("usedMemoryMb", usedMemoryMb);
        health.put("maxMemoryMb", maxMemoryMb);
        health.put("cpuCores", runtime.availableProcessors());
        health.put("uptime", uptimeFormatted);
        health.put("timestamp", new Date().toString());

        resp.put("status", "SUCCESS");
        resp.put("counts", counts);
        resp.put("health", health);
        resp.put("recentAuditEvents", StockObserverService.getInstance().getRecentEvents());

        return ResponseEntity.ok(resp);
    }

    // ==========================================
    // 2. USER MANAGEMENT (FULL CRUD)
    // ==========================================
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> userList = new ArrayList<>();

        for (User u : users) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("name", u.getName());
            m.put("email", u.getEmail());
            m.put("role", u.getRole() != null ? u.getRole().name() : "PATIENT");
            m.put("phone", u.getPhone() != null ? u.getPhone() : "");
            m.put("customAvatar", u.getCustomAvatar() != null ? u.getCustomAvatar() : "");

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
            } else if (u instanceof Admin) {
                Admin a = (Admin) u;
                m.put("accessLevel", a.getAccessLevel());
            }

            userList.add(m);
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("users", userList);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String email = body.get("email");
        String password = body.get("password");
        String roleStr = body.getOrDefault("role", "PATIENT").toUpperCase();

        if (name == null || email == null || password == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", "Name, Email, and Password are required.");
            return ResponseEntity.badRequest().body(err);
        }

        UserRole role;
        try {
            role = UserRole.valueOf(roleStr);
        } catch (Exception e) {
            role = UserRole.PATIENT;
        }

        Map<String, String> extra = new HashMap<>(body);
        try {
            User created = userService.registerUser(role, name, email, password, extra);
            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "SUCCESS");
            resp.put("message", "User " + name + " created successfully with ID: " + created.getId());
            resp.put("id", created.getId());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(@PathVariable String id, @RequestBody Map<String, Object> updates) {
        try {
            User updated = userService.adminUpdateUser(id, updates);
            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "SUCCESS");
            resp.put("message", "User " + updated.getName() + " updated successfully.");
            resp.put("user", updated);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable String id) {
        boolean deleted = userService.deleteUser(id);
        Map<String, Object> resp = new HashMap<>();
        if (deleted) {
            resp.put("status", "SUCCESS");
            resp.put("message", "User with ID " + id + " has been deleted from database.");
            return ResponseEntity.ok(resp);
        } else {
            resp.put("status", "ERROR");
            resp.put("message", "User not found with ID: " + id);
            return ResponseEntity.badRequest().body(resp);
        }
    }

    // ==========================================
    // 3. MEDICINE CATALOG (FULL CRUD)
    // ==========================================
    @GetMapping("/medicines")
    public ResponseEntity<Map<String, Object>> getAllMedicines() {
        List<Medicine> meds = medicineService.findAll();
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("medicines", meds);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/medicines")
    public ResponseEntity<Map<String, Object>> createMedicine(@RequestBody Map<String, Object> body) {
        try {
            String brandName = (String) body.get("brandName");
            String genericName = (String) body.get("genericName");
            String company = (String) body.getOrDefault("company", "Generic Pharma");
            String strength = (String) body.getOrDefault("strength", "500mg");
            String formulation = (String) body.getOrDefault("formulation", "Tablet");
            String category = (String) body.getOrDefault("category", "General");
            String sideEffects = (String) body.getOrDefault("sideEffects", "None listed.");

            double unitPrice = 5.0;
            if (body.get("unitPrice") instanceof Number) {
                unitPrice = ((Number) body.get("unitPrice")).doubleValue();
            } else if (body.get("unitPrice") instanceof String) {
                try { unitPrice = Double.parseDouble((String) body.get("unitPrice")); } catch (Exception ignored) {}
            }

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

            Medicine med = new Medicine(id, brandName, genericName, company, strength, formulation, unitPrice, rxReq, category, sideEffects);
            Medicine saved = medicineService.saveMedicine(med);

            // Announce new medicine to SSE stream
            StockObserverService.getInstance().onNotification("NEW_MEDICINE_CATALOG", "Admin added new medicine: " + saved.getBrandName() + " (" + saved.getCompany() + ")");

            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "SUCCESS");
            resp.put("message", "Medicine " + saved.getBrandName() + " created successfully.");
            resp.put("medicine", saved);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PutMapping("/medicines/{id}")
    public ResponseEntity<Map<String, Object>> updateMedicine(@PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            Medicine updated = medicineService.updateMedicine(id, body);
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
    public ResponseEntity<Map<String, Object>> deleteMedicine(@PathVariable String id) {
        boolean deleted = medicineService.deleteMedicine(id);
        Map<String, Object> resp = new HashMap<>();
        if (deleted) {
            resp.put("status", "SUCCESS");
            resp.put("message", "Medicine with ID " + id + " has been deleted.");
            return ResponseEntity.ok(resp);
        } else {
            resp.put("status", "ERROR");
            resp.put("message", "Medicine not found with ID: " + id);
            return ResponseEntity.badRequest().body(resp);
        }
    }

    // ==========================================
    // 4. PRESCRIPTIONS MASTER QUEUE
    // ==========================================
    @GetMapping("/prescriptions")
    public ResponseEntity<Map<String, Object>> getAllPrescriptions() {
        List<Prescription> list = prescriptionService.findAll();
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("prescriptions", list);
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/prescriptions/{id}/status")
    public ResponseEntity<Map<String, Object>> updatePrescriptionStatus(@PathVariable String id, @RequestBody Map<String, String> body) {
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
            return ResponseEntity.badRequest().body(err);
        }

        Prescription p = pOpt.get();
        p.setStatus(newStatus.toUpperCase().trim());
        if ("VERIFIED".equalsIgnoreCase(newStatus)) {
            p.setDispenseReady(true);
            p.setVerifiedByPharmacistId("usr_admin_01");
        } else if ("DISPENSED".equalsIgnoreCase(newStatus)) {
            p.setDispenseReady(false);
        }
        prescriptionService.save(p);

        StockObserverService.getInstance().onNotification("PRESCRIPTION_STATUS_OVERRIDE", "Admin updated prescription " + id + " to " + newStatus);

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("message", "Prescription " + id + " status set to " + newStatus);
        resp.put("prescription", p);
        return ResponseEntity.ok(resp);
    }

    // ==========================================
    // 5. PHARMACY NETWORK MANAGEMENT (CRUD)
    // ==========================================
    @GetMapping("/pharmacies")
    public ResponseEntity<Map<String, Object>> getAllPharmacies() {
        List<Pharmacy> list = pharmacyService.findAllPharmacies();
        List<Map<String, Object>> resList = new ArrayList<>();
        for (Pharmacy p : list) {
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
        resp.put("pharmacies", resList);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/pharmacies")
    public ResponseEntity<Map<String, Object>> createPharmacy(@RequestBody Map<String, Object> body) {
        try {
            String name = (String) body.get("name");
            String address = (String) body.get("address");
            String area = (String) body.getOrDefault("area", "Dhaka");
            String phone = (String) body.getOrDefault("phone", "");
            Boolean is24Hours = body.get("is24Hours") instanceof Boolean ? (Boolean) body.get("is24Hours") : true;
            Boolean hasEmergencyDelivery = body.get("hasEmergencyDelivery") instanceof Boolean ? (Boolean) body.get("hasEmergencyDelivery") : true;

            Double lat = 23.777176;
            Double lng = 90.399452;
            if (body.get("latitude") != null) {
                try { lat = Double.parseDouble(body.get("latitude").toString()); } catch (Exception ignored) {}
            }
            if (body.get("longitude") != null) {
                try { lng = Double.parseDouble(body.get("longitude").toString()); } catch (Exception ignored) {}
            }

            String customId = (String) body.get("id");
            String id = (customId != null && !customId.trim().isEmpty())
                    ? customId.trim()
                    : "pharma_" + UUID.randomUUID().toString().substring(0, 8);

            Pharmacy p = new Pharmacy(id, name, address, area, phone, lat, lng, is24Hours, hasEmergencyDelivery);
            Pharmacy saved = pharmacyService.savePharmacy(p);

            StockObserverService.getInstance().onNotification("PHARMACY_REGISTERED", "New partner pharmacy registered: " + saved.getName() + " (" + saved.getArea() + ")");

            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "SUCCESS");
            resp.put("message", "Pharmacy created successfully");
            resp.put("pharmacy", saved);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", "Failed to create pharmacy: " + e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PutMapping("/pharmacies/{id}")
    public ResponseEntity<Map<String, Object>> updatePharmacy(@PathVariable String id, @RequestBody Map<String, Object> body) {
        Optional<Pharmacy> opt = pharmacyService.findPharmacyById(id);
        if (!opt.isPresent()) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", "Pharmacy not found: " + id);
            return ResponseEntity.badRequest().body(err);
        }

        Pharmacy p = opt.get();
        if (body.containsKey("name")) p.setName((String) body.get("name"));
        if (body.containsKey("address")) p.setAddress((String) body.get("address"));
        if (body.containsKey("area")) p.setArea((String) body.get("area"));
        if (body.containsKey("phone")) p.setPhone((String) body.get("phone"));
        if (body.containsKey("is24Hours")) p.set24Hours((Boolean) body.get("is24Hours"));
        if (body.containsKey("hasEmergencyDelivery")) p.setHasEmergencyDelivery((Boolean) body.get("hasEmergencyDelivery"));
        if (body.containsKey("latitude")) {
            try { p.setLatitude(Double.parseDouble(body.get("latitude").toString())); } catch (Exception ignored) {}
        }
        if (body.containsKey("longitude")) {
            try { p.setLongitude(Double.parseDouble(body.get("longitude").toString())); } catch (Exception ignored) {}
        }

        Pharmacy updated = pharmacyService.savePharmacy(p);

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("message", "Pharmacy updated successfully");
        resp.put("pharmacy", updated);
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/pharmacies/{id}")
    public ResponseEntity<Map<String, Object>> deletePharmacy(@PathVariable String id) {
        boolean deleted = pharmacyService.deletePharmacy(id);
        Map<String, Object> resp = new HashMap<>();
        if (deleted) {
            StockObserverService.getInstance().onNotification("PHARMACY_REMOVED", "Partner pharmacy ID " + id + " was removed from platform registry.");
            resp.put("status", "SUCCESS");
            resp.put("message", "Pharmacy successfully removed.");
            return ResponseEntity.ok(resp);
        } else {
            resp.put("status", "ERROR");
            resp.put("message", "Pharmacy not found or could not be removed.");
            return ResponseEntity.badRequest().body(resp);
        }
    }

    // ==========================================
    // 6. GLOBAL SSE SYSTEM BROADCAST
    // ==========================================
    @PostMapping("/broadcast")
    public ResponseEntity<Map<String, Object>> sendSystemBroadcast(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "System announcement from MediLink Administration.");
        StockObserverService.getInstance().onNotification("ADMIN_BROADCAST", message);

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("message", "Broadcast event successfully dispatched to all active client streams.");
        resp.put("broadcastMessage", message);
        return ResponseEntity.ok(resp);
    }
}
