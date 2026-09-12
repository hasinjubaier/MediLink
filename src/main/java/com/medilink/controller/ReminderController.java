package com.medilink.controller;

import com.medilink.model.reminder.Reminder;
import com.medilink.service.ReminderService;
import com.medilink.service.StockObserverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/reminders")
@CrossOrigin(origins = "*")
public class ReminderController {

    private final ReminderService reminderService;
    private final StockObserverService stockObserverService;

    @Autowired
    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
        this.stockObserverService = StockObserverService.getInstance();
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllReminders(@RequestParam(name = "patientId", required = false) String patientId,
                                                               @RequestParam(name = "email", required = false) String email) {
        List<Reminder> list = reminderService.findForPatient(patientId, email);
        String today = LocalDate.now().toString();

        List<Map<String, Object>> reminders = new ArrayList<>();
        int takenToday = 0;
        int activeCount = 0;

        for (Reminder r : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("medicine", r.getMedicineName());
            map.put("dosage", r.getDosage());
            map.put("time", r.getReminderTime());
            map.put("frequency", r.getFrequency());
            map.put("instructions", r.getInstructions());
            map.put("active", r.isActive());
            map.put("category", r.getCategory() != null ? r.getCategory() : "MEDICATION");
            map.put("mealTiming", r.getMealTiming() != null ? r.getMealTiming() : "AFTER_MEAL");
            map.put("lastTakenDate", r.getLastTakenDate());

            boolean isTaken = today.equals(r.getLastTakenDate());
            map.put("isTakenToday", isTaken);
            if (isTaken) takenToday++;
            if (r.isActive()) activeCount++;

            reminders.add(map);
        }

        int adherencePct = activeCount > 0 ? (int) Math.round(((double) takenToday / activeCount) * 100) : 100;

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("reminders", reminders);
        response.put("totalCount", list.size());
        response.put("activeCount", activeCount);
        response.put("takenTodayCount", takenToday);
        response.put("pendingTodayCount", Math.max(0, activeCount - takenToday));
        response.put("adherencePercentage", adherencePct);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createReminder(@RequestBody Map<String, String> data) {
        String patientId = data.get("patientId");
        if (patientId == null || "usr_patient_01".equals(patientId) || patientId.trim().isEmpty()) {
            patientId = "ML-9824-A";
        }
        String email = data.getOrDefault("email", "rahim@medilink.com");
        String med = data.getOrDefault("medicine", "Napa Extra");
        String dosage = data.getOrDefault("dosage", "1 Tab");
        String time = data.getOrDefault("time", "14:00");
        String freq = data.getOrDefault("frequency", "DAILY");
        String instr = data.getOrDefault("instructions", "Take after food with water");
        String mealTiming = data.getOrDefault("mealTiming", "AFTER_MEAL");
        String category = data.getOrDefault("category", "MEDICATION");

        Reminder r = reminderService.createReminder(patientId, email, med, dosage, time, freq, instr, mealTiming, category);

        stockObserverService.onNotification("REMINDER_CREATED",
                "Scheduled medicine reminder for " + med + " at " + time);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("reminderId", r.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/take")
    public ResponseEntity<Map<String, Object>> markDoseTaken(@PathVariable String id) {
        boolean ok = reminderService.markDoseTaken(id);
        Map<String, Object> response = new HashMap<>();
        response.put("status", ok ? "SUCCESS" : "ERROR");
        response.put("message", ok ? "Dose status logged." : "Reminder not found.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggleActive(@PathVariable String id) {
        boolean ok = reminderService.toggleActive(id);
        Map<String, Object> response = new HashMap<>();
        response.put("status", ok ? "SUCCESS" : "ERROR");
        response.put("message", ok ? "Reminder schedule toggled." : "Reminder not found.");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteReminder(@PathVariable String id) {
        boolean ok = reminderService.deleteReminder(id);
        Map<String, Object> response = new HashMap<>();
        response.put("status", ok ? "SUCCESS" : "ERROR");
        response.put("message", ok ? "Reminder removed." : "Reminder not found.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sync-prescriptions")
    public ResponseEntity<Map<String, Object>> syncFromPrescriptions(@RequestBody Map<String, String> body) {
        String patientId = body.getOrDefault("patientId", "ML-9824-A");
        String email = body.getOrDefault("email", "rahim@medilink.com");

        int count = reminderService.syncFromPrescriptions(patientId, email);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("addedCount", count);
        response.put("message", count > 0
                ? "Generated " + count + " reminders from your active prescriptions!"
                : "All prescription medicines are already scheduled in your reminders.");
        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/test-alert", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> triggerTestAlert() {
        stockObserverService.onNotification("REMINDER_ALARM",
                "DEMO NOTIFICATION: Time to take your scheduled dose of Napa Extra 500mg (1 Tablet) - Take after lunch with water!");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Instant alarm triggered!");
        return ResponseEntity.ok(response);
    }
}
