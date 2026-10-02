package com.medilink.service.market.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medilink.config.MarketApiProperties;
import com.medilink.dto.market.ExternalMedicinePriceDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

import java.net.URI;
import java.time.Instant;
import java.util.*;

/**
 * Enterprise client for executing authenticated requests against the Apify MedEx Scraper
 * Actor (or approved pharmaceutical market REST endpoints).
 * Configured with connection/read timeouts, exponential backoff retries, and strict secret protection.
 */
@Component
public class ExternalMedicineApiClient {

    private static final Logger log = LoggerFactory.getLogger(ExternalMedicineApiClient.class);

    private final MarketApiProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private volatile boolean lastCallSuccessful = false;
    private volatile Instant lastSuccessfulRequestTime = null;
    private volatile String lastErrorReason = null;

    @Autowired
    public ExternalMedicineApiClient(MarketApiProperties properties) {
        this(properties, createDefaultRestTemplate(properties), new ObjectMapper());
    }

    public ExternalMedicineApiClient(MarketApiProperties properties, RestTemplate restTemplate) {
        this(properties, restTemplate, new ObjectMapper());
    }

    public ExternalMedicineApiClient(MarketApiProperties properties, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    private static RestTemplate createDefaultRestTemplate(MarketApiProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getConnectTimeoutMs() > 0 ? props.getConnectTimeoutMs() : 10000);
        factory.setReadTimeout(props.getReadTimeoutMs() > 0 ? props.getReadTimeoutMs() : 120000);
        return new RestTemplate(factory);
    }

    /**
     * Resolves the target endpoint URL according to whether Apify Actor execution or direct REST is configured.
     */
    public String resolveEndpointUrl() {
        String baseUrl = properties.getBaseUrl() != null ? properties.getBaseUrl().trim() : "";
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        String actorId = properties.getApifyActorId();
        if (actorId != null && !actorId.trim().isEmpty()) {
            String sanitizedActor = actorId.trim().replace('/', '~');
            if (baseUrl.contains("/actors/")) {
                return baseUrl.endsWith("/run-sync-get-dataset-items") ? baseUrl : baseUrl + "/run-sync-get-dataset-items";
            }
            if (baseUrl.contains("apify.com") || baseUrl.endsWith("/v2")) {
                return baseUrl + "/actors/" + sanitizedActor + "/run-sync-get-dataset-items";
            }
        }
        return baseUrl;
    }

    /**
     * Builds the exact Apify Actor input payload according to the MedEx scraper schema.
     */
    public Map<String, Object> buildActorInput() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("searchDepth", properties.getSearchDepth() > 0 ? properties.getSearchDepth() : 1);
        input.put("maxResults", properties.getMaxResults() >= 0 ? properties.getMaxResults() : 100);
        input.put("requestDelayMs", properties.getRequestDelayMs() > 0 ? properties.getRequestDelayMs() : 200);
        return input;
    }

    /**
     * Executes authenticated request to fetch latest medicine prices from the Apify actor.
     */
    public List<ExternalMedicinePriceDto> fetchPrices() {
        if (!isConfiguredAndValid()) {
            return Collections.emptyList();
        }

        String targetUrl = resolveEndpointUrl();
        boolean isApify = targetUrl.contains("/actors/") || targetUrl.contains("apify.com");
        HttpMethod method = isApify ? HttpMethod.POST : HttpMethod.GET;

        HttpHeaders headers = buildSafeHeaders();
        HttpEntity<?> entity = isApify
                ? new HttpEntity<>(buildActorInput(), headers)
                : new HttpEntity<>(headers);

        int attempts = 0;
        int maxRetries = Math.max(0, properties.getMaxRetries());

        while (attempts <= maxRetries) {
            attempts++;
            try {
                log.info("[ExternalMedicineApiClient] Requesting market prices from external provider (attempt {}/{}) via {} {}",
                        attempts, maxRetries + 1, method, targetUrl);

                ResponseEntity<String> response = restTemplate.exchange(
                        URI.create(targetUrl),
                        method,
                        entity,
                        String.class
                );

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    List<ExternalMedicinePriceDto> items = parseResponseBody(response.getBody());
                    lastCallSuccessful = true;
                    lastSuccessfulRequestTime = Instant.now();
                    lastErrorReason = null;
                    log.info("[ExternalMedicineApiClient] Successfully fetched {} price records from external provider.", items.size());
                    return items;
                } else {
                    lastCallSuccessful = false;
                    lastErrorReason = "HTTP " + response.getStatusCodeValue();
                }
            } catch (HttpClientErrorException e) {
                lastCallSuccessful = false;
                lastErrorReason = "Client Error " + e.getRawStatusCode();
                log.warn("[ExternalMedicineApiClient] Client error calling external provider: Status {}", e.getRawStatusCode());
                if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                    // Do not retry 401/403 authentication failures
                    break;
                }
            } catch (HttpServerErrorException e) {
                lastCallSuccessful = false;
                lastErrorReason = "Server Error " + e.getRawStatusCode();
                log.warn("[ExternalMedicineApiClient] External provider returned 5xx server error: Status {}", e.getRawStatusCode());
            } catch (ResourceAccessException e) {
                lastCallSuccessful = false;
                lastErrorReason = "Network / Timeout: " + e.getMessage();
                log.warn("[ExternalMedicineApiClient] Network timeout or unreachable host for external provider.");
            } catch (Exception e) {
                lastCallSuccessful = false;
                lastErrorReason = "Unexpected failure: " + e.getMessage();
                log.error("[ExternalMedicineApiClient] Error executing market price request: {}", e.getMessage());
            }

            if (attempts <= maxRetries) {
                try {
                    Thread.sleep(500L * attempts);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        return Collections.emptyList();
    }

    /**
     * Parses the raw JSON response into a list of normalized DTOs.
     */
    public List<ExternalMedicinePriceDto> parseResponseBody(String rawJson) {
        if (rawJson == null || rawJson.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<ExternalMedicinePriceDto> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            if (root.isArray()) {
                for (JsonNode node : root) {
                    ExternalMedicinePriceDto dto = objectMapper.treeToValue(node, ExternalMedicinePriceDto.class);
                    if (dto != null) {
                        result.add(dto);
                    }
                }
            } else if (root.isObject()) {
                // If wrapped in results/items/data container
                JsonNode itemsNode = root.has("items") ? root.get("items")
                        : (root.has("results") ? root.get("results")
                        : (root.has("data") ? root.get("data") : null));

                if (itemsNode != null && itemsNode.isArray()) {
                    for (JsonNode node : itemsNode) {
                        ExternalMedicinePriceDto dto = objectMapper.treeToValue(node, ExternalMedicinePriceDto.class);
                        if (dto != null) {
                            result.add(dto);
                        }
                    }
                } else {
                    // Single item fallback
                    ExternalMedicinePriceDto dto = objectMapper.treeToValue(root, ExternalMedicinePriceDto.class);
                    if (dto != null) {
                        result.add(dto);
                    }
                }
            }
        } catch (Exception e) {
            log.error("[ExternalMedicineApiClient] Failed to parse external JSON response: {}", e.getMessage());
        }
        return result;
    }

    /**
     * Queries the external provider for a specific medicine by brand name.
     */
    public Optional<ExternalMedicinePriceDto> fetchPriceByBrand(String brandName) {
        if (!isConfiguredAndValid() || brandName == null || brandName.trim().isEmpty()) {
            return Optional.empty();
        }

        // Search in fetched prices
        List<ExternalMedicinePriceDto> allPrices = fetchPrices();
        String targetNorm = brandName.trim().toLowerCase();
        for (ExternalMedicinePriceDto item : allPrices) {
            if (item.getBrandName() != null && item.getBrandName().trim().toLowerCase().equals(targetNorm)) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    /**
     * Builds HTTP headers securely, injecting Authorization without logging secrets.
     */
    public HttpHeaders buildSafeHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("User-Agent", "MediLink-Healthcare-Platform/2.0");

        String apiKey = properties.getApiKey();
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            String headerName = properties.getApiKeyHeader();
            if (headerName == null || headerName.trim().isEmpty()) {
                headerName = "Authorization";
            }
            String prefix = properties.getApiKeyPrefix();
            String headerVal = (prefix != null && !prefix.trim().isEmpty())
                    ? prefix.trim() + " " + apiKey.trim()
                    : apiKey.trim();
            headers.set(headerName, headerVal);
        }
        return headers;
    }

    public boolean isConfiguredAndValid() {
        if (!properties.isEnabled()) return false;
        String url = properties.getBaseUrl();
        if (url == null || url.trim().isEmpty()) return false;
        String clean = url.trim().toLowerCase();
        return clean.startsWith("http://") || clean.startsWith("https://");
    }

    public boolean isLastCallSuccessful() {
        return lastCallSuccessful;
    }

    public Instant getLastSuccessfulRequestTime() {
        return lastSuccessfulRequestTime;
    }

    public String getLastErrorReason() {
        return lastErrorReason;
    }
}
