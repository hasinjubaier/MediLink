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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminControllerComprehensiveTest {

    @Mock private UserService userService;
    @Mock private MedicineService medicineService;
    @Mock private PharmacyService pharmacyService;
    @Mock private PrescriptionService prescriptionService;
    @Mock private UserRepository userRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private PharmacistRepository pharmacistRepository;
    @Mock private AdminRepository adminRepository;
    @Mock private MedicineRepository medicineRepository;
    @Mock private PharmacyRepository pharmacyRepository;
    @Mock private PharmacyStockRepository pharmacyStockRepository;
    @Mock private PrescriptionRepository prescriptionRepository;
    @Mock private ReminderRepository reminderRepository;
    @Mock private MedicinePriceHistoryRepository priceHistoryRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private MarketPriceSyncService marketPriceSyncService;
    @Mock private EventStreamController eventStreamController;
    @Mock private AiVisionConfig aiVisionConfig;

    private AdminController controller;

    @BeforeEach
    public void setUp() {
        controller = new AdminController(
                userService,
                medicineService,
                pharmacyService,
                prescriptionService,
                userRepository,
                patientRepository,
                pharmacistRepository,
                adminRepository,
                medicineRepository,
                pharmacyRepository,
                pharmacyStockRepository,
                prescriptionRepository,
                reminderRepository,
                priceHistoryRepository,
                auditLogService,
                marketPriceSyncService,
                eventStreamController,
                aiVisionConfig
        );
    }

    private MockHttpServletRequest createAdminRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("adminId", "ADM_TEST_01");
        request.setAttribute("adminName", "Test Admin");
        request.setRemoteAddr("127.0.0.1");
        return request;
    }

    // ==========================================
    // 1. TELEMETRY & HEALTH
    // ==========================================
    @Test
    @DisplayName("Should return platform live KPIs and telemetry")
    public void testGetSystemTelemetry() {
        when(userRepository.count()).thenReturn(50L);
        when(patientRepository.count()).thenReturn(35L);
        when(pharmacistRepository.count()).thenReturn(10L);
        when(adminRepository.count()).thenReturn(5L);
        when(medicineRepository.count()).thenReturn(120L);
        when(pharmacyRepository.count()).thenReturn(15L);
        when(prescriptionRepository.count()).thenReturn(40L);
        when(prescriptionRepository.findAll()).thenReturn(Collections.emptyList());
        when(reminderRepository.count()).thenReturn(25L);
        when(pharmacyStockRepository.findAll()).thenReturn(Collections.emptyList());
        when(eventStreamController.getActiveClientCount()).thenReturn(3);

        ResponseEntity<Map<String, Object>> response = controller.getSystemTelemetry();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        Map<?, ?> counts = (Map<?, ?>) body.get("counts");
        assertNotNull(counts);
        assertEquals(50L, counts.get("totalUsers"));
        assertEquals(35L, counts.get("patients"));
        assertEquals(120L, counts.get("medicines"));
        assertEquals(3, counts.get("activeSseClients"));
    }

    @Test
    @DisplayName("Should return system diagnostics including JVM, DB, and AI statuses")
    public void testGetSystemHealthDiagnostics() {
        when(userRepository.count()).thenReturn(10L);
        when(aiVisionConfig.isGeminiConfigured()).thenReturn(true);
        when(aiVisionConfig.isGroqConfigured()).thenReturn(true);
        when(aiVisionConfig.getGeminiModel()).thenReturn("gemini-1.5-flash");
        when(aiVisionConfig.getGroqModel()).thenReturn("llama-3.2-11b-vision-preview");
        when(marketPriceSyncService.getLastSyncTime()).thenReturn(LocalDateTime.now().minusMinutes(2));
        when(marketPriceSyncService.getLastSyncStatus()).thenReturn("SUCCESS");
        when(marketPriceSyncService.getLastSyncError()).thenReturn(null);

        ResponseEntity<Map<String, Object>> response = controller.getSystemHealthDiagnostics();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        Map<?, ?> db = (Map<?, ?>) body.get("database");
        Map<?, ?> ai = (Map<?, ?>) body.get("aiServices");
        assertEquals("HEALTHY", db.get("status"));
        assertEquals("HEALTHY", ai.get("geminiStatus"));
        assertEquals("HEALTHY", ai.get("groqStatus"));
    }

    // ==========================================
    // 2. USER MANAGEMENT
    // ==========================================
    @Test
    @DisplayName("Should list users with role and search filters")
    public void testListUsers() {
        User u1 = new User("u1", "Rahim Ahmed", "rahim@patient.com", "pass", UserRole.PATIENT, "01711111111");
        User u2 = new User("u2", "Karim Pharmacist", "karim@pharmacy.com", "pass", UserRole.PHARMACIST, "01822222222");

        when(userRepository.findAll()).thenReturn(Arrays.asList(u1, u2));

        ResponseEntity<Map<String, Object>> response = controller.getAllUsers("Rahim", "PATIENT", null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        List<?> users = (List<?>) body.get("users");
        assertEquals(1, users.size());
    }

    @Test
    @DisplayName("Creating user with duplicate email should return 409 Conflict")
    public void testCreateUserDuplicateEmailReturnsConflict() {
        when(userRepository.existsByEmailIgnoreCase("exist@medilink.com")).thenReturn(true);

        Map<String, String> payload = new HashMap<>();
        payload.put("name", "Duplicate User");
        payload.put("email", "exist@medilink.com");
        payload.put("password", "secret123");
        payload.put("role", "PATIENT");

        ResponseEntity<Map<String, Object>> response = controller.createUser(payload, createAdminRequest());
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    @DisplayName("Creating user with empty name should return 400 Bad Request")
    public void testCreateUserEmptyNameReturnsBadRequest() {
        Map<String, String> payload = new HashMap<>();
        payload.put("name", "   ");
        payload.put("email", "valid@medilink.com");
        payload.put("password", "secret123");

        ResponseEntity<Map<String, Object>> response = controller.createUser(payload, createAdminRequest());
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("Creating user with invalid email format should return 400 Bad Request")
    public void testCreateUserInvalidEmailReturnsBadRequest() {
        Map<String, String> payload = new HashMap<>();
        payload.put("name", "Valid Name");
        payload.put("email", "not-an-email");
        payload.put("password", "secret123");

        ResponseEntity<Map<String, Object>> response = controller.createUser(payload, createAdminRequest());
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("Admin attempting self-delete should return 400 Bad Request")
    public void testDeleteUserSelfDeleteReturnsBadRequest() {
        MockHttpServletRequest request = createAdminRequest(); // adminId is ADM_TEST_01

        ResponseEntity<Map<String, Object>> response = controller.deleteUser("ADM_TEST_01", request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().get("message").toString().contains("cannot delete their own active administrative account"));
    }

    @Test
    @DisplayName("Deleting non-existent user should return 404 Not Found")
    public void testDeleteUserNotFoundReturns404() {
        when(userRepository.findById("NONEXISTENT")).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = controller.deleteUser("NONEXISTENT", createAdminRequest());
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ==========================================
    // 3. MEDICINE CATALOG
    // ==========================================
    @Test
    @DisplayName("Creating medicine with negative price should return 400 Bad Request")
    public void testCreateMedicineNegativePriceReturnsBadRequest() {
        Map<String, Object> med = new HashMap<>();
        med.put("brandName", "Napa Extra");
        med.put("genericName", "Paracetamol + Caffeine");
        med.put("unitPrice", -5.0);

        ResponseEntity<Map<String, Object>> response = controller.createMedicine(med, createAdminRequest());
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().get("message").toString().contains("negative"));
    }

    @Test
    @DisplayName("Creating medicine with blank brand name should return 400 Bad Request")
    public void testCreateMedicineBlankBrandReturnsBadRequest() {
        Map<String, Object> med = new HashMap<>();
        med.put("brandName", "   ");
        med.put("genericName", "Paracetamol");
        med.put("unitPrice", 2.5);

        ResponseEntity<Map<String, Object>> response = controller.createMedicine(med, createAdminRequest());
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("Creating valid medicine should return 201 Created and log action")
    public void testCreateMedicineSuccess() {
        Map<String, Object> medMap = new HashMap<>();
        medMap.put("brandName", "Napa Extend");
        medMap.put("genericName", "Paracetamol");
        medMap.put("company", "Beximco Pharmaceuticals Ltd.");
        medMap.put("strength", "665mg");
        medMap.put("formulation", "Tablet");
        medMap.put("unitPrice", 3.5);

        Medicine savedMed = new Medicine("med_101", "Napa Extend", "Paracetamol", "Beximco Pharmaceuticals Ltd.",
                "665mg", "Tablet", 3.5, false, "General", "None listed.");

        when(medicineService.saveMedicine(any(Medicine.class))).thenReturn(savedMed);

        ResponseEntity<Map<String, Object>> response = controller.createMedicine(medMap, createAdminRequest());
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody().get("medicine"));
        verify(auditLogService, times(1)).logAction(any(), any(), eq("CREATE_MEDICINE"), eq("MEDICINE"), any(), any(), eq("SUCCESS"), any());
    }

    @Test
    @DisplayName("Deleting medicine should clean up associated pharmacy stocks")
    public void testDeleteMedicineCleansUpStock() {
        Medicine med = new Medicine();
        med.setId("med_paracetamol");
        med.setBrandName("Napa");

        when(medicineRepository.findById("med_paracetamol")).thenReturn(Optional.of(med));
        when(pharmacyStockRepository.findByMedicineId("med_paracetamol")).thenReturn(Collections.emptyList());
        when(medicineService.deleteMedicine("med_paracetamol")).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = controller.deleteMedicine("med_paracetamol", createAdminRequest());
        assertEquals(HttpStatus.OK, response.getStatusCode());

        verify(medicineService, times(1)).deleteMedicine("med_paracetamol");
        verify(auditLogService, times(1)).logAction(any(), any(), eq("DELETE_MEDICINE"), eq("MEDICINE"), eq("med_paracetamol"), any(), eq("SUCCESS"), any());
    }

    // ==========================================
    // 4. PHARMACY MANAGEMENT
    // ==========================================
    @Test
    @DisplayName("Creating pharmacy with missing address should return 400 Bad Request")
    public void testCreatePharmacyMissingAddressReturnsBadRequest() {
        Map<String, Object> pharm = new HashMap<>();
        pharm.put("name", "Lazz Pharma");
        pharm.put("address", "  ");

        ResponseEntity<Map<String, Object>> response = controller.createPharmacy(pharm, createAdminRequest());
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("Creating pharmacy with missing name should return 400 Bad Request")
    public void testCreatePharmacyMissingNameReturnsBadRequest() {
        Map<String, Object> pharm = new HashMap<>();
        pharm.put("name", "  ");
        pharm.put("address", "Dhanmondi, Dhaka");

        ResponseEntity<Map<String, Object>> response = controller.createPharmacy(pharm, createAdminRequest());
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("Deleting pharmacy should safely purge stock cascade")
    public void testDeletePharmacyPurgesStockCascade() {
        Pharmacy pharm = new Pharmacy();
        pharm.setId("ph_dhanmondi");
        pharm.setName("Dhanmondi Pharma");

        when(pharmacyService.findPharmacyById("ph_dhanmondi")).thenReturn(Optional.of(pharm));
        when(pharmacyStockRepository.findByPharmacyId("ph_dhanmondi")).thenReturn(Collections.emptyList());
        when(pharmacyService.deletePharmacy("ph_dhanmondi")).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = controller.deletePharmacy("ph_dhanmondi", createAdminRequest());
        assertEquals(HttpStatus.OK, response.getStatusCode());

        verify(pharmacyService, times(1)).deletePharmacy("ph_dhanmondi");
        verify(auditLogService, times(1)).logAction(any(), any(), eq("DELETE_PHARMACY"), eq("PHARMACY"), eq("ph_dhanmondi"), any(), eq("SUCCESS"), any());
    }

    // ==========================================
    // 5. PRESCRIPTION MONITORING & COMPLIANCE
    // ==========================================
    @Test
    @DisplayName("Compliance queue should detect prescriptions with missing items or unverified status")
    public void testGetComplianceQueueIdentifiesIssues() {
        Prescription rxEmpty = new Prescription();
        rxEmpty.setId("rx_empty");
        rxEmpty.setUploadedAt(LocalDateTime.now().minusDays(3));
        rxEmpty.setStatus("VERIFIED");
        rxEmpty.setItems(Collections.emptyList());

        Prescription rxUnverified = new Prescription();
        rxUnverified.setId("rx_unverified");
        rxUnverified.setUploadedAt(LocalDateTime.now().minusDays(5));
        rxUnverified.setStatus("EXTRACTED");
        PrescriptionItem item = new PrescriptionItem();
        item.setMedicineName("Seclo 20mg");
        rxUnverified.setItems(Collections.singletonList(item));

        when(prescriptionService.findAll()).thenReturn(Arrays.asList(rxEmpty, rxUnverified));

        ResponseEntity<Map<String, Object>> response = controller.getPrescriptionComplianceQueue();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);

        List<?> issues = (List<?>) body.get("complianceQueue");
        assertEquals(2, issues.size());
    }

    // ==========================================
    // 6. MARKET PRICE CONTROL
    // ==========================================
    @Test
    @DisplayName("Should return market price sync telemetry")
    public void testGetMarketSyncStatus() {
        when(marketPriceSyncService.getLastSyncTime()).thenReturn(LocalDateTime.now());
        when(marketPriceSyncService.getLastSyncStatus()).thenReturn("SUCCESS");
        when(marketPriceSyncService.getLastSyncError()).thenReturn(null);
        when(marketPriceSyncService.getLastUpdatedCount()).thenReturn(14);

        ResponseEntity<Map<String, Object>> response = controller.getMarketStatus();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("SUCCESS", body.get("syncStatus"));
        assertEquals(14, body.get("lastUpdatedCount"));
    }

    @Test
    @DisplayName("Manual market sync trigger should invoke sync service and record audit log")
    public void testTriggerManualMarketSync() {
        MarketPriceSyncService.SyncResult syncResult = new MarketPriceSyncService.SyncResult(
                50, 5, Collections.emptyList(), LocalDateTime.now());
        when(marketPriceSyncService.syncAllMedicines(anyString())).thenReturn(syncResult);

        ResponseEntity<Map<String, Object>> response = controller.triggerMarketSync(createAdminRequest());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(5, body.get("updatedMedicines"));
        verify(auditLogService, times(1)).logAction(any(), any(), eq("SYNC_MARKET"), eq("MARKET"), any(), any(), eq("SUCCESS"), any());
    }

    // ==========================================
    // 7. PHARMACIST VERIFICATION
    // ==========================================
    @Test
    @DisplayName("Verifying pharmacist license should update accreditation status")
    public void testVerifyPharmacistSuccess() {
        Pharmacist pharmacist = new Pharmacist("ph_01", "Karim", "karim@pharm.com", "pass", "ph_01", "Lazz Pharma", "DGDA-LIC-9988");
        pharmacist.setVerificationStatus("PENDING_VERIFICATION");

        when(pharmacistRepository.findById("ph_01")).thenReturn(Optional.of(pharmacist));
        when(pharmacistRepository.save(any(Pharmacist.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<Map<String, Object>> response = controller.verifyPharmacist("ph_01", createAdminRequest());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("VERIFIED", pharmacist.getVerificationStatus());
        assertNotNull(pharmacist.getVerifiedAt());
        assertEquals("ADM_TEST_01", pharmacist.getVerifiedByAdminId());
        verify(auditLogService, times(1)).logAction(any(), any(), eq("VERIFY_PHARMACIST"), eq("PHARMACIST"), eq("ph_01"), any(), eq("SUCCESS"), any());
    }

    // ==========================================
    // 8. SYSTEM BROADCAST
    // ==========================================
    @Test
    @DisplayName("Empty broadcast message should return 400 Bad Request")
    public void testBroadcastEmptyMessageReturnsBadRequest() {
        Map<String, String> payload = new HashMap<>();
        payload.put("message", "   ");

        ResponseEntity<Map<String, Object>> response = controller.sendSystemBroadcast(payload, createAdminRequest());
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("Broadcast message exceeding 500 characters should return 400 Bad Request")
    public void testBroadcastOverLimitMessageReturnsBadRequest() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 550; i++) sb.append("A");

        Map<String, String> payload = new HashMap<>();
        payload.put("message", sb.toString());

        ResponseEntity<Map<String, Object>> response = controller.sendSystemBroadcast(payload, createAdminRequest());
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("Broadcast message should strip HTML/script tags and dispatch")
    public void testBroadcastStripsHtmlTags() {
        Map<String, String> payload = new HashMap<>();
        payload.put("category", "EMERGENCY");
        payload.put("message", "<script>alert('hack')</script>Severe cyclone warning in coastal zones! <b>Take shelter</b>");

        when(eventStreamController.getActiveClientCount()).thenReturn(4);

        ResponseEntity<Map<String, Object>> response = controller.sendSystemBroadcast(payload, createAdminRequest());
        assertEquals(HttpStatus.OK, response.getStatusCode());

        verify(auditLogService, times(1)).logAction(
                any(),
                any(),
                eq("BROADCAST"),
                eq("SYSTEM"),
                eq("EMERGENCY"),
                argThat(desc -> !desc.contains("<script>") && !desc.contains("<b>") && desc.contains("Take shelter")),
                eq("SUCCESS"),
                any()
        );
    }
}
