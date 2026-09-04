package com.medilink.model.strategy;

import com.medilink.model.medicine.Medicine;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete Strategy: Search strictly or primarily by Brand Name (e.g. Napa, Seclo).
 */
public class BrandSearchStrategy implements MedicineSearchStrategy {

    @Override
    public List<Medicine> search(List<Medicine> allMedicines, String query) {
        List<Medicine> results = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return allMedicines;
        }
        String q = query.trim().toLowerCase();
        for (Medicine m : allMedicines) {
            if (m.getBrandName().toLowerCase().contains(q) || m.getCompany().toLowerCase().contains(q)) {
                results.add(m);
            }
        }
        return results;
    }

    @Override
    public String getStrategyName() {
        return "BRAND_SEARCH";
    }
}
