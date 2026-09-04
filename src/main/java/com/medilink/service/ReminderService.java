package com.medilink.service;

import com.medilink.model.reminder.Reminder;
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
    private final StockObserverService observerService;
    private final Set<String> dispatchedAlarmsToday = Collections.synchronizedSet(new HashSet<>());

    @Autowired
    public ReminderService(ReminderRepository reminderRepository) {
        this.reminderRepository = reminderRepository;
        this.observerService = StockObserverService.getInstance();
    }

    public List<Reminder> findByEmail(String email) {
        if (email == null) return Collections.emptyList();
        return reminderRepository.findByPatientEmailIgnoreCaseAndActiveTrue(email.trim());
    }

    public List<Reminder> findAllActive() {
        return reminderRepository.findByActiveTrue();
    }

    public Reminder createReminder(String patientId, String email, String medicineName,
                                   String dosage, String reminderTime, String frequency, String instructions) {
        String id = "rem_" + System.currentTimeMillis();
        Reminder r = new Reminder(id, patientId, email.trim().toLowerCase(), medicineName, dosage, reminderTime, frequency, instructions);
        return reminderRepository.save(r);
    }

    public boolean deleteReminder(String id) {
        if (!reminderRepository.existsById(id)) return false;
        reminderRepository.deleteById(id);
        return true;
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
