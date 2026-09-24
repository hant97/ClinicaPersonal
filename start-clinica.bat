@echo off
setlocal enabledelayedexpansion
title VidaSaludable - Clinica Personal
REM Arranque local de backend y frontend. ini.bat lo invoca por compatibilidad.

echo ===================================================
echo   VidaSaludable - Clinica Personal
echo ===================================================
echo.

java -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java no esta instalado o no esta en el PATH. Instala JDK 17+: https://adoptium.net/
    echo         Si usas el JDK portable junto al proyecto, ejecuta primero prueba.bat.
    pause
    exit /b 1
)

node -v >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Node.js no esta instalado o no esta en el PATH. Instala Node 24 LTS: https://nodejs.org/
    pause
    exit /b 1
)

for /f "tokens=1 delims=." %%v in ('node -v') do set "NODE_MAJOR=%%v"
set "NODE_MAJOR=!NODE_MAJOR:v=!"
if !NODE_MAJOR! LSS 24 (
    echo [AVISO] Node !NODE_MAJOR! detectado; el proyecto requiere Node 24 ^(ver .nvmrc^).
    echo         Usa prueba.bat o nvm para seleccionar la version correcta.
    echo.
)

echo [OK] Java y Node.js detectados.
echo.

set "LOCAL_IP="
for /f "tokens=*" %%a in ('powershell -NoProfile -Command "[System.Net.Dns]::GetHostAddresses([System.Net.Dns]::GetHostName()) | Where-Object { $_.AddressFamily -eq 'InterNetwork' -and $_.IPAddressToString -notlike '127.*' -and $_.IPAddressToString -notlike '169.254.*' } | Select-Object -First 1 -ExpandProperty IPAddressToString"') do (
    set "LOCAL_IP=%%a"
)

echo [1/2] Levantando Backend (Spring Boot)...
start "Backend - Spring Boot" cmd /k "cd /d ""%~dp0backend"" && mvnw.cmd spring-boot:run"

echo [2/2] Levantando Frontend (Angular)...
if not exist "%~dp0frontend\node_modules" (
    echo Instalando dependencias de npm...
    pushd "%~dp0frontend"
    call npm ci
    popd
)
start "Frontend - Angular" cmd /k "cd /d ""%~dp0frontend"" && npm start"

echo.
echo ===================================================
echo   Frontend:  http://localhost:4200
if defined LOCAL_IP echo   Red local: http://!LOCAL_IP!:4200
echo   Backend:   http://localhost:8080
if defined LOCAL_IP echo   Red local: http://!LOCAL_IP!:8080
echo.
echo   Requiere PostgreSQL local con la base 'clinica_db'.
echo ===================================================
echo.
pause
