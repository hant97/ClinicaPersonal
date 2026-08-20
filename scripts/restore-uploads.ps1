<#
.SYNOPSIS
    Script de Restauración de Archivos Clínicos uploads/
.DESCRIPTION
    Verifica la integridad SHA-256 de un archivo ZIP de backup y lo restaura
    en el directorio uploads/ destino.
.PARAMETER BackupFile
    Ruta del archivo ZIP de backup a restaurar.
.PARAMETER TargetDir
    Ruta del directorio uploads de destino (por defecto: .\uploads).
.EXAMPLE
    .\scripts\restore-uploads.ps1 -BackupFile ".\backups\uploads\uploads_backup_2026-08-20_020000.zip" -TargetDir ".\uploads"
#>

param (
    [Parameter(Mandatory=$true)]
    [string]$BackupFile,
    [string]$TargetDir = ".\uploads"
)

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  ClinicaPersonal - Restauración de Archivos Clínicos     " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Validar que el archivo de backup exista
if (-not (Test-Path -Path $BackupFile)) {
    Write-Error "El archivo de respaldo especificado no existe: $BackupFile"
    exit 1
}

# 2. Verificar Checksum SHA-256 si existe el archivo .sha256
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

# 3. Preparar directorio de destino
if (-not (Test-Path -Path $TargetDir)) {
    New-Item -ItemType Directory -Path $TargetDir -Force | Out-Null
}

# 4. Descomprimir backup
Write-Host "[*] Restaurando archivos en '$TargetDir'..." -ForegroundColor Yellow
try {
    Expand-Archive -Path $BackupFile -DestinationPath $TargetDir -Force
    Write-Host "[OK] Restauración completada satisfactoriamente." -ForegroundColor Green
} catch {
    Write-Error "Error al extraer los archivos de respaldo: $_"
    exit 1
}

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Proceso de restauración finalizado exitosamente         " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan
