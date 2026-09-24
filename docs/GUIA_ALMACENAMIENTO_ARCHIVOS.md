# Guía de almacenamiento de archivos

El backend guarda fotos de lesiones y de pacientes, documentos clínicos, logos e imágenes del
sitio web mediante una única abstracción (`BlobStore`) con dos implementaciones, elegidas con
`STORAGE_PROVIDER`:

| Valor | Implementación | Uso |
|---|---|---|
| `local` (por defecto) | `LocalBlobStore`: disco, en `STORAGE_LOCAL_ROOT` | Desarrollo o servidor propio con disco persistente. |
| `s3` | `S3BlobStore`: bucket compatible con S3 | Producción en Render (su disco se borra en cada deploy). Probado para Cloudflare R2 y AWS S3. |

## 1. Organización de las claves

Las claves son iguales en disco y en el bucket, así que se pueden copiar de uno a otro sin
transformarlas:

```text
clinical/lesions/<uuid>.<ext>     Fotos de lesiones (privadas)
clinical/documents/<uuid>.<ext>   Documentos clínicos (privados)
clinical/patients/<uuid>.<ext>    Fotos de pacientes (privadas)
website/<categoría>/<uuid>.<ext>  Imágenes publicadas del sitio e inventario (públicas)
website/draft/<categoría>/...     Borradores del editor del sitio
logos/<uuid>.<ext>                Logos de la clínica
```

Acceso:

- `clinical/` solo se sirve mediante endpoints autenticados que filtran por especialidad
  (`/api/v1/lesion-photos/{id}/file`, `/api/v1/clinical-documents/{id}/file`,
  `/api/v1/patients/{id}/photo`).
- `website/` se sirve sin autenticación en `/api/v1/public/website-assets/`. **No debe
  contener datos de pacientes**; ese endpoint rechaza explícitamente `patients/`.
- El bucket debe ser **privado**: los archivos nunca se descargan directamente desde él.

Validación en la subida: tipo declarado, extensión coherente con el tipo y **firma de bytes**
del contenido (PNG, JPEG, WEBP y PDF). Límite de 2 MB para imágenes y 5 MB para documentos.

## 2. Configurar Cloudflare R2 (producción)

1. En el panel de Cloudflare: **R2 → Create bucket**, por ejemplo `clinica-archivos`. No
   activar el acceso público ni un dominio público.
2. **R2 → Manage R2 API Tokens → Create API token**, con permiso **Object Read & Write**
   limitado a ese bucket. Guardar el *Access Key ID*, el *Secret Access Key* y el endpoint
   S3 (`https://<account-id>.r2.cloudflarestorage.com`).
3. En Render, en las variables del servicio `clinica-backend` (`render.yaml` ya declara
   `STORAGE_PROVIDER=s3`):

   | Variable | Valor |
   |---|---|
   | `STORAGE_S3_BUCKET` | `clinica-archivos` |
   | `STORAGE_S3_ENDPOINT` | `https://<account-id>.r2.cloudflarestorage.com` |
   | `STORAGE_S3_REGION` | `auto` |
   | `STORAGE_S3_ACCESS_KEY` | Access Key ID |
   | `STORAGE_S3_SECRET_KEY` | Secret Access Key |

4. Desplegar. Si falta el bucket o una credencial, el backend no arranca y el log indica
   qué variable falta.
5. Comprobar: subir una foto de lesión, reiniciar el servicio en Render y verificar que la
   foto sigue visible.

Para AWS S3: dejar `STORAGE_S3_ENDPOINT` vacío y usar la región real del bucket
(por ejemplo, `us-east-1`).

## 3. Desarrollo local

```dotenv
STORAGE_PROVIDER=local
STORAGE_LOCAL_ROOT=C:/ruta/absoluta/ClinicaPersonal/uploads
```

Si `STORAGE_LOCAL_ROOT` no se define, se usa `uploads` relativo al directorio de arranque, y el
backend lo advierte en el log al iniciar.

## 4. Migrar archivos existentes al bucket

Con [rclone](https://rclone.org/) configurado para R2 (`rclone config`, proveedor
`Cloudflare`), y dado que las claves coinciden con la estructura de carpetas:

```bash
rclone copy ./uploads r2:clinica-archivos --progress
```

Antes de la migración `V14`, las fotos de pacientes se guardaban en `website/patients/`. Si
existen archivos ahí, deben moverse a `clinical/patients/` (en disco, antes de copiar al
bucket):

```bash
mkdir -p uploads/clinical/patients && mv uploads/website/patients/* uploads/clinical/patients/
```

## 5. Respaldo del bucket

R2 no conserva versiones anteriores de los objetos: un borrado o una sobrescritura es
definitiva. Programar una copia periódica fuera del bucket, por ejemplo diaria:

```bash
rclone sync r2:clinica-archivos /backup/clinica/archivos --progress
```

`sync` refleja los borrados en el destino. Para conservar históricos, copiar a una carpeta
fechada (`rclone copy r2:clinica-archivos /backup/clinica/$(date +%F)`) y aplicar la misma
política de retención que en
[GUIA_BACKUP_RESTAURACION_ARCHIVOS.md](GUIA_BACKUP_RESTAURACION_ARCHIVOS.md).
