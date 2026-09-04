@echo off
setlocal
echo ===================================================
echo   MediLink 2.0 - Spring Boot Build and Run Script
echo ===================================================

:: Ensure Maven is in PATH
where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    set "PATH=C:\Users\Hasin\AppData\Local\Programs\apache-maven-3.8.8\bin;%PATH%"
)

:: Set default PostgreSQL password if not already set in environment
if "%DB_PASSWORD%"=="" (
    set "DB_PASSWORD=Jubaier2@"
)

echo Building and starting MediLink 2.0 Spring Boot application...
echo Running at: http://localhost:8080
echo Press Ctrl+C to stop.
echo.

mvn spring-boot:run

endlocal
