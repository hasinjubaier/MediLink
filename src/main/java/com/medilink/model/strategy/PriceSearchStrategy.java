package com.medilink.model.strategy;

import com.medilink.model.medicine.Medicine;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Concrete Strategy: Search alternatives matching generic name, sorted ascending by price (Best Value).
 */
public class PriceSearchStrategy implements MedicineSearchStrategy {

    @Override
    public List<Medicine> search(List<Medicine> allMedicines, String query) {
        List<Medicine> matches = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            matches.addAll(allMedicines);
        } else {
            String q = query.trim().toLowerCase();
            for (Medicine m : allMedicines) {
                boolean matchGeneric = m.getGenericName() != null && m.getGenericName().toLowerCase().contains(q);
                boolean matchBrand = m.getBrandName() != null && m.getBrandName().toLowerCase().contains(q);
                if (matchGeneric || matchBrand) {
                    matches.add(m);
                }
            }
        }
        // Sort lowest unit price first
        Collections.sort(matches, new Comparator<Medicine>() {
            @Override
            public int compare(Medicine m1, Medicine m2) {
                return Double.compare(m1.getUnitPrice(), m2.getUnitPrice());
            }
        });
        return matches;
    }

    @Override
    public String getStrategyName() {
        return "BEST_PRICE_STRATEGY";
    }
}
