#!/usr/bin/env bash
# ==============================================================================
# ClinicaPersonal - Script de Restauración de Archivos uploads/
# ==============================================================================
set -euo pipefail

BACKUP_FILE="${1:-}"
TARGET_DIR="${2:-./uploads}"

if [ -z "$BACKUP_FILE" ]; then
    echo "Uso: $0 <archivo-backup.tar.gz> [directorio-destino]"
    exit 1
fi

if [ ! -f "$BACKUP_FILE" ]; then
    echo "Error: El archivo de respaldo '$BACKUP_FILE' no existe."
    exit 1
fi

echo "=========================================================="
echo "  ClinicaPersonal - Restauración de Archivos (Linux)      "
echo "=========================================================="

HASH_FILE="${BACKUP_FILE}.sha256"
if [ -f "$HASH_FILE" ]; then
    echo "[*] Verificando suma de comprobación SHA-256..."
    if command -v sha256sum >/dev/null 2>&1; then
        sha256sum -c "$HASH_FILE"
    elif command -v shasum >/dev/null 2>&1; then
        shasum -a 256 -c "$HASH_FILE"
    fi
    echo "[OK] Integridad verificada correctamente."
fi

mkdir -p "$TARGET_DIR"
echo "[*] Descomprimiendo en '$TARGET_DIR'..."
tar -xzf "$BACKUP_FILE" -C "$TARGET_DIR"

echo "=========================================================="
echo "  Restauración completada con éxito.                      "
echo "=========================================================="
