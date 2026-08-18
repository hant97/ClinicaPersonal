@echo off
setlocal enabledelayedexpansion
title VidaSaludable - Clinica Vida

echo ===================================================
echo   Iniciando Sistema Clinica Vida (Psicologia)
echo ===================================================
echo.

echo Detectando direccion IP local...
set "LOCAL_IP="
for /f "tokens=*" %%a in ('powershell -NoProfile -Command "[System.Net.Dns]::GetHostAddresses([System.Net.Dns]::GetHostName()) | Where-Object { $_.AddressFamily -eq 'InterNetwork' -and $_.IPAddressToString -notlike '127.*' -and $_.IPAddressToString -notlike '169.254.*' } | Select-Object -First 1 -ExpandProperty IPAddressToString"') do (
    set "LOCAL_IP=%%a"
)

echo.
echo ===================================================
echo   Direcciones de acceso al Frontend:
echo   - En esta PC:          http://localhost:4200
if defined LOCAL_IP (
echo   - En tu red local:     http://!LOCAL_IP!:4200
) else (
echo   - En tu red local:     http://[TU_IP_LOCAL]:4200
)
echo.
echo   Direcciones del Backend (API):
echo   - En esta PC:          http://localhost:8080
if defined LOCAL_IP (
echo   - En tu red local:     http://!LOCAL_IP!:8080
)
echo ===================================================
echo.

echo [1/2] Levantando Backend (Spring Boot)...
start "Backend - Spring Boot" cmd /k "cd /d ""%~dp0backend"" && mvnw.cmd spring-boot:run"

echo.
echo [2/2] Levantando Frontend (Angular 18 - Red Local)...
start "Frontend - Angular" cmd /k "cd /d ""%~dp0frontend"" && npm start"

echo.
echo ===================================================
echo Las terminales se abriran en ventanas separadas.
echo Asegurate de tener PostgreSQL corriendo localmente
echo con la base de datos 'clinica_db' creada.
echo ===================================================
echo.
pause
