package com.medilink.controller;

import com.medilink.model.prescription.Prescription;
import com.medilink.model.prescription.PrescriptionItem;
import com.medilink.service.PrescriptionService;
import com.medilink.service.StockObserverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/prescriptions")
@CrossOrigin(origins = "*")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final StockObserverService stockObserverService;

    @Autowired
    public PrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
        this.stockObserverService = StockObserverService.getInstance();
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllPrescriptions() {
        List<Prescription> list = prescriptionService.findAll();
        List<Map<String, Object>> rxs = new ArrayList<>();

        for (Prescription p : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("patientName", p.getPatientName());
            map.put("doctorName", p.getDoctorName());
            map.put("hospital", p.getHospitalOrClinic());
            map.put("rawScanText", p.getRawScanText());
            map.put("status", p.getStatus());
            map.put("isDispenseReady", p.isDispenseReady());
            map.put("voiceNoteAudio", p.getVoiceNoteAudio() != null ? p.getVoiceNoteAudio() : "");

            List<Map<String, String>> items = new ArrayList<>();
            for (PrescriptionItem item : p.getItems()) {
                Map<String, String> imap = new HashMap<>();
                imap.put("medicineName", item.getMedicineName());
                imap.put("dosage", item.getDosage());
                imap.put("frequency", item.getFrequency());
                imap.put("instructions", item.getInstructions());
                items.add(imap);
            }
            map.put("items", items);
            rxs.add(map);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("prescriptions", rxs);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadPrescription(@RequestBody Map<String, String> data) {
        String patientId = data.getOrDefault("patientId", "usr_patient_01");
        String patientName = data.getOrDefault("patientName", "Rahim Ahmed");
        String doctor = data.getOrDefault("doctorName", "Dr. S. K. Roy");
        String hospital = data.getOrDefault("hospital", "Square Hospital Dhaka");
        String rawText = data.getOrDefault("scanText", "Rx: Tab Napa Extra 500mg (1+1+1), Cap Seclo 20mg (1+0+1)");

        String voiceAudio = data.get("voiceNoteAudio");
        if (voiceAudio == null) voiceAudio = data.get("voiceNote");

        List<PrescriptionItem> items = new ArrayList<>();
        items.add(new PrescriptionItem("med_01", "Napa Extra", "Paracetamol + Caffeine", "500mg", "1+1+1", "5 days", "For fever and pain"));
        items.add(new PrescriptionItem("med_05", "Seclo 20", "Omeprazole", "20mg", "1+0+1", "7 days", "Before meals"));

        Prescription rx = prescriptionService.createPrescription(patientId, patientName, doctor, hospital, rawText, items);
        if (voiceAudio != null && !voiceAudio.trim().isEmpty()) {
            rx.setVoiceNoteAudio(voiceAudio.trim());
        }
        rx.advanceWorkflow(); // Advances to EXTRACTED state
        prescriptionService.save(rx);

        stockObserverService.onNotification("PRESCRIPTION_SUBMITTED",
                "New prescription scanned for " + patientName + " by " + doctor);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("prescriptionId", rx.getId());
        response.put("workflowStatus", rx.getStatus());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/advance")
    public ResponseEntity<Map<String, Object>> advanceWorkflow(@RequestBody Map<String, String> data) {
        String rxId = data.get("prescriptionId");
        Optional<Prescription> updated = prescriptionService.advanceWorkflow(rxId);

        Map<String, Object> response = new HashMap<>();
        if (updated.isPresent()) {
            Prescription rx = updated.get();
            rx.setVerifiedByPharmacistId("usr_pharma_01");
            prescriptionService.save(rx);

            stockObserverService.onNotification("PRESCRIPTION_VERIFIED",
                    "Prescription " + rx.getId() + " verified by Pharmacist. Status: " + rx.getStatus());

            response.put("status", "SUCCESS");
            response.put("newStatus", rx.getStatus());
            response.put("dispenseReady", rx.isDispenseReady());
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "ERROR");
            response.put("message", "Prescription not found");
            return ResponseEntity.status(404).body(response);
        }
    }

    @PostMapping("/delete")
    public ResponseEntity<Map<String, Object>> deletePrescription(@RequestBody Map<String, String> data) {
        String rxId = data.get("prescriptionId");
        boolean deleted = prescriptionService.deletePrescription(rxId);

        Map<String, Object> response = new HashMap<>();
        if (deleted) {
            stockObserverService.onNotification("PRESCRIPTION_DELETED",
                    "Prescription " + rxId + " deleted from active records.");
            response.put("status", "SUCCESS");
            response.put("message", "Prescription deleted successfully");
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "ERROR");
            response.put("message", "Prescription not found");
            return ResponseEntity.status(404).body(response);
        }
    }
}
