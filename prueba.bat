@echo off
REM Abre una consola con el JDK 17 y el Node 24 portables que se guardan junto al proyecto
REM (en la carpeta padre de ClinicaPersonal), sin depender de lo instalado en el sistema.
set "JAVA_HOME=%~dp0..\jdk-17.0.18+8"
set "NODE_PATH=%~dp0..\node-v24.21.0-win-x64"
set "PATH=%JAVA_HOME%\bin;%NODE_PATH%;%PATH%"

echo --- Entorno Forzado ---
java -version
node -v
cmd /k
