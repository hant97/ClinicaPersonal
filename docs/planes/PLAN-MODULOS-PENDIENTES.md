# Plan de Implementación: Módulos Pendientes y Expansión Funcional

> **Estado**: ⏳ Propuesta pendiente de autorización para iniciar ejecución
> **Última actualización**: 2026-08-19
> **Base**: análisis del sistema actual (backend Java 17/Spring Boot, frontend Angular 18, migraciones Flyway consolidadas en `V1`–`V4`) y de los planes de referencia `PLAN-MULTI-ESPECIALIDAD.md`, `PLAN-HISTORIA-CLINICA.md`, `docs/`.
>
> **Nota histórica**: las fases 0–1 de la versión anterior de este plan (Recetas y
> Documentos) ya están implementadas y sus migraciones se consolidaron en
> `V1__init_schema.sql`. La numeración de migraciones nuevas continúa en `V5`.

---

## 1. Resumen ejecutivo

El sistema es maduro y está completo en lo clínico: multi-especialidad (Psicología/
Dermatología), historia clínica normalizada, seguridad de sesiones (JWT + refresh rotatorio
+ revocación), autorización clínica owner-or-admin, borrado lógico, sitio público editable,
recetas con verificación pública por QR, documentos del paciente, inventario con alertas de
stock, servicios clínicos y catálogos dinámicos administrables.

Las **brechas reales** ya no están en lo clínico sino en la operación del negocio:

1. **Pagos → facturación** (P0): sin estados, sin cuotas/abonos, sin saldo por paciente.
2. **Recordatorios de citas** (P0): no existe infraestructura de notificaciones (ni email ni WhatsApp); el ausentismo no tiene mitigación.
3. **Auditoría de acciones** (P1): no hay registro de quién creó/modificó/eliminó datos sensibles.
4. **Agenda avanzada** (P1): sin recurrencia, sin disponibilidad/bloqueos por profesional.
5. **Atenciones formalizadas** (P2): el encuentro clínico no es una entidad de primera clase.
6. **Valor clínico y deuda técnica** (P2): reportes clínicos, backup de archivos, pruebas E2E, observabilidad.

---

## 2. Estado por módulo

| # | Módulo | Estado | Qué existe hoy |
|---|--------|--------|----------------|
| 1 | Usuarios y permisos | ✅ Completo | `User` (specialty, roles, enabled), auth JWT+refresh+logout, `LoginAttemptService`, gestión admin completa (listado, habilitar/deshabilitar, reset de contraseña, reasignar rol), perfil propio, cambio de contraseña. Roles: `ROLE_ADMIN` y `ROLE_SITE_ADMIN`. **Falta solo auditoría de acciones.** |
| 2 | Pacientes | ✅ Completo | CRUD paginado + búsqueda, género obligatorio, `specialty`, UUID público, foto, ficha 360°, soft delete, exportación Excel. |
| 3 | Agenda y citas | ⚠️ Básico+ | `Appointment` con estados, modalidad, `professional_id`, enlace a sesión clínica, sync con Google Calendar, vista semanal/lista, **recordatorios por email + confirmación pública por token + botón WhatsApp**. **Falta**: recurrencia, disponibilidad/bloqueos, vista por profesional. |
| 4 | Historia clínica | ✅ Completo | Árbol por especialidad, agregador `GET /clinical-history`, autorización owner-or-admin, impresión configurable. |
| 5 | Atenciones | ⚠️ Implícito | `ClinicalSession` funciona como "atención" embebida en la ficha; `Appointment` ya enlaza `clinical_session_id`. No hay entidad de atención con ciclo de vida propio. |
| 6 | Psicología | ✅ Completo | Evaluación, sesiones SOAP, diagnósticos, plan terapéutico, pruebas psicométricas (`Assessment`/`PsychometricTest`). |
| 7 | Dermatología | ✅ Completo | Evaluación, historia dermatológica, lesiones con fotos, diagnósticos, tratamientos, procedimientos, exámenes, evoluciones. |
| 8 | Recetas / indicaciones | ✅ Completo | `Prescription` + `PrescriptionItem`, impresión con QR y verificación pública (`/verificar-receta/:code`). |
| 9 | Documentos | ✅ Completo | `ClinicalDocument` con storage local seguro, subida/descarga/vista previa, borrado lógico. |
| 10 | Pagos / facturación | ✅ Completo | `Payment` + `PaymentItem` + `payment_transactions` (abonos), estados `PENDIENTE`/`PARCIAL`/`PAGADO`, `due_date`, vínculo `appointment_id`/`clinical_session_id`, saldo por paciente, resumen con gráficos por rango de fechas, KPI "Por cobrar", recibo/estado de cuenta imprimible, exportación. |
| 11 | Inventario | ✅ Completo | `Supply` + transacciones, alertas de bajo stock, imágenes. |
| 12 | Servicios clínicos | ✅ Completo | `ClinicalService` con precios, vinculado a citas y cobros. |
| 13 | Alertas de riesgo | ✅ Completo | `RiskAlert` por paciente + alertas globales activas en dashboard. |
| 14 | Catálogos dinámicos | ✅ Completo | `Catalog`/`CatalogItem` administrables (géneros, métodos de pago, modalidades, etc.). |
| 15 | Sitio web público | ✅ Completo | Editor visual con draft/publish, assets, revisiones, limpieza programada de huérfanos. |

---

## 3. Brechas identificadas (priorizadas)

1. ~~**Pagos → facturación (P0)**~~ ✅ Completada (Fase 0): estados, abonos parciales, saldo por paciente, `due_date`, vínculo a sesión clínica y reporte financiero por rango.
2. ~~**Recordatorios de citas (P0)**~~ ✅ Completada (Fase 1): email con `spring-boot-starter-mail`, confirmación pública por token, tarea programada diaria y botón WhatsApp (`wa.me`).
3. **Auditoría de acciones (P1)** — las entidades tienen `created_at`/`updated_at`/`professional_id`, pero no hay un log de acciones (quién hizo qué y cuándo) sobre datos sensibles de salud.
4. **Agenda avanzada (P1)** — sin recurrencia (clave para terapias semanales de psicología), sin bloques de disponibilidad por profesional, sin vista por profesional.
5. **Atenciones formalizadas (P2)** — citas, sesiones, recetas y pagos existen pero están aislados; falta el hilo `AGENDADA → EN_PROCESO → ATENDIDA → COBRADA`.
6. **Valor clínico y técnico (P2)** — sin reportes de evolución (puntajes psicométricos, comparativa fotográfica), sin estrategia de backup de `uploads/`, sin pruebas E2E, sin health checks (Actuator).

---

## 4. Propuestas de implementación

### P0 — Pagos → facturación

Ampliar `Payment` con `status` (`PENDIENTE`/`PARCIAL`/`PAGADO`), `due_date` y vínculo
`clinical_session_id` (ya existe `appointment_id`). Nueva tabla `payment_transactions` para
**abonos parciales** (monto, fecha, método, nota). Reglas:

- `status` se deriva de la suma de abonos vs. `amount`: 0 → `PENDIENTE`, parcial → `PARCIAL`, completo → `PAGADO`.
- **Saldo por paciente**: endpoint `GET /api/v1/patients/{id}/payments/balance` (total cargado, abonado, pendiente) visible en la ficha del paciente.
- **Estado de cuenta imprimible** extendiendo el patrón de `PaymentDetailComponent` (recibo) y `clinical-history-print`.
- **Reporte financiero** en Dashboard: ingresos por rango de fechas y método de pago, filtrado por `specialty`, reutilizando Chart.js (patrón ya usado en Cobros).
- Compatibilidad: pagos existentes migran a `PAGADO` con un abono único equivalente al monto.

### P0 — Recordatorios de citas

- **Email** con `spring-boot-starter-mail` (única dependencia nueva, justificada): plantilla simple con datos de la cita, profesional y enlace de confirmación. Configuración por `application.yml` + marcadores en `backend/.env.example` (nunca credenciales reales).
- Campos en `appointments`: `reminder_sent_at`, `confirmation_token` (UUID), `confirmed_at`.
- **Tarea programada** diaria (patrón `TokenCleanupService`/asset-cleanup) que envía recordatorios 24–48 h antes, una sola vez por cita.
- **Confirmación pública por enlace**: `GET /api/v1/public/appointments/confirm/{token}` → estado `CONFIRMADA`. Replica el patrón de verificación pública de recetas (`PublicPrescriptionController` + página `/verificar-receta`), protegido por `PublicRateLimitFilter`.
- **WhatsApp sin API**: botón en la agenda que abre `wa.me/{telefono}?text=...` con mensaje prellenado (costo cero, sin dependencias).
- Compatible con la sync de Google Calendar existente (los recordatorios no duplican eventos).

### P1 — Auditoría de acciones

- Migración `audit_log`: `id`, `user_id`, `username`, `specialty`, `action` (`CREATE`/`UPDATE`/`DELETE`/`LOGIN`/`EXPORT`), `entity_type`, `entity_id`, `detail` (TEXT/JSON), `ip`, `created_at`.
- Registro desde los servicios de operaciones sensibles: pacientes, historia clínica, recetas, documentos, pagos y usuarios (implementación por AOP o llamadas explícitas, la que resulte menos intrusiva).
- Endpoint de consulta solo para `ROLE_ADMIN` + pantalla en `settings` con filtros (fecha, usuario, entidad, acción) y paginación de servidor.
- Inmutable: sin update ni delete; retención por tarea programada si el volumen lo requiere.

### P1 — Agenda avanzada

- **Recurrencia**: `recurrence_group_id` + regla semanal simple (ej. "cada lunes 10:00, N sesiones") que genera la serie al crear la cita; edición/borrado por serie o por ocurrencia.
- **Disponibilidad**: `professional_schedules` (horario semanal por profesional) y `schedule_blocks` (vacaciones/bloqueos); el formulario de citas valida contra disponibilidad.
- **Vista por profesional**: filtro/columnas en la agenda usando `professional_id` (ya existe en `appointments`).

### P2 — Atenciones (formalizar)

Promover la atención a entidad de primera clase con ciclo `AGENDADA → EN_PROCESO → ATENDIDA →
COBRADA`, duración real y enlaces bidireccionales cita → atención → sesión/historia → receta →
pago (gran parte de los enlaces ya existen: `Appointment.clinicalSessionId`,
`Payment.appointment`). Menú "Atenciones" con lista del día y captura rápida. Conecta los
módulos 3, 5, 8 y 10.

### P2 — Valor clínico y deuda técnica

- **Reportes clínicos**: gráfico de evolución de puntajes psicométricos por paciente (datos de `Assessment`, Chart.js); comparativa fotográfica de lesiones (datos de `LesionPhoto`).
- **Backup documentado** de `uploads/` (script + guía en `docs/`, siguiendo el estilo de `GUIA_CONFIGURACION_GOOGLE_CALENDAR.md`).
- **Pruebas E2E** (Playwright) de los flujos críticos: login → cita → consulta → receta → cobro.
- **Observabilidad**: Spring Actuator con `health` expuesto solo internamente.

---

## 5. Decisiones de diseño confirmadas

| Pregunta | Decisión |
|----------|----------|
| ¿Nuevas dependencias? | **Solo `spring-boot-starter-mail`** (justificada por recordatorios). WhatsApp vía deep links `wa.me`, sin API de pago. Todo lo demás reutiliza lo existente. |
| ¿Almacenamiento de archivos? | **Local (filesystem)** — bajo `uploads/…`, patrón `ClinicalFileStorage`; sin S3. El backup queda documentado como responsabilidad del despliegue. |
| ¿Alcance por especialidad? | **Sí** — todo recurso nuevo usa `specialty` como discriminator, igual que la historia clínica actual. |
| ¿Borrado? | **Lógico** — `deleted` (+ auditoría `created_at`/`updated_at`/`professional_id` donde aplique), coherente con `POLITICA-AUTORIZACION-CLINICA.md`. Excepción: `audit_log` es inmutable. |
| ¿Autorización? | **Owner-or-admin de la especialidad**, vía `ClinicalAuthorizationService`; `ROLE_SITE_ADMIN` no concede acceso clínico. La consulta de auditoría es solo `ROLE_ADMIN`. |
| ¿Numeración de migraciones? | **Continúa en `V5`** — el historial previo se consolidó en `V1__init_schema.sql`…`V4`. |
| ¿Endpoints públicos nuevos? | **Mínimos y con rate limit** — solo confirmación de cita por token, bajo `/api/v1/public/**` con `PublicRateLimitFilter`. |

---

## 6. Plan por fases

> Regla de trabajo: completar una fase, ejecutar su verificación y revisar el diff antes de
> iniciar la siguiente. Toda tarea que toque API actualiza en el mismo cambio controlador, DTO,
> servicio, cliente Angular, modelo TypeScript y pruebas.

### Fase 0 — Pagos → facturación ✅

- [x] Migración `V5__expand_payments.sql`: `status`, `due_date`, `clinical_session_id` en `payments`; tabla `payment_transactions`; backfill de pagos existentes a `PAGADO` + abono único.
- [x] Backend: ampliar `PaymentService` con abonos parciales, cálculo de `status`, saldo por paciente (`GET /patients/{id}/payments/balance`) y vínculo a sesión.
- [x] Backend: pruebas de servicio (`PaymentServiceTest`): abonos, transiciones de estado, saldo (30 pruebas).
- [x] Frontend: `billing` con estado, registro de abonos, saldo; estado de cuenta imprimible; badge de estado en listado y ficha del paciente.
- [x] Dashboard: reporte financiero por rango de fechas y método de pago, filtrado por `specialty` (panel de resumen en Cobros + KPI "Por cobrar").
- [x] Verificación: cobro parcial/abonos, saldo por paciente, estado de cuenta, reporte. `mvnw test` (OK), `npm test` (102 OK), `npm run build` OK.

### Fase 1 — Recordatorios de citas ✅

- [x] Migración `V6__appointment_reminders.sql`: `reminder_sent_at`, `confirmation_token`, `confirmed_at` en `appointments`.
- [x] Backend: dependencia `spring-boot-starter-mail`, `EmailService`, plantilla de recordatorio, tarea `@Scheduled` diaria (`AppointmentReminderService`), `PublicAppointmentController` (`GET /api/v1/public/appointments/confirm/{token}`) con rate limit (vía `/api/v1/public/**`).
- [x] Config: marcadores SMTP en `backend/.env.example` y `application.yml` (sin credenciales reales); sin `spring.mail.host` no hay bean de correo y el sistema degrada con gracia.
- [x] Backend: pruebas (`AppointmentReminderServiceTest`, `AppointmentConfirmationServiceTest`).
- [x] Frontend: página pública de confirmación (`/confirmar-cita/:token`), indicadores de recordatorio en agenda y botón WhatsApp (`wa.me`).
- [x] Verificación: envío único por cita (guard `reminder_sent_at`), confirmación pública con token, rate limit del endpoint público. `mvnw test` OK, `npm test` (102 OK), `npm run build` OK.

### Fase 2 — Auditoría de acciones ✅

- [x] Migración `V7__create_audit_log.sql`: tabla `audit_log` inmutable.
- [x] Backend: `AuditLogService` + registro en operaciones sensibles (pacientes, historia, recetas, documentos, pagos, usuarios, login); endpoint de consulta `ROLE_ADMIN`.
- [x] Backend: pruebas (`AuditLogServiceTest`, `AuditLogControllerTest`, `ClinicalAuditMigrationTest`).
- [x] Frontend: pantalla `settings/audit` con filtros y paginación; enlace de menú para `ROLE_ADMIN`.
- [x] Verificación: cada operación sensible deja rastro con usuario, IP y detalle; `403` para no-admin. `mvnw test` (205 OK), `npm test` (110 OK), `npm run build` OK.

### Fase 3 — Agenda avanzada ✅

- [x] Migración `V8__agenda_advanced.sql`: `recurrence_group_id`/regla en `appointments`; tablas `professional_schedules` y `schedule_blocks`.
- [x] Backend: generación de series recurrentes, validación de disponibilidad, CRUD de horarios/bloqueos (`ProfessionalScheduleService`, `ScheduleBlockService`, `AppointmentService` con chequeo atómico multi-semana y borrado en serie/ocurrencia).
- [x] Frontend: selector de recurrencia en el formulario de cita con previsualización interactiva de sesiones, gestión de disponibilidad y bloqueos (`ScheduleManagementComponent`), filtro por profesional en agenda (`AgendaComponent`).
- [x] Verificación: serie semanal generada, choque con bloqueo rechazado, edición por serie vs. ocurrencia. `mvnw test` (220 OK), `npm test` (116 OK), `npm run build` OK.

### Fase 4 — Atenciones (formalizar) ✅

- [x] Migración `V9__create_attentions.sql`: entidad de atención con ciclo `AGENDADA → EN_PROCESO → ATENDIDA → COBRADA` y enlaces bidireccionales cita/sesión/receta/pago.
- [x] Backend: entidad `Attention`, DTOs (`AttentionDto`, `AttentionSummaryDto`, `UpdateAttentionStatusRequest`), repositorio `AttentionRepository`, servicio `AttentionService` (cálculo de duraciones, enlaces cruzados, auditoría), controlador `AttentionController` (`/api/v1/attentions`). Enlaces actualizados en `Appointment`, `ClinicalSession`, `Prescription`, `Payment`.
- [x] Frontend: menú lateral "Atenciones" en `main-layout`, modelo `attention.model.ts`, servicio `AttentionService`, pantalla integral `AttentionListComponent` con KPI cards del día, filtros rápidos por fecha/estado, pipeline interactivo de acciones clínicas, modal de captura rápida y diálogo de transición de estado. Botón "Iniciar Atención Clínica" en `AgendaComponent`.
- [x] Verificación: suite backend `mvnw test` (230 tests OK), suite frontend `npm test` (127 tests OK), `npm run build` OK.

### Fase 5 — Valor clínico y deuda técnica ✅

- [x] Reportes clínicos: evolución de puntajes psicométricos (gráfico dinámico por paciente con resumen longitudinal, línea base, delta y tendencia) y comparativa fotográfica de lesiones (modos Side-by-Side y Split Slider con cálculo de días transcurridos).
- [x] Guía de backup de `uploads/` en `docs/GUIA_BACKUP_RESTAURACION_ARCHIVOS.md` y scripts ejecutables multiplataforma (`scripts/backup-uploads.ps1`, `scripts/restore-uploads.ps1`, `scripts/backup-uploads.sh`, `scripts/restore-uploads.sh`) con checksum SHA-256 y rotación.
- [x] Pruebas E2E (Playwright) de flujos críticos (`playwright.config.ts` y 6 suites en `frontend/e2e/`: auth, agenda, atenciones, historia clínica SOAP, recetas/cobros y reportes/evolución).
- [x] Spring Boot Actuator (`health`, `info`) con exposición restringida, sin fuga de configuración interna y tests de integración dedicados.
- [x] Verificación: suite backend `mvnw test` (236 tests OK), suite frontend `npm test` (135 tests OK), `npm run build` OK, y scripts de backup validados.

---

## 7. Orden recomendado de implementación

1. **Pagos → facturación** (Fase 0, P0): ✅ completada — desbloquea el ciclo económico completo (ingresos directos).
2. **Recordatorios de citas** (Fase 1, P0): ✅ completada — reduce el ausentismo; reutiliza patrones probados (scheduled tasks, verificación pública).
3. **Auditoría de acciones** (Fase 2, P1): ✅ completada — registro de operaciones críticas y visor administrativo con filtros y paginación.
4. **Agenda avanzada** (Fase 3, P1): ✅ completada — disponibilidad horaria semanal, bloqueos de agenda y series de citas recurrentes.
5. **Atenciones + valor clínico/técnico** (Fases 4–5, P2): ✅ completada — ciclo de atenciones, reportes de evolución psicométrica, comparador fotográfico de lesiones, Actuator, scripts/guía de backup y Playwright E2E.

---

## 8. Archivos esperados (resumen)

- **Migraciones Flyway**: 3 nuevas restantes (`V7`–`V9`); `V5` (Fase 0) y `V6` (Fase 1) ya aplicadas.
- **Backend nuevos**: `payment_transactions` (+ lógica en `PaymentService`), `EmailService` + `PublicAppointmentController`, `AuditLog`/`AuditLogService`, `ProfessionalSchedule`/`ScheduleBlock`, `Attention` (+ DTOs, repositorios, servicios, controladores por dominio).
- **Backend a modificar**: `Payment`, `Appointment`, `SecurityConfig` (endpoint público de confirmación), `application.yml`/`backend/.env.example` (SMTP), servicios sensibles (auditoría).
- **Frontend nuevos**: página pública de confirmación de cita, pantalla `settings/audit`, gestión de disponibilidad, módulo `attentions`, reportes clínicos.
- **Frontend a modificar**: `billing`, `agenda`, `patient-detail` (saldo), `dashboard` (reporte financiero), `main-layout` (menú), `app.routes.ts`.

---

## 9. Riesgos y deuda a considerar

- **Backup/capacidad** de `uploads/` (documentos, fotos de lesiones, assets) sigue a cargo del despliegue, sin S3; la Fase 5 lo documenta pero no lo automatiza en la nube.
- **Entrega de email (Fase 1)**: depende de credenciales SMTP reales del despliegue; el diseño debe degradar con gracia (log + estado "no enviado") si el correo no está configurado.
- **Abuso del endpoint público** de confirmación: mitigado con `PublicRateLimitFilter` y tokens UUID no secuenciales.
- **Volumen de `audit_log`**: crecimiento continuo; definir retención por tarea programada si se vuelve un problema medido.
- **Recurrencia + Google Calendar**: la sync debe decidir si exporta la serie o cada ocurrencia; resolver en la Fase 3 con pruebas.
- **Matriz de permisos finos**: se mantiene fuera de alcance; 2 roles + especialidad cubren los flujos actuales.

---

## 10. Trabajo completado (histórico)

Fases de la versión anterior de este plan ya verificadas (su esquema vive consolidado en
`V1__init_schema.sql`):

- ✅ **Recetas / indicaciones**: `Prescription` + `PrescriptionItem`, CRUD por paciente con autorización owner-or-admin, impresión con QR y verificación pública (`PrescriptionServiceTest` OK).
- ✅ **Documentos**: `ClinicalDocument` + `ClinicalDocumentStorage` (PDF/imágenes, máx. 5 MB), lista/subida/descarga/vista previa, borrado lógico (`ClinicalDocumentServiceTest` OK).
- ✅ **Pagos → facturación (Fase 0)**: estados `PENDIENTE`/`PARCIAL`/`PAGADO`, abonos parciales (`payment_transactions`), saldo por paciente, vínculo a sesión clínica, estado de cuenta imprimible y reporte financiero por rango de fechas con KPI "Por cobrar".
- ✅ **Recordatorios de citas (Fase 1)**: email de recordatorio (`spring-boot-starter-mail`, `AppointmentReminderService` programado), confirmación pública por token (`AppointmentConfirmationService` + página `/confirmar-cita/:token`), indicador de recordatorio y botón WhatsApp en la agenda. Degradación elegante si SMTP no está configurado.
- ✅ **Usuarios y permisos (gestión)**: listado, habilitar/deshabilitar, reset de contraseña administrativo, reasignación de rol/especialidad, pantalla `settings/users` (resta solo la auditoría, Fase 2 de este plan).
- ✅ **Módulos no contemplados originalmente**: inventario con alertas de stock, servicios clínicos, alertas de riesgo, catálogos dinámicos, editor visual del sitio público, sync con Google Calendar.
