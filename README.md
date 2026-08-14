# Clínica Personal

Aplicación clínica con backend Java 17/Spring Boot y frontend Angular 18. El entorno reproducible usa Node `20.20.2` (declarado en `.nvmrc`) y npm 10.

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
npm test -- --watch=false --browsers=ChromeHeadless
npm run build
```

La automatización equivalente está en `.github/workflows/ci.yml`, con Java 17 y Node 20.20.2 fijados.

## Limpieza de recursos del sitio

El editor visual de la landing guarda las imágenes en dos espacios: los recursos de
borrador (`draft/…`) y los publicados. Los archivos huérfanos (que ya no referencia ni el
borrador ni la versión publicada) se eliminan mediante una tarea programada que corre con el
cron `${website.asset-cleanup-cron:0 30 3 * * *}` (por defecto, a las 03:30, diario) y solo
borra archivos con más de 7 días sin referencia. La limpieza nunca ocurre dentro de la
transacción que reemplaza el contenido publicado.
