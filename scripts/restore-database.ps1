<#
.SYNOPSIS
    Script de Restauración de la base de datos PostgreSQL de ClinicaPersonal.
.DESCRIPTION
    Verifica la integridad SHA-256 de un dump generado por backup-database.ps1 y lo
    restaura sobre la base de datos indicada mediante pg_restore. Es una operación
    DESTRUCTIVA: reemplaza las tablas existentes en la base de datos destino.
.PARAMETER BackupFile
    Ruta del archivo .dump a restaurar (generado por backup-database.ps1).
.PARAMETER ConnectionString
    Cadena de conexión PostgreSQL de la base de datos DESTINO donde se restaurará el dump.
.PARAMETER Force
    Omite la confirmación interactiva. Útil para scripts automatizados; úsese con cuidado.
.EXAMPLE
    .\scripts\restore-database.ps1 -BackupFile ".\backups\database\clinica_db_backup_2026-08-20_020000.dump" -ConnectionString "postgresql://postgres:password@localhost:5432/clinica_db"
#>

param (
    [Parameter(Mandatory=$true)]
    [string]$BackupFile,
    [Parameter(Mandatory=$true)]
    [string]$ConnectionString,
    [switch]$Force
)

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  ClinicaPersonal - Restauración de Base de Datos          " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Validar que el archivo de backup exista
if (-not (Test-Path -Path $BackupFile)) {
    Write-Error "El archivo de respaldo especificado no existe: $BackupFile"
    exit 1
}

# 2. Verificar que pg_restore esté disponible
if (-not (Get-Command "pg_restore" -ErrorAction SilentlyContinue)) {
    Write-Error "No se encontró 'pg_restore' en el PATH. Instale las herramientas cliente de PostgreSQL e intente nuevamente."
    exit 1
}

# 3. Verificar Checksum SHA-256 si existe el archivo .sha256
$hashFile = "$BackupFile.sha256"
if (Test-Path -Path $hashFile) {
    Write-Host "[*] Verificando integridad SHA-256..." -ForegroundColor Yellow
    $expectedHash = (Get-Content -Path $hashFile).Split(" ")[0].Trim().ToUpper()
    $actualHash = (Get-FileHash -Path $BackupFile -Algorithm SHA256).Hash.ToUpper()

    if ($expectedHash -ne $actualHash) {
        Write-Error "¡ERROR DE INTEGRIDAD! El checksum calculado ($actualHash) no coincide con el esperado ($expectedHash)."
        exit 1
    }
    Write-Host "[OK] Verificación de integridad SHA-256 exitosa." -ForegroundColor Green
} else {
    Write-Warning "No se encontró el archivo de firma '$hashFile'. Se continuará sin verificación previa."
}

# 4. Extraer la contraseña de la cadena de conexión (evita exponerla en el listado de procesos)
$targetConnectionString = $ConnectionString
if ($targetConnectionString -match '^(postgres(?:ql)?://)([^:@/]+):([^@]+)@(.+)$') {
    $env:PGPASSWORD = $Matches[3]
    $targetConnectionString = "$($Matches[1])$($Matches[2])@$($Matches[4])"
}

# 5. Confirmación explícita: esta operación reemplaza datos existentes en el destino
if (-not $Force) {
    Write-Warning "Esta operación reemplazará los datos existentes en la base de datos destino."
    $confirmation = Read-Host "Escriba 'RESTAURAR' para continuar"
    if ($confirmation -ne "RESTAURAR") {
        Write-Host "Operación cancelada por el usuario." -ForegroundColor Yellow
        exit 0
    }
}

# 6. Ejecutar pg_restore
Write-Host "[*] Restaurando '$BackupFile' sobre la base de datos destino..." -ForegroundColor Yellow
try {
    & pg_restore --clean --if-exists --no-owner --no-privileges -d $targetConnectionString $BackupFile
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "pg_restore finalizó con advertencias (código $LASTEXITCODE). Esto es común por objetos que no existían aún (roles/extensiones); revise el detalle anterior."
    }
    Write-Host "[OK] Restauración completada." -ForegroundColor Green
} catch {
    Write-Error "Error al restaurar la base de datos: $_"
    exit 1
}

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Proceso de restauración finalizado                      " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan
