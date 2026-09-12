param (
    [string]$Mode = ""
)

# If run from inside frontend folder, delegate to root orchestrator if available
$rootDir = (Resolve-Path "$PSScriptRoot\..").Path
$rootScript = Join-Path $rootDir "run.ps1"

if (Test-Path $rootScript) {
    & $rootScript @PSBoundParameters
} else {
    Write-Host "===================================================" -ForegroundColor Cyan
    Write-Host "  MediLink 2.0 - Standalone Frontend Client        " -ForegroundColor Cyan
    Write-Host "===================================================" -ForegroundColor Cyan
    Write-Host "Starting Frontend Client on http://localhost:3000 ..." -ForegroundColor Yellow
    node "$PSScriptRoot\server.js"
}
