package com.medilink.service;

import com.medilink.model.prescription.Prescription;
import com.medilink.model.reminder.Reminder;
import com.medilink.model.user.*;
import com.medilink.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceAdminTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PharmacistRepository pharmacistRepository;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private ReminderRepository reminderRepository;

    private UserService userService;

    @BeforeEach
    public void setUp() {
        userService = new UserService(userRepository, patientRepository, pharmacistRepository, adminRepository);
        userService.setPrescriptionRepository(prescriptionRepository);
        userService.setReminderRepository(reminderRepository);
    }

    @Test
    @DisplayName("Admin can register a new administrator account")
    public void testRegisterAdminUser() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(adminRepository.save(any(Admin.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, String> extra = new HashMap<>();
        extra.put("accessLevel", "2");

        User admin = userService.registerUser(UserRole.ADMIN, "Admin User", "newadmin@medilink.com", "pass12345", extra);

        assertNotNull(admin);
        assertEquals(UserRole.ADMIN, admin.getRole());
        assertEquals("newadmin@medilink.com", admin.getEmail());
        assertEquals("ACTIVE", admin.getStatus());
        verify(adminRepository, times(1)).save(any(Admin.class));
    }

    @Test
    @DisplayName("Admin cannot delete their own active account")
    public void testSelfDeletePreventionThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            userService.deleteUser("ADM_01", "ADM_01");
        });
        assertTrue(ex.getMessage().contains("cannot delete their own active account"));
    }

    @Test
    @DisplayName("Deleting a user with prescriptions should perform safe deactivation (soft-delete)")
    public void testDeleteUserWithPrescriptionsPerformsSafeDeactivation() {
        String patientId = "PATIENT_WITH_RX";
        Patient patient = new Patient(patientId, "Tanvir", "tanvir@patient.com", "pass", "01712345678", "Dhaka", "01712345678");
        patient.setStatus("ACTIVE");

        when(userRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(prescriptionRepository.findByPatientIdOrderByUploadedAtDesc(patientId))
                .thenReturn(Collections.singletonList(new Prescription()));

        boolean result = userService.deleteUser(patientId, "ADM_99");

        assertTrue(result);
        assertEquals("DEACTIVATED", patient.getStatus());
        verify(userRepository, times(1)).save(patient);
        verify(userRepository, never()).deleteById(patientId);
    }

    @Test
    @DisplayName("Deleting a user with active medication reminders should perform safe deactivation")
    public void testDeleteUserWithRemindersPerformsSafeDeactivation() {
        String patientId = "PATIENT_WITH_REMINDERS";
        Patient patient = new Patient(patientId, "Sadia", "sadia@patient.com", "pass", "01787654321", "Chittagong", "01787654321");
        patient.setStatus("ACTIVE");

        Reminder reminder = new Reminder();
        reminder.setId("rem_1");
        reminder.setPatientId(patientId);
        reminder.setPatientEmail("sadia@patient.com");

        when(userRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(prescriptionRepository.findByPatientIdOrderByUploadedAtDesc(patientId)).thenReturn(Collections.emptyList());
        when(reminderRepository.findByPatientIdOrPatientEmailIgnoreCaseOrderByReminderTimeAsc(eq(patientId), eq("sadia@patient.com")))
                .thenReturn(Collections.singletonList(reminder));

        boolean result = userService.deleteUser(patientId, "ADM_99");

        assertTrue(result);
        assertEquals("DEACTIVATED", patient.getStatus());
        verify(userRepository, times(1)).save(patient);
        verify(userRepository, never()).deleteById(patientId);
    }

    @Test
    @DisplayName("Deleting a clean user with no medical records should perform hard delete")
    public void testDeleteUserWithoutRecordsPerformsHardDelete() {
        String userId = "CLEAN_USER";
        User user = new User(userId, "Clean User", "clean@medilink.com", "pass", UserRole.PATIENT, "01900000000");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(prescriptionRepository.findByPatientIdOrderByUploadedAtDesc(userId)).thenReturn(Collections.emptyList());
        when(reminderRepository.findByPatientIdOrPatientEmailIgnoreCaseOrderByReminderTimeAsc(eq(userId), eq("clean@medilink.com")))
                .thenReturn(Collections.emptyList());

        boolean result = userService.deleteUser(userId, "ADM_99");

        assertTrue(result);
        verify(userRepository, times(1)).deleteById(userId);
    }

    @Test
    @DisplayName("Suspend and activate user functions should update status correctly")
    public void testSuspendAndActivateUser() {
        User user = new User("USR_01", "Rahim", "rahim@test.com", "pass", UserRole.PATIENT, "01711111111");
        when(userRepository.findById("USR_01")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User suspended = userService.suspendUser("USR_01");
        assertEquals("SUSPENDED", suspended.getStatus());

        User activated = userService.activateUser("USR_01");
        assertEquals("ACTIVE", activated.getStatus());
    }
}
