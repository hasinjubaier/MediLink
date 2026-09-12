# MediLink 2.0 - Standalone Web Frontend

Decoupled, high-performance Single Page Application (SPA) for the MediLink 2.0 Healthcare & Pharmacy Ecosystem.

---

## Architecture Overview

The MediLink 2.0 frontend is an independent web application decoupled from the Spring Boot backend REST API. It communicates with the backend via HTTP REST endpoints and Server-Sent Events (SSE) for real-time stock sync.

- **Port**: `http://localhost:3000` (configurable)
- **Target Backend API**: `http://localhost:8080` (configured in `config.js`)
- **Technology Stack**: Vanilla JavaScript (ES6+), HTML5, CSS3 Glassmorphism UI, Tesseract.js v5 OCR, Web Audio API / MediaRecorder, Server-Sent Events (SSE).

---

## Directory Structure

```
frontend/
├── config.js          # API Base URL & runtime environment config
├── index.html         # Single Page Application layout & modals
├── style.css          # Responsive design system & animations (~5,750 lines)
├── app.js             # Core client state, controllers, OCR & SSE (~3,850 lines)
├── server.js          # Zero-dependency Node.js HTTP server
├── package.json       # Standard npm scripts
├── flags/             # Country dial-code flag icons
└── README.md          # Frontend documentation
```

---

## Quick Start

### Option 1: Using Node.js (Zero external dependencies)
```bash
# From the project root or frontend directory:
node server.js
```
Or with npm:
```bash
npm start
```
The application will launch immediately at **`http://localhost:3000`**.

### Option 2: Using Any Static Web Server
You can also serve this folder with any static web server of your choice:
- **VS Code Live Server**: Right-click `index.html` -> "Open with Live Server" (`http://127.0.0.1:5500`)
- **Python**: `python -m http.server 3000`
- **Serve**: `npx serve . -l 3000`

---

## Backend API Configuration

By default, the client is configured to connect to `http://localhost:8080`.
To point to a different backend server or cloud deployment, simply edit `config.js`:

```javascript
window.MEDILINK_CONFIG = {
    API_BASE_URL: 'http://your-backend-api-host:8080',
    APP_NAME: 'MediLink 2.0',
    ENVIRONMENT: 'development',
    SSE_RETRY_INTERVAL_MS: 5000
};
```

If hosted behind an API gateway or reverse proxy where the frontend and backend share the same origin, set `API_BASE_URL: ''`.

---

## Features & Capabilities

1. **Multi-Role Authentication & Enterprise RBAC**:
   - Distinct views for Patient, Pharmacist, and Administrator with automated role navigation.
   - Public self-registration for `ADMIN` is strictly disabled.
2. **Admin Dashboard & System Observability**:
   - Live JVM telemetry (used heap MB, CPU processors, system uptime, DB connectivity).
   - Full CRUD over User Directory, Medicine Catalog, and Partner Pharmacy Network.
   - One-click CSV reports export for Users, Medicines, and Pharmacies.
3. **Prescription Scanner & Voice Memos**:
   - Client-side OCR extraction with **Tesseract.js v5**.
   - Voice memo dictation and playback with HTML5 **Web Audio API** / **MediaRecorder**.
4. **Medication Reminders & Smart Pill Tracker**:
   - Interactive daily dose logging ("✓ Take Dose"), 15m snooze, audio chime alarm loop, and 1-click prescription intake schedule sync.
5. **Emergency Pharmacy Geo-Locator**: Real-time Haversine distance calculation and status mapping.
6. **SSE Real-Time Sync**: Live medicine stock alerts and administrative emergency broadcasts pushed through `EventSource`.
7. **Gemini AI Health Assistant**: Intelligent clinical reasoning and conversational medical assistance.
