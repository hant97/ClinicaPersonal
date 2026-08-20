#!/usr/bin/env bash
# ==============================================================================
# ClinicaPersonal - Script de Respaldo Automatizado de uploads/
# ==============================================================================
set -euo pipefail

SOURCE_DIR="${1:-./uploads}"
BACKUP_DIR="${2:-./backups/uploads}"
RETENTION_DAYS="${3:-30}"

echo "=========================================================="
echo "  ClinicaPersonal - Respaldo de Archivos Clínicos (Linux) "
echo "=========================================================="

mkdir -p "$SOURCE_DIR"
mkdir -p "$BACKUP_DIR"

TIMESTAMP=$(date +"%Y-%m-%d_%H%M%S")
ARCHIVE_NAME="uploads_backup_${TIMESTAMP}.tar.gz"
ARCHIVE_PATH="${BACKUP_DIR}/${ARCHIVE_NAME}"
HASH_PATH="${ARCHIVE_PATH}.sha256"

echo "[*] Comprimiendo '$SOURCE_DIR' en '$ARCHIVE_PATH'..."
tar -czf "$ARCHIVE_PATH" -C "$SOURCE_DIR" .

echo "[*] Calculando suma de comprobación SHA-256..."
if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$ARCHIVE_PATH" > "$HASH_PATH"
elif command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$ARCHIVE_PATH" > "$HASH_PATH"
fi

echo "[OK] Respaldo generado: $ARCHIVE_PATH"

echo "[*] Aplicando política de retención ($RETENTION_DAYS días)..."
find "$BACKUP_DIR" -type f -name "uploads_backup_*" -mtime "+$RETENTION_DAYS" -delete || true

echo "=========================================================="
echo "  Respaldo completado con éxito a las $(date +"%H:%M:%S")"
echo "=========================================================="
