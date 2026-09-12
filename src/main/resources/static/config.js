/**
 * MediLink 2.0 - Client Configuration
 * Single source of truth for connecting the decoupled frontend SPA to the Spring Boot REST API.
 */
window.MEDILINK_CONFIG = {
    // Spring Boot Backend REST API Base URL (Default port 8080)
    API_BASE_URL: 'http://localhost:8080',

    // Application Metadata
    APP_NAME: 'MediLink 2.0',
    ENVIRONMENT: 'development',
    VERSION: '2.0.0',

    // Real-time Event Stream settings
    SSE_RETRY_INTERVAL_MS: 5000
};
