-- ============================================================================
-- MediLink 2.0 - Complete PostgreSQL Database Schema & Seed Dataset
-- ============================================================================

-- 1. Create Database (Run separately if executing within an existing session)
-- CREATE DATABASE medilink_db;
-- \c medilink_db;

-- Drop existing tables to allow clean re-runs
DROP TABLE IF EXISTS support_tickets CASCADE;
DROP TABLE IF EXISTS fake_medicine_verifications CASCADE;
DROP TABLE IF EXISTS chat_messages CASCADE;
DROP TABLE IF EXISTS reminders CASCADE;
DROP TABLE IF EXISTS pharmacy_inventory CASCADE;
DROP TABLE IF EXISTS pharmacies CASCADE;
DROP TABLE IF EXISTS prescription_items CASCADE;
DROP TABLE IF EXISTS prescriptions CASCADE;
DROP TABLE IF EXISTS medicines CASCADE;
DROP TABLE IF EXISTS generics CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- ============================================================================
-- 1. Users Table
-- ============================================================================
CREATE TABLE users (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL CHECK (role IN ('PATIENT', 'PHARMACIST', 'ADMIN', 'DOCTOR')),
    phone VARCHAR(30),
    avatar_emoji VARCHAR(10) DEFAULT '👤',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- 2. Generics & Medicines Tables
-- ============================================================================
CREATE TABLE generics (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    therapeutic_class VARCHAR(100),
    description TEXT,
    side_effects TEXT
);

CREATE TABLE medicines (
    id VARCHAR(50) PRIMARY KEY,
    brand_name VARCHAR(100) NOT NULL,
    generic_name VARCHAR(100) NOT NULL,
    manufacturer VARCHAR(100) NOT NULL,
    dosage_form VARCHAR(50) NOT NULL,
    strength VARCHAR(50) NOT NULL,
    unit_price NUMERIC(10, 2) NOT NULL,
    is_prescription_required BOOLEAN DEFAULT FALSE,
    verified_genuine BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- 3. Prescriptions & Prescription Items Tables
-- ============================================================================
CREATE TABLE prescriptions (
    id VARCHAR(50) PRIMARY KEY,
    patient_id VARCHAR(50) REFERENCES users(id) ON DELETE CASCADE,
    patient_name VARCHAR(100) NOT NULL,
    doctor_name VARCHAR(100) NOT NULL,
    hospital VARCHAR(150),
    raw_scan_text TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'UPLOADED',
    is_dispense_ready BOOLEAN DEFAULT FALSE,
    verified_by_pharmacist_id VARCHAR(50),
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE prescription_items (
    id SERIAL PRIMARY KEY,
    prescription_id VARCHAR(50) REFERENCES prescriptions(id) ON DELETE CASCADE,
    medicine_name VARCHAR(100) NOT NULL,
    dosage VARCHAR(50) NOT NULL,
    frequency VARCHAR(50) NOT NULL,
    instructions TEXT
);

-- ============================================================================
-- 4. Pharmacies & Live Stock Inventory Tables
-- ============================================================================
CREATE TABLE pharmacies (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(200) NOT NULL,
    phone VARCHAR(30),
    latitude NUMERIC(9, 6),
    longitude NUMERIC(9, 6),
    is_24_hours BOOLEAN DEFAULT TRUE
);

CREATE TABLE pharmacy_inventory (
    id SERIAL PRIMARY KEY,
    pharmacy_id VARCHAR(50) REFERENCES pharmacies(id) ON DELETE CASCADE,
    medicine_id VARCHAR(50) REFERENCES medicines(id) ON DELETE CASCADE,
    medicine_name VARCHAR(100) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    unit_price NUMERIC(10, 2) NOT NULL,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(pharmacy_id, medicine_id)
);

-- ============================================================================
-- 5. Medication Reminders Table
-- ============================================================================
CREATE TABLE reminders (
    id VARCHAR(50) PRIMARY KEY,
    patient_email VARCHAR(100) NOT NULL,
    medicine_name VARCHAR(100) NOT NULL,
    dosage VARCHAR(50) NOT NULL,
    reminder_time VARCHAR(20) NOT NULL,
    days_of_week VARCHAR(100) DEFAULT 'Daily',
    instructions VARCHAR(200),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- 6. Live Chat Messages Table
-- ============================================================================
CREATE TABLE chat_messages (
    id SERIAL PRIMARY KEY,
    sender_id VARCHAR(50) NOT NULL,
    sender_name VARCHAR(100) NOT NULL,
    sender_role VARCHAR(30) NOT NULL,
    receiver_id VARCHAR(50) NOT NULL,
    message_text TEXT NOT NULL,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- 7. Fake Medicine Verifier Table
-- ============================================================================
CREATE TABLE fake_medicine_verifications (
    id SERIAL PRIMARY KEY,
    batch_number VARCHAR(100) NOT NULL,
    medicine_name VARCHAR(100) NOT NULL,
    manufacturer VARCHAR(100) NOT NULL,
    security_hash VARCHAR(100) NOT NULL,
    is_authentic BOOLEAN NOT NULL,
    verification_notes TEXT,
    verified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- 8. Support Tickets & Help Center Table
-- ============================================================================
CREATE TABLE support_tickets (
    id VARCHAR(50) PRIMARY KEY,
    requester_name VARCHAR(100) NOT NULL,
    requester_email VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    severity VARCHAR(30) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(30) DEFAULT 'OPEN',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- SEED DATASET INSERTIONS
-- ============================================================================

-- Users
INSERT INTO users (id, name, email, password_hash, role, phone, avatar_emoji) VALUES
('usr_pat_01', 'Rahim Ahmed', 'rahim@medilink.com', 'patient123', 'PATIENT', '+880 1711-223344', '👤'),
('usr_pat_02', 'Fatima Begum', 'fatima@medilink.com', 'patient123', 'PATIENT', '+880 1819-334455', '👩'),
('usr_pharma_01', 'Tariq Islam (B.Pharm)', 'tariq@medilink.com', 'pharma123', 'PHARMACIST', '+880 1912-889900', '💊'),
('usr_pharma_02', 'Dr. Sumaiya Khan', 'sumaiya@medilink.com', 'pharma123', 'PHARMACIST', '+880 1515-447788', '🩺'),
('usr_admin_01', 'Admin MediLink', 'admin@medilink.com', 'admin123', 'ADMIN', '+880 1313-001122', '🛡️');

-- Generics
INSERT INTO generics (id, name, therapeutic_class, description, side_effects) VALUES
('gen_01', 'Paracetamol', 'Analgesic & Antipyretic', 'Effective fever reducer and analgesic for mild-to-moderate pain.', 'Rare; mild nausea or liver stress at high doses.'),
('gen_02', 'Omeprazole', 'Proton Pump Inhibitor (PPI)', 'Reduces stomach gastric acid production for GERD and peptic ulcer.', 'Headache, mild abdominal pain, constipation.'),
('gen_03', 'Esomeprazole', 'Proton Pump Inhibitor (PPI)', 'Potent S-isomer PPI for acid reflux, GERD and erosive esophagitis.', 'Dry mouth, headache, flatulence.'),
('gen_04', 'Fexofenadine', 'Second-Generation Antihistamine', 'Non-sedating antihistamine for allergic rhinitis and chronic urticaria.', 'Mild drowsiness, fatigue, headache.'),
('gen_05', 'Azithromycin', 'Macrolide Antibiotic', 'Broad spectrum antibiotic for respiratory tract and skin infections.', 'Nausea, diarrhea, abdominal cramps.'),
('gen_06', 'Montelukast', 'Leukotriene Receptor Antagonist', 'Prevents asthma symptoms and manages seasonal allergic rhinitis.', 'Upper respiratory infection, fever, headache.'),
('gen_07', 'Lisinopril', 'ACE Inhibitor', 'Manages essential hypertension and protects cardiac function post-MI.', 'Dry cough, dizziness, elevated potassium.');

-- Medicines
INSERT INTO medicines (id, brand_name, generic_name, manufacturer, dosage_form, strength, unit_price, is_prescription_required, verified_genuine) VALUES
('med_01', 'Napa Extra', 'Paracetamol + Caffeine', 'Beximco Pharmaceuticals Ltd.', 'Tablet', '500mg+65mg', 2.50, FALSE, TRUE),
('med_02', 'Napa', 'Paracetamol', 'Beximco Pharmaceuticals Ltd.', 'Tablet', '500mg', 1.20, FALSE, TRUE),
('med_03', 'Ace Plus', 'Paracetamol + Caffeine', 'Square Pharmaceuticals PLC', 'Tablet', '500mg+65mg', 2.50, FALSE, TRUE),
('med_04', 'Seclo 20', 'Omeprazole', 'Square Pharmaceuticals PLC', 'Capsule', '20mg', 6.00, FALSE, TRUE),
('med_05', 'Sergel 20', 'Esomeprazole', 'Healthcare Pharmaceuticals Ltd.', 'Capsule', '20mg', 8.00, FALSE, TRUE),
('med_06', 'Maxpro 20', 'Esomeprazole', 'Renata Limited', 'Capsule', '20mg', 8.00, FALSE, TRUE),
('med_07', 'Fexo 120', 'Fexofenadine', 'Square Pharmaceuticals PLC', 'Tablet', '120mg', 9.50, FALSE, TRUE),
('med_08', 'Alatrol', 'Cetirizine', 'Square Pharmaceuticals PLC', 'Tablet', '10mg', 3.50, FALSE, TRUE),
('med_09', 'Azithrocin 500', 'Azithromycin', 'Beximco Pharmaceuticals Ltd.', 'Tablet', '500mg', 35.00, TRUE, TRUE),
('med_10', 'Monas 10', 'Montelukast', 'Acme Laboratories Ltd.', 'Tablet', '10mg', 17.50, TRUE, TRUE),
('med_11', 'Lisinopril 10', 'Lisinopril', 'Square Pharmaceuticals PLC', 'Tablet', '10mg', 7.00, TRUE, TRUE),
('med_12', 'Calbo D', 'Calcium + Vit D3', 'Square Pharmaceuticals PLC', 'Tablet', '500mg+200IU', 6.50, FALSE, TRUE);

-- Prescriptions
INSERT INTO prescriptions (id, patient_id, patient_name, doctor_name, hospital, raw_scan_text, status, is_dispense_ready, verified_by_pharmacist_id) VALUES
('rx_101', 'usr_pat_01', 'Rahim Ahmed', 'Dr. A. K. Azad (FCPS)', 'Dhaka Medical College Hospital', 'Rx: Tab Napa Extra 1+1+1 5 days, Cap Seclo 20mg 1+0+1 before meal 7 days, Tab Fexo 120 0+0+1 10 days.', 'VERIFIED_BY_PHARMACIST', TRUE, 'usr_pharma_01'),
('rx_102', 'usr_pat_01', 'Rahim Ahmed', 'Dr. Kazi Mahbubur Rahman', 'Square Hospital Dhaka', 'Rx: Tab Azithrocin 500 1+0+0 5 days, Tab Monas 10 0+0+1 14 days.', 'EXTRACTED', FALSE, NULL),
('rx_103', 'usr_pat_02', 'Fatima Begum', 'Dr. Sultana Razia (MD)', 'Labaid Specialized Hospital', 'Rx: Tab Lisinopril 10 1+0+0 morning daily, Tab Calbo D 0+1+0 after lunch.', 'UPLOADED', FALSE, NULL);

-- Prescription Items
INSERT INTO prescription_items (prescription_id, medicine_name, dosage, frequency, instructions) VALUES
('rx_101', 'Napa Extra', '500mg+65mg', '1+1+1', 'After meal for fever and pain'),
('rx_101', 'Seclo 20', '20mg', '1+0+1', 'Before meal for gastric acidity'),
('rx_101', 'Fexo 120', '120mg', '0+0+1', 'At night for allergy relief'),
('rx_102', 'Azithrocin 500', '500mg', '1+0+0', 'Once daily 1 hour before meal'),
('rx_102', 'Monas 10', '10mg', '0+0+1', 'Nightly for breathing support'),
('rx_103', 'Lisinopril 10', '10mg', '1+0+0', 'Morning blood pressure control'),
('rx_103', 'Calbo D', '500mg+200IU', '0+1+0', 'Afternoon calcium replenishment');

-- Pharmacies
INSERT INTO pharmacies (id, name, address, phone, latitude, longitude, is_24_hours) VALUES
('pharma_01', 'Lazz Pharma (Dhanmondi Branch)', 'House 41, Road 2, Dhanmondi, Dhaka', '+880 1711-002233', 23.7461, 90.3742, TRUE),
('pharma_02', 'Tamanna Pharmacy (Gulshan 1)', 'Circle 1, Road 11, Gulshan, Dhaka', '+880 1819-556677', 23.7808, 90.4192, TRUE),
('pharma_03', 'Arogga Hub (Uttara Sector 3)', 'Plot 12, Rabindra Sarani, Uttara, Dhaka', '+880 1912-334411', 23.8681, 90.3995, TRUE),
('pharma_04', 'Green Life Drug Corner', '32 Green Road, Farmgate, Dhaka', '+880 1515-889922', 23.7533, 90.3877, TRUE);

-- Pharmacy Inventory
INSERT INTO pharmacy_inventory (pharmacy_id, medicine_id, medicine_name, stock_quantity, unit_price) VALUES
('pharma_01', 'med_01', 'Napa Extra', 450, 2.50),
('pharma_01', 'med_04', 'Seclo 20', 320, 6.00),
('pharma_01', 'med_05', 'Sergel 20', 210, 8.00),
('pharma_01', 'med_07', 'Fexo 120', 180, 9.50),
('pharma_02', 'med_01', 'Napa Extra', 300, 2.50),
('pharma_02', 'med_04', 'Seclo 20', 190, 6.00),
('pharma_02', 'med_09', 'Azithrocin 500', 95, 35.00),
('pharma_03', 'med_01', 'Napa Extra', 500, 2.50),
('pharma_03', 'med_10', 'Monas 10', 140, 17.50),
('pharma_04', 'med_01', 'Napa Extra', 280, 2.50),
('pharma_04', 'med_11', 'Lisinopril 10', 80, 7.00);

-- Medication Reminders
INSERT INTO reminders (id, patient_email, medicine_name, dosage, reminder_time, days_of_week, instructions, is_active) VALUES
('rem_01', 'rahim@medilink.com', 'Napa Extra 500mg', '1 Tablet', '08:00 AM', 'Daily', 'Take after breakfast with water', TRUE),
('rem_02', 'rahim@medilink.com', 'Seclo 20mg', '1 Capsule', '07:30 AM', 'Daily', 'Take 30 mins before breakfast', TRUE),
('rem_03', 'rahim@medilink.com', 'Napa Extra 500mg', '1 Tablet', '02:00 PM', 'Daily', 'Take after lunch', TRUE),
('rem_04', 'rahim@medilink.com', 'Fexo 120mg', '1 Tablet', '10:00 PM', 'Daily', 'Take at bedtime', TRUE),
('rem_05', 'fatima@medilink.com', 'Lisinopril 10mg', '1 Tablet', '08:30 AM', 'Daily', 'Take morning blood pressure dose', TRUE);

-- Chat Messages
INSERT INTO chat_messages (sender_id, sender_name, sender_role, receiver_id, message_text) VALUES
('usr_pat_01', 'Rahim Ahmed', 'PATIENT', 'usr_pharma_01', 'Hello Dr. Tariq, is Napa Extra safe to take with Seclo 20?'),
('usr_pharma_01', 'Tariq Islam (B.Pharm)', 'PHARMACIST', 'usr_pat_01', 'Hello Rahim! Yes, it is completely safe. Take Seclo 20 before meal and Napa Extra after food.'),
('usr_pat_01', 'Rahim Ahmed', 'PATIENT', 'usr_pharma_01', 'Thank you so much! All clear.');

-- Fake Medicine Authenticity Hash Verifications
INSERT INTO fake_medicine_verifications (batch_number, medicine_name, manufacturer, security_hash, is_authentic, verification_notes) VALUES
('BX-2026-991A', 'Napa Extra 500mg', 'Beximco Pharmaceuticals Ltd.', 'SHA256:7b1e8a4f9c2d1e0a8f7c6b5a4d3e2f1a', TRUE, 'Verified Genuine with Beximco Holographic QR.'),
('SQ-2025-442B', 'Seclo 20mg', 'Square Pharmaceuticals PLC', 'SHA256:3a9d2f7b1e8a4f9c2d1e0a8f7c6b5a4d', TRUE, 'Verified Authentic via DGDA Security Gateway.'),
('FK-2024-000X', 'Napa Extra Counterfeit', 'Unknown / Illicit Lab', 'SHA256:00000000000000000000000000000000', FALSE, 'COUNTERFEIT DETECTED: Invalid manufacturer signature & incorrect blister seal.');

-- Support Tickets
INSERT INTO support_tickets (id, requester_name, requester_email, category, severity, subject, description, status) VALUES
('TCK-1001', 'Rahim Ahmed', 'rahim@medilink.com', 'PRESCRIPTION', 'MEDIUM', 'OCR prescription text alignment clarification', 'The doctor handwritten dosage in my clinic pad was slightly faded.', 'RESOLVED'),
('TCK-1002', 'Fatima Begum', 'fatima@medilink.com', 'PHARMACY_STOCK', 'LOW', 'Query on stock availability in Gulshan 1', 'Checked whether Tamanna Pharmacy has Lisinopril stock.', 'OPEN');
