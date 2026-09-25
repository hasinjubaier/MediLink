package com.medilink.controller;

import com.medilink.model.user.User;
import com.medilink.model.user.UserRole;
import com.medilink.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");

        Optional<User> userOpt = userService.findByEmail(email);
        if (!userOpt.isPresent()) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "ERROR");
            resp.put("code", "USER_NOT_FOUND");
            resp.put("message", "Email does not exist! Please Sign Up first.");
            resp.put("email", email != null ? email : "");
            return ResponseEntity.ok(resp);
        }

        User user = userService.authenticate(email, password);
        if (user == null) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "ERROR");
            resp.put("code", "INVALID_PASSWORD");
            resp.put("message", "Incorrect password! Please check your credentials or reset password.");
            return ResponseEntity.ok(resp);
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("id", user.getId());
        resp.put("name", user.getName());
        resp.put("email", user.getEmail());
        resp.put("role", user.getRole().name());
        resp.put("phone", user.getPhone() != null ? user.getPhone() : "");
        resp.put("customAvatar", user.getCustomAvatar() != null ? user.getCustomAvatar() : "");
        resp.put("dashboard", user.getDashboardInfo());
        resp.put("permissions", user.getPermissions());

        if (user instanceof com.medilink.model.user.Patient) {
            com.medilink.model.user.Patient p = (com.medilink.model.user.Patient) user;
            resp.put("dob", p.getDateOfBirth() != null ? p.getDateOfBirth() : "");
            resp.put("gender", p.getGender() != null ? p.getGender() : "");
            resp.put("bloodType", p.getBloodType() != null ? p.getBloodType() : "");
            resp.put("allergies", p.getAllergies() != null ? p.getAllergies() : "");
            resp.put("chronicConditions", p.getChronicConditions() != null ? p.getChronicConditions() : "");
            resp.put("emergencyContact", p.getEmergencyContact() != null ? p.getEmergencyContact() : "");
            resp.put("emergencyContactsJson", p.getEmergencyContactsJson() != null ? p.getEmergencyContactsJson() : "[]");
        }
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String name = body.getOrDefault("name", "New User");
        String email = body.getOrDefault("email", "");
        String password = body.getOrDefault("password", "123456");
        String roleStr = body.getOrDefault("role", "PATIENT");

        try {
            UserRole role = UserRole.valueOf(roleStr.toUpperCase());
            if (role == UserRole.ADMIN) {
                Map<String, Object> errResp = new HashMap<>();
                errResp.put("status", "ERROR");
                errResp.put("message", "Administrator accounts cannot be self-registered publicly. Contact system administration.");
                return ResponseEntity.status(403).body(errResp);
            }
            User newUser = userService.registerUser(role, name, email, password, body);
            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "SUCCESS");
            resp.put("message", "Registered successfully as " + role);
            resp.put("id", newUser.getId());
            resp.put("name", newUser.getName());
            resp.put("email", newUser.getEmail());
            resp.put("role", newUser.getRole().name());
            resp.put("phone", newUser.getPhone() != null ? newUser.getPhone() : "");
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "ERROR");
            resp.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }

    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, Object>> sendOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String purpose = body.get("purpose");
        try {
            if (email == null || email.trim().isEmpty()) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("status", "ERROR");
                resp.put("message", "Email address is required.");
                return ResponseEntity.badRequest().body(resp);
            }

            if ("RESET_PASSWORD".equalsIgnoreCase(purpose)) {
                if (!userService.findByEmail(email).isPresent()) {
                    Map<String, Object> resp = new HashMap<>();
                    resp.put("status", "ERROR");
                    resp.put("message", "No account registered with " + email + ". Please check your email or Sign Up.");
                    return ResponseEntity.badRequest().body(resp);
                }
            }

            UserService.OtpDispatchResult result = userService.generateAndSendOtpDetails(email);
            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "SUCCESS");
            resp.put("message", result.getMessage());
            resp.put("liveEmailSent", result.isLiveEmailSent());
            resp.put("otp", result.getCode()); // Available for convenient testing and fallback
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "ERROR");
            resp.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp = body.get("otp");
        boolean valid = userService.verifyOtp(email, otp, false);

        Map<String, Object> resp = new HashMap<>();
        if (valid) {
            resp.put("status", "SUCCESS");
            resp.put("message", "OTP verified successfully");
        } else {
            resp.put("status", "ERROR");
            resp.put("message", "Invalid or expired OTP");
        }
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp = body.get("otp");
        String newPassword = body.get("newPassword");

        try {
            userService.resetPassword(email, otp, newPassword);
            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "SUCCESS");
            resp.put("message", "Password has been reset successfully. You may now log in with your new password.");
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "ERROR");
            resp.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }
}
