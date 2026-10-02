package com.medilink.service.market;

import com.medilink.config.MarketApiProperties;
import com.medilink.model.market.MarketPriceItem;
import com.medilink.model.market.MedicinePriceHistory;
import com.medilink.model.medicine.Medicine;
import com.medilink.model.pharmacy.PharmacyStock;
import com.medilink.repository.MedicinePriceHistoryRepository;
import com.medilink.repository.MedicineRepository;
import com.medilink.repository.PharmacyStockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MarketPriceSyncServiceTest {

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private PharmacyStockRepository pharmacyStockRepository;

    @Mock
    private MedicinePriceHistoryRepository priceHistoryRepository;

    @Mock
    private MarketPriceProvider marketPriceProvider;

    private MedicinePriceMatchingService priceMatchingService;
    private MarketApiProperties properties;
    private MarketPriceSyncService syncService;

    private Medicine testMedicine;
    private PharmacyStock testStock;

    @BeforeEach
    public void setUp() {
        priceMatchingService = new MedicinePriceMatchingService();
        properties = new MarketApiProperties();
        properties.setMaxPriceChangePercent(50.0);

        syncService = new MarketPriceSyncService(
                medicineRepository,
                pharmacyStockRepository,
                priceHistoryRepository,
                marketPriceProvider,
                priceMatchingService,
                properties
        );

        testMedicine = new Medicine();
        testMedicine.setId("med_01");
        testMedicine.setBrandName("Napa Extra");
        testMedicine.setGenericName("Paracetamol + Caffeine");
        testMedicine.setCompany("Beximco Pharmaceuticals");
        testMedicine.setUnitPrice(2.50);

        testStock = new PharmacyStock();
        testStock.setId("stk_01");
        testStock.setMedicineId("med_01");
        testStock.setPharmacyId("pharma_01");
        testStock.setUnitPrice(2.50);
        testStock.setQuantity(100);
    }

    @Test
    @DisplayName("Price increase updates medicine, updates pharmacy stock, and logs INCREASED history")
    public void testPriceIncrease() {
        when(medicineRepository.findAll()).thenReturn(Collections.singletonList(testMedicine));
        when(pharmacyStockRepository.findByMedicineId("med_01")).thenReturn(Collections.singletonList(testStock));

        MarketPriceItem quote = new MarketPriceItem(
                "Napa Extra", "Paracetamol + Caffeine", "Beximco", "500mg+65mg", "Tablet", 3.00, "2026-09-26", "MedEx via Apify"
        );
        when(marketPriceProvider.fetchPriceByBrand("Napa Extra")).thenReturn(Optional.of(quote));

        MarketPriceSyncService.SyncResult result = syncService.syncAllMedicines("Scheduled Auto-Sync");

        assertEquals(1, result.getCheckedCount());
        assertEquals(1, result.getUpdatedCount());
        assertEquals(3.00, testMedicine.getUnitPrice());
        assertEquals(3.00, testStock.getUnitPrice());

        ArgumentCaptor<MedicinePriceHistory> histCaptor = ArgumentCaptor.forClass(MedicinePriceHistory.class);
        verify(priceHistoryRepository, times(1)).save(histCaptor.capture());
        MedicinePriceHistory saved = histCaptor.getValue();
        assertEquals("INCREASED", saved.getDirection());
        assertEquals(2.50, saved.getOldPrice());
        assertEquals(3.00, saved.getNewPrice());
        assertEquals(0.50, saved.getPriceChange());
        assertEquals(20.0, saved.getPercentageChange());
    }

    @Test
    @DisplayName("Price decrease updates medicine, updates pharmacy stock, and logs DECREASED history")
    public void testPriceDecrease() {
        testMedicine.setUnitPrice(3.00);
        testStock.setUnitPrice(3.00);

        when(medicineRepository.findAll()).thenReturn(Collections.singletonList(testMedicine));
        when(pharmacyStockRepository.findByMedicineId("med_01")).thenReturn(Collections.singletonList(testStock));

        MarketPriceItem quote = new MarketPriceItem(
                "Napa Extra", "Paracetamol + Caffeine", "Beximco", "500mg+65mg", "Tablet", 2.60, "2026-09-26", "MedEx via Apify"
        );
        when(marketPriceProvider.fetchPriceByBrand("Napa Extra")).thenReturn(Optional.of(quote));

        MarketPriceSyncService.SyncResult result = syncService.syncAllMedicines("DGDA Regulatory Decrement");

        assertEquals(1, result.getUpdatedCount());
        assertEquals(2.60, testMedicine.getUnitPrice());
        assertEquals(2.60, testStock.getUnitPrice());

        ArgumentCaptor<MedicinePriceHistory> histCaptor = ArgumentCaptor.forClass(MedicinePriceHistory.class);
        verify(priceHistoryRepository, times(1)).save(histCaptor.capture());
        MedicinePriceHistory saved = histCaptor.getValue();
        assertEquals("DECREASED", saved.getDirection());
    }

    @Test
    @DisplayName("Unchanged price does NOT create duplicate price history or trigger save")
    public void testUnchangedPriceIgnored() {
        testMedicine.setUnitPrice(2.50);
        when(medicineRepository.findAll()).thenReturn(Collections.singletonList(testMedicine));

        MarketPriceItem quote = new MarketPriceItem(
                "Napa Extra", "Paracetamol + Caffeine", "Beximco", "500mg+65mg", "Tablet", 2.50, "2026-09-26", "MedEx via Apify"
        );
        when(marketPriceProvider.fetchPriceByBrand("Napa Extra")).thenReturn(Optional.of(quote));

        MarketPriceSyncService.SyncResult result = syncService.syncAllMedicines("Scheduled Auto-Sync");

        assertEquals(1, result.getCheckedCount());
        assertEquals(0, result.getUpdatedCount());
        verify(priceHistoryRepository, never()).save(any());
        verify(medicineRepository, never()).save(any());
    }

    @Test
    @DisplayName("Negative or zero invalid market price is rejected and does not corrupt database")
    public void testInvalidNegativePriceRejected() {
        testMedicine.setUnitPrice(2.50);
        when(medicineRepository.findAll()).thenReturn(Collections.singletonList(testMedicine));

        MarketPriceItem negativeQuote = new MarketPriceItem(
                "Napa Extra", "Paracetamol + Caffeine", "Beximco", "500mg+65mg", "Tablet", -5.00, "2026-09-26", "Corrupt Feed"
        );
        when(marketPriceProvider.fetchPriceByBrand("Napa Extra")).thenReturn(Optional.of(negativeQuote));

        MarketPriceSyncService.SyncResult result = syncService.syncAllMedicines("Scheduled Auto-Sync");

        assertEquals(0, result.getUpdatedCount());
        assertEquals(2.50, testMedicine.getUnitPrice()); // Remains intact
        verify(priceHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Excessive runaway price increase beyond maxPriceChangePercent is rejected")
    public void testExcessivePriceChangeRejected() {
        testMedicine.setUnitPrice(2.50);
        when(medicineRepository.findAll()).thenReturn(Collections.singletonList(testMedicine));

        // Attempting to set 10.00 from 2.50 (+300% change, exceeds 50% limit)
        MarketPriceItem runawayQuote = new MarketPriceItem(
                "Napa Extra", "Paracetamol + Caffeine", "Beximco", "500mg+65mg", "Tablet", 10.00, "2026-09-26", "Erroneous Feed"
        );
        when(marketPriceProvider.fetchPriceByBrand("Napa Extra")).thenReturn(Optional.of(runawayQuote));

        MarketPriceSyncService.SyncResult result = syncService.syncAllMedicines("Scheduled Auto-Sync");

        assertEquals(0, result.getUpdatedCount());
        assertEquals(2.50, testMedicine.getUnitPrice()); // Preserved previous price
        verify(priceHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Provider failure or missing quote preserves last known valid database price")
    public void testProviderFailurePreservesPrice() {
        testMedicine.setUnitPrice(2.50);
        when(medicineRepository.findAll()).thenReturn(Collections.singletonList(testMedicine));
        when(marketPriceProvider.fetchPriceByBrand("Napa Extra")).thenReturn(Optional.empty());

        MarketPriceSyncService.SyncResult result = syncService.syncAllMedicines("Scheduled Auto-Sync");

        assertEquals(0, result.getUpdatedCount());
        assertEquals(2.50, testMedicine.getUnitPrice());
        verify(priceHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Direct webhook update persists new price and modifies pharmacy stocks")
    public void testDirectPriceUpdate() {
        when(medicineRepository.findAll()).thenReturn(Collections.singletonList(testMedicine));
        when(pharmacyStockRepository.findByMedicineId("med_01")).thenReturn(Collections.singletonList(testStock));

        boolean updated = syncService.updatePriceDirectly("Napa Extra", 3.50, "Distributor Live Webhook");

        assertTrue(updated);
        assertEquals(3.50, testMedicine.getUnitPrice());
        assertEquals(3.50, testStock.getUnitPrice());
        verify(priceHistoryRepository, times(1)).save(any(MedicinePriceHistory.class));
    }

    @Test
    @DisplayName("Partial sync (>100 problem): local DB has more medicines than external feed. Reports PARTIAL_SUCCESS, correct coverage, and leaves unmatched untouched")
    public void testPartialSyncHandlingMoreLocalMedicinesThanExternal() {
        Medicine m2 = new Medicine();
        m2.setId("med_02");
        m2.setBrandName("Seclo 20");
        m2.setUnitPrice(6.00);

        Medicine m3 = new Medicine();
        m3.setId("med_03");
        m3.setBrandName("Monas 10");
        m3.setUnitPrice(15.00);

        when(medicineRepository.findAll()).thenReturn(Arrays.asList(testMedicine, m2, m3));
        when(pharmacyStockRepository.findByMedicineId("med_01")).thenReturn(Collections.singletonList(testStock));

        // External quotes only contains Napa Extra (1 out of 3)
        MarketPriceItem quote1 = new MarketPriceItem(
                "Napa Extra", "Paracetamol + Caffeine", "Beximco", "500mg+65mg", "Tablet", 3.00, "2026-09-26", "MedEx via Apify"
        );
        when(marketPriceProvider.fetchLatestMarketPrices()).thenReturn(Collections.singletonList(quote1));

        MarketPriceSyncService.SyncResult result = syncService.syncAllMedicines("Scheduled Auto-Sync");

        MarketPriceSyncService.MarketSyncStatistics stats = result.getStatistics();
        assertNotNull(stats);
        assertEquals(3, stats.getLocalMedicinesConsidered());
        assertEquals(1, stats.getExternalRecordsRetrieved());
        assertEquals(1, stats.getMatchedMedicines());
        assertEquals(1, stats.getUpdatedMedicines());
        assertEquals(0, stats.getUnchangedMedicines());
        assertEquals(2, stats.getUnmatchedMedicines());
        assertEquals(0, stats.getFailedExternalRequests());
        assertEquals("33.3%", stats.getSyncCoveragePercent());
        assertEquals("PARTIAL_SUCCESS", stats.getStatus());

        // Verify unmatched medicines remain completely untouched
        assertEquals(6.00, m2.getUnitPrice());
        assertEquals(15.00, m3.getUnitPrice());
        verify(medicineRepository, times(1)).save(testMedicine);
        verify(medicineRepository, never()).save(m2);
        verify(medicineRepository, never()).save(m3);
    }

    @Test
    @DisplayName("Deduplication: multiple external quotes for same medicine are updated only once without duplicate price history")
    public void testDeduplicationForDuplicateExternalQuotes() {
        when(medicineRepository.findAll()).thenReturn(Collections.singletonList(testMedicine));
        when(pharmacyStockRepository.findByMedicineId("med_01")).thenReturn(Collections.singletonList(testStock));

        MarketPriceItem quote1 = new MarketPriceItem(
                "Napa Extra", "Paracetamol + Caffeine", "Beximco", "500mg+65mg", "Tablet", 3.20, "2026-09-26", "MedEx via Apify"
        );
        MarketPriceItem quote2 = new MarketPriceItem(
                "Napa Extra", "Paracetamol + Caffeine", "Beximco", "500mg+65mg", "Tablet", 3.20, "2026-09-26", "MedEx via Apify (Duplicate)"
        );
        when(marketPriceProvider.fetchLatestMarketPrices()).thenReturn(Arrays.asList(quote1, quote2));

        MarketPriceSyncService.SyncResult result = syncService.syncAllMedicines("Scheduled Auto-Sync");

        assertEquals(1, result.getUpdatedCount());
        assertEquals(1, result.getStatistics().getMatchedMedicines());
        // Only 1 save to price history repository, not 2
        verify(priceHistoryRepository, times(1)).save(any(MedicinePriceHistory.class));
        verify(medicineRepository, times(1)).save(testMedicine);
    }

    @Test
    @DisplayName("Apify timeout during sync logs warning, preserves local prices, and sets TIMED_OUT status")
    public void testExternalTimeoutPreservesDatabase() {
        when(medicineRepository.findAll()).thenReturn(Collections.singletonList(testMedicine));
        when(marketPriceProvider.fetchLatestMarketPrices()).thenThrow(new RuntimeException("Read timed out after 120000 ms"));

        MarketPriceSyncService.SyncResult result = syncService.syncAllMedicines("Scheduled Auto-Sync");

        assertEquals(0, result.getUpdatedCount());
        assertEquals(2.50, testMedicine.getUnitPrice()); // Untouched
        verify(medicineRepository, never()).save(any());
        verify(priceHistoryRepository, never()).save(any());

        MarketPriceSyncService.MarketSyncStatistics stats = result.getStatistics();
        assertEquals(1, stats.getFailedExternalRequests());
        assertEquals("TIMED_OUT", stats.getStatus());
        assertTrue(stats.getErrorSummary().contains("timed out"));
    }
}
