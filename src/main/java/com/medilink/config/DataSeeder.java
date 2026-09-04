package com.medilink.config;

import com.medilink.model.chat.ChatMessage;
import com.medilink.model.medicine.Medicine;
import com.medilink.model.pharmacy.Pharmacy;
import com.medilink.model.pharmacy.PharmacyStock;
import com.medilink.model.prescription.Prescription;
import com.medilink.model.prescription.PrescriptionItem;
import com.medilink.model.reminder.Reminder;
import com.medilink.model.user.*;
import com.medilink.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PharmacistRepository pharmacistRepository;
    private final AdminRepository adminRepository;
    private final MedicineRepository medicineRepository;
    private final PharmacyRepository pharmacyRepository;
    private final PharmacyStockRepository pharmacyStockRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ReminderRepository reminderRepository;

    @Autowired
    public DataSeeder(UserRepository userRepository,
                      PatientRepository patientRepository,
                      PharmacistRepository pharmacistRepository,
                      AdminRepository adminRepository,
                      MedicineRepository medicineRepository,
                      PharmacyRepository pharmacyRepository,
                      PharmacyStockRepository pharmacyStockRepository,
                      PrescriptionRepository prescriptionRepository,
                      ChatMessageRepository chatMessageRepository,
                      ReminderRepository reminderRepository) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.pharmacistRepository = pharmacistRepository;
        this.adminRepository = adminRepository;
        this.medicineRepository = medicineRepository;
        this.pharmacyRepository = pharmacyRepository;
        this.pharmacyStockRepository = pharmacyStockRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.reminderRepository = reminderRepository;
    }

    @Override
    public void run(String... args) {
        seedUsers();
        seedMedicines();
        seedPharmacies();
        seedStocks();
        seedPrescriptions();
        seedChatMessages();
        seedReminders();
    }

    private void seedUsers() {
        if (userRepository.count() == 0) {
            Map<String, String> pData = new HashMap<>();
            pData.put("phone", "+8801711223344");
            pData.put("address", "House 12, Road 5, Dhanmondi, Dhaka");
            pData.put("emergencyContact", "+8801988776655");
            Patient patient = (Patient) UserFactory.createUserWithId("ML-9824-A", UserRole.PATIENT, "Rahim Ahmed", "rahim@medilink.com", "patient123", pData);
            patientRepository.save(patient);

            Map<String, String> phData = new HashMap<>();
            phData.put("pharmacyId", "pharma_01");
            phData.put("pharmacyName", "Lazz Pharma (Dhanmondi)");
            phData.put("licenseNumber", "DGDA-PH-99201");
            Pharmacist pharmacist = (Pharmacist) UserFactory.createUserWithId("usr_pharma_01", UserRole.PHARMACIST, "Dr. Farhan Kabir", "farhan@lazzpharma.com", "pharma123", phData);
            pharmacistRepository.save(pharmacist);

            Map<String, String> admData = new HashMap<>();
            admData.put("accessLevel", "3");
            Admin admin = (Admin) UserFactory.createUserWithId("usr_admin_01", UserRole.ADMIN, "System Administrator", "admin@medilink.com", "admin123", admData);
            adminRepository.save(admin);

            System.out.println("[DataSeeder] Seeded default polymorphic users (Patient, Pharmacist, Admin).");
        }
    }

    private void seedMedicines() {
        if (medicineRepository.count() == 0) {
            addMed("med_01", "Napa Extra", "Paracetamol + Caffeine", "Beximco Pharmaceuticals", "500mg+65mg", "Tablet", 3.00, false, "Analgesic / Antipyretic", "Mild insomnia if taken late at night", Arrays.asList("BEX-2026-A1", "BEX-2026-A2", "BATCH-NAPA-99"));
            addMed("med_02", "Ace Plus", "Paracetamol + Caffeine", "Square Pharmaceuticals", "500mg+65mg", "Tablet", 2.80, false, "Analgesic / Antipyretic", "Minimal side effects", Arrays.asList("SQR-ACE-101", "SQR-ACE-102"));
            addMed("med_03", "Reset", "Paracetamol", "Incepta Pharmaceuticals", "500mg", "Tablet", 1.50, false, "Analgesic / Antipyretic", "Nausea at high doses", Arrays.asList("INC-RES-550"));
            addMed("med_04", "Fast", "Paracetamol", "Acme Laboratories", "500mg", "Tablet", 1.40, false, "Analgesic / Antipyretic", "Safe at normal dosage", Arrays.asList("ACM-FST-881"));
            addMed("med_05", "Seclo 20", "Omeprazole", "Square Pharmaceuticals", "20mg", "Capsule", 6.00, false, "Anti-ulcerant / PPI", "Headache, mild dizziness", Arrays.asList("SQR-SEC-201", "SQR-SEC-202"));
            addMed("med_06", "Maxpro 20", "Esomeprazole", "Renata Limited", "20mg", "Tablet", 8.00, false, "Anti-ulcerant / PPI", "Dry mouth, abdominal pain", Arrays.asList("REN-MAX-901", "REN-MAX-902"));
            addMed("med_07", "Sergel 20", "Esomeprazole", "Healthcare Pharmaceuticals", "20mg", "Capsule", 7.50, false, "Anti-ulcerant / PPI", "Constipation or diarrhea", Arrays.asList("HCL-SRG-404"));
            addMed("med_08", "Losectil 20", "Omeprazole", "SK-F (Eskayef)", "20mg", "Capsule", 5.50, false, "Anti-ulcerant / PPI", "Stomach cramps", Arrays.asList("SKF-LOS-303"));
            addMed("med_09", "Azithrocin 500", "Azithromycin", "Square Pharmaceuticals", "500mg", "Tablet", 35.00, true, "Antibiotic", "Gastrointestinal discomfort, nausea", Arrays.asList("SQR-AZI-771"));
            addMed("med_10", "Zimax 500", "Azithromycin", "Beximco Pharmaceuticals", "500mg", "Tablet", 36.00, true, "Antibiotic", "Mild headache, stomach ache", Arrays.asList("BEX-ZIM-662"));
            addMed("med_11", "Ciprocin 500", "Ciprofloxacin", "Square Pharmaceuticals", "500mg", "Tablet", 16.00, true, "Antibiotic", "Tendon pain, sun sensitivity", Arrays.asList("SQR-CIP-501"));
            addMed("med_12", "Monas 10", "Montelukast", "Acme Laboratories", "10mg", "Tablet", 18.00, false, "Respiratory / Anti-Asthma", "Drowsiness, upper respiratory infection", Arrays.asList("ACM-MON-101", "ACM-MON-102"));
            addMed("med_13", "Odmon 10", "Montelukast", "Square Pharmaceuticals", "10mg", "Tablet", 17.50, false, "Respiratory / Anti-Asthma", "Sleep disturbances", Arrays.asList("SQR-ODM-331"));
            addMed("med_14", "Fexo 120", "Fexofenadine", "Square Pharmaceuticals", "120mg", "Tablet", 9.00, false, "Antihistamine", "Drowsiness (rare)", Arrays.asList("SQR-FEX-121"));

            System.out.println("[DataSeeder] Seeded 14 default medicines with authentic DGDA batch codes.");
        }
    }

    private void addMed(String id, String brand, String generic, String company, String strength,
                        String form, double price, boolean rx, String cat, String sideEffects, List<String> batches) {
        Medicine m = new Medicine(id, brand, generic, company, strength, form, price, rx, cat, sideEffects);
        for (String b : batches) {
            m.addVerifiedBatch(b);
        }
        medicineRepository.save(m);
    }

    private void seedPharmacies() {
        if (pharmacyRepository.count() == 0) {
            pharmacyRepository.save(new Pharmacy("pharma_01", "Lazz Pharma (Dhanmondi)", "Road 7, Dhanmondi, Dhaka", "Dhanmondi", "+8801711001122", 23.7465, 90.3760, true, true));
            pharmacyRepository.save(new Pharmacy("pharma_02", "Tamanna Pharmacy (Banani)", "Block C, Kemal Ataturk Ave, Banani", "Banani", "+8801722334455", 23.7937, 90.4066, true, true));
            pharmacyRepository.save(new Pharmacy("pharma_03", "Al-Madina Medicine Corner", "Section 10, Mirpur, Dhaka", "Mirpur", "+8801733445566", 23.8071, 90.3686, false, true));
            pharmacyRepository.save(new Pharmacy("pharma_04", "Popular Pharmacy (Gulshan)", "Gulshan 1 Circle, Dhaka", "Gulshan", "+8801744556677", 23.7786, 90.4162, true, true));
            pharmacyRepository.save(new Pharmacy("pharma_05", "Care & Cure Pharmacy (Uttara)", "Sector 3, Uttara, Dhaka", "Uttara", "+8801755667788", 23.8728, 90.3980, true, true));

            System.out.println("[DataSeeder] Seeded 5 Dhaka pharmacies.");
        }
    }

    private void seedStocks() {
        if (pharmacyStockRepository.count() == 0) {
            pharmacyStockRepository.save(new PharmacyStock("stk_01", "pharma_01", "Lazz Pharma (Dhanmondi)", "med_01", "Napa Extra", "Paracetamol + Caffeine", 140, 3.00));
            pharmacyStockRepository.save(new PharmacyStock("stk_08", "pharma_04", "Popular Pharmacy (Gulshan)", "med_01", "Napa Extra", "Paracetamol + Caffeine", 200, 3.20));
            pharmacyStockRepository.save(new PharmacyStock("stk_10", "pharma_05", "Care & Cure Pharmacy (Uttara)", "med_01", "Napa Extra", "Paracetamol + Caffeine", 75, 3.10));
            pharmacyStockRepository.save(new PharmacyStock("stk_11", "pharma_02", "Tamanna Pharmacy (Banani)", "med_01", "Napa Extra", "Paracetamol + Caffeine", 95, 2.80));
            pharmacyStockRepository.save(new PharmacyStock("stk_12", "pharma_03", "Al-Madina Medicine Corner", "med_01", "Napa Extra", "Paracetamol + Caffeine", 50, 2.90));

            pharmacyStockRepository.save(new PharmacyStock("stk_02", "pharma_01", "Lazz Pharma (Dhanmondi)", "med_05", "Seclo 20", "Omeprazole", 85, 6.00));
            pharmacyStockRepository.save(new PharmacyStock("stk_13", "pharma_02", "Tamanna Pharmacy (Banani)", "med_05", "Seclo 20", "Omeprazole", 40, 5.80));
            pharmacyStockRepository.save(new PharmacyStock("stk_14", "pharma_03", "Al-Madina Medicine Corner", "med_05", "Seclo 20", "Omeprazole", 65, 5.70));
            pharmacyStockRepository.save(new PharmacyStock("stk_15", "pharma_04", "Popular Pharmacy (Gulshan)", "med_05", "Seclo 20", "Omeprazole", 110, 6.20));

            pharmacyStockRepository.save(new PharmacyStock("stk_03", "pharma_01", "Lazz Pharma (Dhanmondi)", "med_06", "Maxpro 20", "Esomeprazole", 4, 8.00));
            pharmacyStockRepository.save(new PharmacyStock("stk_06", "pharma_02", "Tamanna Pharmacy (Banani)", "med_06", "Maxpro 20", "Esomeprazole", 60, 7.80));
            pharmacyStockRepository.save(new PharmacyStock("stk_16", "pharma_04", "Popular Pharmacy (Gulshan)", "med_06", "Maxpro 20", "Esomeprazole", 35, 8.20));

            pharmacyStockRepository.save(new PharmacyStock("stk_04", "pharma_01", "Lazz Pharma (Dhanmondi)", "med_09", "Azithrocin 500", "Azithromycin", 32, 35.00));
            pharmacyStockRepository.save(new PharmacyStock("stk_17", "pharma_02", "Tamanna Pharmacy (Banani)", "med_09", "Azithrocin 500", "Azithromycin", 20, 34.50));
            pharmacyStockRepository.save(new PharmacyStock("stk_18", "pharma_05", "Care & Cure Pharmacy (Uttara)", "med_09", "Azithrocin 500", "Azithromycin", 15, 36.00));

            pharmacyStockRepository.save(new PharmacyStock("stk_05", "pharma_02", "Tamanna Pharmacy (Banani)", "med_02", "Ace Plus", "Paracetamol + Caffeine", 90, 2.80));
            pharmacyStockRepository.save(new PharmacyStock("stk_19", "pharma_01", "Lazz Pharma (Dhanmondi)", "med_02", "Ace Plus", "Paracetamol + Caffeine", 80, 2.90));
            pharmacyStockRepository.save(new PharmacyStock("stk_07", "pharma_02", "Tamanna Pharmacy (Banani)", "med_11", "Ciprocin 500", "Ciprofloxacin", 45, 16.00));
            pharmacyStockRepository.save(new PharmacyStock("stk_20", "pharma_03", "Al-Madina Medicine Corner", "med_11", "Ciprocin 500", "Ciprofloxacin", 30, 15.50));
            pharmacyStockRepository.save(new PharmacyStock("stk_09", "pharma_04", "Popular Pharmacy (Gulshan)", "med_12", "Monas 10", "Montelukast", 55, 18.00));
            pharmacyStockRepository.save(new PharmacyStock("stk_21", "pharma_01", "Lazz Pharma (Dhanmondi)", "med_12", "Monas 10", "Montelukast", 40, 17.50));

            System.out.println("[DataSeeder] Seeded 21 cross-pharmacy stock records.");
        }
    }

    private void seedPrescriptions() {
        if (prescriptionRepository.count() == 0) {
            Prescription rx1 = new Prescription("rx_101", "ML-9824-A", "Rahim Ahmed", "Dr. A. K. Azad (FCPS)",
                    "Dhaka Medical College Hospital", "Rx: Tab Napa Extra 1+1+1 5 days, Cap Seclo 20mg 1+0+1 before meal 7 days, Tab Fexo 120 0+0+1 10 days.");
            rx1.addItem(new PrescriptionItem("med_01", "Napa Extra", "Paracetamol + Caffeine", "500mg+65mg", "1+1+1 (After meal)", "5 days", "For fever and pain"));
            rx1.addItem(new PrescriptionItem("med_05", "Seclo 20", "Omeprazole", "20mg", "1+0+1 (Before meal)", "7 days", "For gastric protection"));
            rx1.addItem(new PrescriptionItem("med_14", "Fexo 120", "Fexofenadine", "120mg", "0+0+1 (Night)", "10 days", "For allergic rhinitis"));
            rx1.advanceWorkflow();
            rx1.advanceWorkflow();
            rx1.setVerifiedByPharmacistId("usr_pharma_01");
            prescriptionRepository.save(rx1);

            System.out.println("[DataSeeder] Seeded verified prescription rx_101.");
        }
    }

    private void seedChatMessages() {
        if (chatMessageRepository.count() == 0) {
            chatMessageRepository.save(new ChatMessage("ML-9824-A", "Rahim Ahmed", "PATIENT", "usr_pharma_01", "Assalamu Alaikum. Is Maxpro 20 available for delivery in Dhanmondi?", "TEXT"));
            chatMessageRepository.save(new ChatMessage("usr_pharma_01", "Dr. Farhan Kabir", "PHARMACIST", "ML-9824-A", "Walaikum Assalam. Maxpro 20 has low stock (4 units), but we also have Sergel 20 (Esomeprazole) in full supply.", "TEXT"));

            System.out.println("[DataSeeder] Seeded initial chat messages.");
        }
    }

    private void seedReminders() {
        if (reminderRepository.count() == 0) {
            reminderRepository.save(new Reminder("rem_01", "ML-9824-A", "rahim@medilink.com", "Seclo 20", "1 Capsule (20mg)", "07:30", "DAILY", "Take 30 mins before breakfast"));
            reminderRepository.save(new Reminder("rem_02", "ML-9824-A", "rahim@medilink.com", "Napa Extra", "1 Tablet", "14:00", "DAILY", "Take with water after lunch if fever persists"));

            System.out.println("[DataSeeder] Seeded initial active reminders.");
        }
    }
}
