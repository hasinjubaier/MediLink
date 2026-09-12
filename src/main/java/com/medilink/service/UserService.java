package com.medilink.service;

import com.medilink.model.user.*;
import com.medilink.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PharmacistRepository pharmacistRepository;
    private final AdminRepository adminRepository;
    private final EmailService emailService;

    // In-memory OTP store: email -> OtpRecord
    private final Map<String, OtpRecord> otpStore = new ConcurrentHashMap<>();

    private static class OtpRecord {
        String code;
        LocalDateTime expiresAt;

        OtpRecord(String code, LocalDateTime expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }
    }

    @Autowired
    public UserService(UserRepository userRepository,
                       PatientRepository patientRepository,
                       PharmacistRepository pharmacistRepository,
                       AdminRepository adminRepository) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.pharmacistRepository = pharmacistRepository;
        this.adminRepository = adminRepository;
        this.emailService = EmailService.getInstance();
    }

    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return userRepository.findByEmailIgnoreCase(email.trim());
    }

    public Optional<User> findById(String id) {
        if (id == null) return Optional.empty();
        return userRepository.findById(id);
    }

    public Optional<Patient> findPatientById(String id) {
        if (id == null) return Optional.empty();
        return patientRepository.findById(id);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User authenticate(String email, String password) {
        if (email == null || password == null) return null;
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email.trim());
        if (!userOpt.isPresent()) return null;

        User user = userOpt.get();
        // Plaintext match or hash check for project demo
        if (user.getPasswordHash().equals(password) ||
            user.getPasswordHash().equalsIgnoreCase(Integer.toHexString(password.hashCode()))) {
            return user;
        }
        return null;
    }

    public User registerUser(UserRole role, String name, String email, String password, Map<String, String> extra) {
        if (email == null || userRepository.existsByEmailIgnoreCase(email.trim())) {
            throw new IllegalArgumentException("User with email " + email + " already exists.");
        }

        User newUser = UserFactory.createUser(role, name, email.trim().toLowerCase(), password, extra);
        if (newUser instanceof Patient) {
            return patientRepository.save((Patient) newUser);
        } else if (newUser instanceof Pharmacist) {
            return pharmacistRepository.save((Pharmacist) newUser);
        } else if (newUser instanceof Admin) {
            return adminRepository.save((Admin) newUser);
        } else {
            return userRepository.save(newUser);
        }
    }

    public static class OtpDispatchResult {
        private final String code;
        private final boolean liveEmailSent;
        private final String message;

        public OtpDispatchResult(String code, boolean liveEmailSent, String message) {
            this.code = code;
            this.liveEmailSent = liveEmailSent;
            this.message = message;
        }

        public String getCode() { return code; }
        public boolean isLiveEmailSent() { return liveEmailSent; }
        public String getMessage() { return message; }
    }

    public OtpDispatchResult generateAndSendOtpDetails(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required.");
        }
        String cleanEmail = email.trim().toLowerCase();
        int codeNum = 100000 + new Random().nextInt(900000);
        String code = String.valueOf(codeNum);
        otpStore.put(cleanEmail, new OtpRecord(code, LocalDateTime.now().plusMinutes(5)));

        boolean liveSent = false;
        String message;

        if (emailService.isConfigured()) {
            try {
                emailService.sendOtpEmail(cleanEmail, code);
                liveSent = true;
                message = "Live verification code sent to " + cleanEmail;
            } catch (Exception e) {
                System.err.println("[UserService] Failed to send live email via SMTP: " + e.getMessage());
                message = "SMTP delivery error (" + e.getMessage() + "). Demo code generated for testing.";
            }
        } else {
            message = "Live Gmail SMTP not configured in medilink_config.properties. Demo code generated for testing.";
            System.out.println("[UserService] Live Gmail SMTP not configured. Simulating OTP code: " + code);
        }

        return new OtpDispatchResult(code, liveSent, message);
    }

    public String generateAndSendOtp(String email) {
        return generateAndSendOtpDetails(email).getCode();
    }

    public boolean verifyOtp(String email, String inputOtp) {
        return verifyOtp(email, inputOtp, true);
    }

    public boolean verifyOtp(String email, String inputOtp, boolean consumeOnSuccess) {
        if (email == null || inputOtp == null) return false;
        String cleanEmail = email.trim().toLowerCase();
        OtpRecord record = otpStore.get(cleanEmail);
        if (record == null) return false;
        if (record.expiresAt.isBefore(LocalDateTime.now())) {
            otpStore.remove(cleanEmail);
            return false;
        }
        boolean match = record.code.equals(inputOtp.trim());
        if (match && consumeOnSuccess) {
            otpStore.remove(cleanEmail);
        }
        return match;
    }

    public boolean resetPassword(String email, String otp, String newPassword) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (otp == null || otp.trim().isEmpty()) {
            throw new IllegalArgumentException("OTP verification code is required.");
        }
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters long.");
        }

        String cleanEmail = email.trim().toLowerCase();
        boolean validOtp = verifyOtp(cleanEmail, otp.trim(), true);
        if (!validOtp) {
            throw new IllegalArgumentException("Invalid or expired OTP verification code.");
        }

        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(cleanEmail);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("User with email " + cleanEmail + " not found.");
        }

        User user = userOpt.get();
        user.setPasswordHash(newPassword.trim());
        userRepository.save(user);
        return true;
    }

    public User updateProfile(String id, Map<String, Object> updateData) {
        if ((id == null || id.trim().isEmpty()) && (!updateData.containsKey("email") || updateData.get("email") == null)) {
            throw new IllegalArgumentException("User ID or Email is required for update.");
        }

        Optional<User> userOpt = (id != null && !id.trim().isEmpty()) ? userRepository.findById(id.trim()) : Optional.empty();
        if (!userOpt.isPresent() && updateData.containsKey("email")) {
            String em = (String) updateData.get("email");
            if (em != null) {
                userOpt = userRepository.findByEmailIgnoreCase(em.trim());
            }
        }

        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("User not found with ID/Email: " + (id != null ? id : updateData.get("email")));
        }

        User user = userOpt.get();

        if (updateData.containsKey("name")) {
            String name = (String) updateData.get("name");
            if (name != null && !name.trim().isEmpty()) {
                user.setName(name.trim());
            }
        }

        if (updateData.containsKey("email")) {
            String newEmail = ((String) updateData.get("email")).trim().toLowerCase();
            if (!newEmail.isEmpty() && !newEmail.equalsIgnoreCase(user.getEmail())) {
                if (userRepository.existsByEmailIgnoreCase(newEmail)) {
                    throw new IllegalArgumentException("Email " + newEmail + " is already in use by another account.");
                }
                user.setEmail(newEmail);
            }
        }

        if (updateData.containsKey("phone")) {
            user.setPhone((String) updateData.get("phone"));
        }

        if (updateData.containsKey("customAvatar")) {
            user.setCustomAvatar((String) updateData.get("customAvatar"));
        }

        if (user instanceof Patient) {
            Patient patient = (Patient) user;
            if (updateData.containsKey("dob")) {
                patient.setDateOfBirth((String) updateData.get("dob"));
            }
            if (updateData.containsKey("dateOfBirth")) {
                patient.setDateOfBirth((String) updateData.get("dateOfBirth"));
            }
            if (updateData.containsKey("gender")) {
                patient.setGender((String) updateData.get("gender"));
            }
            if (updateData.containsKey("bloodType")) {
                patient.setBloodType((String) updateData.get("bloodType"));
            }
            if (updateData.containsKey("allergies")) {
                patient.setAllergies((String) updateData.get("allergies"));
            }
            if (updateData.containsKey("chronicConditions")) {
                patient.setChronicConditions((String) updateData.get("chronicConditions"));
            }
            if (updateData.containsKey("emergencyContact")) {
                patient.setEmergencyContact((String) updateData.get("emergencyContact"));
            }
            if (updateData.containsKey("address")) {
                patient.setAddress((String) updateData.get("address"));
            }
            if (updateData.containsKey("emergencyContactsJson")) {
                patient.setEmergencyContactsJson((String) updateData.get("emergencyContactsJson"));
            } else if (updateData.containsKey("emergencyContacts")) {
                Object ec = updateData.get("emergencyContacts");
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    patient.setEmergencyContactsJson(mapper.writeValueAsString(ec));
                } catch (Exception ignored) {}
            }
            return patientRepository.save(patient);
        } else if (user instanceof Pharmacist) {
            Pharmacist pharmacist = (Pharmacist) user;
            if (updateData.containsKey("pharmacyId")) {
                pharmacist.setPharmacyId((String) updateData.get("pharmacyId"));
            }
            if (updateData.containsKey("pharmacyName")) {
                pharmacist.setPharmacyName((String) updateData.get("pharmacyName"));
            }
            if (updateData.containsKey("licenseNumber")) {
                pharmacist.setLicenseNumber((String) updateData.get("licenseNumber"));
            }
            return pharmacistRepository.save(pharmacist);
        } else if (user instanceof Admin) {
            Admin admin = (Admin) user;
            if (updateData.containsKey("accessLevel")) {
                Object lvl = updateData.get("accessLevel");
                if (lvl instanceof Number) admin.setAccessLevel(((Number) lvl).intValue());
                else if (lvl instanceof String) {
                    try { admin.setAccessLevel(Integer.parseInt((String) lvl)); } catch (Exception ignored) {}
                }
            }
            return adminRepository.save(admin);
        } else {
            return userRepository.save(user);
        }
    }

    public User adminUpdateUser(String id, Map<String, Object> updateData) {
        User user = updateProfile(id, updateData);
        if (updateData.containsKey("password") && updateData.get("password") != null) {
            String newPass = ((String) updateData.get("password")).trim();
            if (!newPass.isEmpty()) {
                user.setPasswordHash(newPass);
                return userRepository.save(user);
            }
        }
        return user;
    }

    public boolean deleteUser(String id) {
        if (id == null || id.trim().isEmpty()) return false;
        Optional<User> userOpt = userRepository.findById(id.trim());
        if (!userOpt.isPresent()) return false;
        userRepository.deleteById(id.trim());
        return true;
    }
}
