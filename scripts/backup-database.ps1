<#
.SYNOPSIS
    Script de Respaldo Automatizado de la base de datos PostgreSQL de ClinicaPersonal.
.DESCRIPTION
    Genera un dump en formato personalizado de PostgreSQL (pg_dump -Fc) comprimido y
    restaurable de forma selectiva, calcula su checksum SHA-256 para auditoría de
    integridad, y aplica la política de retención eliminando respaldos antiguos.
    Funciona igual para la base local, Neon (staging) o cualquier Postgres de producción:
    solo cambia la cadena de conexión.
.PARAMETER ConnectionString
    Cadena de conexión PostgreSQL (formato "postgresql://usuario:password@host:puerto/base?sslmode=require").
    Si no se especifica, se arma a partir de las variables de entorno DB_HOST, DB_PORT,
    DB_NAME, DB_USER y DB_PASSWORD (mismas usadas por el backend, ver backend/.env.example).
.PARAMETER BackupDir
    Ruta del directorio de destino para los backups (por defecto: .\backups\database).
.PARAMETER RetentionDays
    Cantidad de días a retener los backups locales (por defecto: 30 días).
.EXAMPLE
    # Local, usando variables de entorno ya cargadas en la sesión
    .\scripts\backup-database.ps1

.EXAMPLE
    # Staging en Neon, pasando la cadena de conexión directamente
    .\scripts\backup-database.ps1 -ConnectionString "postgresql://usuario:password@ep-xxx.neon.tech/clinica_db?sslmode=require" -BackupDir "D:\Backups\ClinicaPersonal\database"
#>

param (
    [string]$ConnectionString = "",
    [string]$BackupDir = ".\backups\database",
    [int]$RetentionDays = 30
)

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  ClinicaPersonal - Respaldo de Base de Datos PostgreSQL   " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Verificar que pg_dump esté disponible
if (-not (Get-Command "pg_dump" -ErrorAction SilentlyContinue)) {
    Write-Error "No se encontró 'pg_dump' en el PATH. Instale las herramientas cliente de PostgreSQL (incluidas en el instalador oficial de PostgreSQL) e intente nuevamente."
    exit 1
}

# 2. Resolver la cadena de conexión
if ([string]::IsNullOrWhiteSpace($ConnectionString)) {
    $dbHost = if ($env:DB_HOST) { $env:DB_HOST } else { "localhost" }
    $dbPort = if ($env:DB_PORT) { $env:DB_PORT } else { "5432" }
    $dbName = if ($env:DB_NAME) { $env:DB_NAME } else { "clinica_db" }
    $dbUser = if ($env:DB_USER) { $env:DB_USER } else { "postgres" }
    $dbPassword = $env:DB_PASSWORD

    if ([string]::IsNullOrWhiteSpace($dbPassword)) {
        Write-Error "No se proporcionó -ConnectionString ni la variable de entorno DB_PASSWORD. Defina una de las dos antes de continuar."
        exit 1
    }

    $ConnectionString = "postgresql://${dbUser}:${dbPassword}@${dbHost}:${dbPort}/${dbName}"
    Write-Host "[*] Usando conexión local armada desde variables de entorno (host: $dbHost, base: $dbName)." -ForegroundColor Yellow
} else {
    Write-Host "[*] Usando la cadena de conexión proporcionada explícitamente." -ForegroundColor Yellow
}

# 2.1 Extraer la contraseña de la cadena de conexión y pasarla por variable de entorno,
#     para que no quede visible en el listado de procesos (Task Manager / ps).
if ($ConnectionString -match '^(postgres(?:ql)?://)([^:@/]+):([^@]+)@(.+)$') {
    $env:PGPASSWORD = $Matches[3]
    $ConnectionString = "$($Matches[1])$($Matches[2])@$($Matches[4])"
}

# 3. Crear directorio de destino si no existe
if (-not (Test-Path -Path $BackupDir)) {
    New-Item -ItemType Directory -Path $BackupDir -Force | Out-Null
    Write-Host "[+] Directorio de backup creado: $BackupDir" -ForegroundColor Green
}

# 4. Generar nombres de archivo con timestamp
$timestamp = Get-Date -Format "yyyy-MM-dd_HHmmss"
$dumpFileName = "clinica_db_backup_$timestamp.dump"
$dumpFilePath = Join-Path -Path $BackupDir -ChildPath $dumpFileName
$hashFilePath = Join-Path -Path $BackupDir -ChildPath "$dumpFileName.sha256"

# 5. Ejecutar pg_dump en formato personalizado (comprimido, restaurable selectivamente con pg_restore)
Write-Host "[*] Generando dump de la base de datos hacia '$dumpFilePath'..." -ForegroundColor Yellow
try {
    & pg_dump $ConnectionString -Fc -f $dumpFilePath
    if ($LASTEXITCODE -ne 0) {
        throw "pg_dump finalizó con código de salida $LASTEXITCODE"
    }
    Write-Host "[OK] Dump generado con éxito." -ForegroundColor Green
} catch {
    Write-Error "Error al generar el dump de la base de datos: $_"
    exit 1
}

# 6. Calcular y guardar Checksum SHA-256
Write-Host "[*] Calculando suma de comprobación SHA-256..." -ForegroundColor Yellow
$hashResult = Get-FileHash -Path $dumpFilePath -Algorithm SHA256
$hashContent = "$($hashResult.Hash)  $dumpFileName"
Set-Content -Path $hashFilePath -Value $hashContent -Encoding UTF8
Write-Host "[OK] Checksum SHA-256 guardado en: $hashFilePath" -ForegroundColor Green
Write-Host "     Hash: $($hashResult.Hash)" -ForegroundColor Cyan

# 7. Aplicar política de retención (limpieza de backups antiguos)
Write-Host "[*] Aplicando política de retención ($RetentionDays días)..." -ForegroundColor Yellow
$cutoffDate = (Get-Date).AddDays(-$RetentionDays)
$oldFiles = Get-ChildItem -Path $BackupDir -Filter "clinica_db_backup_*" | Where-Object { $_.LastWriteTime -lt $cutoffDate }

$deletedCount = 0
foreach ($file in $oldFiles) {
    Remove-Item -Path $file.FullName -Force
    $deletedCount++
}

Write-Host "[OK] Retención aplicada. Archivos eliminados por antigüedad: $deletedCount" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Respaldo completado exitosamente a las $(Get-Date -Format 'HH:mm:ss') " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan
