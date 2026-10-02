package com.medilink.service.market;

import com.medilink.config.MarketApiProperties;
import com.medilink.dto.market.ExternalMedicinePriceDto;
import com.medilink.service.market.client.ExternalMedicineApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExternalMedicineApiClientTest {

    @Mock
    private RestTemplate restTemplate;

    private MarketApiProperties properties;
    private ExternalMedicineApiClient client;

    @BeforeEach
    public void setUp() {
        properties = new MarketApiProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("https://api.apify.com/v2");
        properties.setApifyActorId("riad_h~medex-medicine-scraper");
        properties.setApiKey("sec_test_api_key_9988");
        properties.setApiKeyHeader("Authorization");
        properties.setApiKeyPrefix("Bearer");
        properties.setMaxRetries(1);
        properties.setSearchDepth(1);
        properties.setMaxResults(100);
        properties.setRequestDelayMs(200);

        client = new ExternalMedicineApiClient(properties, restTemplate);
    }

    @Test
    @DisplayName("1. Provider successfully fetches and parses valid external Apify prices")
    public void testFetchPricesSuccess() {
        String jsonResponse = "[{"
                + "\"brand_name\": \"Napa Extra\","
                + "\"generic_name\": \"Paracetamol + Caffeine\","
                + "\"manufacturer\": \"Beximco Pharmaceuticals\","
                + "\"strength\": \"500mg+65mg\","
                + "\"dosage_form\": \"Tablet\","
                + "\"unit_price\": 3.20,"
                + "\"currency\": \"BDT\""
                + "}]";

        ResponseEntity<String> response = ResponseEntity.ok(jsonResponse);

        when(restTemplate.exchange(
                eq(URI.create("https://api.apify.com/v2/actors/riad_h~medex-medicine-scraper/run-sync-get-dataset-items")),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(response);

        List<ExternalMedicinePriceDto> results = client.fetchPrices();

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Napa Extra", results.get(0).getBrandName());
        assertEquals(new BigDecimal("3.2"), results.get(0).getUnitPrice());
        assertEquals("Tablet", results.get(0).getDosageForm());
        assertTrue(client.isLastCallSuccessful());
        assertNotNull(client.getLastSuccessfulRequestTime());
    }

    @Test
    @DisplayName("2. Provider handles empty response gracefully")
    public void testFetchPricesEmpty() {
        ResponseEntity<String> response = ResponseEntity.ok("[]");

        when(restTemplate.exchange(
                any(URI.class),
                any(HttpMethod.class),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(response);

        List<ExternalMedicinePriceDto> results = client.fetchPrices();

        assertNotNull(results);
        assertTrue(results.isEmpty());
        assertTrue(client.isLastCallSuccessful());
    }

    @Test
    @DisplayName("3. Provider handles null/malformed response body")
    public void testFetchPricesNullBody() {
        ResponseEntity<String> response = ResponseEntity.ok(null);

        when(restTemplate.exchange(
                any(URI.class),
                any(HttpMethod.class),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(response);

        List<ExternalMedicinePriceDto> results = client.fetchPrices();

        assertNotNull(results);
        assertTrue(results.isEmpty());
        assertFalse(client.isLastCallSuccessful());
    }

    @Test
    @DisplayName("4. Provider handles HTTP 401 Unauthorized without wasteful retries")
    public void testFetchPricesHttp401() {
        when(restTemplate.exchange(
                any(URI.class),
                any(HttpMethod.class),
                any(HttpEntity.class),
                eq(String.class)
        )).thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

        List<ExternalMedicinePriceDto> results = client.fetchPrices();

        assertTrue(results.isEmpty());
        assertFalse(client.isLastCallSuccessful());
        assertEquals("Client Error 401", client.getLastErrorReason());
        // Should only be called once because 401 is non-retriable
        verify(restTemplate, times(1)).exchange(any(URI.class), any(HttpMethod.class), any(), eq(String.class));
    }

    @Test
    @DisplayName("5. Provider handles HTTP 403 Forbidden")
    public void testFetchPricesHttp403() {
        when(restTemplate.exchange(
                any(URI.class),
                any(HttpMethod.class),
                any(HttpEntity.class),
                eq(String.class)
        )).thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN));

        List<ExternalMedicinePriceDto> results = client.fetchPrices();

        assertTrue(results.isEmpty());
        assertFalse(client.isLastCallSuccessful());
        verify(restTemplate, times(1)).exchange(any(URI.class), any(HttpMethod.class), any(), eq(String.class));
    }

    @Test
    @DisplayName("6. Provider handles HTTP 429 Rate Limiting with retry")
    public void testFetchPricesHttp429() {
        when(restTemplate.exchange(
                any(URI.class),
                any(HttpMethod.class),
                any(HttpEntity.class),
                eq(String.class)
        )).thenThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS));

        List<ExternalMedicinePriceDto> results = client.fetchPrices();

        assertTrue(results.isEmpty());
        assertFalse(client.isLastCallSuccessful());
        // maxRetries is 1, so 1 initial + 1 retry = 2 calls
        verify(restTemplate, times(2)).exchange(any(URI.class), any(HttpMethod.class), any(), eq(String.class));
    }

    @Test
    @DisplayName("7. Provider handles HTTP 500 Internal Server Error")
    public void testFetchPricesHttp500() {
        when(restTemplate.exchange(
                any(URI.class),
                any(HttpMethod.class),
                any(HttpEntity.class),
                eq(String.class)
        )).thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        List<ExternalMedicinePriceDto> results = client.fetchPrices();

        assertTrue(results.isEmpty());
        assertFalse(client.isLastCallSuccessful());
        assertEquals("Server Error 500", client.getLastErrorReason());
    }

    @Test
    @DisplayName("8. Provider handles socket timeout gracefully")
    public void testFetchPricesTimeout() {
        when(restTemplate.exchange(
                any(URI.class),
                any(HttpMethod.class),
                any(HttpEntity.class),
                eq(String.class)
        )).thenThrow(new ResourceAccessException("Read timed out"));

        List<ExternalMedicinePriceDto> results = client.fetchPrices();

        assertTrue(results.isEmpty());
        assertFalse(client.isLastCallSuccessful());
        assertTrue(client.getLastErrorReason().contains("Timeout"));
    }

    @Test
    @DisplayName("9. API key header and Apify input body are correctly formatted and injected")
    public void testApiKeyHeaderInjection() {
        properties.setApiKeyPrefix("Bearer");
        properties.setApiKeyHeader("Authorization");
        properties.setApiKey("secret_token_1234");

        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("[]"));

        client.fetchPrices();

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(any(URI.class), eq(HttpMethod.POST), captor.capture(), eq(String.class));

        HttpHeaders headers = captor.getValue().getHeaders();
        assertEquals("Bearer secret_token_1234", headers.getFirst("Authorization"));

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        assertNotNull(body);
        assertEquals(1, body.get("searchDepth"));
        assertEquals(100, body.get("maxResults"));
        assertEquals(200, body.get("requestDelayMs"));
    }

    @Test
    @DisplayName("10. Invalid negative or null price in DTO is rejected by validation")
    public void testDtoValidation() {
        ExternalMedicinePriceDto valid = new ExternalMedicinePriceDto();
        valid.setBrandName("Napa");
        valid.setUnitPrice(new BigDecimal("1.50"));
        assertTrue(valid.isValid());

        ExternalMedicinePriceDto negative = new ExternalMedicinePriceDto();
        negative.setBrandName("Napa");
        negative.setUnitPrice(new BigDecimal("-1.00"));
        assertFalse(negative.isValid());

        ExternalMedicinePriceDto nullPrice = new ExternalMedicinePriceDto();
        nullPrice.setBrandName("Napa");
        nullPrice.setUnitPrice(null);
        assertFalse(nullPrice.isValid());

        ExternalMedicinePriceDto blankBrand = new ExternalMedicinePriceDto();
        blankBrand.setBrandName("   ");
        blankBrand.setUnitPrice(new BigDecimal("2.00"));
        assertFalse(blankBrand.isValid());
    }

    @Test
    @DisplayName("11. DTO parses successfully and is valid when generic_name and manufacturer are absent")
    public void testDtoValidWithoutGenericOrManufacturer() {
        String jsonWithMissingFields = "[{"
                + "\"brand_name\": \"Napa\","
                + "\"strength\": \"500mg\","
                + "\"dosage_form\": \"Tablet\","
                + "\"unit_price\": 1.20"
                + "}]";

        List<ExternalMedicinePriceDto> dtos = client.parseResponseBody(jsonWithMissingFields);
        assertNotNull(dtos);
        assertEquals(1, dtos.size());

        ExternalMedicinePriceDto dto = dtos.get(0);
        assertEquals("Napa", dto.getBrandName());
        assertEquals("500mg", dto.getStrength());
        assertNull(dto.getGenericName());
        assertNull(dto.getManufacturer());
        assertEquals(new BigDecimal("1.2"), dto.getUnitPrice());
        assertTrue(dto.isValid(), "Medicine without generic or manufacturer must still be valid for price updates");
    }

    @Test
    @DisplayName("12. Tolerant price parsing handles currency strings like '৳ 3.50'")
    public void testTolerantPriceParsing() {
        String jsonWithCurrencyString = "[{"
                + "\"brand_name\": \"Ace Plus\","
                + "\"unit_price\": \"৳ 3.50\""
                + "}]";

        List<ExternalMedicinePriceDto> dtos = client.parseResponseBody(jsonWithCurrencyString);
        assertNotNull(dtos);
        assertEquals(1, dtos.size());
        assertEquals(new BigDecimal("3.50"), dtos.get(0).getUnitPrice());
        assertTrue(dtos.get(0).isValid());
    }
}
