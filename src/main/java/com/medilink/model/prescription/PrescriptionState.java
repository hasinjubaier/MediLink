package com.medilink.model.prescription;

/**
 * State interface for the State Design Pattern governing Prescription workflow.
 */
public interface PrescriptionState {
    void next(Prescription context);
    void prev(Prescription context);
    String getStatusName();
    boolean canDispense();
}
