package com.medilink.model.strategy;

import com.medilink.model.medicine.Medicine;
import java.util.List;

/**
 * Strategy interface for searching medicines.
 * Demonstrates the Strategy Design Pattern.
 */
public interface MedicineSearchStrategy {
    List<Medicine> search(List<Medicine> allMedicines, String query);
    String getStrategyName();
}
