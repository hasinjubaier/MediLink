Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "   MediLink 2.0 - Standalone Frontend Launcher     " -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan

$frontendDir = Join-Path $PSScriptRoot "frontend"
Push-Location $frontendDir
try {
    Write-Host "Starting Frontend Client on http://localhost:3000 ..." -ForegroundColor Yellow
    Write-Host "Target API: http://localhost:8080" -ForegroundColor DarkGray
    node server.js
} finally {
    Pop-Location
}
