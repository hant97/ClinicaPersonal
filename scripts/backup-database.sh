#!/usr/bin/env bash
# ==============================================================================
# ClinicaPersonal - Script de Respaldo Automatizado de la base de datos PostgreSQL
# ==============================================================================
# Uso:
#   ./scripts/backup-database.sh [connection-string] [backup-dir] [retention-days]
#
# Si no se pasa connection-string, se arma desde las variables de entorno
# DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD (las mismas del backend).
set -euo pipefail

CONNECTION_STRING="${1:-}"
BACKUP_DIR="${2:-./backups/database}"
RETENTION_DAYS="${3:-30}"

echo "=========================================================="
echo "  ClinicaPersonal - Respaldo de Base de Datos (Linux)     "
echo "=========================================================="

if ! command -v pg_dump >/dev/null 2>&1; then
    echo "Error: no se encontró 'pg_dump' en el PATH. Instale el cliente de PostgreSQL (paquete postgresql-client)." >&2
    exit 1
fi

if [ -z "$CONNECTION_STRING" ]; then
    DB_HOST="${DB_HOST:-localhost}"
    DB_PORT="${DB_PORT:-5432}"
    DB_NAME="${DB_NAME:-clinica_db}"
    DB_USER="${DB_USER:-postgres}"
    if [ -z "${DB_PASSWORD:-}" ]; then
        echo "Error: no se proporcionó connection-string ni la variable de entorno DB_PASSWORD." >&2
        exit 1
    fi
    CONNECTION_STRING="postgresql://${DB_USER}:${DB_PASSWORD}@${DB_HOST}:${DB_PORT}/${DB_NAME}"
    echo "[*] Usando conexión armada desde variables de entorno (host: ${DB_HOST}, base: ${DB_NAME})."
else
    echo "[*] Usando la cadena de conexión proporcionada explícitamente."
fi

# Extraer la contraseña de la URI y pasarla por PGPASSWORD para que no quede
# visible en el listado de procesos (ps aux).
if [[ "$CONNECTION_STRING" =~ ^(postgres(ql)?://)([^:@/]+):([^@]+)@(.+)$ ]]; then
    export PGPASSWORD="${BASH_REMATCH[4]}"
    CONNECTION_STRING="${BASH_REMATCH[1]}${BASH_REMATCH[3]}@${BASH_REMATCH[5]}"
fi

mkdir -p "$BACKUP_DIR"

TIMESTAMP=$(date +"%Y-%m-%d_%H%M%S")
DUMP_NAME="clinica_db_backup_${TIMESTAMP}.dump"
DUMP_PATH="${BACKUP_DIR}/${DUMP_NAME}"
HASH_PATH="${DUMP_PATH}.sha256"

echo "[*] Generando dump hacia '$DUMP_PATH'..."
pg_dump "$CONNECTION_STRING" -Fc -f "$DUMP_PATH"

echo "[*] Calculando suma de comprobación SHA-256..."
if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$DUMP_PATH" > "$HASH_PATH"
elif command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$DUMP_PATH" > "$HASH_PATH"
fi

echo "[OK] Respaldo generado: $DUMP_PATH"

echo "[*] Aplicando política de retención (${RETENTION_DAYS} días)..."
find "$BACKUP_DIR" -type f -name "clinica_db_backup_*" -mtime "+${RETENTION_DAYS}" -delete || true

echo "=========================================================="
echo "  Respaldo completado con éxito a las $(date +"%H:%M:%S")"
echo "=========================================================="
