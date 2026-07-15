@echo off
echo ===================================================
echo   Iniciando Sistema Clinica Vida (Psicologia)
echo ===================================================
echo.
echo Levantando Backend (Spring Boot - Java 18)...
start cmd /k "cd backend && mvnw spring-boot:run"
echo.
echo Levantando Frontend (Angular 18)...
start cmd /k "cd frontend && npm start"
echo.
echo ===================================================
echo Las terminales se abriran en ventanas separadas.
echo Asegurate de tener PostgreSQL corriendo localmente
echo con la base de datos 'clinica_db' creada.
echo ===================================================
pause
