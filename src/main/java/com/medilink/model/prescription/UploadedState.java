package com.medilink.model.prescription;

/**
 * Concrete State: Prescription is newly uploaded and awaiting OCR/Text extraction.
 */
public class UploadedState implements PrescriptionState {

    @Override
    public void next(Prescription context) {
        context.setState(new ExtractedState());
    }

    @Override
    public void prev(Prescription context) {
        // Initial state, cannot go back
    }

    @Override
    public String getStatusName() {
        return "UPLOADED";
    }

    @Override
    public boolean canDispense() {
        return false;
    }
}
