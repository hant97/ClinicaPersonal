@echo off
REM Abre una consola con el JDK 21 y el Node 24 portables que se guardan junto al proyecto
REM (en la carpeta padre de ClinicaPersonal), sin depender de lo instalado en el sistema.
set "JAVA_HOME=%~dp0..\jdk-21.0.12.1+1"
if not exist "%JAVA_HOME%\bin\javac.exe" (
    echo [ERROR] No se encontro el JDK 21 portable en "%JAVA_HOME%".
    exit /b 1
)
set "NODE_PATH=%~dp0..\node-v24.21.0-win-x64"
set "PATH=%JAVA_HOME%\bin;%NODE_PATH%;%PATH%"

echo --- Entorno Forzado ---
"%JAVA_HOME%\bin\java.exe" -version
node -v
cmd /k
