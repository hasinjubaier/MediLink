package com.medilink.repository;

import com.medilink.model.reminder.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, String> {
    List<Reminder> findByPatientEmailIgnoreCaseAndActiveTrue(String patientEmail);
    List<Reminder> findByActiveTrue();
}
