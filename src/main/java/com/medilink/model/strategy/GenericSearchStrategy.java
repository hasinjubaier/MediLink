package com.medilink.model.strategy;

import com.medilink.model.medicine.Medicine;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete Strategy: Search by Generic Molecule / Chemical Composition (e.g. Paracetamol, Esomeprazole).
 */
public class GenericSearchStrategy implements MedicineSearchStrategy {

    @Override
    public List<Medicine> search(List<Medicine> allMedicines, String query) {
        List<Medicine> results = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return allMedicines;
        }
        String q = query.trim().toLowerCase();
        for (Medicine m : allMedicines) {
            if (m.getGenericName().toLowerCase().contains(q) || m.getCategory().toLowerCase().contains(q)) {
                results.add(m);
            }
        }
        return results;
    }

    @Override
    public String getStrategyName() {
        return "GENERIC_SEARCH";
    }
}
