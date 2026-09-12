package com.medilink.model.prescription;

/**
 * Concrete State: Prescription has been dispensed and fulfilled by the pharmacy.
 */
public class DispensedState implements PrescriptionState {

    @Override
    public void next(Prescription context) {
        // Terminal fulfilled state
    }

    @Override
    public void prev(Prescription context) {
        context.setState(new VerifiedState());
    }

    @Override
    public String getStatusName() {
        return "DISPENSED";
    }

    @Override
    public boolean canDispense() {
        return true;
    }
}
