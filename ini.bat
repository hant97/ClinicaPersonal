@echo off
title VidaSaludable - Levantar Proyecto
color 0A

echo ============================================
echo         VidaSaludable - Inicio Rapido
echo ============================================
echo.

REM Verificar que Java este disponible
java -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java no esta instalado o no esta en el PATH.
    echo Instala JDK 17+ desde: https://adoptium.net/
    pause
    exit /b 1
)

REM Verificar que Node este disponible
node -v >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Node.js no esta instalado o no esta en el PATH.
    echo Instala Node.js desde: https://nodejs.org/
    pause
    exit /b 1
)

echo [OK] Java y Node.js detectados.
echo.

REM ---- BACKEND ----
echo [1/2] Iniciando Backend (Spring Boot)...
cd /d "%~dp0backend"
start "Backend - Spring Boot" cmd /k "mvnw.cmd clean spring-boot:run"

REM Esperar un momento para que el backend arranque
timeout /t 5 /nobreak >nul

REM ---- FRONTEND ----
echo [2/2] Iniciando Frontend (Angular)...
cd /d "%~dp0frontend"

REM Instalar dependencias si no existen
if not exist "node_modules" (
    echo Instalando dependencias de npm...
    call npm install
)

start "Frontend - Angular" cmd /k "npm start"

echo.
echo ============================================
echo   Proyecto levantado correctamente!
echo.
echo   Backend:  http://localhost:8080
echo   Frontend: http://localhost:4200
echo.
echo   (Se abrieron 2 ventanas de terminal)
echo ============================================
echo.
pause
