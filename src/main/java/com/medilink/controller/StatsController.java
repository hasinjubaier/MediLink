package com.medilink.controller;

import com.medilink.repository.MedicineRepository;
import com.medilink.repository.PharmacyRepository;
import com.medilink.repository.PrescriptionRepository;
import com.medilink.repository.ReminderRepository;
import com.medilink.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/stats")
@CrossOrigin(origins = "*")
public class StatsController {

    private final UserRepository userRepository;
    private final PharmacyRepository pharmacyRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final ReminderRepository reminderRepository;

    @Autowired
    public StatsController(UserRepository userRepository,
                           PharmacyRepository pharmacyRepository,
                           PrescriptionRepository prescriptionRepository,
                           ReminderRepository reminderRepository) {
        this.userRepository = userRepository;
        this.pharmacyRepository = pharmacyRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.reminderRepository = reminderRepository;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getStats() {
        int userCount = (int) userRepository.count();
        int pharmacyCount = (int) pharmacyRepository.count();
        int rxCount = (int) prescriptionRepository.count();
        int reminderCount = (int) reminderRepository.count();

        int activeUsers = 10000 + (userCount * 45) + (rxCount * 80);
        int certifiedPharmacists = 500 + (pharmacyCount * 12);
        double remindersSentMillions = 1.0 + (reminderCount * 0.05);

        int docTimeReduction = 30 + Math.min(rxCount * 2, 15);
        int engagementRate = 40 + Math.min(userCount * 3, 20);

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("activeUsers", activeUsers);
        resp.put("activeUsersFormatted", String.format("%,d+", activeUsers));
        resp.put("certifiedPharmacists", certifiedPharmacists);
        resp.put("certifiedPharmacistsFormatted", certifiedPharmacists + "+");
        resp.put("certifiedDoctors", certifiedPharmacists);
        resp.put("remindersSent", String.format(Locale.US, "%.1fM+", remindersSentMillions));
        resp.put("docTimeReduction", -docTimeReduction);
        resp.put("docTimeReductionFormatted", "-" + docTimeReduction + "%");
        resp.put("engagementRate", engagementRate);
        resp.put("engagementRateFormatted", "+" + engagementRate + "%");

        return ResponseEntity.ok(resp);
    }
}
