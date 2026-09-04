package com.medilink.controller;

import com.medilink.model.reminder.Reminder;
import com.medilink.service.ReminderService;
import com.medilink.service.StockObserverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Map<String, Object>> getAllReminders() {
        List<Reminder> list = reminderService.findAllActive();
        List<Map<String, Object>> reminders = new ArrayList<>();
        for (Reminder r : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("medicine", r.getMedicineName());
            map.put("dosage", r.getDosage());
            map.put("time", r.getReminderTime());
            map.put("frequency", r.getFrequency());
            map.put("instructions", r.getInstructions());
            map.put("active", r.isActive());
            reminders.add(map);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("reminders", reminders);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createReminder(@RequestBody Map<String, String> data) {
        String email = data.getOrDefault("email", "rahim@medilink.com");
        String med = data.getOrDefault("medicine", "Napa Extra");
        String dosage = data.getOrDefault("dosage", "1 Tab");
        String time = data.getOrDefault("time", "14:00");
        String freq = data.getOrDefault("frequency", "DAILY");
        String instr = data.getOrDefault("instructions", "Take after food with water");

        Reminder r = reminderService.createReminder("usr_patient_01", email, med, dosage, time, freq, instr);

        stockObserverService.onNotification("REMINDER_CREATED",
                "Scheduled medicine reminder for " + med + " at " + time);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("reminderId", r.getId());
        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/test-alert", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> triggerTestAlert() {
        stockObserverService.onNotification("REMINDER_ALARM",
                "DEMO NOTIFICATION: Time to take your scheduled dose of Napa Extra 500mg!");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Instant alarm triggered!");
        return ResponseEntity.ok(response);
    }
}
