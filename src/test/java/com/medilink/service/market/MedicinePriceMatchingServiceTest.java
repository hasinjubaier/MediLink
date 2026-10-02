package com.medilink.service.market;

import com.medilink.dto.market.ExternalMedicinePriceDto;
import com.medilink.model.medicine.Medicine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class MedicinePriceMatchingServiceTest {

    private MedicinePriceMatchingService matchingService;
    private List<Medicine> catalog;

    @BeforeEach
    public void setUp() {
        matchingService = new MedicinePriceMatchingService();
        catalog = new ArrayList<>();

        Medicine m1 = new Medicine();
        m1.setId("med_01");
        m1.setBrandName("Napa Extra");
        m1.setGenericName("Paracetamol + Caffeine");
        m1.setCompany("Beximco Pharmaceuticals");
        m1.setStrength("500mg+65mg");
        m1.setDosageForm("Tablet");
        m1.setUnitPrice(2.50);
        catalog.add(m1);

        Medicine m2 = new Medicine();
        m2.setId("med_02");
        m2.setBrandName("Napa");
        m2.setGenericName("Paracetamol");
        m2.setCompany("Beximco Pharmaceuticals");
        m2.setStrength("500mg");
        m2.setDosageForm("Tablet");
        m2.setUnitPrice(1.20);
        catalog.add(m2);

        Medicine m3 = new Medicine();
        m3.setId("med_03");
        m3.setBrandName("Seclo 20");
        m3.setGenericName("Omeprazole");
        m3.setCompany("Square Pharmaceuticals");
        m3.setStrength("20mg");
        m3.setDosageForm("Capsule");
        m3.setUnitPrice(6.00);
        catalog.add(m3);
    }

    @Test
    @DisplayName("Tier 1: Exact brand name match works with normalized casing and trimming")
    public void testExactMatch() {
        Optional<Medicine> match = matchingService.findSafeMedicineMatch("  napa extra  ", catalog);
        assertTrue(match.isPresent());
        assertEquals("med_01", match.get().getId());
        assertEquals("Napa Extra", match.get().getBrandName());
    }

    @Test
    @DisplayName("Tier 2: Brand + Strength matching via ExternalMedicinePriceDto")
    public void testBrandAndStrengthMatch() {
        ExternalMedicinePriceDto dto = new ExternalMedicinePriceDto();
        dto.setBrandName("Napa");
        dto.setStrength("500mg");

        Optional<Medicine> match = matchingService.findSafeMedicineMatch(dto, catalog);
        assertTrue(match.isPresent());
        assertEquals("med_02", match.get().getId());
    }

    @Test
    @DisplayName("Tier 3: Brand + Dosage Form matching via ExternalMedicinePriceDto")
    public void testBrandAndDosageFormMatch() {
        ExternalMedicinePriceDto dto = new ExternalMedicinePriceDto();
        dto.setBrandName("Seclo 20");
        dto.setDosageForm("Capsule");

        Optional<Medicine> match = matchingService.findSafeMedicineMatch(dto, catalog);
        assertTrue(match.isPresent());
        assertEquals("med_03", match.get().getId());
    }

    @Test
    @DisplayName("Tier 4: Generic + Strength + Manufacturer matching")
    public void testGenericStrengthManufacturerMatch() {
        ExternalMedicinePriceDto dto = new ExternalMedicinePriceDto();
        dto.setBrandName("DifferentBrandName");
        dto.setGenericName("Omeprazole");
        dto.setStrength("20mg");
        dto.setManufacturer("Square Pharmaceuticals");

        Optional<Medicine> match = matchingService.findSafeMedicineMatch(dto, catalog);
        assertTrue(match.isPresent());
        assertEquals("med_03", match.get().getId());
    }

    @Test
    @DisplayName("Tier 5: Known alias mapping resolves to correct canonical medicine")
    public void testAliasMatch() {
        Optional<Medicine> match = matchingService.findSafeMedicineMatch("Napa Extra 500mg+65mg", catalog);
        assertTrue(match.isPresent());
        assertEquals("med_01", match.get().getId());

        Optional<Medicine> match2 = matchingService.findSafeMedicineMatch("Seclo 20 Capsule", catalog);
        assertTrue(match2.isPresent());
        assertEquals("med_03", match2.get().getId());
    }

    @Test
    @DisplayName("Does not confuse separate brand variants (e.g. Napa vs Napa Extra)")
    public void testDistinctVariantsNotConfused() {
        Optional<Medicine> matchNapa = matchingService.findSafeMedicineMatch("Napa", catalog);
        assertTrue(matchNapa.isPresent());
        assertEquals("med_02", matchNapa.get().getId());

        Optional<Medicine> matchNapaExtra = matchingService.findSafeMedicineMatch("Napa Extra", catalog);
        assertTrue(matchNapaExtra.isPresent());
        assertEquals("med_01", matchNapaExtra.get().getId());
    }

    @Test
    @DisplayName("Ambiguous duplicate brand names are rejected to avoid corrupting prices")
    public void testAmbiguousMatchRejected() {
        Medicine duplicate = new Medicine();
        duplicate.setId("med_99");
        duplicate.setBrandName("Napa Extra");
        catalog.add(duplicate);

        Optional<Medicine> match = matchingService.findSafeMedicineMatch("Napa Extra", catalog);
        assertFalse(match.isPresent());
    }

    @Test
    @DisplayName("Gracefully matches by brand and strength when generic_name and manufacturer are absent")
    public void testMissingGenericAndManufacturerStillMatchesByBrand() {
        ExternalMedicinePriceDto dto = new ExternalMedicinePriceDto();
        dto.setBrandName("Napa Extra");
        dto.setStrength("500mg+65mg");
        dto.setGenericName(null); // Missing generic
        dto.setManufacturer(null); // Missing manufacturer

        Optional<Medicine> match = matchingService.findSafeMedicineMatch(dto, catalog);
        assertTrue(match.isPresent());
        assertEquals("med_01", match.get().getId());
    }

    @Test
    @DisplayName("Tier 4 Generic matching skips gracefully when generic_name is absent")
    public void testTier4SkippedWhenGenericMissing() {
        ExternalMedicinePriceDto dto = new ExternalMedicinePriceDto();
        dto.setBrandName("Unknown Brand");
        dto.setGenericName(null);
        dto.setStrength("20mg");
        dto.setManufacturer("Square Pharmaceuticals");

        Optional<Medicine> match = matchingService.findSafeMedicineMatch(dto, catalog);
        assertFalse(match.isPresent());
    }

    @Test
    @DisplayName("Ambiguous generic matches rejected if manufacturer is absent and multiple brands share generic+strength")
    public void testAmbiguousGenericRejectedWhenManufacturerMissing() {
        Medicine m4 = new Medicine();
        m4.setId("med_04");
        m4.setBrandName("Losectil 20");
        m4.setGenericName("Omeprazole");
        m4.setCompany("SK+F Pharmaceuticals");
        m4.setStrength("20mg");
        catalog.add(m4);

        ExternalMedicinePriceDto dto = new ExternalMedicinePriceDto();
        dto.setBrandName("DifferentBrand");
        dto.setGenericName("Omeprazole");
        dto.setStrength("20mg");
        dto.setManufacturer(null); // No manufacturer to disambiguate med_03 (Square) and med_04 (SK+F)

        Optional<Medicine> match = matchingService.findSafeMedicineMatch(dto, catalog);
        assertFalse(match.isPresent());
    }

    @Test
    @DisplayName("Non-existent medicine returns empty without error")
    public void testNonExistentMedicine() {
        Optional<Medicine> match = matchingService.findSafeMedicineMatch("NonExistentDrug 999", catalog);
        assertFalse(match.isPresent());
    }
}
