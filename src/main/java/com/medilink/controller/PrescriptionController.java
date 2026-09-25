package com.medilink.controller;

import com.medilink.dto.prescription.PrescriptionScanResult;
import com.medilink.model.medicine.Medicine;
import com.medilink.model.prescription.Prescription;
import com.medilink.model.prescription.PrescriptionItem;
import com.medilink.service.MedicineService;
import com.medilink.service.PrescriptionScanService;
import com.medilink.service.PrescriptionService;
import com.medilink.service.StockObserverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/prescriptions")
@CrossOrigin(origins = "*")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final MedicineService medicineService;
    private final PrescriptionScanService prescriptionScanService;
    private final StockObserverService stockObserverService;

    @Autowired
    public PrescriptionController(PrescriptionService prescriptionService,
                                  MedicineService medicineService,
                                  @Autowired(required = false) PrescriptionScanService prescriptionScanService) {
        this.prescriptionService = prescriptionService;
        this.medicineService = medicineService;
        this.prescriptionScanService = prescriptionScanService;
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
                imap.put("genericName", item.getGenericName() != null ? item.getGenericName() : "");
                imap.put("dosage", item.getDosage() != null ? item.getDosage() : "");
                imap.put("frequency", item.getFrequency() != null ? item.getFrequency() : "");
                imap.put("duration", item.getDuration() != null ? item.getDuration() : "");
                imap.put("instructions", item.getInstructions() != null ? item.getInstructions() : "");
                imap.put("takenTime", extractMedicineTakenTime(item.getFrequency(), item.getInstructions()));
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

    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> scanPrescription(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "patientId", required = false) String patientId,
            @RequestParam(value = "patientName", required = false) String patientName) {

        Map<String, Object> response = new HashMap<>();

        if (prescriptionScanService == null) {
            response.put("success", false);
            response.put("code", "PRESCRIPTION_SCAN_SERVICE_UNAVAILABLE");
            response.put("message", "Prescription scan service is not currently available.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }

        try {
            PrescriptionScanResult result = prescriptionScanService.scanPrescription(file, patientId, patientName);
            response.put("success", true);
            response.put("data", result);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("code", "INVALID_PRESCRIPTION_REQUEST");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("code", "PRESCRIPTION_SCAN_FAILED");
            response.put("message", "Unable to analyze prescription image: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadPrescription(@RequestBody Map<String, Object> data) {
        String patientId = Objects.toString(data.get("patientId"), "").trim();
        if (patientId.isEmpty() || "usr_patient_01".equals(patientId)) {
            patientId = "ML-9824-A";
        }

        // Use scanned prescription patient name as heading patient name (not the account name)
        String patientName = Objects.toString(data.get("patientName"), "").trim();
        if (patientName.isEmpty() || "Uncertain".equalsIgnoreCase(patientName)) {
            patientName = Objects.toString(data.get("loggedInPatientName"), "Patient").trim();
            if (patientName.isEmpty()) patientName = "Patient";
        }

        // Use scanned prescription doctor name
        String doctor = Objects.toString(data.get("doctorName"), "").trim();
        if (doctor.isEmpty() || "Uncertain".equalsIgnoreCase(doctor)) {
            doctor = "Dr. Consulting Physician";
        }

        String hospital = Objects.toString(data.get("hospital"), "Hospital / Clinic").trim();
        if (hospital.isEmpty() || "Uncertain".equalsIgnoreCase(hospital)) {
            hospital = "Hospital / Clinic";
        }

        String rawText = Objects.toString(data.get("scanText"), "").trim();
        String voiceAudio = Objects.toString(data.get("voiceNoteAudio"), null);
        if (voiceAudio == null) voiceAudio = Objects.toString(data.get("voiceNote"), null);

        List<PrescriptionItem> items = new ArrayList<>();

        // 1. First priority: Use structured items extracted by AI vision scan
        Object rawItemsObj = data.get("items");
        if (rawItemsObj instanceof List) {
            List<?> rawList = (List<?>) rawItemsObj;
            for (Object obj : rawList) {
                if (obj instanceof Map) {
                    Map<?, ?> itemMap = (Map<?, ?>) obj;
                    String medName = Objects.toString(itemMap.get("medicineName"), "").trim();
                    if (medName.isEmpty()) {
                        medName = Objects.toString(itemMap.get("name"), "Medication").trim();
                    }
                    if ("Medication".equalsIgnoreCase(medName) && itemMap.get("brandName") != null) {
                        medName = Objects.toString(itemMap.get("brandName"), "Medication").trim();
                    }

                    String generic = Objects.toString(itemMap.get("genericName"), "").trim();
                    String dosage = Objects.toString(itemMap.get("dosage"), "").trim();
                    if (dosage.isEmpty()) {
                        dosage = Objects.toString(itemMap.get("dosageAmount"), "").trim();
                    }
                    if (dosage.isEmpty()) {
                        dosage = Objects.toString(itemMap.get("strength"), "Standard").trim();
                    }

                    String freq = Objects.toString(itemMap.get("frequency"), "").trim();
                    if (freq.isEmpty()) {
                        freq = Objects.toString(itemMap.get("freq"), "1+0+1").trim();
                    }

                    String duration = Objects.toString(itemMap.get("duration"), "As prescribed").trim();

                    // Extract taken time and timing components
                    String timing = Objects.toString(itemMap.get("timing"), "").trim();
                    String exactTimes = Objects.toString(itemMap.get("exactTimes"), "").trim();
                    String meal = Objects.toString(itemMap.get("mealRelation"), "").trim();
                    String note = Objects.toString(itemMap.get("instructions"), "").trim();

                    // Format structured instructions to preserve taken time and clinical notes
                    StringBuilder instrBuilder = new StringBuilder();
                    if (!timing.isEmpty()) {
                        instrBuilder.append("Taken Time: ").append(timing);
                    }
                    if (!exactTimes.isEmpty()) {
                        if (instrBuilder.length() > 0) instrBuilder.append(" (").append(exactTimes).append(")");
                        else instrBuilder.append("Taken Time: ").append(exactTimes);
                    }
                    if (!meal.isEmpty()) {
                        if (instrBuilder.length() > 0) instrBuilder.append(" | Meal: ").append(meal);
                        else instrBuilder.append("Meal: ").append(meal);
                    }
                    if (!note.isEmpty()) {
                        if (instrBuilder.length() > 0) instrBuilder.append(" | Note: ").append(note);
                        else instrBuilder.append(note);
                    }

                    String fullInstructions = instrBuilder.length() > 0 ? instrBuilder.toString() : "Take as clinically prescribed";
                    String medId = "med_" + UUID.randomUUID().toString().substring(0, 8);

                    items.add(new PrescriptionItem(
                        medId,
                        medName,
                        generic,
                        dosage,
                        freq,
                        duration,
                        fullInstructions
                    ));
                }
            }
        }

        // 2. Second priority: If no structured items sent, match against medicine catalog
        if (items.isEmpty() && !rawText.isEmpty()) {
            String lowerScan = rawText.toLowerCase();
            List<Medicine> catalog = medicineService.findAll();
            for (Medicine m : catalog) {
                if (lowerScan.contains(m.getBrandName().toLowerCase()) || lowerScan.contains(m.getGenericName().toLowerCase())) {
                    items.add(new PrescriptionItem(
                        m.getId(),
                        m.getBrandName(),
                        m.getGenericName(),
                        m.getStrength(),
                        "1+0+1",
                        "7 days",
                        "Taken Time: Morning, Night | Take as clinically prescribed"
                    ));
                }
            }
        }

        // 3. Fallback only if absolutely no medicines detected anywhere
        if (items.isEmpty()) {
            items.add(new PrescriptionItem("med_01", "Prescribed Medication", "Clinical Therapy", "Standard", "1+0+1", "5 days", "Taken Time: Morning, Night | Take as directed"));
        }

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

    private String extractMedicineTakenTime(String freq, String instructions) {
        String instr = instructions != null ? instructions : "";
        String f = freq != null ? freq.trim() : "";

        // Check if instructions already has "Taken Time:" or "Time:"
        if (instr.contains("Taken Time:")) {
            int start = instr.indexOf("Taken Time:") + 11;
            int end = instr.indexOf("|", start);
            String sub = (end != -1 ? instr.substring(start, end) : instr.substring(start)).trim();
            if (!sub.isEmpty()) return sub;
        } else if (instr.contains("Time:")) {
            int start = instr.indexOf("Time:") + 5;
            int end = instr.indexOf("|", start);
            String sub = (end != -1 ? instr.substring(start, end) : instr.substring(start)).trim();
            if (!sub.isEmpty()) return sub;
        }

        // Map standard clinical frequencies to explicit times of day
        if (f.contains("1+1+1") || f.toLowerCase().contains("3 times") || f.equalsIgnoreCase("tid")) {
            return "Morning, Afternoon & Night";
        } else if (f.contains("1+0+1") || f.toLowerCase().contains("2 times") || f.equalsIgnoreCase("bd") || f.equalsIgnoreCase("bid")) {
            return "Morning & Night";
        } else if (f.contains("1+0+0") || f.equalsIgnoreCase("morning")) {
            return "Morning (Breakfast)";
        } else if (f.contains("0+1+0") || f.equalsIgnoreCase("afternoon")) {
            return "Afternoon (Lunch)";
        } else if (f.contains("0+0+1") || f.toLowerCase().contains("night") || f.equalsIgnoreCase("hs")) {
            return "Night (Bedtime)";
        } else if (f.toLowerCase().contains("stat")) {
            return "Immediately (Stat Dose)";
        } else if (f.toLowerCase().contains("once daily") || f.equalsIgnoreCase("od")) {
            return "Once Daily";
        } else if (f.toLowerCase().contains("sos") || f.toLowerCase().contains("needed")) {
            return "As Needed (SOS)";
        }

        return f.isEmpty() ? "As Clinically Prescribed" : f;
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

    @PostMapping("/revert")
    public ResponseEntity<Map<String, Object>> revertWorkflow(@RequestBody Map<String, String> data) {
        String rxId = data.get("prescriptionId");
        Optional<Prescription> updated = prescriptionService.revertWorkflow(rxId);

        Map<String, Object> response = new HashMap<>();
        if (updated.isPresent()) {
            Prescription rx = updated.get();
            stockObserverService.onNotification("PRESCRIPTION_REVERTED",
                    "Prescription " + rx.getId() + " reverted. Status: " + rx.getStatus());

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
