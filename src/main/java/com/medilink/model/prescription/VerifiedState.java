package com.medilink.model.prescription;

/**
 * Concrete State: Prescription verified by a licensed pharmacist; valid for dispensing.
 */
public class VerifiedState implements PrescriptionState {

    @Override
    public void next(Prescription context) {
        // Terminal valid state
    }

    @Override
    public void prev(Prescription context) {
        context.setState(new ExtractedState());
    }

    @Override
    public String getStatusName() {
        return "VERIFIED_BY_PHARMACIST";
    }

    @Override
    public boolean canDispense() {
        return true;
    }
}
