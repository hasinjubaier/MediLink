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

    public String generateAndSendOtp(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required.");
        }
        String cleanEmail = email.trim().toLowerCase();
        int codeNum = 100000 + new Random().nextInt(900000);
        String code = String.valueOf(codeNum);
        otpStore.put(cleanEmail, new OtpRecord(code, LocalDateTime.now().plusMinutes(5)));

        // Try to send via EmailService if configured
        try {
            if (emailService.isConfigured()) {
                emailService.sendOtpEmail(cleanEmail, code);
            }
        } catch (Exception e) {
            System.err.println("[UserService] Could not send live email: " + e.getMessage());
        }
        return code;
    }

    public boolean verifyOtp(String email, String inputOtp) {
        if (email == null || inputOtp == null) return false;
        String cleanEmail = email.trim().toLowerCase();
        OtpRecord record = otpStore.get(cleanEmail);
        if (record == null) return false;
        if (record.expiresAt.isBefore(LocalDateTime.now())) {
            otpStore.remove(cleanEmail);
            return false;
        }
        boolean match = record.code.equals(inputOtp.trim());
        if (match) {
            otpStore.remove(cleanEmail);
        }
        return match;
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
        } else {
            return userRepository.save(user);
        }
    }
}
