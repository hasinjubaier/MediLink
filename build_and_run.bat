@echo off
setlocal
echo ===================================================
echo   MediLink 2.0 - Fullstack Build and Run Script
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

echo.
echo [1] Start Backend REST API (:8080)
echo [2] Start Standalone Frontend Client (:3000)
echo [3] Start Fullstack (Backend in new window + Frontend here)
echo.
set /p CHOICE="Choose an option (1/2/3) [Default: 3]: "
if "%CHOICE%"=="" set CHOICE=3

if "%CHOICE%"=="1" (
    echo Starting Backend API at http://localhost:8080 ...
    mvn spring-boot:run
) else if "%CHOICE%"=="2" (
    echo Starting Frontend Client at http://localhost:3000 ...
    cd frontend
    node server.js
) else (
    echo Starting Backend REST API in background window...
    start "MediLink Backend API" cmd /k "run-backend.bat"
    timeout /t 2 /nobreak >nul
    echo Starting Frontend Client at http://localhost:3000 ...
    cd frontend
    node server.js
)

endlocal
