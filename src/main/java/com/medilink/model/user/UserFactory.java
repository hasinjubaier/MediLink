package com.medilink.model.user;

import java.util.Map;

/**
 * Factory class for instantiating polymorphic User objects.
 * Demonstrates the Factory Design Pattern.
 */
public class UserFactory {

    private static final java.util.Random RANDOM = new java.util.Random();

    public static User createUser(UserRole role, String name, String email, String passwordHash, Map<String, String> extraData) {
        String id;
        if (role == UserRole.PATIENT) {
            int num = 1000 + RANDOM.nextInt(9000);
            char letter = (char) ('A' + RANDOM.nextInt(26));
            id = "PA-" + num + "-" + letter;
        } else if (role == UserRole.PHARMACIST) {
            int num = 1000 + RANDOM.nextInt(9000);
            id = "PH-" + num + "-DGDA";
        } else {
            int num = 1000 + RANDOM.nextInt(9000);
            id = "ADM-" + num;
        }
        return createUserWithId(id, role, name, email, passwordHash, extraData);
    }

    public static User createUserWithId(String id, UserRole role, String name, String email, String passwordHash, Map<String, String> extraData) {
        if (role == null) {
            throw new IllegalArgumentException("UserRole cannot be null");
        }

        switch (role) {
            case PATIENT:
                String phone = null;
                if (extraData != null) {
                    phone = extraData.get("phone");
                    if (phone == null || phone.trim().isEmpty()) {
                        phone = extraData.get("extra");
                    }
                    if (phone == null || phone.trim().isEmpty()) {
                        phone = extraData.get("emergencyPhone");
                    }
                    if (phone == null || phone.trim().isEmpty()) {
                        phone = extraData.get("emergencyContact");
                    }
                }
                if (phone == null || phone.trim().isEmpty()) {
                    phone = "+8801700000000";
                }
                String address = extraData != null ? extraData.getOrDefault("address", "Dhaka, Bangladesh") : "Dhaka, Bangladesh";
                String emergency = extraData != null ? extraData.getOrDefault("emergencyContact", phone) : phone;
                return new Patient(id, name, email, passwordHash, phone.trim(), address, emergency.trim());

            case PHARMACIST:
                String pharmacyId = "pharma_01";
                String pharmacyName = "Lazz Pharma (Dhanmondi)";
                String license = "DGDA-2024-" + (id.length() > 4 ? id.substring(4) : "001");
                String pharmaPhone = "+8801711001122";

                if (extraData != null) {
                    if (extraData.containsKey("pharmacyId")) pharmacyId = extraData.get("pharmacyId");
                    if (extraData.containsKey("pharmacyName")) pharmacyName = extraData.get("pharmacyName");
                    if (extraData.containsKey("licenseNumber")) license = extraData.get("licenseNumber");
                    if (extraData.containsKey("phone")) pharmaPhone = extraData.get("phone");

                    // Handle extra text formatted as "Name | License" from frontend
                    String extra = extraData.get("extra");
                    if (extra != null && !extra.trim().isEmpty()) {
                        if (extra.contains("|")) {
                            String[] parts = extra.split("\\|", 2);
                            pharmacyName = parts[0].trim();
                            license = parts[1].trim();
                        } else {
                            pharmacyName = extra.trim();
                        }
                    }
                }
                Pharmacist pharmacist = new Pharmacist(id, name, email, passwordHash, pharmacyId, pharmacyName, license);
                pharmacist.setPhone(pharmaPhone);
                return pharmacist;

            case ADMIN:
                int accessLevel = 1;
                if (extraData != null && extraData.containsKey("accessLevel")) {
                    try {
                        accessLevel = Integer.parseInt(extraData.get("accessLevel"));
                    } catch (NumberFormatException ignored) {}
                }
                Admin admin = new Admin(id, name, email, passwordHash, accessLevel);
                if (extraData != null && extraData.containsKey("phone")) {
                    admin.setPhone(extraData.get("phone"));
                }
                return admin;

            default:
                throw new IllegalArgumentException("Unsupported UserRole: " + role);
        }
    }
}
