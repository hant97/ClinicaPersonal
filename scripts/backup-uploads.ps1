<#
.SYNOPSIS
    Script de Respaldo Automatizado para el directorio de archivos clínicos uploads/
.DESCRIPTION
    Comprime el contenido de uploads/ en un archivo ZIP con timestamp,
    genera su suma de comprobación SHA-256 para auditoría de integridad,
    y aplica la política de retención eliminando archivos de backup antiguos.
.PARAMETER SourceDir
    Ruta del directorio uploads a respaldar (por defecto: .\uploads).
.PARAMETER BackupDir
    Ruta del directorio de destino para los backups (por defecto: .\backups\uploads).
.PARAMETER RetentionDays
    Cantidad de días a retener los backups locales (por defecto: 30 días).
.EXAMPLE
    .\scripts\backup-uploads.ps1 -SourceDir ".\uploads" -BackupDir ".\backups\uploads" -RetentionDays 30
#>

param (
    [string]$SourceDir = ".\uploads",
    [string]$BackupDir = ".\backups\uploads",
    [int]$RetentionDays = 30
)

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  ClinicaPersonal - Respaldo de Archivos Clínicos (uploads) " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Validar directorio origen
if (-not (Test-Path -Path $SourceDir)) {
    Write-Warning "El directorio origen '$SourceDir' no existe. Creando carpeta vacía..."
    New-Item -ItemType Directory -Path $SourceDir -Force | Out-Null
}

# 2. Crear directorio de destino si no existe
if (-not (Test-Path -Path $BackupDir)) {
    New-Item -ItemType Directory -Path $BackupDir -Force | Out-Null
    Write-Host "[+] Directorio de backup creado: $BackupDir" -ForegroundColor Green
}

# 3. Generar nombres de archivo con timestamp
$timestamp = Get-Date -Format "yyyy-MM-dd_HHmmss"
$zipFileName = "uploads_backup_$timestamp.zip"
$zipFilePath = Join-Path -Path $BackupDir -ChildPath $zipFileName
$hashFilePath = Join-Path -Path $BackupDir -ChildPath "$zipFileName.sha256"

Write-Host "[*] Comprimiendo archivos desde '$SourceDir' hacia '$zipFilePath'..." -ForegroundColor Yellow

# Comprimir usando .NET ZipFile
try {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [System.IO.Compression.ZipFile]::CreateFromDirectory($SourceDir, $zipFilePath, [System.IO.Compression.CompressionLevel]::Optimal, $false)
    Write-Host "[OK] Archivo de respaldo generado con éxito." -ForegroundColor Green
} catch {
    Write-Error "Error al comprimir el directorio uploads: $_"
    exit 1
}

# 4. Calcular y guardar Checksum SHA-256
Write-Host "[*] Calculando suma de comprobación SHA-256..." -ForegroundColor Yellow
$hashResult = Get-FileHash -Path $zipFilePath -Algorithm SHA256
$hashContent = "$($hashResult.Hash)  $zipFileName"
Set-Content -Path $hashFilePath -Value $hashContent -Encoding UTF8
Write-Host "[OK] Checksum SHA-256 guardado en: $hashFilePath" -ForegroundColor Green
Write-Host "     Hash: $($hashResult.Hash)" -ForegroundColor Cyan

# 5. Aplicar política de retención (limpieza de backups antiguos)
Write-Host "[*] Aplicando política de retención ($RetentionDays días)..." -ForegroundColor Yellow
$cutoffDate = (Get-Date).AddDays(-$RetentionDays)
$oldFiles = Get-ChildItem -Path $BackupDir -Filter "uploads_backup_*" | Where-Object { $_.LastWriteTime -lt $cutoffDate }

$deletedCount = 0
foreach ($file in $oldFiles) {
    Remove-Item -Path $file.FullName -Force
    $deletedCount++
}

Write-Host "[OK] Retención aplicada. Archivos eliminados por antigüedad: $deletedCount" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Respaldo completado exitosamente a las $(Get-Date -Format 'HH:mm:ss') " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan
