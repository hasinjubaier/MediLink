package com.medilink.model.prescription;

/**
 * Concrete State: Prescription details extracted, pending Pharmacist verification.
 */
public class ExtractedState implements PrescriptionState {

    @Override
    public void next(Prescription context) {
        context.setState(new VerifiedState());
    }

    @Override
    public void prev(Prescription context) {
        context.setState(new UploadedState());
    }

    @Override
    public String getStatusName() {
        return "EXTRACTED";
    }

    @Override
    public boolean canDispense() {
        return false;
    }
}
