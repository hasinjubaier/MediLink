package com.medilink.service;

import com.medilink.dto.prescription.PrescriptionScanResult;
import com.medilink.dto.prescription.RawExtractedPrescription;
import com.medilink.model.user.Patient;
import com.medilink.repository.PatientRepository;
import com.medilink.service.ai.GeminiPrescriptionClient;
import com.medilink.service.ai.GroqPrescriptionClient;
import com.medilink.service.ai.PrescriptionReconciliationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

/**
 * Service orchestrating dual-engine AI prescription vision scan,
 * validation, fallback handling, and reconciliation.
 */
@Service
public class PrescriptionScanService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = new HashSet<>(Arrays.asList(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/webp"
    ));

    private final GeminiPrescriptionClient geminiClient;
    private final GroqPrescriptionClient groqClient;
    private final PrescriptionReconciliationService reconciliationService;
    private final PatientRepository patientRepository;

    @Autowired
    public PrescriptionScanService(GeminiPrescriptionClient geminiClient,
                                   GroqPrescriptionClient groqClient,
                                   PrescriptionReconciliationService reconciliationService,
                                   @Autowired(required = false) PatientRepository patientRepository) {
        this.geminiClient = geminiClient;
        this.groqClient = groqClient;
        this.reconciliationService = reconciliationService;
        this.patientRepository = patientRepository;
    }

    public PrescriptionScanResult scanPrescription(MultipartFile file, String patientId, String clientPatientName) throws Exception {
        validateFile(file);

        byte[] imageBytes = file.getBytes();
        String contentType = resolveContentType(file);

        String loggedInName = clientPatientName;
        if (patientId != null && !patientId.trim().isEmpty() && patientRepository != null) {
            Optional<Patient> pOpt = patientRepository.findById(patientId.trim());
            if (pOpt.isPresent()) {
                loggedInName = pOpt.get().getName();
            }
        }

        System.out.println("[AI Vision] Prescription scan started: fileName=" + file.getOriginalFilename() +
            ", size=" + file.getSize() + " bytes, patientId=" + patientId);

        RawExtractedPrescription geminiResult = null;
        boolean geminiOk = false;
        String geminiError = null;

        try {
            geminiResult = geminiClient.extract(imageBytes, contentType);
            geminiOk = true;
        } catch (Exception e) {
            geminiError = e.getMessage();
            System.err.println("[AI Vision] Gemini execution failed: " + geminiError);
        }

        RawExtractedPrescription groqResult = null;
        boolean groqOk = false;
        String groqError = null;

        try {
            if (geminiResult != null && geminiResult.getRawText() != null) {
                groqClient.setPrescriptionTextContext(geminiResult.getRawText());
            }
            groqResult = groqClient.extract(imageBytes, contentType);
            groqOk = true;
        } catch (Exception e) {
            groqError = e.getMessage();
            System.err.println("[AI Vision] Groq execution failed: " + groqError);
        } finally {
            groqClient.clearPrescriptionTextContext();
        }

        // Case D: Both failed / unavailable
        if (!geminiOk && !groqOk) {
            String combinedError = "Both Gemini and Groq AI engines failed to process the prescription.";
            if (geminiError != null && groqError != null) {
                combinedError = "AI Vision services unavailable. (Gemini: " + geminiError + " | Groq: " + groqError + ")";
            }
            throw new IllegalArgumentException(combinedError);
        }

        // Case E: Image unreadable or clearly not a medical prescription
        boolean geminiNotRx = (geminiResult != null && Boolean.FALSE.equals(geminiResult.getIsPrescription()));
        boolean groqNotRx = (groqResult != null && Boolean.FALSE.equals(groqResult.getIsPrescription()));
        int totalMeds = (geminiResult != null && geminiResult.getMedicines() != null ? geminiResult.getMedicines().size() : 0)
                      + (groqResult != null && groqResult.getMedicines() != null ? groqResult.getMedicines().size() : 0);

        if ((geminiNotRx && groqNotRx) || (geminiOk && groqOk && totalMeds == 0 && (geminiResult.getRawText() == null || geminiResult.getRawText().trim().length() < 10))) {
            throw new IllegalArgumentException("Prescription could not be read clearly. Please upload a clearer image.");
        }

        // Reconcile outputs (Handles Case A, B, C)
        PrescriptionScanResult reconciled = reconciliationService.reconcile(
            geminiResult,
            groqResult,
            geminiOk,
            groqOk,
            patientId,
            loggedInName
        );

        if (geminiError != null) {
            reconciled.getWarnings().add("Gemini note: " + geminiError);
        }
        if (groqError != null) {
            reconciled.getWarnings().add("Groq note: " + groqError);
        }

        return reconciled;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Prescription file is empty or missing. Please select a valid document.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds 10MB limit. Please upload an image under 10MB.");
        }

        String contentType = file.getContentType();
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";

        boolean matchesType = contentType != null && ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase());
        boolean matchesExt = name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp");

        if (!matchesType && !matchesExt) {
            throw new IllegalArgumentException("Unsupported file type (" + (contentType != null ? contentType : "unknown") +
                "). Only JPG, PNG, and WebP images are supported.");
        }

        try {
            if (file.getBytes().length < 50) {
                throw new IllegalArgumentException("Corrupted or empty image file.");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read uploaded file: " + e.getMessage());
        }
    }

    private String resolveContentType(MultipartFile file) {
        if (file.getContentType() != null && !file.getContentType().trim().isEmpty()) {
            return file.getContentType().trim().toLowerCase();
        }
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }
}
