package com.medilink.service.market;

import com.medilink.dto.market.ExternalMedicinePriceDto;
import com.medilink.model.medicine.Medicine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Enterprise Service for safe, multi-tiered, deterministic matching between external
 * market quotes and local medicine catalog entities.
 *
 * Matching Priority:
 * 1. Exact normalized brand name
 * 2. Brand + Strength
 * 3. Brand + Dosage form
 * 4. Generic + Strength + Manufacturer
 * 5. Constrained Canonical Alias lookup
 *
 * Strictly rejects ambiguous and broad fuzzy matches (e.g. Napa vs Napa Extra).
 */
@Service
public class MedicinePriceMatchingService {

    private static final Logger log = LoggerFactory.getLogger(MedicinePriceMatchingService.class);

    private final Map<String, String> aliasMap = new ConcurrentHashMap<>();

    public MedicinePriceMatchingService() {
        initDefaultAliases();
    }

    private void initDefaultAliases() {
        aliasMap.put("napa extra 500mg+65mg", "napa extra");
        aliasMap.put("napa extra tablet", "napa extra");
        aliasMap.put("ace plus tablet", "ace plus");
        aliasMap.put("ace plus 500mg+65mg", "ace plus");
        aliasMap.put("seclo 20mg", "seclo 20");
        aliasMap.put("seclo 20 capsule", "seclo 20");
        aliasMap.put("sergel 20 capsule", "sergel 20");
        aliasMap.put("sergel 20mg", "sergel 20");
        aliasMap.put("maxpro 20 capsule", "maxpro 20");
        aliasMap.put("maxpro 20 tablet", "maxpro 20");
        aliasMap.put("fexo 120 tablet", "fexo 120");
        aliasMap.put("fexo 120mg", "fexo 120");
        aliasMap.put("monas 10 tablet", "monas 10");
        aliasMap.put("monas 10mg", "monas 10");
        aliasMap.put("azithrocin 500 tablet", "azithrocin 500");
        aliasMap.put("azithrocin 500mg", "azithrocin 500");
        aliasMap.put("zimax 500 tablet", "zimax 500");
        aliasMap.put("ciprocin 500 tablet", "ciprocin 500");
    }

    public void registerAlias(String variant, String canonicalBrand) {
        if (variant != null && canonicalBrand != null) {
            aliasMap.put(normalize(variant), normalize(canonicalBrand));
        }
    }

    /**
     * Resolves a single unambiguous local Medicine for the given external DTO using the 5-tier strategy.
     */
    public Optional<Medicine> findSafeMedicineMatch(ExternalMedicinePriceDto external, List<Medicine> localMedicines) {
        if (external == null || localMedicines == null || localMedicines.isEmpty()) {
            return Optional.empty();
        }

        String brand = external.getBrandName();
        String strength = external.getStrength();
        String dosageForm = external.getDosageForm();
        String generic = external.getGenericName();
        String manufacturer = external.getManufacturer();

        return findSafeMedicineMatch(brand, strength, dosageForm, generic, manufacturer, localMedicines);
    }

    /**
     * Resolves a local Medicine from brand name and optional secondary attributes.
     */
    public Optional<Medicine> findSafeMedicineMatch(String brand, String strength, String dosageForm,
                                                   String generic, String manufacturer, List<Medicine> localMedicines) {
        if (brand == null || brand.trim().isEmpty() || localMedicines == null || localMedicines.isEmpty()) {
            return Optional.empty();
        }

        String normBrand = normalize(brand);
        if (normBrand.isEmpty()) {
            return Optional.empty();
        }

        // TIER 1: Exact normalized brand-name match
        List<Medicine> tier1Matches = localMedicines.stream()
                .filter(m -> normalize(m.getBrandName()).equals(normBrand))
                .collect(Collectors.toList());

        if (tier1Matches.size() == 1) {
            return Optional.of(tier1Matches.get(0));
        } else if (tier1Matches.size() > 1) {
            // Ambiguity on brand alone; try differentiating by strength / form if provided
            if (strength != null && !strength.trim().isEmpty()) {
                String normStrength = normalize(strength);
                List<Medicine> withStrength = tier1Matches.stream()
                        .filter(m -> normalize(m.getStrength()).equals(normStrength))
                        .collect(Collectors.toList());
                if (withStrength.size() == 1) return Optional.of(withStrength.get(0));
            }
            log.warn("[MedicinePriceMatchingService] Ambiguous match: multiple medicines found for exact brand '{}'. Skipping auto-update.", brand);
            return Optional.empty();
        }

        // TIER 2: Brand + Strength match (e.g. external "Napa" with strength "500 mg" matching local "Napa 500")
        if (strength != null && !strength.trim().isEmpty()) {
            String normStrength = normalize(strength);
            List<Medicine> tier2Matches = localMedicines.stream()
                    .filter(m -> {
                        String localBrand = normalize(m.getBrandName());
                        String localStrength = normalize(m.getStrength());
                        boolean brandMatch = localBrand.equals(normBrand) || (normBrand.startsWith(localBrand + " ") && normBrand.contains(normStrength));
                        return brandMatch && localStrength.equals(normStrength);
                    })
                    .collect(Collectors.toList());

            if (tier2Matches.size() == 1) {
                return Optional.of(tier2Matches.get(0));
            } else if (tier2Matches.size() > 1) {
                log.warn("[MedicinePriceMatchingService] Ambiguous match: multiple medicines found for brand '{}' + strength '{}'.", brand, strength);
                return Optional.empty();
            }
        }

        // TIER 3: Brand + Dosage Form match (e.g. "Napa Extra Tablet" matching local "Napa Extra" form "Tablet")
        if (dosageForm != null && !dosageForm.trim().isEmpty()) {
            String normForm = normalize(dosageForm);
            List<Medicine> tier3Matches = localMedicines.stream()
                    .filter(m -> {
                        String localBrand = normalize(m.getBrandName());
                        String localForm = normalize(m.getDosageForm());
                        boolean brandMatch = localBrand.equals(normBrand) || normBrand.equals(localBrand + " " + normForm);
                        return brandMatch && (localForm.equals(normForm) || localForm.contains(normForm));
                    })
                    .collect(Collectors.toList());

            if (tier3Matches.size() == 1) {
                return Optional.of(tier3Matches.get(0));
            } else if (tier3Matches.size() > 1) {
                log.warn("[MedicinePriceMatchingService] Ambiguous match: multiple medicines found for brand '{}' + dosageForm '{}'.", brand, dosageForm);
                return Optional.empty();
            }
        }

        // TIER 4: Generic + Strength + Manufacturer match
        if (generic != null && !generic.trim().isEmpty() && strength != null && !strength.trim().isEmpty()) {
            String normGeneric = normalize(generic);
            String normStrength = normalize(strength);
            String normMfg = manufacturer != null ? normalize(manufacturer) : "";

            List<Medicine> tier4Matches = localMedicines.stream()
                    .filter(m -> {
                        boolean gMatch = normalize(m.getGenericName()).equals(normGeneric);
                        boolean sMatch = normalize(m.getStrength()).equals(normStrength);
                        boolean mMatch = normMfg.isEmpty() || normalize(m.getCompany()).contains(normMfg) || normMfg.contains(normalize(m.getCompany()));
                        return gMatch && sMatch && mMatch;
                    })
                    .collect(Collectors.toList());

            if (tier4Matches.size() == 1) {
                return Optional.of(tier4Matches.get(0));
            } else if (tier4Matches.size() > 1) {
                log.warn("[MedicinePriceMatchingService] Ambiguous match for generic '{}' + strength '{}'. Skipping auto-update.", generic, strength);
                return Optional.empty();
            }
        }

        // TIER 5: Constrained Canonical Alias lookup
        String resolvedAlias = aliasMap.get(normBrand);
        if (resolvedAlias != null) {
            List<Medicine> aliasMatches = localMedicines.stream()
                    .filter(m -> normalize(m.getBrandName()).equals(resolvedAlias))
                    .collect(Collectors.toList());

            if (aliasMatches.size() == 1) {
                return Optional.of(aliasMatches.get(0));
            } else if (aliasMatches.size() > 1) {
                log.warn("[MedicinePriceMatchingService] Ambiguous alias match for '{}' -> '{}'.", brand, resolvedAlias);
                return Optional.empty();
            }
        }

        // Strict protection against broad substring matches: "Napa" must NEVER match "Napa Extra"
        return Optional.empty();
    }

    /**
     * Backward-compatible overload for string brand names.
     */
    public Optional<Medicine> findSafeMedicineMatch(String externalBrandName, List<Medicine> localMedicines) {
        return findSafeMedicineMatch(externalBrandName, null, null, null, null, localMedicines);
    }

    public String normalize(String str) {
        if (str == null) return "";
        return str.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s+]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
