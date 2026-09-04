# MediLink: Comprehensive Project Plan
## Smart Emergency Medicine & Health Assistant
### AOOP Course Project (CSE 2118) — Team Robust

---

## Executive Summary

**MediLink** is a patient-focused healthcare assistant designed to solve real medicine-related problems in Bangladesh. The system helps patients interpret prescriptions, find medicines, locate nearby pharmacies, communicate directly with pharmacists, and manage medicine schedules—all through an integrated platform that demonstrates advanced OOP design, multithreading, networking, and real-time data synchronization.

**Project Scope:** 6 core features delivered in 2 phases  
**Team:** Team Robust (4 members)  
**Duration:** 1 semester (approximately 14-16 weeks)  
**Technology Stack:** Java Spring Boot, PostgreSQL, WebSockets, HTML/CSS/JavaScript  

---

## Part 1: Project Discovery & Planning

### 1.1 Problem Statement

#### Current Issues in Bangladesh
- **Prescription Readability:** Handwritten prescriptions are difficult to interpret
- **Medicine Confusion:** Thousands of medicine brands create confusion about generics
- **Emergency Delays:** Finding medicine in emergencies wastes critical time calling multiple pharmacies
- **Lack of Visibility:** No real-time stock information across pharmacies
- **Communication Gap:** No direct channel between patients and pharmacists

#### Target Users
1. **Patients:** Need clarity on prescriptions, alternatives, and pharmacy locations
2. **Pharmacists:** Need efficient stock management and direct patient communication
3. **Administrators:** Need data verification and workflow coordination

### 1.2 Project Objectives

**Primary Goals:**
- Reduce time spent finding medicines during emergencies
- Eliminate prescription misinterpretation
- Provide real-time pharmacy stock visibility
- Enable direct patient-pharmacist communication
- Demonstrate advanced OOP architecture and design patterns

**Success Criteria:**
- ✅ All 6 core features working and demonstrable
- ✅ Object-oriented design clearly visible in code structure
- ✅ Real-time updates functioning via WebSockets
- ✅ Multi-user workflow (Patient, Pharmacist, Admin) fully implemented
- ✅ Clean, well-documented codebase following SOLID principles

### 1.3 Scope Definition: What's In & What's Out

#### INCLUDED (Core 6 Features)
1. **Prescription Scanner** (typed/printed prescriptions; handwriting as documented stretch)
2. **Medicine Alternative Finder** (generic vs. brand search)
3. **Real-Time Pharmacy Stock Updates** (WebSocket Observer pattern)
4. **Live Patient–Pharmacist Chat** (socket-based messaging)
5. **Emergency Medicine Finder** (quick access to available pharmacies)
6. **Fake Medicine Detection** (simulated dataset, QR/barcode scan)

#### STRETCH FEATURES (if time remains)
- AI Health Timeline (pattern detection in medication history)
- Voice notes & prescription uploads
- Drug interaction checker (curated rule set)
- Advanced geolocation services

#### EXPLICITLY OUT OF SCOPE
- Real handwriting OCR (documented as simulated)
- Clinical-grade interaction database
- Real manufacturer verification registry
- Native mobile apps (web frontend only)
- Payment integration
- Insurance processing

---

## Part 2: Technical Architecture & Design

### 2.1 System Architecture (High Level)

```
┌─────────────────────────────────────────────────────────────┐
│                    CLIENT LAYER (Web Frontend)              │
│  HTML/CSS/JavaScript - Patient / Pharmacist / Admin Views   │
└─────────────────────────────────────────────────────────────┘
                           ↕️ (REST + WebSocket)
┌─────────────────────────────────────────────────────────────┐
│                 APPLICATION LAYER (Backend)                 │
│           Java Spring Boot - REST APIs + WebSocket          │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Controllers → Services → Repositories → Domain     │   │
│  │  - User Management (Auth/Authorization)             │   │
│  │  - Medicine Management                              │   │
│  │  - Prescription Processing                          │   │
│  │  - Stock Management                                 │   │
│  │  - Chat/Messaging Service                           │   │
│  │  - Reminder Scheduler (Background Threads)          │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                           ↕️ (JDBC/JPA)
┌─────────────────────────────────────────────────────────────┐
│                    DATABASE LAYER                           │
│  PostgreSQL: Users, Medicines, Prescriptions, Stock, Chat  │
└─────────────────────────────────────────────────────────────┘

Real-Time Layer: WebSocket Server
├── Stock update push (Observer pattern)
├── Live chat messaging
├── Notification service
└── Connected client management
```

### 2.2 Core OOP Design

#### Class Hierarchy

```java
// 1. USER HIERARCHY (Abstract + Concrete)
abstract class User {
    - id: String
    - name: String
    - email: String
    - passwordHash: String
    - createdAt: LocalDateTime
    + abstract getDashboard(): Dashboard
    + abstract getPermissions(): Set<Permission>
}

class Patient extends User {
    - prescriptions: List<Prescription>
    - reminderPreferences: Map<String, Object>
    - emergencyContacts: List<Contact>
    + uploadPrescription(image): Prescription
    + searchMedicine(query, strategy): List<Medicine>
    + findNearbyPharmacies(location): List<Pharmacy>
    + initiateChat(pharmacistId): ChatSession
}

class Pharmacist extends User {
    - managedPharmacy: Pharmacy
    - inventory: PharmacyStock
    + updateStock(medicineId, quantity): void
    + respondToPatient(patientId, message): ChatMessage
    + verifyMedicine(code): VerificationResult
}

class Admin extends User {
    - permissions: Set<AdminPermission>
    + verifyMedicineData(medicine): void
    + manageUsers(action): void
    + generateReport(type): Report
}

// 2. DOMAIN CLASSES
class Medicine {
    - id: String
    - genericName: String
    - brandNames: Set<String>
    - company: String
    - strength: String
    - formulation: String
    - price: BigDecimal
    - imageUrl: String
    + getAlternatives(): List<Medicine>
    + getInteractions(other: Medicine): InteractionResult
}

class Prescription {
    - id: String
    - patientId: String
    - uploadedImage: File
    - extractedItems: List<PrescriptionItem>
    - status: PrescriptionStatus (Enum)
    - extractedAt: LocalDateTime
    + extract(): List<PrescriptionItem>
    + verify(): Boolean
}

class PrescriptionItem {
    - medicineId: String
    - dosage: String
    - frequency: String
    - duration: String
    - instructions: String
}

class PharmacyStock {
    - pharmacyId: String
    - medicineId: String
    - quantity: Integer
    - lastUpdated: LocalDateTime
    - observers: Set<Notifiable>
    + updateQuantity(newQuantity): void
    + notifyObservers(): void
}

class ChatMessage {
    - id: String
    - senderId: String
    - receiverId: String
    - content: String
    - messageType: MessageType (TEXT, VOICE, IMAGE)
    - timestamp: LocalDateTime
    - isRead: Boolean
}

class Reminder {
    - id: String
    - patientId: String
    - medicineId: String
    - scheduledTime: LocalTime
    - frequency: Frequency (ONCE, DAILY, WEEKLY)
    - duration: Integer
    - isActive: Boolean
    + execute(): void
}

class Pharmacy {
    - id: String
    - name: String
    - address: String
    - location: GeoPoint
    - stock: PharmacyStock
    - operatingHours: Map<String, TimeRange>
    + getDistance(userLocation): Double
    + hasAvailable(medicineId): Boolean
}

class HealthTimelineEntry {
    - id: String
    - patientId: String
    - date: LocalDate
    - medicineId: String
    - type: TimelineEventType
    - notes: String
    + buildTimeline(): HealthTimeline
}
```

#### Design Patterns Implementation

| Pattern | Location | Purpose | Implementation |
|---------|----------|---------|-----------------|
| **Observer** | Real-time Stock Updates | Notify subscribed patients when stock changes | `PharmacyStock` (subject) notifies `Notifiable` observers via WebSocket |
| **Factory** | User Registration | Create correct user type (Patient/Pharmacist/Admin) | `UserFactory.create(email, type, data)` |
| **Strategy** | Medicine Search | Switch between brand, generic, or price-based search | `MedicineSearchStrategy` interface with 3 implementations |
| **Singleton** | Database & Socket Server | Ensure single connection pool and server instance | `DatabaseManager.getInstance()`, `WebSocketServer.getInstance()` |
| **State** | Prescription Workflow | Model multi-step prescription states | `PrescriptionStatus` enum: `Uploaded → Extracted → Verified → Archived` |
| **Decorator** | Medicine Badges | Add "Verified", "Low Stock", "Suspicious" labels | Wrap `Medicine` with display attributes |

### 2.3 Database Schema (Key Tables)

```sql
-- Users Table (with role discrimination)
users (
  id UUID PRIMARY KEY,
  email VARCHAR UNIQUE NOT NULL,
  password_hash VARCHAR NOT NULL,
  role ENUM('PATIENT', 'PHARMACIST', 'ADMIN'),
  profile_data JSONB,
  created_at TIMESTAMP
)

-- Medicines Table
medicines (
  id UUID PRIMARY KEY,
  generic_name VARCHAR NOT NULL,
  brand_name VARCHAR NOT NULL,
  company VARCHAR,
  strength VARCHAR,
  formulation VARCHAR,
  price DECIMAL,
  created_at TIMESTAMP
)

-- Prescriptions Table
prescriptions (
  id UUID PRIMARY KEY,
  patient_id UUID REFERENCES users,
  image_url VARCHAR,
  status VARCHAR,
  created_at TIMESTAMP,
  extracted_at TIMESTAMP
)

-- Prescription Items
prescription_items (
  id UUID PRIMARY KEY,
  prescription_id UUID REFERENCES prescriptions,
  medicine_id UUID REFERENCES medicines,
  dosage VARCHAR,
  frequency VARCHAR,
  duration VARCHAR
)

-- Pharmacy Stock (Real-time Updates)
pharmacy_stock (
  id UUID PRIMARY KEY,
  pharmacy_id UUID REFERENCES pharmacies,
  medicine_id UUID REFERENCES medicines,
  quantity INTEGER,
  last_updated TIMESTAMP
)

-- Chat Messages (Real-time)
chat_messages (
  id UUID PRIMARY KEY,
  sender_id UUID REFERENCES users,
  receiver_id UUID REFERENCES users,
  content TEXT,
  message_type VARCHAR,
  created_at TIMESTAMP
)

-- Reminders (Scheduled)
reminders (
  id UUID PRIMARY KEY,
  patient_id UUID REFERENCES users,
  medicine_id UUID REFERENCES medicines,
  scheduled_time TIME,
  frequency VARCHAR,
  is_active BOOLEAN
)
```

---

## Part 3: Project Phases & Timeline

### Phase 1: Architecture & Foundation (Weeks 1-3)
**Goal:** Establish solid OOP structure and authentication

#### Deliverables
- ✅ Project setup (Spring Boot, PostgreSQL, Maven/Gradle)
- ✅ User hierarchy implemented (Abstract User, Patient, Pharmacist, Admin)
- ✅ Authentication & authorization (JWT tokens, role-based access)
- ✅ Database schema created
- ✅ Base REST API endpoints for user operations
- ✅ Class diagram & OOP design documentation
- ✅ WebSocket server skeleton

#### Tasks
1. Set up Spring Boot project with required dependencies
2. Configure PostgreSQL and create schema
3. Implement `User` abstract class and concrete subclasses
4. Create `UserController` with registration/login endpoints
5. Implement JWT authentication filter
6. Set up Spring Security role-based access control
7. Create basic entity classes (Medicine, Prescription, Pharmacy)
8. Write unit tests for authentication flow
9. Document class hierarchy and design patterns

#### Team Distribution
- **Dev 1:** Spring Boot setup, database, schema
- **Dev 2:** User entity hierarchy, authentication
- **Dev 3:** API endpoints, controllers
- **Dev 4:** Testing, documentation

**Checkpoint Deliverable:** Working authentication + class diagram

---

### Phase 2: Core Features (Weeks 4-9)
**Goal:** Implement 4 main features with working demos

#### Feature 1: Prescription Scanner (Weeks 4-5)
**Objective:** Upload and extract prescription data

**Technical Stack:**
- Tesseract OCR (printed text) or file upload with manual entry (simulated handwriting)
- Spring File Upload handling
- ImageProcessing library

**Implementation Details:**
```
Patient Flow:
1. Upload prescription image → /api/prescriptions/upload
2. Backend: Process image with OCR or accept manual entry
3. Extract medicine details → Parse prescription items
4. Display extracted data → Show alternatives via Medicine Alternative Finder
```

**OOP Application:**
- `PrescriptionService` uses `Strategy` pattern for OCR vs. manual extraction
- `PrescriptionState` enum manages workflow (Uploaded → Extracted → Verified)
- File handling via `FileService` singleton

**Deliverables:**
- REST endpoint: `POST /api/prescriptions/upload`
- OCR/extraction logic working for printed text
- Prescription entity persisted to database
- UI form for manual entry as fallback

---

#### Feature 2: Medicine Alternative Finder (Weeks 5-6)
**Objective:** Search by generic name and show brand alternatives

**Technical Stack:**
- Database query optimization (search by generic, brand, price)
- Full-text search (if using PostgreSQL extensions)

**Implementation Details:**
```
Search Strategies:
1. Brand Search: "Napa" → Shows Paracetamol alternatives (Ace, Reset, DP)
2. Generic Search: "Paracetamol" → Shows all brands + prices
3. Price Search: Find cheapest Paracetamol option
```

**OOP Application:**
- `MedicineSearchStrategy` interface with 3 implementations
- `MedicineService.search(query, strategy)` method
- Decorator pattern for medicine badges ("Verified", "Low Stock")

**Deliverables:**
- REST endpoint: `GET /api/medicines/search?query=...&strategy=...`
- Search logic using Strategy pattern
- UI dropdown to switch search strategies
- Comparison table showing brands + prices

---

#### Feature 3: Real-Time Pharmacy Stock Updates (Weeks 6-8)
**Objective:** Push stock changes to patients via WebSocket (Observer pattern)

**Technical Stack:**
- Spring WebSocket API
- SockJS (fallback for non-WebSocket browsers)
- STOMP messaging protocol

**Architecture:**
```
Pharmacist Updates Stock
→ PharmacyStock.updateQuantity()
→ notifyObservers() (Observer Pattern)
→ WebSocket Server broadcasts to subscribed patients
→ Patient sees real-time update (no page refresh)
```

**OOP Application:**
- `PharmacyStock` as Observable subject
- `Notifiable` interface for observers (patient sessions, chat clients)
- `StockUpdateEvent` for event propagation
- `WebSocketHandler` manages client subscriptions

**Deliverables:**
- WebSocket endpoint: `/ws/stock-updates`
- Pharmacist stock update API: `PUT /api/pharmacies/{id}/stock`
- Client-side JavaScript handling real-time updates
- UI showing live stock status with notifications
- Unit tests for observer notification

**Code Skeleton:**
```java
@RestController
@RequestMapping("/api/pharmacies")
public class PharmacyStockController {
    @PutMapping("/{id}/stock")
    public ResponseEntity<?> updateStock(
        @PathVariable String id,
        @RequestBody StockUpdateRequest request
    ) {
        PharmacyStock stock = stockService.findByPharmacyAndMedicine(id, request.getMedicineId());
        stock.updateQuantity(request.getQuantity());
        // Observer pattern: notifies all subscribed patients
        stock.notifyObservers(new StockUpdateEvent(stock));
        return ResponseEntity.ok("Updated");
    }
}

// Observer Implementation
@Component
public class WebSocketNotifier implements StockObserver {
    @Override
    public void update(StockUpdateEvent event) {
        webSocketMessagingTemplate.convertAndSend(
            "/topic/stock/" + event.getPharmacyId(),
            event
        );
    }
}
```

---

#### Feature 4: Live Patient–Pharmacist Chat (Weeks 7-8)
**Objective:** Real-time 1-on-1 messaging

**Technical Stack:**
- WebSocket for messaging
- Message persistence (PostgreSQL)
- Notification triggers

**Implementation Details:**
```
Patient initiates chat → Creates ChatSession
Message flow:
1. Patient sends message → WebSocket to server
2. Server persists to database
3. Server broadcasts to pharmacist (if online)
4. Offline delivery: stored, sent when pharmacist comes online
```

**OOP Application:**
- `ChatMessage` entity with multiple message types (TEXT, VOICE, IMAGE)
- `ChatSession` state management
- `ChatNotificationService` for offline message handling

**Deliverables:**
- WebSocket endpoint: `/ws/chat`
- Chat API: `POST /api/chat/messages`, `GET /api/chat/history`
- Message persistence
- UI chat interface with typing indicators
- Read/unread status tracking

---

### Phase 3: Advanced Features & Polish (Weeks 9-12)
**Goal:** Complete remaining features and optimize

#### Feature 5: Emergency Medicine Finder (Weeks 9-10)
**Objective:** Quick access to urgent medicines at nearby open pharmacies

**Implementation:**
```
Emergency Mode Activated
→ Find pharmacies open now (geolocation)
→ Check if emergency medicine available (Insulin, Aspirin, etc.)
→ Show 3 nearest options
→ Direct connect to pharmacist
```

**OOP Application:**
- State pattern: `EmergencyModeState` (Idle → Searching → Connected)
- Geolocation service integration
- Priority queue for distance-sorted results

---

#### Feature 6: Fake Medicine Detection (Weeks 10-11)
**Objective:** Scan QR/barcode and verify against simulated manufacturer dataset

**Implementation:**
```
User scans QR code
→ Extract batch number, manufacturer
→ Check against sample database
→ Return: Authentic ✓ or Suspicious ⚠️
```

**OOP Application:**
- `MedicineVerificationStrategy` for different scan types
- Decorator pattern: Add "Verified" or "Suspicious" badge to medicine

**Deliverables:**
- QR code scanning library integration
- Verification API: `GET /api/medicines/verify/{code}`
- Sample dataset of 50-100 medicines (simulated)
- UI showing verification badge

---

### Phase 4: Testing, Documentation & Demo Prep (Weeks 12-14)
**Goal:** Full integration testing and demo readiness

#### Tasks
1. Integration testing across all features
2. Load testing for WebSocket connections
3. Security review (SQL injection, XSS, CSRF)
4. UI/UX refinement
5. Final documentation and README
6. Demo script and walkthrough
7. Performance optimization

#### Deliverables
- ✅ Full test suite (unit + integration)
- ✅ Performance benchmarks
- ✅ Complete API documentation (Swagger/OpenAPI)
- ✅ Architecture documentation
- ✅ User manual and admin guide
- ✅ Demo video/script
- ✅ Deployment instructions

---

## Part 4: Landing Page & User Onboarding

### 4.1 Landing Page Structure

```
homepage/
├── Hero Section
│   ├── Tagline: "Smart Medicine. Instant Access."
│   ├── Call-to-action: [Get Started]
│   └── Demo video (1 min)
├── Problem & Solution Section
│   ├── "The Problem" (3 bullets)
│   ├── "How MediLink Solves It" (visual)
│   └── Feature highlights (4-6 features)
├── How It Works Section
│   ├── Patient flow (4 steps)
│   ├── Pharmacist flow (3 steps)
│   └── Admin dashboard overview
├── Feature Showcase
│   ├── Prescription Scanner
│   ├── Real-time Stock Updates
│   ├── Emergency Finder
│   ├── Live Chat
│   └── (etc.)
├── Social Proof / Comparison
│   ├── Feature table vs. competitors
│   └── Testimonials (if available)
├── Pricing / Access Info
│   └── "Free for students during demo period"
└── Call-to-Action Footer
    ├── Sign up (Patient)
    ├── Register Pharmacy (Pharmacist)
    └── Contact us (Admin)
```

### 4.2 User Registration Flows

#### Patient Registration
```
1. Email & Password
2. Personal info (name, date of birth, location)
3. Emergency contacts
4. Medical history (optional)
5. Dashboard setup
→ Ready to upload first prescription
```

#### Pharmacist Registration
```
1. Email & Password
2. Pharmacy info (name, address, license number)
3. Operating hours
4. Stock initial setup
5. Verification by admin
→ Activate after admin review
```

#### Admin Registration
```
1. System creates admin account (CLI or manual)
2. Login with credentials
3. Dashboard to verify pharmacists and medicines
```

### 4.3 Onboarding Screens

**Patient Onboarding (Post-Registration):**
1. "Welcome to MediLink!" - 30 second intro
2. "Upload your first prescription" - Screenshot guide
3. "Find medicines nearby" - Map view tutorial
4. "Chat with pharmacists" - Chat interface intro
5. "Set medicine reminders" - Scheduler walkthrough

**Pharmacist Onboarding:**
1. "Manage your pharmacy" - Dashboard overview
2. "Update stock in real-time" - Stock update demo
3. "Respond to patient inquiries" - Chat incoming demo
4. "View patient history" - Patient data access

---

## Part 5: Development Workflow & Collaboration

### 5.1 Git Workflow

```
main (production-ready)
  ↑
release/v1.0 (QA & testing)
  ↑
develop (integration branch)
  ↑
feature branches:
  ├── feature/user-authentication
  ├── feature/prescription-scanner
  ├── feature/medicine-finder
  ├── feature/real-time-stock
  ├── feature/live-chat
  └── feature/emergency-finder
```

### 5.2 Code Organization

```
medilink-backend/
├── src/main/java/com/medilink/
│   ├── config/          # Spring configs, WebSocket, Security
│   ├── controller/      # REST & WebSocket controllers
│   ├── service/         # Business logic (Observer, Strategy, etc.)
│   ├── repository/      # Database access (JPA/Spring Data)
│   ├── entity/          # Domain classes (User, Medicine, etc.)
│   ├── dto/            # Data Transfer Objects
│   ├── exception/      # Custom exceptions
│   ├── util/           # Utilities
│   └── MediLinkApplication.java
├── src/test/java/      # Unit & integration tests
├── src/main/resources/
│   ├── application.properties
│   ├── schema.sql
│   └── data.sql
├── pom.xml
└── README.md

medilink-frontend/
├── index.html
├── css/
│   ├── style.css
│   └── responsive.css
├── js/
│   ├── app.js
│   ├── auth.js
│   ├── websocket-client.js
│   ├── prescription.js
│   ├── medicine-search.js
│   ├── stock-updates.js
│   └── chat.js
├── pages/
│   ├── landing.html
│   ├── dashboard.html
│   ├── prescription-upload.html
│   ├── medicine-finder.html
│   ├── emergency.html
│   └── chat.html
└── assets/           # Images, icons
```

### 5.3 Team Responsibilities

| Member | Primary Role | Responsibilities |
|--------|--------------|------------------|
| Dev 1 | Backend Lead | Spring Boot setup, user management, auth, database design |
| Dev 2 | Full Stack | Prescription scanner, medicine finder, API integration |
| Dev 3 | Real-Time Dev | WebSocket server, observer pattern, stock updates, chat |
| Dev 4 | Frontend + Testing | UI/UX, integration testing, documentation, demo prep |

### 5.4 Code Review & Quality Standards

- **Code Style:** Google Java Style Guide
- **Unit Test Coverage:** Minimum 80% for business logic
- **API Documentation:** Swagger/OpenAPI auto-generated
- **Pull Request Checklist:**
  - [ ] Tests written and passing
  - [ ] Code reviewed by 1+ teammate
  - [ ] Documentation updated
  - [ ] No breaking changes to schema
  - [ ] Performance impact considered

---

## Part 6: Testing Strategy

### 6.1 Unit Testing (Per Feature)

**Example: Medicine Alternative Finder**
```java
@SpringBootTest
class MedicineSearchServiceTests {
    
    @Test
    void testBrandSearch_ReturnsMedicineByBrand() {
        // Given
        MedicineSearchStrategy strategy = new BrandSearchStrategy();
        String query = "Napa";
        
        // When
        List<Medicine> results = medicineService.search(query, strategy);
        
        // Then
        assertThat(results).extracting("brandName").contains("Napa");
    }
    
    @Test
    void testGenericSearch_ReturnsMedicineAndAlternatives() {
        // Given
        MedicineSearchStrategy strategy = new GenericSearchStrategy();
        String query = "Paracetamol";
        
        // When
        List<Medicine> results = medicineService.search(query, strategy);
        
        // Then
        assertThat(results).hasSize(greaterThan(1));
        assertThat(results).allMatch(m -> "Paracetamol".equals(m.getGenericName()));
    }
}
```

### 6.2 Integration Testing (Feature-to-Feature)

**Example: Prescription → Medicine Finder Flow**
```java
@SpringBootTest
@AutoConfigureMockMvc
class PrescriptionMedicineIntegrationTests {
    
    @Test
    void testPrescriptionUpload_ThenFindAlternatives() throws Exception {
        // 1. Patient uploads prescription
        MockMultipartFile file = new MockMultipartFile("file", "prescription.jpg", ...);
        MvcResult uploadResult = mockMvc.perform(
            multipart("/api/prescriptions/upload").file(file)
        ).andReturn();
        
        String prescriptionId = extractPrescriptionId(uploadResult);
        
        // 2. Verify medicines extracted
        mockMvc.perform(get("/api/prescriptions/" + prescriptionId))
            .andExpect(jsonPath("$.items[0].medicineName").exists());
        
        // 3. Search for alternatives
        mockMvc.perform(get("/api/medicines/search?query=Paracetamol"))
            .andExpect(jsonPath("$[*].brandName").isArray())
            .andExpect(status().isOk());
    }
}
```

### 6.3 WebSocket Testing

```java
@SpringBootTest
class WebSocketStockUpdateTests {
    
    @Test
    void testStockUpdate_NotifiesSubscribedClients() throws Exception {
        // Set up WebSocket connection
        StompSession session = stompClient.connect(
            wsUrl, new StompSessionHandlerAdapter() {}
        ).get();
        
        // Subscribe to stock updates
        List<String> received = new ArrayList<>();
        session.subscribe("/topic/stock/pharmacy1", 
            new StompFrameHandler() {
                @Override
                public Object handleFrame(StompHeaders headers, Object payload) {
                    received.add((String) payload);
                    return null;
                }
            });
        
        // Pharmacist updates stock
        pharmacyStockService.updateQuantity("med1", 50);
        
        // Verify notification received
        Thread.sleep(1000);
        assertThat(received).isNotEmpty();
    }
}
```

### 6.4 Performance Testing

**Load Test (simulate 100 concurrent users):**
```
- 50 patients searching medicines simultaneously
- 20 patients viewing real-time stock updates
- 30 chat messages per second
Target: All responses < 500ms, 0 dropped connections
```

---

## Part 7: Deployment & Launch

### 7.1 Deployment Architecture

```
Development         → Staging          → Production
(Local)              (Test Server)      (Live Server)

├── Database         ├── Database       ├── PostgreSQL Cluster
│ SQLite/Local       │ PostgreSQL        │ (RDS or managed)
│                    │                   │
├── Backend          ├── Backend        ├── Java App
│ Spring Boot Dev    │ Spring Boot      │ (Docker container)
│                    │                   │
├── Frontend         ├── Frontend       ├── Static hosting
│ npm dev server     │ nginx            │ (S3 + CloudFront)
│                    │                   │
└── WebSocket        └── WebSocket      └── Load balancer
  localhost:8080       staging.ws        ws.medilink.app
```

### 7.2 Deployment Checklist

- [ ] Environment variables configured (DB URL, JWT secret, etc.)
- [ ] Database migrations applied
- [ ] SSL certificates installed
- [ ] CORS configured for production domain
- [ ] Logging aggregation set up (ELK stack or CloudWatch)
- [ ] Backup strategy in place
- [ ] Rollback plan documented
- [ ] Monitoring alerts configured

### 7.3 Launch Timeline

**Week 1-2:** Demo to instructors (in-class presentation)
**Week 3:** Minor fixes based on feedback
**Week 4:** Submit final code + documentation

---

## Part 8: Success Metrics & Acceptance Criteria

### Functional Acceptance Criteria

| Feature | Acceptance Criteria |
|---------|-------------------|
| **User Auth** | ✅ Patient/Pharmacist/Admin roles separate; JWT tokens work; password hashing in place |
| **Prescription Scanner** | ✅ Upload image → Extract medicine name & dosage; display alternatives automatically |
| **Medicine Finder** | ✅ Search by brand/generic; show 5+ alternatives with prices |
| **Real-time Stock** | ✅ Pharmacist updates stock → Patient notification arrives in < 1 second (no refresh) |
| **Live Chat** | ✅ Send/receive messages; offline delivery; typing indicators |
| **Emergency Mode** | ✅ Activate → Show 3 nearest open pharmacies with available medicine |
| **Fake Medicine Detection** | ✅ Scan QR code → Show "Authentic" or "Suspicious" badge |

### Non-Functional Criteria

| Criteria | Target |
|----------|--------|
| **Response Time** | API responses < 500ms (95th percentile) |
| **WebSocket Latency** | Stock/chat updates < 200ms |
| **Uptime** | 99% during demo period |
| **Test Coverage** | ≥ 80% for business logic |
| **Code Quality** | SonarQube rating ≥ B |
| **Documentation** | All public methods have JavaDoc; API documented via Swagger |

### Demo Script (10-15 minutes)

1. **(0-1 min)** Welcome & problem overview
2. **(1-3 min)** Patient registration and landing page tour
3. **(3-5 min)** Upload prescription → Show extracted medicines → Find alternatives
4. **(5-7 min)** Real-time stock demo (2 browsers: Pharmacist updates, Patient sees live update)
5. **(7-9 min)** Emergency mode walkthrough
6. **(9-11 min)** Live chat between patient and pharmacist
7. **(11-13 min)** Admin dashboard showing data verification
8. **(13-15 min)** Q&A

---

## Part 9: Risk Management & Mitigation

| Risk | Severity | Probability | Mitigation |
|------|----------|-------------|-----------|
| Scope creep (too many features) | HIGH | HIGH | Agreed 6-feature limit; use change control process |
| WebSocket scaling issues | MEDIUM | MEDIUM | Load test early; use message queue (RabbitMQ) if needed |
| Database performance | MEDIUM | MEDIUM | Index foreign keys; monitor query performance weekly |
| Handwriting OCR accuracy | MEDIUM | LOW | Scope it as documented stretch; use typed entries for demo |
| Team coordination delays | MEDIUM | MEDIUM | Weekly standups; clear git workflow; code review SLA (24 hrs) |
| Integration bugs in final week | MEDIUM | HIGH | Weekly integration testing; CI/CD pipeline automated |

---

## Part 10: Documentation Deliverables

### Documents to Produce

1. **Technical Architecture Document**
   - System design diagrams
   - Database schema with ER diagram
   - API specification (Swagger/OpenAPI)
   - Deployment architecture

2. **Design Patterns & OOP Analysis**
   - Class hierarchy diagram
   - Pattern usage explanation
   - Design decisions rationale

3. **User Manual**
   - Patient guide (with screenshots)
   - Pharmacist guide (with screenshots)
   - Admin guide

4. **Developer Guide**
   - Setup instructions
   - Code organization
   - Contribution guidelines
   - Running tests & builds

5. **API Documentation**
   - Auto-generated Swagger UI
   - WebSocket protocol documentation
   - Example cURL requests for each endpoint

6. **Deployment Guide**
   - Environment setup
   - Database migration steps
   - Running on different platforms
   - Troubleshooting

---

## Conclusion

MediLink is a technically ambitious yet achievable project that solves real healthcare problems in Bangladesh while demonstrating core AOOP concepts: inheritance, polymorphism, design patterns, multithreading, networking, and database design.

By following this phased plan, Team Robust can deliver a polished, demoable system that showcases professional software engineering practices—clean OOP design, thorough testing, and clear documentation—within a realistic semester timeline.

**Key to Success:**
- ✅ Stick to the 6-core-feature scope
- ✅ Complete architecture phase thoroughly (weeks 1-3)
- ✅ Test and integrate continuously (not at the end)
- ✅ Document as you build (not after)
- ✅ Demo early and often to instructors

**Good luck, Team Robust!** 🚀

---

*Generated: August 2026*  
*Project: MediLink AI (AOOP Course CSE 2118)*  
*Team: Robust (4 members)*
