# Guía Operativa: Estrategia de Backup y Recuperación de la Base de Datos

Esta guía complementa a [`GUIA_BACKUP_RESTAURACION_ARCHIVOS.md`](./GUIA_BACKUP_RESTAURACION_ARCHIVOS.md) (que cubre `uploads/`) y establece el protocolo de respaldo, retención y recuperación ante desastres (DRP) para la **base de datos PostgreSQL** de **ClinicaPersonal**, donde vive toda la historia clínica, diagnósticos, recetas y datos de facturación.

---

## 📊 1. Estado por entorno

| Entorno | Motor | Estrategia de respaldo |
| :--- | :--- | :--- |
| **Local (desarrollo)** | PostgreSQL en la PC del desarrollador | No crítico: la base es descartable y se reconstruye con las migraciones Flyway. Se recomienda un dump rápido con `backup-database` antes de operaciones destructivas (pruebas de migraciones, limpieza masiva de datos). |
| **Staging** | [Neon](https://neon.tech) | Neon aplica **backups continuos con Point-in-Time Recovery (PITR)** de forma automática y sin configuración adicional: permite restaurar la base a cualquier segundo dentro de la ventana de retención del plan contratado (ver [documentación de Neon](https://neon.tech/docs/introduction/point-in-time-restore) para la retención vigente de tu plan). Para staging, esto es suficiente por sí solo. |
| **Producción** | *Aún no definido* | Ver recomendación en la sección 2. Los scripts de esta guía funcionan igual sin importar el proveedor final: solo cambia la cadena de conexión. |

> [!IMPORTANT]
> Ningún entorno de producción de un sistema con historias clínicas debería depender de un único mecanismo de respaldo gestionado por un solo proveedor. Se recomienda la regla **3-2-1**: al menos 2 copias, en 2 medios distintos, con 1 copia fuera del proveedor que aloja la base activa.

---

## 🛡️ 2. Recomendación para producción (pendiente de definir)

Cuando se decida el hosting de producción, se recomienda:

1. **Mantener el respaldo automático nativo del proveedor** (PITR de Neon, snapshots de RDS, etc.) como primera línea de defensa — cubre incidentes operativos comunes (borrado accidental, error de migración) con recuperación rápida.
2. **Sumar una copia lógica independiente y periódica** (dump con `pg_dump`, usando los scripts de esta guía) hacia un almacenamiento fuera de ese proveedor — protege contra un incidente que afecte a la cuenta o plataforma del proveedor mismo (suspensión, error de facturación, incidente de seguridad en el proveedor).
3. Definir la **ventana de retención** según el marco médico-legal aplicable (el sistema ya conserva historial clínico indefinidamente vía borrado lógico; los respaldos de base de datos no necesitan igualar ese horizonte, pero sí cubrir un período razonable de detección de incidentes — 30 a 90 días es un punto de partida común).

---

## ⚙️ 3. Scripts Automatizados Disponibles

En `scripts/`, junto a los scripts de `uploads/`, existen los equivalentes para la base de datos. Usan `pg_dump`/`pg_restore` en **formato personalizado** (`-Fc`): comprimido y restaurable de forma selectiva.

### A. Windows / PowerShell
- [`scripts/backup-database.ps1`](../scripts/backup-database.ps1): genera el dump, calcula su checksum SHA-256 y aplica retención.
- [`scripts/restore-database.ps1`](../scripts/restore-database.ps1): verifica el checksum y restaura con `pg_restore` (pide confirmación explícita por ser destructivo, salvo `-Force`).

```powershell
# Backup local (usa DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD del entorno)
powershell -ExecutionPolicy Bypass -File .\scripts\backup-database.ps1 -RetentionDays 30

# Backup de staging (Neon), pasando la cadena de conexión directamente
powershell -ExecutionPolicy Bypass -File .\scripts\backup-database.ps1 -ConnectionString "postgresql://usuario:password@ep-xxx.neon.tech/clinica_db?sslmode=require" -BackupDir "D:\Backups\ClinicaPersonal\database"

# Restaurar un dump específico
powershell -ExecutionPolicy Bypass -File .\scripts\restore-database.ps1 -BackupFile ".\backups\database\clinica_db_backup_2026-08-20_020000.dump" -ConnectionString "postgresql://postgres:password@localhost:5432/clinica_db"
```

### B. Linux / Docker / Servidores
- [`scripts/backup-database.sh`](../scripts/backup-database.sh)
- [`scripts/restore-database.sh`](../scripts/restore-database.sh)

```bash
chmod +x scripts/*.sh

# Backup
./scripts/backup-database.sh "postgresql://usuario:password@ep-xxx.neon.tech/clinica_db?sslmode=require" /backup/clinica/database 30

# Restaurar (pide confirmación interactiva; usar --force para omitirla en un pipeline)
./scripts/restore-database.sh /backup/clinica/database/clinica_db_backup_2026-08-20_020000.dump "postgresql://postgres:password@localhost:5432/clinica_db"
```

> [!NOTE]
> Ambos scripts requieren tener instalado el cliente de PostgreSQL (`pg_dump`/`pg_restore`), coherente con la versión de PostgreSQL 16 usada por el proyecto (ver `.github/workflows/ci.yml`). La contraseña embebida en la cadena de conexión se extrae automáticamente y se pasa por la variable de entorno `PGPASSWORD`, para que no quede visible en el listado de procesos del sistema.

---

## ⏰ 4. Programación de Tareas Automáticas

Igual que para `uploads/` (ver la guía de archivos para el detalle paso a paso de `taskschd.msc` / `crontab`), apuntando al script de base de datos:

```cron
# Backup diario de base de datos a las 02:30 AM (staging Neon)
30 2 * * * DB_CONNECTION="postgresql://usuario:password@ep-xxx.neon.tech/clinica_db?sslmode=require" /var/www/clinica/scripts/backup-database.sh "$DB_CONNECTION" /backup/clinica/database 30 >> /var/log/clinica_backup_db.log 2>&1
```

---

## 🚨 5. Plan de Recuperación ante Desastres (Disaster Recovery Plan)

1. **Aislar el servicio**: detener el backend para evitar escrituras concurrentes durante la restauración.
2. **Elegir el punto de restauración**:
   - Si el incidente es reciente y el proveedor tiene PITR (Neon), evaluar primero una restauración nativa del proveedor — suele ser más precisa (a nivel de segundo) que el último dump lógico.
   - Si se requiere restaurar fuera del proveedor (o el proveedor no está disponible), usar el dump lógico más reciente de `backups/database/`.
3. **Verificar integridad**: confirmar el checksum SHA-256 del dump antes de restaurar.
4. **Ejecutar la restauración** con `restore-database.ps1` / `restore-database.sh` sobre la base destino.
5. **Verificar consistencia con `uploads/`**: tras restaurar la base, confirmar que las rutas referenciadas en `clinical_documents`, `lesion_photos` y `patients.photo_path` sigan existiendo en el respaldo de archivos restaurado correspondiente a una fecha cercana (ambos respaldos deberían tomarse en ventanas de tiempo próximas para mantener la correlación).
6. **Reiniciar los servicios** y validar el flujo completo (login, ficha de paciente, documentos, cobros).
