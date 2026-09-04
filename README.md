# MediLink 2.0: Smart Emergency Medicine & Health Assistant

> **Advanced Object-Oriented Programming (AOOP) Course Project — Spring Boot & PostgreSQL**

---

## 🌟 Overview
**MediLink 2.0** is an enterprise-grade healthcare management and emergency assistant platform tailored for the medicine supply and prescription ecosystem in Bangladesh. It connects patients, licensed pharmacies, and administrators with real-time inventory tracking, intelligent alternative finders, counterfeit detection, and emergency medical dispatch.

---

## 🏗️ Architecture & Technology Stack

- **Framework**: Spring Boot 2.7 (compatible with JDK 8 / 11 / 17 / 21)
- **Build System**: Apache Maven
- **Persistence Layer**: Spring Data JPA with Hibernate ORM
- **Database**: PostgreSQL (`medilink_db`)
- **Real-Time Streaming**: Server-Sent Events (`SseEmitter`)
- **Frontend**: Vanilla JavaScript SPA (served from `src/main/resources/static/`)

---

## 📐 Advanced OOP Design & Architecture

MediLink 2.0 strictly adheres to core OOP principles (Encapsulation, Abstraction, Polymorphism, Inheritance) and implements standard Gang of Four (GoF) design patterns:

### 1. **Factory Pattern**
- **Location**: `com.medilink.model.user.UserFactory`
- **Purpose**: Dynamically creates role-specific `User` instances (`Patient`, `Pharmacist`, `Admin`) with distinct permissions and dashboard capabilities.

### 2. **Observer Pattern**
- **Location**: `com.medilink.model.observer.Subject`, `Notifiable`, `com.medilink.model.pharmacy.PharmacyStock`, `com.medilink.service.StockObserverService`
- **Purpose**: When pharmacy stock changes, all registered observers and SSE stream consumers receive real-time reactive event broadcasts at `/api/events/stream`.

### 3. **Strategy Pattern**
- **Location**: `com.medilink.model.strategy.MedicineSearchStrategy` (`BrandSearchStrategy`, `GenericSearchStrategy`, `PriceSearchStrategy`), `InteractionCheckStrategy` (`StandardWarningStrategy`, `ClinicalStrictStrategy`), `AiChatStrategy` (`GeminiAiStrategy`, `LocalClinicalFallbackStrategy`)
- **Purpose**: Enables interchangeable search algorithms, clinical drug-interaction risk calculation, and AI fallback strategies.

### 4. **State Pattern**
- **Location**: `com.medilink.model.prescription.PrescriptionState` (`UploadedState`, `ExtractedState`, `VerifiedState`)
- **Purpose**: Models the multi-stage clinical prescription lifecycle (`Uploaded` $\rightarrow$ `Extracted` $\rightarrow$ `Verified by Pharmacist` $\rightarrow$ `Dispense Ready`), bridged seamlessly with JPA persistence.

### 5. **Decorator Pattern**
- **Location**: `com.medilink.model.medicine.MedicineBadgeDecorator` (`VerifiedBadgeDecorator`, `LowStockBadgeDecorator`)
- **Purpose**: Dynamically attaches visual metadata and regulatory assurance badges to medicine objects at runtime.

### 6. **Polymorphic JPA Inheritance (JOINED)**
- **Location**: `com.medilink.model.user.User` (`@Inheritance(strategy = InheritanceType.JOINED)`), extended by `Patient`, `Pharmacist`, and `Admin`.
- **Purpose**: Preserves strict object polymorphism and normalized database tables.

### 7. **Singleton & IoC Scoping**
- **Location**: Spring ApplicationContext manages singletons (`@Service`, `@Repository`, `@RestController`), with classic Singleton access preserved for legacy compatibility.

### 8. **Multithreading & Scheduling**
- **Location**: `com.medilink.service.ReminderService` (`@Scheduled`, `@EnableScheduling`)
- **Purpose**: Runs background scheduled intervals checking medication dosage times and notifying active patients.

---

## 🚀 How to Run MediLink 2.0

### Prerequisites:
- Java JDK 8 or higher
- Apache Maven 3.8+ (auto-resolved by scripts)
- PostgreSQL (default port `5433`, database `medilink_db`)

### Quick Run:
Run with PowerShell:
```powershell
.\run.ps1
```
Or with Batch:
```cmd
build_and_run.bat
```
Or directly with Maven:
```powershell
$env:DB_PASSWORD="your_password"
mvn spring-boot:run
```

Open your browser at: **`http://localhost:8080`**

---

## 👥 Demo User Credentials
- **Patient**: `rahim@medilink.com` (Rahim Ahmed) / Password: `patient123`
- **Pharmacist**: `farhan@lazzpharma.com` (Dr. Farhan Kabir, Lazz Pharma) / Password: `pharma123`
- **Administrator**: `admin@medilink.com` (System Administrator) / Password: `admin123`
