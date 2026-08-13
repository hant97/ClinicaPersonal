# Plan de corrección y refactorización

> **Estado:** Fases 0–5 completadas  
> **Base:** análisis técnico del 2026-08-13  
> **Regla de trabajo:** completar una fase, ejecutar su verificación y revisar el diff antes de iniciar la siguiente.

## Objetivo

Corregir riesgos de seguridad, errores funcionales y deuda técnica sin alterar los contratos REST existentes de forma accidental. Las tareas que afecten API deben actualizar en el mismo cambio: controlador, DTO, servicio, cliente Angular, modelo TypeScript y pruebas.

## Alcance y prioridades

| Prioridad | Área | Resultado esperado |
|---|---|---|
| P0 | Secretos y sesiones | Credenciales fuera del repositorio y revocación de sesión segura. |
| P1 | Navegación, paginación y pruebas | Flujos de Dermatología accesibles y suite Angular en verde. |
| P1 | Confidencialidad clínica | Acceso y edición restringidos por una política explícita. |
| P2 | Validación y errores | Respuestas HTTP predecibles y formularios protegidos. |
| P3 | Mantenibilidad y entrega | Código tipado, pruebas aisladas y entorno reproducible. |

---

## Fase 0 — Preparación y línea base ✅

**Objetivo:** asegurar una implementación controlada y reversible.

- [x] Crear una rama de corrección y conservar el trabajo actual sin confirmar separado de los cambios del plan.
- [x] Inventariar los valores de producción de `SPRING_DATASOURCE_*`, `JWT_SECRET`, `AUTH_COOKIE_SECURE` y `CORS_ALLOWED_ORIGINS` sin registrarlos en Git.
- [x] Documentar el contrato actual de autenticación: login, refresh, logout, cookie `refresh_token`, access token y códigos de error.
- [x] Documentar el contrato de evaluaciones dermatológicas: `GET/POST /api/v1/patients/{patientId}/dermatological-evaluations`, `GET/PUT/DELETE /api/v1/dermatological-evaluations/{id}`.
- [x] Registrar como línea base: `mvn clean test` pasa; Angular build pasa con Node 20; Angular test tiene 4 fallos.
- [x] Configurar una base de datos exclusiva de pruebas; las pruebas no deben usar la base local de desarrollo.

**Criterio de cierre**

- [x] No hay secretos nuevos en el diff.
- [x] Los contratos y resultados iniciales están documentados para comparar al finalizar.

**Evidencia:** [línea base y contratos](docs/FASE-0-LINEA-BASE.md). El contexto Spring usa H2 en memoria bajo el perfil `test`.

---

## Fase 1 — Seguridad de configuración y sesiones (P0) ✅

**Objetivo:** eliminar secretos expuestos y cerrar las brechas del ciclo de vida de tokens.

### 1.1 Configuración segura

- [x] Sustituir el valor fijo de `jwt.secret` por una variable de entorno obligatoria; abortar el arranque si falta o no cumple la longitud mínima.
- [x] Retirar la contraseña de base de datos por defecto del perfil compartido. Si se necesita un valor local, aislarlo en un perfil de desarrollo no versionado o plantilla `.example` sin secretos.
- [x] Establecer `AUTH_COOKIE_SECURE=true` en despliegues HTTPS y dejarlo explícito en la configuración de cada entorno.
- [x] Revisar `CORS_ALLOWED_ORIGINS`: permitir solamente orígenes concretos y evitar valores de desarrollo en producción.
- [x] Rotar la clave JWT expuesta y las credenciales que hayan podido usarse fuera de desarrollo.

### 1.2 Revocación y rotación de refresh tokens

- [x] Añadir al repositorio una operación para revocar todos los refresh tokens activos de un usuario.
- [x] Invocarla al cambiar contraseña, deshabilitar usuario o restablecer credenciales.
- [x] Hacer atómica la rotación del refresh token: usar bloqueo de fila u operación condicional que solo revoque un token aún activo.
- [x] Definir el comportamiento de concurrencia: el primer refresh es válido; los reintentos posteriores con el token anterior reciben `401`.
- [x] Definir una política de sesiones por usuario (por ejemplo, límite de sesiones activas o familias de tokens por dispositivo).
- [x] Implementar limpieza periódica de `refresh_tokens` y `revoked_tokens` expirados, con una prueba de la consulta de limpieza.

### 1.3 Pruebas de seguridad

- [x] Probar que cambiar contraseña invalida access tokens y refresh tokens anteriores.
- [x] Probar dos refresh simultáneos con el mismo token y confirmar que solo uno obtiene un nuevo token.
- [x] Probar logout, token revocado, token vencido, token manipulado y cookie ausente.
- [x] Probar que la aplicación falla de forma segura si falta `JWT_SECRET`.

**Criterio de cierre**

- [x] Ningún secreto real está versionado.
- [x] Las pruebas de cambio de contraseña, rotación, logout y revocación pasan.
- [x] `./mvnw.cmd test` pasa contra la base de datos de pruebas.

---

## Fase 2 — Correcciones funcionales y contrato frontend/backend (P1) ✅

**Objetivo:** reparar navegación dermatológica, paginación y regresiones de pruebas.

### 2.1 Navegación de evaluaciones dermatológicas

- [x] Decidir el flujo único: eliminar el enlace global de “Evaluaciones Derm.” o crear una pantalla de búsqueda/listado que seleccione un paciente antes de mostrar sus evaluaciones.
- [x] No registrar una ruta que instancie `DermatologicalEvaluationListComponent` sin un `patientId` válido.
- [x] Asegurar que la ruta elegida use `authGuard` y `specialtyGuard('DERMATOLOGIA')`.
- [x] Añadir prueba de navegación para usuario dermatólogo y prueba de rechazo para otra especialidad.

### 2.2 Paginación dermatológica

- [x] Cambiar `getByPatientId` para devolver `Observable<PageResponse<DermatologicalEvaluation>>`, sin `any`.
- [x] Leer `response.page.totalPages` y `response.page.totalElements` en el componente.
- [x] Confirmar la paginación base cero y que el cambio de página conserva `patientId`, `page` y `size`.
- [x] Añadir pruebas de servicio HTTP y de componente: primera página, página siguiente, vacío y error.

### 2.3 Reparar la suite Angular

- [x] En los tests de `MainLayoutComponent` y `DashboardComponent`, proveer `HttpClient` de pruebas o sustituir `SpecialtyService` y `AuthService` por mocks enfocados.
- [x] Evitar que los tests de componentes arrastren servicios reales no relevantes para el caso probado.
- [x] Ejecutar `npm test -- --watch=false --browsers=ChromeHeadless` con Node 22.12 (versión compatible disponible localmente).

**Criterio de cierre**

- [x] El enlace de Dermatología no conduce a una ruta inexistente.
- [x] La paginación muestra los totales y permite navegar.
- [x] Las 33 pruebas Angular pasan, sin pruebas deshabilitadas.
- [x] Ruta, verbo, parámetros y respuesta coinciden en backend y frontend.

---

## Fase 3 — Protección de información clínica e integridad (P1) ✅

**Objetivo:** hacer efectiva la confidencialidad de sesiones y reforzar la trazabilidad clínica.

### 3.1 Política de autorización clínica

- [x] Definir por escrito quién puede leer, editar y borrar una sesión confidencial: creador, administrador de especialidad y/o profesional asignado.
- [x] Derivar el profesional autenticado en el backend; no aceptar `professionalId` como fuente de autorización desde el cliente.
- [x] Aplicar la política a listados, detalle, creación, edición y eliminación de sesiones clínicas.
- [x] Aplicar el mismo criterio de especialidad y propiedad a historias médicas y evaluaciones dermatológicas.
- [x] Devolver `403 Forbidden` para recursos fuera del ámbito autorizado, sin filtrar datos en el mensaje.

### 3.2 Auditoría y borrado clínico

- [x] Decidir, con criterio clínico/legal, qué registros requieren borrado lógico en vez de borrado físico.
- [x] Si se adopta borrado lógico, crear una nueva migración Flyway incremental; no modificar migraciones `V1`–`V17`.
- [x] Añadir `deleted`, fecha y usuario de eliminación donde corresponda; filtrar registros borrados desde los repositorios.
- [x] Mantener acceso de auditoría solo para roles autorizados.

### 3.3 Pruebas de autorización

- [x] Probar lectura y edición de sesión confidencial por creador, administrador permitido y usuario no autorizado.
- [x] Probar acceso cruzado entre Psicología y Dermatología.
- [x] Probar que el `professionalId` enviado por el cliente no permite suplantar al profesional autenticado.

**Criterio de cierre**

- [x] No existe un TODO de confidencialidad en rutas clínicas activas.
- [x] Las pruebas demuestran 401, 403, 404 y éxito según la política definida.
- [x] Toda migración nueva funciona en una base de datos vacía y una ya migrada.

---

## Fase 4 — Validación, manejo de errores y calidad de código (P2) ✅

**Objetivo:** reducir errores 500 evitables y consolidar convenciones.

### 4.1 Validación y errores API

- [x] Añadir restricciones Bean Validation a los DTOs de escrituras: pacientes, sesiones, historias, insumos, servicios, pagos, catálogos, evaluaciones y citas.
- [x] Aplicar `@Valid` a cada `@RequestBody` correspondiente.
- [x] Sustituir `RuntimeException` genéricas por excepciones de dominio (`ResourceNotFoundException`, regla de negocio, conflicto).
- [x] Ampliar `GlobalExceptionHandler` para responder de forma consistente con `400`, `404`, `409` y `422` si se adopta este último código.
- [x] Mantener mensajes de usuario en español y sin exponer detalles internos.

### 4.2 Refactorización incremental

- [x] Reemplazar `@Autowired` por inyección de constructor con `@RequiredArgsConstructor` y campos `private final` en clases modificadas.
- [x] Eliminar nombres de clase completamente cualificados dentro de métodos y usar imports explícitos.
- [x] Reemplazar `Observable<any>` y parámetros `any` por interfaces y tipos de formulario concretos.
- [x] Reutilizar `PageResponse<T>` para todo listado paginado.
- [x] Migrar las plantillas tocadas de `*ngIf`/`*ngFor` a `@if`/`@for` con `track` estable.
- [x] Dividir el CSS de la landing o ajustar su composición para cumplir el presupuesto de 10 kB.

**Criterio de cierre**

- [x] Los endpoints de escritura devuelven errores de validación estructurados.
- [x] No quedan `any` nuevos ni inyección por campo en los módulos intervenidos.
- [x] El build Angular no presenta advertencias de presupuesto de estilos.

---

## Fase 5 — Pruebas, entorno reproducible y entrega (P3) ✅

**Objetivo:** evitar regresiones y hacer que cualquier desarrollador o CI pueda validar el sistema.

- [x] Añadir `.nvmrc` o documentar Node 20 LTS y el requisito en `package.json` mediante `engines`.
- [x] Añadir un comando documentado para backend y frontend que use variables de entorno de ejemplo, nunca secretos reales.
- [x] Separar datos mock e inicializadores del perfil de pruebas para que no contaminen resultados.
- [x] Crear pruebas de integración para autenticación, autorización clínica, paginación dermatológica y validación de DTOs.
- [x] Mantener pruebas unitarias rápidas para servicios Angular y guardas.
- [x] Configurar CI para ejecutar backend tests, Angular tests y Angular build con versiones fijadas de Java y Node.
- [x] Revisar el diff completo y confirmar que no se incluyan `dist/`, `target/`, cargas locales ni archivos de entorno.

**Criterio de cierre global**

- [x] `backend`: `./mvnw.cmd test` exitoso.
- [x] `frontend`: `npm test -- --watch=false --browsers=ChromeHeadless` exitoso.
- [x] `frontend`: `npm run build` exitoso y sin advertencias de presupuesto.
- [x] Las pruebas se ejecutan con base de datos aislada y Node/Java documentados.
- [x] Se verificó el contrato REST de cada flujo modificado y no quedan secretos en el repositorio.

**Evidencia:** CI reproducible en `.github/workflows/ci.yml`, comandos seguros en `README.md`, 47 pruebas backend y 36 pruebas Angular exitosas, y build Angular de producción sin advertencias de presupuesto.

---

## Orden recomendado de implementación

1. Fase 0 y Fase 1 antes de desplegar nuevas versiones.
2. Fase 2 inmediatamente después: restaura flujos visibles y la señal de calidad del frontend.
3. Fase 3 antes de ampliar el uso clínico o dar acceso a más profesionales.
4. Fases 4 y 5 en entregas pequeñas, con pruebas añadidas junto a cada corrección.
