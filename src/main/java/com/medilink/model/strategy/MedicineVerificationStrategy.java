package com.medilink.model.strategy;

import com.medilink.model.medicine.Medicine;

/**
 * Strategy interface for verifying medicine authenticity (Fake Medicine Detection).
 */
public interface MedicineVerificationStrategy {
    VerificationResult verify(Medicine medicine, String inputCode);

    class VerificationResult {
        private final boolean authentic;
        private final String status;
        private final String details;
        private final String manufacturer;

        public VerificationResult(boolean authentic, String status, String details, String manufacturer) {
            this.authentic = authentic;
            this.status = status;
            this.details = details;
            this.manufacturer = manufacturer;
        }

        public boolean isAuthentic() { return authentic; }
        public String getStatus() { return status; }
        public String getDetails() { return details; }
        public String getManufacturer() { return manufacturer; }
    }
}
