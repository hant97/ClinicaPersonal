@echo off
set JAVA_HOME=C:\Users\lricce\Downloads\VidaSaludable-main\jdk-17.0.18+8
set NODE_PATH=C:\Users\lricce\Downloads\VidaSaludable-main\node-v20.20.2-win-x64
set PATH=%JAVA_HOME%\bin;%NODE_PATH%;%PATH%

echo --- Entorno Forzado ---
java -version
node -v
cmd /k