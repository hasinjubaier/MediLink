package com.medilink.repository;

import com.medilink.model.prescription.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, String> {
    List<Prescription> findByPatientIdOrderByUploadedAtDesc(String patientId);
    List<Prescription> findAllByOrderByUploadedAtDesc();
}
