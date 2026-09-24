# Guía Operativa: Estrategia de Backup y Recuperación de Archivos Clínicos (`uploads/`)

Esta guía establece el protocolo formal de respaldo, retención, cifrado y recuperación ante desastres (DRP) para todos los activos digitales y documentos clínicos almacenados en el directorio local `uploads/` de **ClinicaPersonal**.

---

## 📁 1. Arquitectura del Almacenamiento Local

> Esta guía cubre el modo `STORAGE_PROVIDER=local`. En producción con un bucket S3/R2
> (`STORAGE_PROVIDER=s3`), el respaldo se hace desde el bucket: ver
> [GUIA_ALMACENAMIENTO_ARCHIVOS.md](GUIA_ALMACENAMIENTO_ARCHIVOS.md), sección 5.

Los archivos se guardan bajo `STORAGE_LOCAL_ROOT` (por defecto `uploads/`, relativo al
directorio de arranque del backend; se recomienda una ruta absoluta):

```text
uploads/
├── clinical/
│   ├── documents/     # Documentos clínicos del paciente (PDFs, estudios, laboratorios)
│   ├── lesions/       # Registro fotográfico longitudinal de lesiones dermatológicas
│   └── patients/      # Fotografías de identificación de pacientes
├── website/           # Imágenes del sitio web público, borradores (draft/) e inventario
└── logos/             # Logos de la clínica
```

### 🔒 Reglas de Seguridad en Almacenamiento
1. **Acceso indirecto**: Los archivos de `uploads/` nunca se sirven como estáticos. Los de `clinical/` solo se entregan mediante endpoints autenticados con alcance por especialidad; los de `website/` se publican sin autenticación en `/api/v1/public/website-assets/` y por eso no deben contener datos de pacientes.
2. **Nombres de archivo sanitizados**: Los nombres se generan con UUIDs o claves aleatorias para evitar colisiones y ataques de Directory Traversal (`..` o paths absolutos).
3. **Permisos del sistema de archivos**:
   - **Linux**: `chmod 700 uploads/` y `chmod 600 uploads/**/*` pertenecientes al usuario del servicio (`clinica:clinica`).
   - **Windows**: Permisos NTFS restringidos exclusivamente a la cuenta de servicio de la aplicación y administradores locales.

---

## 🛡️ 2. Estrategia de Respaldos (Esquema GFS)

Para garantizar la disponibilidad y continuidad del negocio sin sobrecargar el almacenamiento, se implementa una rotación **Grandfather-Father-Son (GFS)**:

| Tipo | Frecuencia | Retención | Destino Recomendado |
| :--- | :--- | :--- | :--- |
| **Son (Diario)** | Cada noche (02:00 AM) | 7 días | Disco secundario / NAS local |
| **Father (Semanal)** | Domingos (03:00 AM) | 4 semanas | Almacenamiento seguro externo / Servidor de contingencia |
| **Grandfather (Mensual)** | 1° de cada mes (04:00 AM) | 12 meses | Almacenamiento inmutable en frío (Cold Storage / WORM) |

---

## ⚙️ 3. Scripts Automatizados Disponibles

En la raíz del proyecto, en el directorio `scripts/`, se encuentran los ejecutables listos para producción:

### A. Para Entornos Windows / PowerShell
- [`scripts/backup-uploads.ps1`](file:///c:/Users/lricce/Downloads/VidaSaludable-main/ClinicaPersonal/scripts/backup-uploads.ps1): Genera un archivo `.zip` comprimido con timestamp, calcula el checksum SHA-256 para auditoría y elimina respaldos que superen los días de retención.
- [`scripts/restore-uploads.ps1`](file:///c:/Users/lricce/Downloads/VidaSaludable-main/ClinicaPersonal/scripts/restore-uploads.ps1): Valida la integridad del archivo mediante su checksum SHA-256 y restaura la estructura en `uploads/`.

#### Ejecución Manual en PowerShell:
```powershell
# Realizar backup inmediato
powershell -ExecutionPolicy Bypass -File .\scripts\backup-uploads.ps1 -SourceDir ".\uploads" -BackupDir ".\backups\uploads" -RetentionDays 30

# Restaurar backup específico
powershell -ExecutionPolicy Bypass -File .\scripts\restore-uploads.ps1 -BackupFile ".\backups\uploads\uploads_backup_2026-08-20_020000.zip" -TargetDir ".\uploads"
```

---

### B. Para Entornos Linux / Docker / Servidores de Producción
- [`scripts/backup-uploads.sh`](file:///c:/Users/lricce/Downloads/VidaSaludable-main/ClinicaPersonal/scripts/backup-uploads.sh): Comprime con `tar.gz`, genera `.sha256` y aplica política de rotación con `find`.
- [`scripts/restore-uploads.sh`](file:///c:/Users/lricce/Downloads/VidaSaludable-main/ClinicaPersonal/scripts/restore-uploads.sh): Verifica SHA-256 y descomprime en el directorio de destino asegurando los permisos.

#### Ejecución Manual en Bash:
```bash
# Otorgar permisos de ejecución
chmod +x scripts/*.sh

# Ejecutar respaldo
./scripts/backup-uploads.sh /var/www/clinica/uploads /backup/clinica/uploads 30

# Restaurar
./scripts/restore-uploads.sh /backup/clinica/uploads/uploads_backup_2026-08-20_020000.tar.gz /var/www/clinica/uploads
```

---

## ⏰ 4. Programación de Tareas Automáticas

### En Windows (Programador de Tareas)
1. Abrir `taskschd.msc` (Programador de Tareas).
2. Crear Tarea Básica:
   - **Nombre**: `ClinicaPersonal_Backup_Uploads`
   - **Desencadenador**: Diariamente a las `02:00 AM`.
   - **Acción**: Iniciar un programa.
   - **Programa o script**: `powershell.exe`
   - **Argumentos**: `-NonInteractive -WindowStyle Hidden -ExecutionPolicy Bypass -File "C:\ClinicaPersonal\scripts\backup-uploads.ps1" -SourceDir "C:\ClinicaPersonal\uploads" -BackupDir "D:\Backups\ClinicaPersonal\uploads" -RetentionDays 30`

### En Linux (Crontab)
Editar el crontab del usuario del sistema:
```bash
sudo crontab -u clinica -e
```
Añadir la siguiente línea:
```cron
# Ejecutar respaldo diario a las 02:00 AM
0 2 * * * /var/www/clinica/scripts/backup-uploads.sh /var/www/clinica/uploads /backup/clinica/uploads 30 >> /var/log/clinica_backup.log 2>&1
```

---

## 🚨 5. Plan de Recuperación ante Desastres (Disaster Recovery Plan)

En caso de fallo de hardware, corrupción de disco o incidente de seguridad:

1. **Aislar el servidor**: Detener el servicio backend (`systemctl stop clinica-backend` o finalizar el proceso Spring Boot) para evitar escrituras concurrentes.
2. **Seleccionar el punto de restauración**: Identificar el archivo de backup más reciente en el almacenamiento secundario.
3. **Verificar integridad**: Confirmar que el archivo de respaldo coincida exactamente con su firma `.sha256`:
   ```powershell
   Get-FileHash -Algorithm SHA256 .\uploads_backup_2026-08-20_020000.zip
   ```
4. **Ejecutar la restauración**:
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\scripts\restore-uploads.ps1 -BackupFile "D:\Backups\uploads_backup_2026-08-20_020000.zip" -TargetDir "C:\ClinicaPersonal\uploads"
   ```
5. **Verificar integridad de la base de datos**: Los registros en las tablas `clinical_documents`, `lesion_photos` y la columna `patients.photo_key` deben correlacionarse con los archivos restaurados.
6. **Reiniciar los servicios**: Iniciar nuevamente el backend y verificar la visualización correcta de documentos y fotos en la interfaz de usuario.
