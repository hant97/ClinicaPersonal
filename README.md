# Clínica Personal

Aplicación clínica con backend Java 17/Spring Boot 4.1 y frontend Angular 21. El entorno reproducible usa Node `24.21.0` LTS (declarado en `.nvmrc`) y npm 11. Las pruebas unitarias del frontend usan Vitest con jsdom; las E2E, Playwright.

## Configuración local segura

El backend carga `backend/.env`, que está excluido de Git. Créalo únicamente a partir de la plantilla y reemplaza sus marcadores con valores locales; nunca copies credenciales reales al repositorio:

```powershell
Copy-Item backend/.env.example backend/.env
```

Los datos mock solo se crean al activar explícitamente el perfil `dev`. Los tests usan el perfil `test`, H2 en memoria y valores exclusivos de prueba, sin inicializadores ni conexión a PostgreSQL:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'dev'
Set-Location backend
.\mvnw.cmd spring-boot:run
```

Para el frontend, instala y selecciona la versión declarada antes de iniciar:

```powershell
nvm install (Get-Content .nvmrc)
nvm use (Get-Content .nvmrc)
Set-Location frontend
npm ci
npm start
```

## Validación completa

Estos comandos no requieren secretos de desarrollo ni producción. El backend toma su configuración aislada de `src/test/resources/application-test.yml` y el frontend no contiene credenciales:

```powershell
Set-Location backend
.\mvnw.cmd test

Set-Location ..\frontend
npm ci
npm run test:coverage
npm run build
```

La automatización equivalente está en `.github/workflows/ci.yml`, con Java 17 y la versión de Node de `.nvmrc`. El CI además escanea vulnerabilidades conocidas de Maven y npm con OSV-Scanner y construye la imagen Docker del backend; Dependabot propone actualizaciones semanales (`.github/dependabot.yml`).

## Almacenamiento de archivos

Fotos, documentos clínicos, logos e imágenes del sitio se guardan en disco
(`STORAGE_PROVIDER=local`, por defecto) o en un bucket privado compatible con S3 como
Cloudflare R2 (`STORAGE_PROVIDER=s3`, requerido en Render porque su disco se borra en cada
deploy). Configuración, migración y respaldo: [docs/GUIA_ALMACENAMIENTO_ARCHIVOS.md](docs/GUIA_ALMACENAMIENTO_ARCHIVOS.md).

El editor visual de la landing guarda las imágenes en dos espacios: borrador (`draft/…`) y
publicado. Actualmente no hay una tarea automática que elimine los archivos huérfanos (los que
ya no referencia ni el borrador ni la versión publicada).
