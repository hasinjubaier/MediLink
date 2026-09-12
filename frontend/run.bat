@echo off
if exist "%~dp0..\run.ps1" (
    powershell -ExecutionPolicy Bypass -File "%~dp0..\run.ps1" %*
) else (
    node "%~dp0server.js"
)
