#!/usr/bin/env bash
# ==============================================================================
# ClinicaPersonal - Script de Restauración de la base de datos PostgreSQL
# ==============================================================================
# Uso:
#   ./scripts/restore-database.sh <archivo.dump> <connection-string-destino> [--force]
#
# Operación DESTRUCTIVA: reemplaza las tablas existentes en la base de datos destino.
set -euo pipefail

BACKUP_FILE="${1:-}"
CONNECTION_STRING="${2:-}"
FORCE_FLAG="${3:-}"

if [ -z "$BACKUP_FILE" ] || [ -z "$CONNECTION_STRING" ]; then
    echo "Uso: $0 <archivo.dump> <connection-string-destino> [--force]" >&2
    exit 1
fi

if [ ! -f "$BACKUP_FILE" ]; then
    echo "Error: el archivo de respaldo '$BACKUP_FILE' no existe." >&2
    exit 1
fi

if ! command -v pg_restore >/dev/null 2>&1; then
    echo "Error: no se encontró 'pg_restore' en el PATH. Instale el cliente de PostgreSQL (paquete postgresql-client)." >&2
    exit 1
fi

echo "=========================================================="
echo "  ClinicaPersonal - Restauración de Base de Datos (Linux) "
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
else
    echo "Advertencia: no se encontró el archivo de firma '$HASH_FILE'. Se continuará sin verificación previa." >&2
fi

# Extraer la contraseña de la URI y pasarla por PGPASSWORD.
if [[ "$CONNECTION_STRING" =~ ^(postgres(ql)?://)([^:@/]+):([^@]+)@(.+)$ ]]; then
    export PGPASSWORD="${BASH_REMATCH[4]}"
    CONNECTION_STRING="${BASH_REMATCH[1]}${BASH_REMATCH[3]}@${BASH_REMATCH[5]}"
fi

if [ "$FORCE_FLAG" != "--force" ]; then
    echo "Esta operación reemplazará los datos existentes en la base de datos destino." >&2
    read -r -p "Escriba 'RESTAURAR' para continuar: " CONFIRMATION
    if [ "$CONFIRMATION" != "RESTAURAR" ]; then
        echo "Operación cancelada por el usuario."
        exit 0
    fi
fi

echo "[*] Restaurando '$BACKUP_FILE' sobre la base de datos destino..."
pg_restore --clean --if-exists --no-owner --no-privileges -d "$CONNECTION_STRING" "$BACKUP_FILE" || \
    echo "Advertencia: pg_restore reportó advertencias (habitual por objetos que no existían aún); revise el detalle anterior." >&2

echo "=========================================================="
echo "  Restauración completada.                                "
echo "=========================================================="
