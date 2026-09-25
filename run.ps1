param (
    [string]$Mode = ""
)

Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "   MediLink 2.0 - Decoupled Fullstack Orchestrator " -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan

# Ensure Maven is in PATH
if (!(Get-Command mvn -ErrorAction SilentlyContinue)) {
    $env:PATH = "C:\Users\Hasin\AppData\Local\Programs\apache-maven-3.8.8\bin;" + $env:PATH
}

# Set default PostgreSQL password if not set
if (!$env:DB_PASSWORD) {
    $env:DB_PASSWORD = "Jubaier2@"
}

if (-not $Mode) {
    Write-Host "Select execution mode:" -ForegroundColor Yellow
    Write-Host "  [1] Launch Fullstack (Backend API :8080 + Frontend :3000)" -ForegroundColor Green
    Write-Host "  [2] Launch Backend REST API Only (:8080)" -ForegroundColor White
    Write-Host "  [3] Launch Frontend Client Only (:3000)" -ForegroundColor White
    Write-Host ""
    $choice = Read-Host "Enter choice (1/2/3) [Default: 1]"
    if (-not $choice) { $choice = "1" }
} else {
    $choice = $Mode
}

switch ($choice) {
    "2" {
        Write-Host "Starting Backend REST API on http://localhost:8080 ..." -ForegroundColor Yellow
        mvn spring-boot:run
    }
    "3" {
        Write-Host "Starting Frontend Client on http://localhost:3000 ..." -ForegroundColor Yellow
        Push-Location "$PSScriptRoot\frontend"
        try {
            node server.js
        } finally {
            Pop-Location
        }
    }
    Default {
        Write-Host "Launching MediLink 2.0 Fullstack Application..." -ForegroundColor Green
        Write-Host "Starting Backend API in separate window..." -ForegroundColor Cyan
        Start-Process cmd -ArgumentList "/k", "title MediLink 2.0 Backend && set ""PATH=C:\Users\Hasin\AppData\Local\Programs\apache-maven-3.8.8\bin;%PATH%"" && set ""DB_PASSWORD=$env:DB_PASSWORD"" && mvn spring-boot:run" -WorkingDirectory $PSScriptRoot
        
        Start-Sleep -Seconds 3
        
        Write-Host "Starting Frontend Client on http://localhost:3000 ..." -ForegroundColor Cyan
        Push-Location "$PSScriptRoot\frontend"
        try {
            node server.js
        } finally {
            Pop-Location
        }
    }
}
