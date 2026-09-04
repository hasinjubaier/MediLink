package com.medilink.service;

import com.medilink.model.prescription.Prescription;
import com.medilink.model.prescription.PrescriptionItem;
import com.medilink.repository.PrescriptionItemRepository;
import com.medilink.repository.PrescriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;

    @Autowired
    public PrescriptionService(PrescriptionRepository prescriptionRepository,
                               PrescriptionItemRepository prescriptionItemRepository) {
        this.prescriptionRepository = prescriptionRepository;
        this.prescriptionItemRepository = prescriptionItemRepository;
    }

    public List<Prescription> findAll() {
        return prescriptionRepository.findAllByOrderByUploadedAtDesc();
    }

    public List<Prescription> findByPatientId(String patientId) {
        return prescriptionRepository.findByPatientIdOrderByUploadedAtDesc(patientId);
    }

    public Optional<Prescription> findById(String id) {
        return prescriptionRepository.findById(id);
    }

    public Prescription save(Prescription prescription) {
        return prescriptionRepository.save(prescription);
    }

    public Prescription createPrescription(String patientId, String patientName, String doctorName,
                                           String hospital, String rawScanText, List<PrescriptionItem> items) {
        String id = "rx_" + System.currentTimeMillis();
        Prescription p = new Prescription(id, patientId, patientName, doctorName, hospital, rawScanText);
        p.setUploadedAt(LocalDateTime.now());

        if (items != null) {
            for (PrescriptionItem item : items) {
                p.addItem(item);
            }
        }
        return prescriptionRepository.save(p);
    }

    public Optional<Prescription> advanceWorkflow(String prescriptionId) {
        Optional<Prescription> pOpt = prescriptionRepository.findById(prescriptionId);
        if (!pOpt.isPresent()) return Optional.empty();

        Prescription p = pOpt.get();
        p.advanceWorkflow();
        return Optional.of(prescriptionRepository.save(p));
    }

    public Optional<Prescription> revertWorkflow(String prescriptionId) {
        Optional<Prescription> pOpt = prescriptionRepository.findById(prescriptionId);
        if (!pOpt.isPresent()) return Optional.empty();

        Prescription p = pOpt.get();
        p.revertWorkflow();
        return Optional.of(prescriptionRepository.save(p));
    }

    public boolean deletePrescription(String id) {
        if (!prescriptionRepository.existsById(id)) return false;
        prescriptionRepository.deleteById(id);
        return true;
    }
}
