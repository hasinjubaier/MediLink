package com.medilink.service;

import com.medilink.model.prescription.Prescription;
import com.medilink.model.prescription.PrescriptionItem;
import com.medilink.model.reminder.Reminder;
import com.medilink.repository.PrescriptionItemRepository;
import com.medilink.repository.PrescriptionRepository;
import com.medilink.repository.ReminderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional
public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final StockObserverService observerService;
    private final Set<String> dispatchedAlarmsToday = Collections.synchronizedSet(new HashSet<>());

    @Autowired
    public ReminderService(ReminderRepository reminderRepository,
                           PrescriptionRepository prescriptionRepository,
                           PrescriptionItemRepository prescriptionItemRepository) {
        this.reminderRepository = reminderRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.prescriptionItemRepository = prescriptionItemRepository;
        this.observerService = StockObserverService.getInstance();
    }

    public List<Reminder> findForPatient(String patientId, String email) {
        if (patientId != null && !patientId.trim().isEmpty() && email != null && !email.trim().isEmpty()) {
            return reminderRepository.findByPatientIdOrPatientEmailIgnoreCaseOrderByReminderTimeAsc(patientId.trim(), email.trim());
        } else if (email != null && !email.trim().isEmpty()) {
            return reminderRepository.findByPatientEmailIgnoreCaseOrderByReminderTimeAsc(email.trim());
        } else if (patientId != null && !patientId.trim().isEmpty()) {
            return reminderRepository.findByPatientIdOrPatientEmailIgnoreCaseOrderByReminderTimeAsc(patientId.trim(), patientId.trim());
        }
        return reminderRepository.findAllByOrderByReminderTimeAsc();
    }

    public List<Reminder> findByEmail(String email) {
        if (email == null) return Collections.emptyList();
        return reminderRepository.findByPatientEmailIgnoreCaseOrderByReminderTimeAsc(email.trim());
    }

    public List<Reminder> findAllActive() {
        return reminderRepository.findByActiveTrue();
    }

    public Reminder createReminder(String patientId, String email, String medicineName,
                                   String dosage, String reminderTime, String frequency,
                                   String instructions, String mealTiming, String category) {
        String id = "rem_" + System.currentTimeMillis() + "_" + ((int) (Math.random() * 900) + 100);
        Reminder r = new Reminder(id, patientId, email.trim().toLowerCase(), medicineName, dosage, reminderTime, frequency, instructions);
        if (mealTiming != null && !mealTiming.trim().isEmpty()) {
            r.setMealTiming(mealTiming.trim());
        }
        if (category != null && !category.trim().isEmpty()) {
            r.setCategory(category.trim());
        }
        return reminderRepository.save(r);
    }

    public Reminder createReminder(String patientId, String email, String medicineName,
                                   String dosage, String reminderTime, String frequency, String instructions) {
        return createReminder(patientId, email, medicineName, dosage, reminderTime, frequency, instructions, "AFTER_MEAL", "MEDICATION");
    }

    public boolean toggleActive(String id) {
        Optional<Reminder> opt = reminderRepository.findById(id);
        if (!opt.isPresent()) return false;
        Reminder r = opt.get();
        r.setActive(!r.isActive());
        reminderRepository.save(r);
        return true;
    }

    public boolean markDoseTaken(String id) {
        Optional<Reminder> opt = reminderRepository.findById(id);
        if (!opt.isPresent()) return false;
        Reminder r = opt.get();
        String today = LocalDate.now().toString();
        // If already taken today, toggle back to pending; otherwise mark taken today
        if (today.equals(r.getLastTakenDate())) {
            r.setLastTakenDate(null);
        } else {
            r.setLastTakenDate(today);
        }
        reminderRepository.save(r);
        return true;
    }

    public boolean deleteReminder(String id) {
        if (!reminderRepository.existsById(id)) return false;
        reminderRepository.deleteById(id);
        return true;
    }

    public int syncFromPrescriptions(String patientId, String email) {
        String pId = (patientId != null && !patientId.trim().isEmpty()) ? patientId.trim() : "ML-9824-A";
        String pEmail = (email != null && !email.trim().isEmpty()) ? email.trim() : "rahim@medilink.com";

        List<Prescription> prescriptions = prescriptionRepository.findByPatientIdOrderByUploadedAtDesc(pId);
        if (prescriptions.isEmpty()) {
            prescriptions = prescriptionRepository.findAllByOrderByUploadedAtDesc();
        }
        if (prescriptions.isEmpty()) return 0;

        List<Reminder> existing = findForPatient(pId, pEmail);
        Set<String> existingKeys = new HashSet<>();
        for (Reminder ex : existing) {
            existingKeys.add(ex.getMedicineName().toLowerCase() + "_" + ex.getReminderTime());
        }

        int countAdded = 0;
        // Check medicines from the prescriptions
        for (Prescription rx : prescriptions) {
            List<PrescriptionItem> items = prescriptionItemRepository.findByPrescriptionId(rx.getId());
            for (PrescriptionItem item : items) {
                String medName = item.getMedicineName();
                String schedule = (item.getFrequency() != null) ? item.getFrequency().trim() : "1+0+1";
                String instr = (item.getInstructions() != null && !item.getInstructions().trim().isEmpty())
                        ? item.getInstructions()
                        : "Take with water as directed";
                String dose = (item.getDosage() != null && !item.getDosage().trim().isEmpty()) ? item.getDosage() : "1 Unit";

                // Parse standard schedules: "1+1+1" (morning, afternoon, night), "1+0+1" (morning, night), etc.
                List<String[]> slots = new ArrayList<>();
                if (schedule.contains("1+1+1") || schedule.equalsIgnoreCase("TID")) {
                    slots.add(new String[]{"08:00", "Morning Dose", "AFTER_MEAL"});
                    slots.add(new String[]{"14:00", "Afternoon Dose", "AFTER_MEAL"});
                    slots.add(new String[]{"20:30", "Night Dose", "AFTER_MEAL"});
                } else if (schedule.contains("1+0+1") || schedule.equalsIgnoreCase("BID")) {
                    slots.add(new String[]{"08:00", "Morning Dose", "AFTER_MEAL"});
                    slots.add(new String[]{"20:30", "Night Dose", "AFTER_MEAL"});
                } else if (schedule.contains("0+0+1") || schedule.toLowerCase().contains("night")) {
                    slots.add(new String[]{"21:00", "Bedtime Dose", "BEDTIME"});
                } else if (schedule.contains("1+0+0") || schedule.toLowerCase().contains("morning")) {
                    slots.add(new String[]{"08:00", "Morning Dose", "BEFORE_MEAL"});
                } else {
                    slots.add(new String[]{"09:00", "Daily Dose", "AFTER_MEAL"});
                }

                for (String[] slot : slots) {
                    String time = slot[0];
                    String key = medName.toLowerCase() + "_" + time;
                    if (!existingKeys.contains(key)) {
                        createReminder(pId, pEmail, medName, "1 Unit (" + slot[1] + ")",
                                time, "DAILY", instr, slot[2], "MEDICATION");
                        existingKeys.add(key);
                        countAdded++;
                    }
                }
            }
        }
        return countAdded;
    }

    @Scheduled(fixedRate = 10000)
    public void checkAndDispatchReminders() {
        List<Reminder> reminders = reminderRepository.findByActiveTrue();
        String currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        String today = LocalDate.now().toString();

        for (Reminder r : reminders) {
            String alarmKey = r.getPatientEmail() + "_" + r.getReminderTime() + "_" + today;
            if (currentTime.equals(r.getReminderTime()) && !dispatchedAlarmsToday.contains(alarmKey)) {
                dispatchedAlarmsToday.add(alarmKey);
                String alert = "MEDICINE ALARM [" + r.getReminderTime() + "]: Time to take " +
                               r.getMedicineName() + " (" + r.getDosage() + ") - " + r.getInstructions() +
                               " for Patient " + r.getPatientEmail();
                observerService.onNotification("REMINDER_ALARM", alert);
            }
        }
    }
}
