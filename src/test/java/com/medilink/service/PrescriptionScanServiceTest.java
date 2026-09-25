package com.medilink.service;

import com.medilink.dto.prescription.PrescriptionScanResult;
import com.medilink.dto.prescription.RawExtractedPrescription;
import com.medilink.dto.prescription.RawMedicineItem;
import com.medilink.dto.prescription.ScannedMedicineItem;
import com.medilink.model.user.Patient;
import com.medilink.repository.PatientRepository;
import com.medilink.service.ai.GeminiPrescriptionClient;
import com.medilink.service.ai.GroqPrescriptionClient;
import com.medilink.service.ai.PrescriptionReconciliationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PrescriptionScanServiceTest {

    @Mock
    private GeminiPrescriptionClient geminiClient;

    @Mock
    private GroqPrescriptionClient groqClient;

    @Mock
    private PatientRepository patientRepository;

    private PrescriptionReconciliationService reconciliationService;
    private PrescriptionScanService scanService;

    @BeforeEach
    public void setUp() {
        reconciliationService = new PrescriptionReconciliationService();
        scanService = new PrescriptionScanService(geminiClient, groqClient, reconciliationService, patientRepository);
    }

    private byte[] createDummyImageBytes() {
        byte[] bytes = new byte[256];
        Arrays.fill(bytes, (byte) 0x2A);
        return bytes;
    }

    private RawMedicineItem createSampleRawMed(String name, String strength, String dose,
                                               String freq, String meal, String duration, String qty) {
        RawMedicineItem item = new RawMedicineItem();
        item.setMedicineName(name);
        item.setStrength(strength);
        item.setDosageAmount(dose);
        item.setFrequency(freq);
        item.setMealRelation(meal);
        item.setDuration(duration);
        item.setTotalQuantity(qty);
        item.setTiming(Arrays.asList("Morning", "Night"));
        item.setInstructions("Take after food with full glass of water");
        return item;
    }

    // 1. Valid image upload
    @Test
    public void testValidImageUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "rx.jpg", "image/jpeg", createDummyImageBytes());

        RawExtractedPrescription raw = new RawExtractedPrescription();
        raw.setPatientName("Rahim Ahmed");
        raw.setDoctorName("Dr. Farhan Kabir");
        raw.setHospitalName("Square Hospital");
        raw.setRawText("Rx: Napa Extra 500mg 1+0+1");
        raw.setMedicines(Collections.singletonList(createSampleRawMed("Napa Extra", "500mg", "1 tablet", "1+0+1", "After meal", "5 days", "10 tablets")));

        when(geminiClient.extract(any(byte[].class), anyString())).thenReturn(raw);
        when(groqClient.extract(any(byte[].class), anyString())).thenReturn(raw);

        PrescriptionScanResult result = scanService.scanPrescription(file, "P100", "Rahim Ahmed");
        assertNotNull(result);
        assertEquals("Rahim Ahmed", result.getPatientName());
        assertEquals("Dr. Farhan Kabir", result.getDoctorName());
        assertEquals("Square Hospital", result.getHospitalName());
        assertEquals(1, result.getMedicines().size());
        assertEquals("VERIFIED_BY_BOTH", result.getVerificationStatus());
        assertFalse(result.isNeedsVerification());
    }

    // 2. Unsupported file type
    @Test
    public void testUnsupportedFileType() {
        MockMultipartFile file = new MockMultipartFile("file", "document.txt", "text/plain", createDummyImageBytes());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            scanService.scanPrescription(file, null, null);
        });
        assertTrue(ex.getMessage().contains("Unsupported file type"));
    }

    // 3. Empty file
    @Test
    public void testEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            scanService.scanPrescription(file, null, null);
        });
        assertTrue(ex.getMessage().contains("empty or missing"));
    }

    // 4. Gemini success (alone)
    @Test
    public void testGeminiSuccessAlone() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "rx.png", "image/png", createDummyImageBytes());

        RawExtractedPrescription gRaw = new RawExtractedPrescription();
        gRaw.setPatientName("Karim");
        gRaw.setDoctorName("Dr. Roy");
        gRaw.setMedicines(Collections.singletonList(createSampleRawMed("Seclo 20", "20mg", "1 cap", "1+0+1", "Before meal", "7 days", "14 capsules")));

        when(geminiClient.extract(any(byte[].class), anyString())).thenReturn(gRaw);
        when(groqClient.extract(any(byte[].class), anyString())).thenThrow(new RuntimeException("Groq API Timeout"));

        PrescriptionScanResult result = scanService.scanPrescription(file, null, null);
        assertNotNull(result);
        assertEquals("Karim", result.getPatientName());
        assertEquals("GROQ_UNAVAILABLE", result.getVerificationStatus());
        assertTrue(result.isNeedsVerification());
        assertEquals(1, result.getMedicines().size());
    }

    // 5. Groq success (alone)
    @Test
    public void testGroqSuccessAlone() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "rx.webp", "image/webp", createDummyImageBytes());

        RawExtractedPrescription qRaw = new RawExtractedPrescription();
        qRaw.setPatientName("Fatima");
        qRaw.setDoctorName("Dr. Alam");
        qRaw.setMedicines(Collections.singletonList(createSampleRawMed("Maxpro 20", "20mg", "1 tablet", "1+0+0", "Before meal", "14 days", "14 tablets")));

        when(geminiClient.extract(any(byte[].class), anyString())).thenThrow(new RuntimeException("Gemini quota exceeded"));
        when(groqClient.extract(any(byte[].class), anyString())).thenReturn(qRaw);

        PrescriptionScanResult result = scanService.scanPrescription(file, null, null);
        assertNotNull(result);
        assertEquals("Fatima", result.getPatientName());
        assertEquals("GEMINI_UNAVAILABLE", result.getVerificationStatus());
        assertTrue(result.isNeedsVerification());
    }

    // 6. Gemini failure
    @Test
    public void testGeminiFailureFallbackToGroq() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "rx.jpg", "image/jpeg", createDummyImageBytes());

        RawExtractedPrescription qRaw = new RawExtractedPrescription();
        qRaw.setMedicines(Collections.singletonList(createSampleRawMed("Fexo 120", "120mg", "1 tablet", "0+0+1", "After meal", "10 days", "10 tablets")));

        when(geminiClient.extract(any(byte[].class), anyString())).thenThrow(new RuntimeException("Gemini 500 Internal Error"));
        when(groqClient.extract(any(byte[].class), anyString())).thenReturn(qRaw);

        PrescriptionScanResult result = scanService.scanPrescription(file, null, null);
        assertEquals("GEMINI_UNAVAILABLE", result.getVerificationStatus());
        assertEquals(1, result.getMedicines().size());
    }

    // 7. Groq failure
    @Test
    public void testGroqFailureFallbackToGemini() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "rx.jpg", "image/jpeg", createDummyImageBytes());

        RawExtractedPrescription gRaw = new RawExtractedPrescription();
        gRaw.setMedicines(Collections.singletonList(createSampleRawMed("Monas 10", "10mg", "1 tablet", "0+0+1", "Night", "1 month", "30 tablets")));

        when(geminiClient.extract(any(byte[].class), anyString())).thenReturn(gRaw);
        when(groqClient.extract(any(byte[].class), anyString())).thenThrow(new RuntimeException("Groq 429 Rate Limit"));

        PrescriptionScanResult result = scanService.scanPrescription(file, null, null);
        assertEquals("GROQ_UNAVAILABLE", result.getVerificationStatus());
        assertEquals(1, result.getMedicines().size());
    }

    // 8. Both APIs fail
    @Test
    public void testBothApisFail() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "rx.jpg", "image/jpeg", createDummyImageBytes());

        when(geminiClient.extract(any(byte[].class), anyString())).thenThrow(new RuntimeException("Gemini service unavailable"));
        when(groqClient.extract(any(byte[].class), anyString())).thenThrow(new RuntimeException("Groq service unavailable"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            scanService.scanPrescription(file, null, null);
        });
        assertTrue(ex.getMessage().contains("unavailable") || ex.getMessage().contains("failed"));
    }

    // 9. Invalid JSON or unreadable image
    @Test
    public void testUnreadableNonPrescriptionImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "selfie.jpg", "image/jpeg", createDummyImageBytes());

        RawExtractedPrescription gRaw = new RawExtractedPrescription();
        gRaw.setIsPrescription(false);
        gRaw.setMedicines(Collections.emptyList());

        RawExtractedPrescription qRaw = new RawExtractedPrescription();
        qRaw.setIsPrescription(false);
        qRaw.setMedicines(Collections.emptyList());

        when(geminiClient.extract(any(byte[].class), anyString())).thenReturn(gRaw);
        when(groqClient.extract(any(byte[].class), anyString())).thenReturn(qRaw);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            scanService.scanPrescription(file, null, null);
        });
        assertTrue(ex.getMessage().contains("Prescription could not be read clearly"));
    }

    // 10. Conflicting medicine dosage
    @Test
    public void testConflictingMedicineDosage() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "rx.jpg", "image/jpeg", createDummyImageBytes());

        RawExtractedPrescription gRaw = new RawExtractedPrescription();
        gRaw.setMedicines(Collections.singletonList(createSampleRawMed("Napa Extra", "500mg", "1 tablet", "1+0+1", "After meal", "5 days", "10 tablets")));

        RawExtractedPrescription qRaw = new RawExtractedPrescription();
        qRaw.setMedicines(Collections.singletonList(createSampleRawMed("Napa Extra", "500mg", "2 tablets", "1+0+1", "After meal", "5 days", "10 tablets")));

        when(geminiClient.extract(any(byte[].class), anyString())).thenReturn(gRaw);
        when(groqClient.extract(any(byte[].class), anyString())).thenReturn(qRaw);

        PrescriptionScanResult result = scanService.scanPrescription(file, null, null);
        assertEquals("CONFLICTS_DETECTED", result.getVerificationStatus());
        assertTrue(result.isNeedsVerification());

        ScannedMedicineItem med = result.getMedicines().get(0);
        assertEquals("Uncertain", med.getDosageAmount());
        assertTrue(med.getUncertainFields().contains("dosageAmount"));
        assertNotNull(med.getAlternatives().get("dosageAmount"));
        assertEquals(2, med.getAlternatives().get("dosageAmount").size());
    }

    // 11. Missing patient name
    @Test
    public void testMissingPatientName() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "rx.jpg", "image/jpeg", createDummyImageBytes());

        RawExtractedPrescription raw = new RawExtractedPrescription();
        raw.setPatientName(null); // not present on prescription
        raw.setDoctorName("Dr. Rahman");
        raw.setHospitalName("Apollo Clinic");
        raw.setMedicines(Collections.singletonList(createSampleRawMed("Ace Plus", "500mg+65mg", "1 tablet", "1+1+1", "After meal", "3 days", "9 tablets")));

        when(geminiClient.extract(any(byte[].class), anyString())).thenReturn(raw);
        when(groqClient.extract(any(byte[].class), anyString())).thenReturn(raw);

        PrescriptionScanResult result = scanService.scanPrescription(file, "P200", "Account User Name");
        assertNull(result.getPatientName()); // Never invented
        assertEquals("Account User Name", result.getLoggedInPatientName()); // Kept separate
    }

    // 12. Missing doctor name
    @Test
    public void testMissingDoctorName() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "rx.jpg", "image/jpeg", createDummyImageBytes());

        RawExtractedPrescription raw = new RawExtractedPrescription();
        raw.setPatientName("Tariq");
        raw.setDoctorName(null);
        raw.setHospitalName("City Hospital");
        raw.setMedicines(Collections.singletonList(createSampleRawMed("Ciprocin", "500mg", "1 tab", "1+0+1", "After meal", "7 days", "14 tablets")));

        when(geminiClient.extract(any(byte[].class), anyString())).thenReturn(raw);
        when(groqClient.extract(any(byte[].class), anyString())).thenReturn(raw);

        PrescriptionScanResult result = scanService.scanPrescription(file, null, null);
        assertNull(result.getDoctorName());
        assertEquals("Tariq", result.getPatientName());
    }

    // 13. Missing hospital
    @Test
    public void testMissingHospital() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "rx.jpg", "image/jpeg", createDummyImageBytes());

        RawExtractedPrescription raw = new RawExtractedPrescription();
        raw.setPatientName("Tariq");
        raw.setDoctorName("Dr. Kamal");
        raw.setHospitalName(null);
        raw.setMedicines(Collections.singletonList(createSampleRawMed("Finix", "20mg", "1 tab", "1+0+0", "Before breakfast", "14 days", "14 tablets")));

        when(geminiClient.extract(any(byte[].class), anyString())).thenReturn(raw);
        when(groqClient.extract(any(byte[].class), anyString())).thenReturn(raw);

        PrescriptionScanResult result = scanService.scanPrescription(file, null, null);
        assertNull(result.getHospitalName());
    }

    // 14. Multiple medicines
    @Test
    public void testMultipleMedicines() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "rx.jpg", "image/jpeg", createDummyImageBytes());

        RawExtractedPrescription raw = new RawExtractedPrescription();
        raw.setMedicines(Arrays.asList(
            createSampleRawMed("Napa Extra", "500mg+65mg", "1 tablet", "1+1+1", "After meal", "5 days", "15 tablets"),
            createSampleRawMed("Seclo 20", "20mg", "1 capsule", "1+0+1", "Before meal", "10 days", "20 capsules"),
            createSampleRawMed("Monas 10", "10mg", "1 tablet", "0+0+1", "Bedtime", "30 days", "30 tablets")
        ));

        when(geminiClient.extract(any(byte[].class), anyString())).thenReturn(raw);
        when(groqClient.extract(any(byte[].class), anyString())).thenReturn(raw);

        PrescriptionScanResult result = scanService.scanPrescription(file, null, null);
        assertEquals(3, result.getMedicines().size());
        assertEquals("Napa Extra", result.getMedicines().get(0).getMedicineName());
        assertEquals("Seclo 20", result.getMedicines().get(1).getMedicineName());
        assertEquals("Monas 10", result.getMedicines().get(2).getMedicineName());
    }

    // 15. Meal timing extraction
    @Test
    public void testMealTimingExtraction() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "rx.jpg", "image/jpeg", createDummyImageBytes());

        RawExtractedPrescription raw = new RawExtractedPrescription();
        raw.setMedicines(Collections.singletonList(createSampleRawMed("Sergel 20", "20mg", "1 cap", "1+0+1", "Before meal", "14 days", "28 capsules")));

        when(geminiClient.extract(any(byte[].class), anyString())).thenReturn(raw);
        when(groqClient.extract(any(byte[].class), anyString())).thenReturn(raw);

        PrescriptionScanResult result = scanService.scanPrescription(file, null, null);
        assertEquals("Before meal", result.getMedicines().get(0).getMealRelation());
    }

    // 16. Frequency extraction
    @Test
    public void testFrequencyExtraction() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "rx.jpg", "image/jpeg", createDummyImageBytes());

        RawExtractedPrescription raw = new RawExtractedPrescription();
        raw.setMedicines(Collections.singletonList(createSampleRawMed("Flamyd", "400mg", "1 tab", "1+1+1 (3 times daily)", "After meal", "5 days", "15 tablets")));

        when(geminiClient.extract(any(byte[].class), anyString())).thenReturn(raw);
        when(groqClient.extract(any(byte[].class), anyString())).thenReturn(raw);

        PrescriptionScanResult result = scanService.scanPrescription(file, null, null);
        assertTrue(result.getMedicines().get(0).getFrequency().contains("1+1+1"));
    }

    // 17. Duration extraction
    @Test
    public void testDurationExtraction() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "rx.jpg", "image/jpeg", createDummyImageBytes());

        RawExtractedPrescription raw = new RawExtractedPrescription();
        raw.setMedicines(Collections.singletonList(createSampleRawMed("Azithrocin 500", "500mg", "1 tab", "1+0+0", "After meal", "5 days", "5 tablets")));

        when(geminiClient.extract(any(byte[].class), anyString())).thenReturn(raw);
        when(groqClient.extract(any(byte[].class), anyString())).thenReturn(raw);

        PrescriptionScanResult result = scanService.scanPrescription(file, null, null);
        assertEquals("5 days", result.getMedicines().get(0).getDuration());
    }

    // 18. Quantity extraction
    @Test
    public void testQuantityExtraction() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "rx.jpg", "image/jpeg", createDummyImageBytes());

        RawExtractedPrescription raw = new RawExtractedPrescription();
        raw.setMedicines(Collections.singletonList(createSampleRawMed("Filwel Gold", "Multivitamin", "1 tablet", "Once daily", "After breakfast", "30 days", "30 tablets")));

        when(geminiClient.extract(any(byte[].class), anyString())).thenReturn(raw);
        when(groqClient.extract(any(byte[].class), anyString())).thenReturn(raw);

        PrescriptionScanResult result = scanService.scanPrescription(file, null, null);
        assertEquals("30 tablets", result.getMedicines().get(0).getTotalQuantity());
    }
}
