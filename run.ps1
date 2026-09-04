Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "  MediLink 2.0 - Spring Boot Runner (PowerShell)   " -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan

# Ensure Maven is in PATH
if (!(Get-Command mvn -ErrorAction SilentlyContinue)) {
    $env:PATH = "C:\Users\Hasin\AppData\Local\Programs\apache-maven-3.8.8\bin;" + $env:PATH
}

# Set default PostgreSQL password if not already set
if (!$env:DB_PASSWORD) {
    $env:DB_PASSWORD = "Jubaier2@"
}

Write-Host "Starting MediLink 2.0 Spring Boot Application..." -ForegroundColor Yellow
Write-Host "Web UI active at: http://localhost:8080" -ForegroundColor Green

mvn spring-boot:run
