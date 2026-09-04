package com.medilink.service;

import com.medilink.model.medicine.Medicine;
import com.medilink.model.strategy.MedicineVerificationStrategy.VerificationResult;
import com.medilink.repository.MedicineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service orchestrating Fake Medicine Verification using Strategy pattern.
 */
@Service
public class VerificationService {
    private static VerificationService instance;

    @Autowired(required = false)
    private MedicineRepository medicineRepository;

    public VerificationService() {
        instance = this;
    }

    public static synchronized VerificationService getInstance() {
        if (instance == null) {
            instance = new VerificationService();
        }
        return instance;
    }

    public VerificationResult verifyMedicineBatch(String medicineId, String batchOrQrCode) {
        if (batchOrQrCode == null || batchOrQrCode.trim().isEmpty()) {
            return new VerificationResult(false, "INVALID_CODE", "Please enter or scan a valid batch / QR code.", "Unknown");
        }

        String cleanCode = batchOrQrCode.trim().toUpperCase();

        if (medicineRepository != null) {
            // If specific medicine was selected, check against its verified batches
            if (medicineId != null && !medicineId.trim().isEmpty()) {
                Optional<Medicine> medOpt = medicineRepository.findById(medicineId.trim());
                if (medOpt.isPresent()) {
                    Medicine medicine = medOpt.get();
                    if (medicine.isBatchValid(cleanCode)) {
                        return new VerificationResult(
                            true,
                            "AUTHENTIC_GENUINE",
                            "Official DGDA regulatory compliance verified. Batch " + cleanCode + " belongs to genuine production line of " + medicine.getCompany() + ".",
                            medicine.getCompany()
                        );
                    } else {
                        return new VerificationResult(
                            false,
                            "SUSPICIOUS_UNVERIFIED",
                            "WARNING: Batch code " + cleanCode + " was not recognized in official manufacturer records for " + medicine.getBrandName() + ". Potential counterfeit risk!",
                            medicine.getCompany()
                        );
                    }
                }
            }

            // Global search across all medicines for the batch code
            List<Medicine> all = medicineRepository.findAll();
            for (Medicine m : all) {
                if (m.isBatchValid(cleanCode)) {
                    return new VerificationResult(
                        true,
                        "AUTHENTIC_GENUINE",
                        "Verified genuine batch for " + m.getBrandName() + " (" + m.getGenericName() + "). Manufactured by " + m.getCompany() + ".",
                        m.getCompany()
                    );
                }
            }
        }

        return new VerificationResult(
            false,
            "SUSPECT_OR_FAKE",
            "CRITICAL: Scanned code " + cleanCode + " has NO matching manufacturer record in the Directorate General of Drug Administration (DGDA) database.",
            "Unidentified / Unauthorized Source"
        );
    }
}
