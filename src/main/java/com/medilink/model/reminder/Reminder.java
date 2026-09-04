package com.medilink.model.reminder;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Domain model for medicine reminders with background multithread scheduling.
 * Mapped as a JPA @Entity for PostgreSQL persistence.
 */
@Entity
@Table(name = "reminders")
public class Reminder {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "patient_id", length = 50)
    private String patientId;

    @Column(name = "patient_email", length = 100, nullable = false)
    private String patientEmail;

    @Column(name = "medicine_name", length = 100, nullable = false)
    private String medicineName;

    @Column(name = "dosage", length = 50)
    private String dosage;

    @Column(name = "reminder_time", length = 20, nullable = false)
    private String reminderTime; // e.g. "08:00", "14:00", "20:00"

    @Column(name = "days_of_week", length = 100)
    private String frequency;    // DAILY, TWICE_DAILY, WEEKLY

    @Column(name = "instructions", length = 255)
    private String instructions;

    @Column(name = "is_active")
    private boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Reminder() {
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    public Reminder(String id, String patientId, String patientEmail, String medicineName,
                    String dosage, String reminderTime, String frequency, String instructions) {
        this.id = id;
        this.patientId = patientId;
        this.patientEmail = patientEmail;
        this.medicineName = medicineName;
        this.dosage = dosage;
        this.reminderTime = reminderTime;
        this.frequency = frequency;
        this.instructions = instructions;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getPatientEmail() { return patientEmail; }
    public void setPatientEmail(String patientEmail) { this.patientEmail = patientEmail; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public String getReminderTime() { return reminderTime; }
    public void setReminderTime(String reminderTime) { this.reminderTime = reminderTime; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
