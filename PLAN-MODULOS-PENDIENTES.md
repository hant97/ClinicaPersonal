# Plan de Implementación: Módulos Pendientes y Expansión Funcional

> **Estado**: ⏳ Propuesta pendiente de autorización para iniciar ejecución
> **Última actualización**: 2026-08-14
> **Base**: análisis del sistema actual (backend Java 17/Spring Boot, frontend Angular 18, 29 migraciones Flyway) y de los planes de referencia `PLAN-MULTI-ESPECIALIDAD.md`, `PLAN-HISTORIA-CLINICA.md`, `docs/`.

---

## 1. Resumen ejecutivo

El sistema ya es maduro: multi-especialidad (Psicología/Dermatología), historia clínica
reestructurada en tablas normalizadas, seguridad de sesiones (JWT + refresh rotatorio +
revocación), autorización clínica owner-or-admin, borrado lógico con auditoría y sitio público
editable. De los 10 módulos propuestos, **5 están completos, 2 parciales y 3 son brechas
reales** (Recetas, Documentos y la expansión de Pagos).

---

## 2. Estado por módulo

| # | Módulo | Estado | Qué existe hoy |
|---|--------|--------|----------------|
| 1 | Usuarios y permisos | ⚠️ Parcial | `User` (specialty, roles, enabled), auth JWT+refresh+logout, `LoginAttemptService`, crear usuario (admin), perfil, cambio de contraseña. Roles: solo `ROLE_ADMIN` y `ROLE_SITE_ADMIN`. Guardas frontend `auth`/`specialty`/`site-admin`. |
| 2 | Pacientes | ✅ Completo | CRUD paginado + búsqueda, género obligatorio, `specialty`, ficha con shell de historia clínica, soft delete. |
| 3 | Agenda y citas | ✅ Básico | `Appointment` con estado, formulario y vista de agenda. |
| 4 | Historia clínica | ✅ Completo | Árbol por especialidad (`general-history`, `allergies`, `medications`, `diagnoses`, `therapeutic-plans`, `psychology-evaluations`, `dermatological-history`, `lesions`, `auxiliary-exams`, `treatments`, `procedures`, `evolutions`), agregador `GET /clinical-history`, autorización owner-or-admin. |
| 5 | Atenciones | ⚠️ Implícito | `ClinicalSession` (SOAP psicología / campos derm) funciona como "atención" pero vive **embebida** en la ficha del paciente; no hay flujo cita→atención de primera clase. |
| 6 | Psicología | ✅ Completo | Evaluación (`PsychologyEvaluation`), Sesiones (SOAP), Diagnósticos, Plan terapéutico + pruebas psicométricas (`Assessment`/`PsychometricTest`). |
| 7 | Dermatología | ✅ Completo | Examen (`DermatologicalEvaluation` + `DermatologicalHistory`), Lesiones, Fotografías (`LesionPhoto` en filesystem local), Diagnósticos, Tratamientos + procedimientos/exámenes/evoluciones. |
| 8 | Recetas / indicaciones | ❌ No existe | Solo hubo campos de texto legacy (`prescriptions`) deprecados en `V28`. |
| 9 | Documentos | ❌ No existe (general) | Solo fotos de lesiones y assets del sitio. No hay gestión documental del paciente. |
| 10 | Pagos básicos | ⚠️ Básico | `Payment` + `PaymentItem`, métodos CASH/TRANSFER/CARD, CRUD y listado por paciente. |

---

## 3. Brechas identificadas (priorizadas)

1. **Recetas / indicaciones (gap completo)** — no hay entidad, ni flujo, ni impresión.
2. **Documentos (gap completo)** — solo storage de lesiones; falta gestión documental genérica (consentimientos, resultados, derivaciones, adjuntos).
3. **Pagos → facturación** — sin estado (pendiente/parcial/pagado), sin cuotas, sin recibo/comprobante PDF, sin saldo por paciente, sin vínculo cita/atención→pago, sin reportes.
4. **Usuarios y permisos** — no hay listado/gestión (habilitar/deshabilitar, reset de contraseña, reasignar rol), ni matriz de permisos finos, ni auditoría de acciones.
5. **Atenciones** — no está formalizado el encuentro clínico como entidad con ciclo de vida y facturación asociada.
6. **Agenda** — sin recordatorios/confirmaciones, recurrencia, disponibilidad/bloqueos ni vista por profesional.

---

## 4. Propuestas de implementación

### P0 — Recetas / indicaciones (nuevo módulo)

Modelo `Prescription` (cabecera + `PrescriptionItem`) con `patient_id`, `specialty`,
`professional_id`, fecha, vigencia, e ítems (medicamento/indicación, dosis, frecuencia, duración,
vía, instrucciones). Reutiliza `Medication` y `Treatment` existentes. Flujo: crear desde la ficha
del paciente y desde la sesión/atención; **impresión/PDF** (ya existe `clinical-history-print`
como patrón) y descarga. `specialty` como discriminator, soft delete y autorización
owner-or-admin (consistente con `POLITICA-AUTORIZACION-CLINICA.md`).

### P0 — Documentos (nuevo módulo genérico)

Entidad `ClinicalDocument` (paciente, especialidad, categoría: consentimiento/resultado/
derivación/receta/otro, nombre, MIME, ruta, tamaño, fecha, profesional). Reutilizar
`ClinicalFileStorage` (ya validado con lesiones) y un endpoint `serve` equivalente a
`ClinicSettingsController.serveLogo`. Lista + subida + descarga + vista previa + borrado lógico.
Cubre consentimientos informados y adjuntos que hoy no tienen dónde vivir.

### P1 — Pagos → facturación

Añadir `status` (PENDIENTE/PARCIAL/PAGADO), `due_date`, soporte de **cuotas/abonos parciales**,
vínculo `appointment_id`/`clinical_session_id`, **saldo por paciente** (suma cobrado vs pendiente),
y **comprobante PDF** reutilizando el patrón de impresión. Reporte financiero básico por rango de
fechas y método de pago en el Dashboard (filtrado por `specialty`).

### P1 — Usuarios y permisos

Listado de usuarios (admin de especialidad), habilitar/deshabilitar (usa `enabled` ya existente),
reset de contraseña administrativo, reasignación de rol/especialidad, y **auditoría de acciones**
(log de cambios en usuarios y registros sensibles, ampliando los campos de auditoría ya presentes).
Definir matriz de permisos finos solo si se detecta necesidad real (hoy 2 roles + especialidad es
suficiente para la mayoría de flujos).

### P2 — Atenciones (formalizar)

Promover la atención clínica a entidad de primera clase con ciclo `AGENDADA → EN PROCESO →
ATENDIDA → COBRADA`, duración real, y enlace bidireccional cita → atención → sesión/historia →
receta → pago. Menú "Atenciones" con lista del día y captura rápida. Conecta los módulos 3, 5, 8
y 10 que hoy están aislados.

### P2 — Agenda

Recordatorios/confirmación de citas (email/WhatsApp, según infraestructura), recurrencia, bloques
de disponibilidad por profesional y vista semanal/mensual mejorada.

---

## 5. Decisiones de diseño confirmadas

| Pregunta | Decisión |
|----------|----------|
| ¿Nuevas dependencias? | **No** — se reutiliza `ClinicalFileStorage` (filesystem local) y el patrón de impresión `clinical-history-print`. |
| ¿Almacenamiento de archivos? | **Local (filesystem)** — bajo `uploads/clinical/…`, patrón `LocalWebsiteFileStorage`; sin S3. |
| ¿Alcance por especialidad? | **Sí** — todo recurso nuevo usa `specialty` como discriminator, igual que la historia clínica actual. |
| ¿Borrado? | **Lógico** — `deleted`, `deleted_at`, `deleted_by` + auditoría `created_at`/`updated_at`/`professional_id`, coherente con `POLITICA-AUTORIZACION-CLINICA.md`. |
| ¿Autorización? | **Owner-or-admin de la especialidad**, vía `ClinicalAuthorizationService`; `ROLE_SITE_ADMIN` no concede acceso clínico. |

---

## 6. Plan por fases

> Regla de trabajo: completar una fase, ejecutar su verificación y revisar el diff antes de
> iniciar la siguiente. Toda tarea que toque API actualiza en el mismo cambio controlador, DTO,
> servicio, cliente Angular, modelo TypeScript y pruebas.

### Fase 0 — Recetas / indicaciones ✅

- [x] Migración `V30__create_prescriptions.sql`: tablas `prescriptions` y `prescription_items`.
- [x] Backend: `Prescription` + `PrescriptionItem` (entidades), DTOs, repositorio, servicio y controlador (`GET/POST/PUT/DELETE /api/v1/patients/{id}/prescriptions[/{id}]`), filtrado por especialidad y autorización owner-or-admin.
- [x] Backend: pruebas de servicio (`PrescriptionServiceTest`).
- [x] Frontend: modelo y servicio (`prescription.model.ts`, `prescription.service.ts`).
- [x] Frontend: sección `prescriptions-section` integrada en `patient-detail` (pestaña "Expediente General" → "Recetas", transversal a ambas especialidades) + impresión.
- [x] Verificación: `mvnw test` (96 pruebas OK), `npm test` (66 OK), `npm run build` OK.

### Fase 1 — Documentos ✅

- [x] Migración `V31__create_clinical_documents.sql`: tabla `clinical_documents`.
- [x] Backend: `ClinicalDocument` (entidad), DTO, repositorio, servicio y controlador con almacenamiento en `ClinicalDocumentStorage` (PDF + imágenes, máx. 5MB, patrón `ClinicalFileStorage`), endpoints de lista/subida/descarga/borrado + `serve`.
- [x] Backend: pruebas de servicio (`ClinicalDocumentServiceTest`).
- [x] Frontend: modelo, servicio y sección `documents-section` (lista, subida con validación de tipo/tamaño, descarga, vista previa) integrada en `patient-detail` (pestaña "Expediente General" → "Documentos").
- [x] Verificación: subida/descarga/preview de documentos; borrado lógico; restricción por especialidad. `mvnw test` (98 OK), `npm test` (68 OK), `npm run build` OK.

### Fase 2 — Pagos → facturación

- [ ] Migración `V32__expand_payments.sql`: `status`, `due_date`, `appointment_id`, `clinical_session_id` y tabla de abonos si se adoptan cuotas.
- [ ] Backend: ampliar `PaymentService` con saldo por paciente, estados y abonos parciales.
- [ ] Backend: pruebas de servicio (`PaymentServiceTest`).
- [ ] Frontend: ampliar `billing` con estado, cuotas, saldo y comprobante PDF.
- [ ] Dashboard: reporte financiero básico por rango de fechas y método de pago, filtrado por `specialty`.
- [ ] Verificación: cobro parcial/abonos, saldo por paciente, comprobante PDF, reporte.

### Fase 3 — Usuarios y permisos + auditoría

- [ ] Backend: listado de usuarios (admin de especialidad), habilitar/deshabilitar, reset de contraseña administrativo, reasignación de rol/especialidad.
- [ ] Backend: auditoría de acciones (log de cambios en usuarios y registros sensibles).
- [ ] Backend: pruebas (`UserServiceTest`, auditoría).
- [ ] Frontend: pantalla `users` (listado + gestión) y enlace de menú para `ROLE_ADMIN`.
- [ ] Verificación: flujos de habilitar/deshabilitar/reset; `401`/`403` según rol.

### Fase 4 — Atenciones (formalizar)

- [ ] Diseñar entidad de atención con ciclo `AGENDADA → EN PROCESO → ATENDIDA → COBRADA` y enlaces cita/sesión/receta/pago.
- [ ] Migración `V33__create_attentions.sql`.
- [ ] Backend: entidad, DTO, repositorio, servicio y controlador.
- [ ] Frontend: menú "Atenciones" con lista del día y captura rápida.
- [ ] Verificación: flujo completo cita → atención → sesión → receta → pago.

### Fase 5 — Agenda

- [ ] Recordatorios/confirmación de citas (según infraestructura disponible).
- [ ] Recurrencia de citas.
- [ ] Bloques de disponibilidad por profesional y vista semanal/mensual mejorada.
- [ ] Verificación: recordatorios, recurrencia y disponibilidad.

---

## 7. Orden recomendado de implementación

1. **Recetas + Documentos** (Fases 0–1, P0): únicas brechas funcionales completas, con patrón claro (storage + impresión ya existentes).
2. **Pagos → facturación** (Fase 2, P1): desbloquea el ciclo económico completo.
3. **Usuarios/permisos + auditoría** (Fase 3, P1): necesario antes de ampliar el uso a más profesionales.
4. **Atenciones + Agenda** (Fases 4–5, P2): integración de flujo y UX.

---

## 8. Archivos esperados (resumen)

- **Migraciones Flyway**: ~4 nuevas (`V30`–`V33`).
- **Backend nuevos**: entidades `Prescription`/`PrescriptionItem`/`ClinicalDocument`/`Attention` (+ DTOs, repositorios, servicios, controladores por dominio).
- **Backend a modificar**: `Payment`/`PaymentService`, `UserService`/`UserController`, auditoría.
- **Frontend nuevos**: secciones `prescriptions-section`, `documents-section`, pantalla `users`, módulo `attentions` (+ modelos/servicios por dominio).
- **Frontend a modificar**: `billing`, `main-layout` (menú), `app.routes.ts`, `dashboard`.

---

## 9. Riesgos y deuda a considerar

- **Backup/capacidad** de `uploads/clinical/…` (documentos y recetas impresas) queda a cargo del despliegue, sin S3.
- **Impresión/PDF**: depende del patrón `clinical-history-print`; si requiere una librería de generación de PDF, debe justificarse como necesidad demostrable antes de añadirla.
- **Recordatorios (Fase 5)**: depende de infraestructura de notificación aún no definida; se aísla para no bloquear las fases anteriores.
- **Matriz de permisos finos**: se propone solo si se detecta necesidad real; hoy 2 roles + especialidad cubren la mayoría de flujos.
