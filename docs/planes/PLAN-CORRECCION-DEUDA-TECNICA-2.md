# Plan de corrección de deuda técnica — Segunda auditoría

> **Estado:** 🟡 Entregas 1 a 4 implementadas. Pendiente: verificaciones en Render (Entregas 1 y 2) y tres puntos de la Entrega 4 pospuestos a cambios propios (división de componentes grandes, migración de Lucide, manual a LFS).
> **Última actualización:** 2026-09-24
> **Base:** auditoría estática posterior al cierre de `PLAN-CORRECCION-DEUDA-TECNICA.md`
> (backend Java 17/Spring Boot 4.1, frontend Angular 21).
> **Alcance:** solo hallazgos que el plan anterior no cubrió o dejó a medias.
> **Regla de trabajo:** completar una entrega, ejecutar su verificación y revisar el diff antes de iniciar la siguiente.

## Registro de implementación — Entrega 1

Implementada el 2026-09-24:

- La IP del cliente se obtiene únicamente de `request.getRemoteAddr()` mediante
  `ClientIpResolver`, que reemplaza tres copias que leían `X-Forwarded-For` directamente
  (`AuthController`, `PublicRateLimitFilter`, `AuditLogService`).
- `server.forward-headers-strategy` pasa de `framework` a `native`: el `RemoteIpValve` de
  Tomcat solo acepta `X-Forwarded-For` desde proxies de confianza y toma la primera IP no
  confiable de derecha a izquierda.
- Límites multipart alineados con los servicios de almacenamiento (5 MB por archivo, 6 MB por
  solicitud; antes regía el valor por defecto de Spring, 1 MB).
- `APP_PUBLIC_BASE_URL` declarada en `render.yaml`.
- Retirado el bloque `spring.flyway.properties.hibernate` (sin efecto) y
  `baseline-on-migrate` desactivado por defecto (`FLYWAY_BASELINE_ON_MIGRATE`).
- Borradores de sesión clínica aislados por usuario (`clinicalDraftKey`) y eliminados en el
  cierre de sesión explícito, incluidos los guardados con el formato anterior.

Verificación local: backend 270 pruebas sin fallos, frontend 177/177, lint y build en verde
(detalle en la sección 8).

## Registro de implementación — Entrega 2

Implementada el 2026-09-24 (destino elegido: bucket S3/R2):

- `BlobStore` con dos implementaciones seleccionadas por `STORAGE_PROVIDER`: `LocalBlobStore`
  (disco, raíz en `STORAGE_LOCAL_ROOT`) y `S3BlobStore` (AWS SDK 2.55.4, compatible con R2; sin
  cliente Netty; sumas de verificación solo cuando el servicio las exige).
- `ClinicalFileStorage`, `ClinicalDocumentStorage`, `BlobWebsiteFileStorage` (antes
  `LocalWebsiteFileStorage`) y el nuevo `LogoFileStorage` comparten `ValidatedBlobStorage`:
  validación de tipo, extensión coherente con el tipo y **firma de bytes** (PNG, JPEG, WEBP,
  PDF). Las claves en la base no cambian; la estructura de carpetas local tampoco.
- **Fotos de pacientes privadas.** Hasta ahora se guardaban en el almacenamiento del sitio y
  se servían sin autenticación en `/api/v1/public/website-assets/patients/…`. Ahora van a
  `clinical/patients/` y se sirven en `GET /api/v1/patients/{id}/photo` (autenticado, por
  especialidad, `Cache-Control: private`). Migración `V14__private_patient_photos.sql`
  (`photo_key`). El endpoint público rechaza `patients/`. El frontend las carga con la
  directiva `appAuthSrc`, que usa `HttpClient` solo para rutas `/api/`.
- La foto anterior se borra solo tras el commit; si hay rollback se borra la nueva.
- `ClinicSettingsController.serveLogo` ya no accede al disco.
- `StorageException` → `500` con mensaje genérico; archivo inexistente → `404` (antes `400`).
- Documentación: nueva `docs/GUIA_ALMACENAMIENTO_ARCHIVOS.md`; corregidas la guía de respaldo
  y el README (describía una limpieza de huérfanos que no existe).

Verificación local: backend 311 pruebas sin fallos, frontend 181/181, lint y build en verde
(detalle en la sección 8).

---

## 1. Resumen ejecutivo

El plan anterior resolvió concurrencia, paginación limitada, carga diferida, N+1, rutas
canónicas y puertas de CI. Esta auditoría encontró riesgos vigentes en la identificación de
clientes, la persistencia de archivos en producción, datos clínicos en el navegador y una
plataforma cuyo runtime (Node 20) ya no tiene soporte.

1. **Entrega 1 — Correcciones rápidas de seguridad y configuración** (implementada).
2. **Entrega 2 — Almacenamiento persistente de archivos** (bloqueante para producción real).
3. **Entrega 3 — Actualización de plataforma y dependencias.**
4. **Entrega 4 — Mantenibilidad y limpieza.**

### Exclusión expresa

Igual que en el plan anterior, quedan fuera los usuarios sembrados en la migración inicial y
sus credenciales.

---

## 2. Entrega 1 — Seguridad y configuración (P0)

### 2.1 IP del cliente falsificable

Las tres implementaciones tomaban el primer valor de `X-Forwarded-For`, controlado por el
cliente. Rotando la cabecera se eludían el bloqueo por IP del login (25 intentos) y el límite
de solicitudes públicas; enviando `127.0.0.1` se activaba la exención de loopback de
`LoginAttemptService`; además se podían falsear las IPs de la auditoría.

- [x] Crear `ClientIpResolver` basado solo en `getRemoteAddr()`.
- [x] Sustituir las copias de `AuthController`, `PublicRateLimitFilter` y `AuditLogService`.
- [x] Cambiar `forward-headers-strategy` a `native`.
- [x] Pruebas: rotar `X-Forwarded-For` no elude el límite; el resolvedor ignora la cabecera.
- [x] Documentar `SERVER_TOMCAT_REMOTEIP_INTERNAL_PROXIES` en `.env.example`.
- [ ] **Verificar en Render** que la auditoría registra la IP real del cliente tras el deploy.
  Si aparece siempre la misma IP interna, el proxy de Render está fuera del rango de
  confianza por defecto: definir `SERVER_TOMCAT_REMOTEIP_INTERNAL_PROXIES` con su rango.
  Sin esto, todos los usuarios compartirían contador de bloqueo por IP.
- [ ] Revisar si el `backend/.env` local define `SERVER_FORWARD_HEADERS_STRATEGY=framework`
  (anularía el cambio).

### 2.2 Borradores clínicos en `localStorage`

- [x] Incluir el usuario autenticado en la clave del borrador.
- [x] Eliminar todos los borradores (formato actual y anterior) en el logout explícito.
- [x] Conservarlos cuando la sesión expira, para que el mismo usuario los recupere.
- [x] Pruebas de `clinical-draft.util` y `AuthService`.
- [ ] Decidir si los borradores deben pasar a `sessionStorage` o al servidor: siguen en
  texto plano si el usuario cierra el navegador sin cerrar sesión.

### 2.3 Configuración

- [x] `spring.servlet.multipart.max-file-size: 5MB` y `max-request-size: 6MB`.
- [x] `APP_PUBLIC_BASE_URL` en `render.yaml` (configurar el valor en el panel de Render).
- [x] Retirar `spring.flyway.properties.hibernate`.
- [x] `baseline-on-migrate` por variable, desactivado por defecto.

**Criterio de cierre**

- [x] Suites backend y frontend en verde.
- [ ] Verificaciones en Render (IP real y enlaces de recordatorio) confirmadas.

---

## 3. Entrega 2 — Almacenamiento persistente (P0 para producción)

Fotos de lesiones, documentos clínicos y logos se guardan en rutas relativas
(`uploads/...`) que dependen del directorio de arranque; en el plan `free` de Render el disco
se borra en cada deploy o reinicio.

- [x] Elegir destino: bucket S3/R2 (decisión del 2026-09-24).
- [x] Unificar `ClinicalFileStorage`, `ClinicalDocumentStorage`, `LocalWebsiteFileStorage` y
  logos detrás de una interfaz común (`BlobStore` + `ValidatedBlobStorage`).
- [x] Sacar la lectura de disco de `ClinicSettingsController.serveLogo`.
- [x] Raíz local configurable (`STORAGE_LOCAL_ROOT`), con advertencia en el log si es relativa.
- [x] Validar el contenido real del archivo (firma de bytes), no solo `Content-Type` y extensión.
- [x] Mapear fallos de E/S a `500` (`StorageException`) y archivos inexistentes a `404`.
- [x] Pruebas de almacenamiento local, S3 (cliente simulado), selección del proveedor,
  validación y *path traversal*.
- [x] Scripts de respaldo: siguen válidos en modo local (misma estructura); respaldo del
  bucket documentado con `rclone` en `GUIA_ALMACENAMIENTO_ARCHIVOS.md`.
- [x] **Hallazgo adicional:** fotos de pacientes servidas sin autenticación → privadas (V14).
- [ ] **En Render:** crear el bucket R2 privado y su token, cargar las 5 variables
  `STORAGE_S3_*` y comprobar que una foto sobrevive a un reinicio.
- [ ] Probar `S3BlobStore` contra un bucket real (solo se probó con cliente simulado).
- [ ] Si hay archivos locales previos, copiarlos al bucket y mover `website/patients/` a
  `clinical/patients/` (pasos en la guía, sección 4).
- [ ] Eliminar la columna `patients.photo_url` en una migración posterior, cuando se confirme
  que no contiene URLs externas en uso.
- [ ] Implementar la limpieza de imágenes huérfanas del editor web (el README la describía,
  pero no existe en el código).
- [ ] Imágenes de inventario: se sirven públicamente en `website/supplies/`; valorar moverlas
  a un espacio autenticado.

---

## 4. Entrega 3 — Plataforma y dependencias (P1)

- [x] Node 20 → **Node 24.21.0 LTS** (npm 11): `.nvmrc`, `engines` (`^24.15.0`), CI,
  `prueba.bat` y README. jsdom 30 ya exige Node ≥ 22.22 / 24.15.
- [x] `@angular-devkit/build-angular` → `@angular/build` (application, dev-server,
  extract-i18n). Angular alineado al parche 21.2.24 (framework y CLI). `package-lock.json`
  regenerado con npm 11.
- [x] Karma/Jasmine → **Vitest 4 + jsdom** (`@angular/build:unit-test`). Specs convertidos
  con el schematic oficial `refactor-jasmine-vitest`, con correcciones manuales: indentación
  restaurada a 2 espacios, casts de `MockedObject`, `fakeAsync` → `vi.useFakeTimers`,
  polyfill de `matchMedia` (`src/test-setup.ts`) y `testTimeout` de 15 s (`vitest.config.ts`).
- [x] Jackson 2 retirado: `PsychometricTestService` y dos pruebas usan Jackson 3; `jjwt` usa
  `jjwt-gson` porque `jjwt-jackson` arrastra Jackson 2. `jackson-databind` 2 ya no aparece
  en el árbol de dependencias. (Se usa un `ObjectMapper` estático de Jackson 3 en lugar de
  inyectarlo: solo hace `readTree` y así no cambia el constructor.)
- [x] `jjwt` 0.11.5 → 0.13.0, sin APIs deprecadas; firma HS256 explícita (con una clave de
  más de 32 bytes jjwt habría pasado a HS384/HS512).
- [x] `JwtAuthenticationFilter`: valida el token una sola vez (`parseClaims`), rechaza
  usuarios deshabilitados, deja de ocultar los fallos de infraestructura (500 en lugar de
  401) y los tokens expirados se registran en `debug`, no como `warn`. Nuevas pruebas del
  filtro.
- [x] ~~`TokenRevocationService.isRevoked`: consultar la caché antes que la base.~~
  **Descartado:** el caso habitual (token no revocado) necesita consultar la base igualmente,
  porque la revocación pudo ocurrir antes de un reinicio o en otra instancia. Reordenar no
  ahorra consultas.
- [x] `LoginAttemptService`: contadores atómicos con `compute`, más una prueba concurrente
  (antes, fallos simultáneos se pisaban y el bloqueo podía no activarse).
- [x] `GlobalExceptionHandler`: el error de formato JSON solo nombra el campo afectado;
  el detalle del parser queda en el log.
- [x] `authInterceptor`: el token solo se envía a `environment.apiUrl`.
- [x] Dependabot (Maven, npm, GitHub Actions, Docker; sin saltos mayores de Angular,
  Spring Boot ni Java) y **OSV-Scanner** en CI para Maven y npm.
- [x] **Hallazgo:** OSV-Scanner detectó 9 vulnerabilidades en el backend (3 críticas en
  Tomcat 11.0.22). Corregidas con Spring Boot 4.1.0 → 4.1.1 y `tomcat.version` 11.0.25.
- [x] `npm audit` completo (incluidas devDependencies): 0 vulnerabilidades; desaparece el
  residual `sockjs`/`uuid`.
- [x] Dockerfile: usuario sin privilegios, `HEALTHCHECK` contra `/actuator/health/liveness`,
  `MaxRAMPercentage`, carpeta de subidas con permisos y `.dockerignore`. Job `docker` en CI.
- [ ] Confirmar en el primer CI que el job `docker` construye la imagen (no hay Docker local).
- [ ] Instalar Node 24.21.0 en los equipos de desarrollo; `prueba.bat` espera la carpeta
  `node-v24.21.0-win-x64` junto a la del JDK.
- [ ] npm 11 no ejecuta scripts de instalación no aprobados (`esbuild`, `lmdb`,
  `msgpackr-extract`, `@parcel/watcher`). El build y las pruebas funcionan sin ellos; si
  alguno fallara, aprobarlo con `npm install-scripts approve`.

---

## 5. Entrega 4 — Mantenibilidad (P2–P3)

- [x] **Roles combinables y eliminación solo para administradores** (decisión del
  2026-09-24). Nuevo `ROLE_PROFESIONAL`, separado de `ROLE_ADMIN`, que antes significaba
  "Profesional Titular (Admin)"; ambos se combinan.
  - Eliminar pacientes, cobros y abonos exige `ROLE_ADMIN` (`@PreAuthorize`), y el frontend
    oculta esos botones al resto.
  - La información clínica (lectura y escritura) exige `ROLE_PROFESIONAL`, verificado en
    `ClinicalAuthorizationService.currentProfessional()`. Acceder a registros de otros
    profesionales o a notas confidenciales exige Profesional + Administrador. Asistentes y
    administradores no profesionales ven la ficha administrativa y los cobros, sin pestañas
    clínicas (`professionalGuard` en las rutas de sesión clínica).
  - Las alertas de riesgo pasaron a ser solo para profesionales (antes, cualquier usuario).
  - La lista de profesionales de la agenda excluye a quienes no tienen `ROLE_PROFESIONAL`
    (antes incluía a los asistentes).
  - Gestión de usuarios con selección múltiple de roles y la misma validación en backend y
    frontend: al menos uno, sin combinar Profesional con Asistente.
  - Migración `V15__professional_role.sql` y política actualizada en
    `docs/POLITICA-AUTORIZACION-CLINICA.md`.
  - Verificado el 2026-09-24: backend `verify` 329 pruebas sin fallos (SpotBugs 0, JaCoCo
    cumple), incluidas pruebas de integración con Spring Security para eliminación,
    acceso clínico y lista de profesionales; frontend 191/191, lint sin errores, build
    552.61 kB, E2E 43 pasan / 2 omitidas.
- [ ] Revisar las cuentas de asistentes: desde V15 **pierden el acceso clínico** que tenían
  por omisión. Si alguno es en realidad profesional, asignarle `ROLE_PROFESIONAL`.
- [x] Eliminado `ClinicalSessionFormComponent` (583 líneas sin uso).
- [x] Los 28 endpoints paginados usan `@PageableDefault(...) Pageable`, con los mismos
  tamaños y órdenes por defecto que antes (ya no queda ningún `PageRequest.of` en los
  controladores). Ahora aceptan `sort` de Spring Data; una propiedad inexistente responde
  `400` tanto en consultas derivadas (`PropertyReferenceException`) como en `@Query`
  (`UnknownPathException` de Hibernate).
- [x] Búsqueda de pacientes sin distinción de tildes ni mayúsculas: columna
  `patients.search_text` (migración `V16`), mantenida por la entidad con `SearchText`.
  Se descartó `unaccent`/`pg_trgm`, que no existen en H2 (donde se prueban las
  migraciones); el relleno usa `TRANSLATE`, válido en ambos motores. Sin índice de
  trigramas: con el volumen de una clínica, el `LIKE` no lo necesita.
- [x] ~~Retirar `sweetalert2` (1 uso) en favor de `ToastService`.~~ **Descartado por
  error de la auditoría:** `sweetalert2` no tiene 1 uso, es el motor de
  `NotificationService`, que muestra los 33 diálogos de confirmación; un toast no puede
  pedir confirmación. La librería está mantenida y cumple su función.
- [ ] Dividir `patient-detail.component.html` (1084 líneas), `clinical-history-print`,
  `agenda.component.ts` y `AttentionService`. **Pospuesto a un cambio propio:** es una
  refactorización grande de pantallas sin pruebas unitarias propias; conviene hacerla
  componente a componente, con E2E de apoyo.
- [x] Pruebas nuevas para `RiskAlertService`, `PaymentReportService` y `EmailService`.
  Umbrales de JaCoCo subidos a la nueva línea base: instrucciones 45→49 %, ramas
  37→42 %, líneas 50→53 %, métodos 43→51 % (medidos: 50,0 / 43,7 / 54,7 / 52,2 %).
  Los de Vitest se mantienen (41/34/31/47 %).
- [ ] `lucide-angular` está deprecado: migrar a `@lucide/angular`. **Pospuesto a un
  cambio propio:** afecta a 58 plantillas y cambia la API de los iconos; el paquete
  actual sigue funcionando.
- [x] Auditoría de pacientes (alta, edición, baja y foto): registra el ID, no el nombre
  ni el documento.
- [x] Planes movidos a `docs/planes/`; `start-clinica.bat` unifica el arranque (comprueba
  Java y Node, avisa si Node es anterior a 24, ya no ejecuta `mvnw clean` en cada
  arranque); `ini.bat` queda como alias; `prueba.bat` usa rutas relativas al proyecto.
- [ ] Mover `manual-usuario.docx` y capturas (14 MB) a Git LFS o a una release.
  **Pendiente de decisión:** mover a LFS no reduce el historial ya publicado (habría que
  reescribirlo) y exige que el remoto admita LFS.
- Verificación del 2026-09-24 (Node 24.21.0): backend `verify` 346 pruebas sin fallos con
  los nuevos umbrales de JaCoCo y SpotBugs 0; frontend lint sin errores, 191/191, build
  552.59 kB. Nuevas pruebas de integración para `sort` inválido, búsqueda sin tildes y
  relleno de `search_text` sobre H2.

---

## 6. Orden recomendado de PR

1. Entrega 1 (este cambio).
2. Almacenamiento persistente.
3. Node LTS + `@angular/build` + Vitest.
4. Jackson 3 + `jjwt` + optimización del filtro JWT.
5. Limpiezas de la Entrega 4, en PR pequeños.

No mezclar actualizaciones mayores de dependencias con cambios de reglas clínicas en el mismo PR.

---

## 7. Definition of Done

La misma que la sección 10 de `PLAN-CORRECCION-DEUDA-TECNICA.md`.

## 8. Verificación de la Entrega 1

Ejecutada el 2026-09-24 en local (JDK 17.0.18, Node 20.20.2):

- Backend `mvnw verify`: 270 pruebas, 0 fallos, 2 omitidas (PostgreSQL sin conexión local);
  JaCoCo cumple los umbrales y SpotBugs reporta 0 hallazgos.
- Frontend: lint sin errores (53 avisos preexistentes de `any`); 177/177 pruebas; cobertura
  51.51 % statements, 28.65 % branches, 43.89 % functions y 53.27 % lines.
- Build Angular de producción exitoso; bundle inicial de 550.82 kB raw.
- No se ejecutaron las pruebas E2E de Playwright ni la prueba de concurrencia PostgreSQL.

### Verificación de la Entrega 2

Ejecutada el 2026-09-24 en local (JDK 17.0.18, Node 20.20.2):

- Backend `mvnw verify`: 311 pruebas, 0 fallos, 2 omitidas (PostgreSQL sin conexión local);
  JaCoCo cumple los umbrales y SpotBugs reporta 0 hallazgos. La migración V14 se ejecuta
  sobre H2 en `ClinicalAuditMigrationTest` (14 migraciones).
- Frontend: lint sin errores (53 avisos preexistentes); 181/181 pruebas; build de producción
  exitoso, bundle inicial de 550.82 kB raw.
- `S3BlobStore` probado con cliente simulado y creación real del cliente con configuración R2;
  **no** se probó contra un bucket real. Tampoco se ejecutaron E2E ni la migración V14 sobre
  PostgreSQL.

### Verificación de la Entrega 3

Ejecutada el 2026-09-24 en local (JDK 17.0.18, **Node 24.21.0 / npm 11.19.0**):

- Backend `mvnw verify` sobre Spring Boot 4.1.1: 322 pruebas, 0 fallos, 2 omitidas
  (PostgreSQL sin conexión local); JaCoCo cumple y SpotBugs reporta 0 hallazgos.
- Frontend: lint sin errores (53 avisos preexistentes); **Vitest 184/184** con umbrales de
  cobertura cumplidos; build de producción con `@angular/build`, bundle inicial 551.01 kB raw.
- **E2E Playwright** (`test:e2e:ci`, dev-server de `@angular/build`): 43 pasan, 2 omitidas.
- `npm audit` (completo y `--omit=dev`): 0 vulnerabilidades.
- OSV-Scanner 2.6.0 sobre `backend/pom.xml` y `frontend/package-lock.json`: sin hallazgos.
- No verificado localmente: build de la imagen Docker (queda en el job `docker` del CI).
