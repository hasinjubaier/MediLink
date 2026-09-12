@echo off
setlocal
echo ===================================================
echo   MediLink 2.0 - Standalone Frontend Launcher
echo ===================================================

cd frontend
echo Starting Frontend Client at http://localhost:3000 ...
echo Target Spring Boot API: http://localhost:8080
echo.

node server.js

endlocal
