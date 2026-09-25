package com.medilink.controller;

import com.medilink.dto.prescription.PrescriptionScanResult;
import com.medilink.dto.prescription.ScannedMedicineItem;
import com.medilink.service.MedicineService;
import com.medilink.service.PrescriptionScanService;
import com.medilink.service.PrescriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PrescriptionScanControllerTest {

    @Mock
    private PrescriptionService prescriptionService;

    @Mock
    private MedicineService medicineService;

    @Mock
    private PrescriptionScanService prescriptionScanService;

    private PrescriptionController controller;

    @BeforeEach
    public void setUp() {
        controller = new PrescriptionController(prescriptionService, medicineService, prescriptionScanService);
    }

    @Test
    public void testScanPrescriptionEndpointSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "rx.jpg", "image/jpeg", new byte[100]);

        PrescriptionScanResult mockResult = new PrescriptionScanResult();
        mockResult.setPatientName("Rahim Ahmed");
        mockResult.setDoctorName("Dr. Farhan Kabir");
        mockResult.setHospitalName("Square Hospital");
        mockResult.setVerificationStatus("VERIFIED_BY_BOTH");
        mockResult.setMedicines(Collections.singletonList(new ScannedMedicineItem("Napa Extra", "500mg", "1 tablet", "1+0+1", "After meal", "5 days", "Take with water")));

        when(prescriptionScanService.scanPrescription(any(), any(), any())).thenReturn(mockResult);

        ResponseEntity<Map<String, Object>> response = controller.scanPrescription(file, "P100", "Rahim Ahmed");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals(mockResult, response.getBody().get("data"));
    }

    @Test
    public void testScanPrescriptionEndpointInvalidInput() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "invalid.txt", "text/plain", new byte[100]);

        when(prescriptionScanService.scanPrescription(any(), any(), any()))
            .thenThrow(new IllegalArgumentException("Unsupported file type"));

        ResponseEntity<Map<String, Object>> response = controller.scanPrescription(file, null, null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("INVALID_PRESCRIPTION_REQUEST", response.getBody().get("code"));
    }

    @Test
    public void testScanPrescriptionEndpointServerError() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "rx.jpg", "image/jpeg", new byte[100]);

        when(prescriptionScanService.scanPrescription(any(), any(), any()))
            .thenThrow(new RuntimeException("Connection timeout"));

        ResponseEntity<Map<String, Object>> response = controller.scanPrescription(file, null, null);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("PRESCRIPTION_SCAN_FAILED", response.getBody().get("code"));
    }

    @Test
    public void testUploadPrescriptionWithStructuredItems() {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("patientId", "usr_123");
        payload.put("patientName", "Vivek S.");
        payload.put("loggedInPatientName", "Maisha Rahman");
        payload.put("doctorName", "Dr. Ramesh (131441)");
        payload.put("hospital", "Adichunchanagiri Institute");
        payload.put("scanText", "Rx: 5% Dextrose, ORS");

        Map<String, Object> item1 = new java.util.HashMap<>();
        item1.put("medicineName", "5% Dextrose (iv)");
        item1.put("dosage", "500ml");
        item1.put("frequency", "stat");
        item1.put("timing", "Immediately / Stat");
        item1.put("duration", "stat");
        item1.put("instructions", "Infuse stat");

        Map<String, Object> item2 = new java.util.HashMap<>();
        item2.put("medicineName", "ORS");
        item2.put("dosage", "2 sachets");
        item2.put("frequency", "1+0+1");
        item2.put("timing", "Morning, Night");
        item2.put("mealRelation", "After meal");
        item2.put("duration", "3 days");
        item2.put("instructions", "Adequate fluid intake");

        payload.put("items", java.util.Arrays.asList(item1, item2));

        com.medilink.model.prescription.Prescription mockRx = new com.medilink.model.prescription.Prescription(
            "rx_test_99", "usr_123", "Vivek S.", "Dr. Ramesh (131441)", "Adichunchanagiri Institute", "Rx: 5% Dextrose, ORS"
        );

        when(prescriptionService.createPrescription(any(), any(), any(), any(), any(), any())).thenReturn(mockRx);

        ResponseEntity<Map<String, Object>> response = controller.uploadPrescription(payload);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("SUCCESS", response.getBody().get("status"));
        assertEquals("rx_test_99", response.getBody().get("prescriptionId"));
    }

    @Test
    public void testGetAllPrescriptionsWithTakenTime() {
        com.medilink.model.prescription.Prescription mockRx = new com.medilink.model.prescription.Prescription(
            "rx_test_100", "usr_123", "Vivek S.", "Dr. Ramesh", "Square Hospital", "Rx text"
        );
        com.medilink.model.prescription.PrescriptionItem item = new com.medilink.model.prescription.PrescriptionItem(
            "m1", "Napa Extra", "Paracetamol", "500mg", "1+0+1", "5 days", "Taken Time: Morning, Night | Meal: After meal"
        );
        mockRx.addItem(item);

        when(prescriptionService.findAll()).thenReturn(Collections.singletonList(mockRx));

        ResponseEntity<Map<String, Object>> response = controller.getAllPrescriptions();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        java.util.List<?> list = (java.util.List<?>) response.getBody().get("prescriptions");
        assertEquals(1, list.size());
        Map<?, ?> rxMap = (Map<?, ?>) list.get(0);
        assertEquals("Vivek S.", rxMap.get("patientName"));
        assertEquals("Dr. Ramesh", rxMap.get("doctorName"));

        java.util.List<?> items = (java.util.List<?>) rxMap.get("items");
        assertEquals(1, items.size());
        Map<?, ?> medMap = (Map<?, ?>) items.get(0);
        assertEquals("Napa Extra", medMap.get("medicineName"));
        assertEquals("Morning, Night", medMap.get("takenTime"));
    }
}
