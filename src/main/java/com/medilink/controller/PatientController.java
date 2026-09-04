package com.medilink.controller;

import com.medilink.model.user.Patient;
import com.medilink.model.user.User;
import com.medilink.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping({"/api/patient", "/api/user"})
@CrossOrigin(origins = "*")
public class PatientController {

    private final UserService userService;

    @Autowired
    public PatientController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfilePost(@RequestBody Map<String, Object> body) {
        return handleProfileUpdate(body);
    }

    @PutMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfilePut(@RequestBody Map<String, Object> body) {
        return handleProfileUpdate(body);
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile(@RequestParam(required = false) String id,
                                                          @RequestParam(required = false) String email) {
        User user = null;
        if (id != null && !id.trim().isEmpty()) {
            user = userService.findById(id.trim()).orElse(null);
        }
        if (user == null && email != null && !email.trim().isEmpty()) {
            user = userService.findByEmail(email.trim()).orElse(null);
        }

        if (user == null) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "ERROR");
            resp.put("message", "User not found");
            return ResponseEntity.badRequest().body(resp);
        }

        return ResponseEntity.ok(buildUserResponse(user, "Profile loaded successfully"));
    }

    private ResponseEntity<Map<String, Object>> handleProfileUpdate(Map<String, Object> body) {
        String id = (String) body.get("id");
        try {
            User updated = userService.updateProfile(id, body);
            return ResponseEntity.ok(buildUserResponse(updated, "Profile updated successfully"));
        } catch (Exception e) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "ERROR");
            resp.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }

    public static Map<String, Object> buildUserResponse(User user, String message) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("message", message);
        resp.put("id", user.getId());
        resp.put("name", user.getName());
        resp.put("email", user.getEmail());
        resp.put("role", user.getRole().name());
        resp.put("phone", user.getPhone() != null ? user.getPhone() : "");
        resp.put("customAvatar", user.getCustomAvatar() != null ? user.getCustomAvatar() : "");
        resp.put("dashboard", user.getDashboardInfo());
        resp.put("permissions", user.getPermissions());

        if (user instanceof Patient) {
            Patient p = (Patient) user;
            resp.put("dob", p.getDateOfBirth() != null ? p.getDateOfBirth() : "");
            resp.put("gender", p.getGender() != null ? p.getGender() : "");
            resp.put("bloodType", p.getBloodType() != null ? p.getBloodType() : "");
            resp.put("allergies", p.getAllergies() != null ? p.getAllergies() : "");
            resp.put("chronicConditions", p.getChronicConditions() != null ? p.getChronicConditions() : "");
            resp.put("emergencyContact", p.getEmergencyContact() != null ? p.getEmergencyContact() : "");
            resp.put("emergencyContactsJson", p.getEmergencyContactsJson() != null ? p.getEmergencyContactsJson() : "[]");
        }
        return resp;
    }
}
