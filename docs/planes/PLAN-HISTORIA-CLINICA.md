# Plan de Implementación: Reestructuración de la Historia Clínica

> **Estado**: ✅ Implementación completa (Fases 0–5). Pendiente solo deprecación física de campos legacy.
> **Última actualización**: 2026-08-13

---

## Decisiones de Diseño Confirmadas

| Pregunta | Decisión |
|----------|----------|
| ¿Historia unificada (ambas especialidades en una vista)? | **No** — se mantiene la separación actual por especialidad |
| ¿Qué rama se muestra? | Solo la rama de la especialidad del usuario autenticado (JWT `specialty`) |
| ¿Cómo se implementan los bloques nuevos? | **Tablas normalizadas nuevas** + migración de datos existentes |
| ¿Fotos de lesiones? | **Sí** — nuevo requerimiento (anula la decisión previa "no fotos") |
| ¿Backend de almacenamiento de fotos? | **Local (filesystem)** — reutilizar el patrón `uploads/` ya existente (logos, website, supplies); sin S3 ni dependencias nuevas |
| ¿`treatments`/`procedures`/`evolutions` como tablas? | **Sí, tablas dedicadas** — son listas/timeline con múltiples registros por paciente (no 1:1) |
| ¿Se toca código ahora? | **No** — solo documento de propuesta |

---

## 1. Diagnóstico del estado actual

La historia clínica está **fragmentada** en 5 entidades y mezcla campos de ambas especialidades dentro de las mismas tablas:

- `MedicalRecord` (`medical_records`): mezcla diagnóstico/medicación/plan (psicología) con tipo de piel/alergias/condiciones/hábitos solares (dermatología).
- `ClinicalSession` (`clinical_sessions`): mezcla campos SOAP (psicología) con hallazgos/diagnóstico/procedimientos/prescripciones (dermatología).
- `DermatologicalEvaluation` (`dermatological_evaluations`): eval. dermatológica plana.
- `Assessment` + `PsychometricTest`: pruebas psicométricas (única parte bien separada).
- `RiskAlert` (`risk_alerts`): alertas de riesgo.

### Mapeo estado actual → estructura objetivo

| Sección objetivo | Estado | Dónde vive hoy |
|---|---|---|
| Antecedentes generales | ❌ No existe | — |
| Alergias | ⚠️ Solo derm | `MedicalRecord.knownAllergies` |
| Medicamentos | ⚠️ Solo derm | `MedicalRecord.currentMedication` |
| PSICOLOGÍA · Evaluación inicial | ❌ No existe | — |
| PSICOLOGÍA · Antecedentes psicológicos | ❌ No existe | — |
| PSICOLOGÍA · Evaluación mental | ⚠️ SOAP suelto | `ClinicalSession.subjective/objective` |
| PSICOLOGÍA · Pruebas psicológicas | ✅ | `Assessment` + `PsychometricTest` |
| PSICOLOGÍA · Diagnósticos | ⚠️ Mezclado | `MedicalRecord.diagnosis` |
| PSICOLOGÍA · Plan terapéutico | ⚠️ Mezclado | `MedicalRecord.treatmentPlan` |
| PSICOLOGÍA · Sesiones / Evoluciones | ✅ | `ClinicalSession` (SOAP) |
| DERMATOLOGÍA · Evaluación inicial | ⚠️ Parcial | `DermatologicalEvaluation` |
| DERMATOLOGÍA · Antecedentes dermatológicos | ⚠️ Repartido | `MedicalRecord.skinType/sunExposure/chronicConditions` |
| DERMATOLOGÍA · Examen dermatológico | ⚠️ Suelto | `ClinicalSession.skinExamFindings` |
| DERMATOLOGÍA · Lesiones + Fotografías | ❌ No existe | — |
| DERMATOLOGÍA · Diagnósticos | ⚠️ Repartido | `ClinicalSession` / `DermatologicalEvaluation` |
| DERMATOLOGÍA · Exámenes auxiliares | ❌ No existe | — |
| DERMATOLOGÍA · Tratamientos | ⚠️ Repartido | `DermatologicalEvaluation.treatmentIndicated` / `ClinicalSession.prescriptions` |
| DERMATOLOGÍA · Procedimientos | ⚠️ Repartido | `DermatologicalEvaluation.procedurePerformed` / `ClinicalSession.proceduresPerformed` |
| DERMATOLOGÍA · Controles / Evoluciones | ⚠️ Repartido | `DermatologicalEvaluation.evolutionNotes` / `ClinicalSession` |

---

## 2. Estructura objetivo

```
PACIENTE
    │
    └── HISTORIA CLÍNICA
          │
          ├── Antecedentes generales
          ├── Alergias
          ├── Medicamentos
          ├── PSICOLOGÍA
          │     ├── Evaluación inicial
          │     ├── Antecedentes psicológicos
          │     ├── Evaluación mental
          │     ├── Pruebas psicológicas
          │     ├── Diagnósticos
          │     ├── Plan terapéutico
          │     └── Sesiones / Evoluciones
          └── DERMATOLOGÍA
                ├── Evaluación inicial
                ├── Antecedentes dermatológicos
                ├── Examen dermatológico
                ├── Lesiones
                │     └── Fotografías
                ├── Diagnósticos
                ├── Exámenes auxiliares
                ├── Tratamientos
                ├── Procedimientos
                └── Controles / Evoluciones
```

Al mantener la separación por especialidad, un usuario ve **"HISTORIA CLÍNICA → [Generales] + [su rama]"**:

- Psicólogo → `Antecedentes generales`, `Alergias`, `Medicamentos`, `PSICOLOGÍA (7)`.
- Dermatólogo → `Antecedentes generales`, `Alergias`, `Medicamentos`, `DERMATOLOGÍA (9)`.

Los bloques generales se almacenan **por especialidad** (`specialty` como discriminator) para no mezclar datos entre consultorios, igual que ya se hace en `medical_records`, `clinical_sessions` y `catalogs`.

---

## 3. Propuesta de modelo de datos (tablas normalizadas)

Convenciones: soft-delete (`deleted`, `deleted_at`, `deleted_by`) y auditoría (`created_at`, `updated_at`, `professional_id`) en todas las tablas nuevas, igual que `MedicalRecord`/`ClinicalSession`. Nombres de columnas en inglés.

### 3.1 Bloques generales (nuevos)

**`general_history`** — antecedentes generales (1 por paciente+especialidad):
`id, patient_id, specialty, pathological_history, surgical_history, family_history, habits, notes, ...`

**`allergies`** — alergias (N por paciente):
`id, patient_id, specialty, allergen, type (MEDICAMENTO/ALIMENTO/CONTACTO/AMBIENTAL), severity (LEVE/MODERADA/GRAVE), reaction, active, notes, ...`

**`medications`** — medicamentos (N por paciente):
`id, patient_id, specialty, name, dose, frequency, start_date, end_date, active, notes, ...`

> Migración de datos: `MedicalRecord.knownAllergies` → `allergies`; `MedicalRecord.currentMedication` → `medications` (texto libre a un único registro inicial, editable después).

### 3.2 Psicología

**`psychology_evaluations`** — cubre "Evaluación inicial + Antecedentes psicológicos + Evaluación mental" como un documento de 3 secciones (N por paciente):
`id, patient_id, evaluation_date, initial_evaluation, psychological_history, mental_exam, notes, ...`

**`diagnoses`** — diagnósticos compartidos (N por paciente, reutilizable por ambas ramas):
`id, patient_id, specialty, category, description, status (ACTIVO/RESUELTO), date, notes, ...`

**`therapeutic_plans`** — plan terapéutico (N por paciente):
`id, patient_id, specialty, objectives, interventions, start_date, end_date, status, notes, ...`

Reutilizados: `assessments` + `psychometric_tests` (Pruebas psicológicas) y `clinical_sessions` (Sesiones/Evoluciones, solo campos SOAP).

### 3.3 Dermatología

**`dermatological_history`** — antecedentes dermatológicos (1 por paciente):
`id, patient_id, skin_type, sun_exposure_habits, personal_skin_history, family_skin_history, chronic_conditions, notes, ...`

**`lesions`** — lesiones (N por paciente):
`id, patient_id, body_area, lesion_type, size, morphology, color, since_date, evolution, notes, ...`

**`lesion_photos`** — fotografías (N por lesión):
`id, lesion_id, file_url, description, taken_date, ...`

> **Almacenamiento resuelto**: archivos en **filesystem local** bajo `uploads/clinical/lesions/`, siguiendo el patrón existente `LocalWebsiteFileStorage` (validación de tipo `.png/.jpg/.jpeg/.webp`, límite 2MB, nombres UUID, `safePath` anti path-traversal) y servidos mediante un endpoint `GET /api/v1/lesions/{id}/photos/{key}` equivalente a `ClinicSettingsController.serveLogo`. Sin S3 ni dependencias externas (coherente con "no añadir dependencias sin necesidad demostrable").

**`auxiliary_exams`** — exámenes auxiliares (N por paciente):
`id, patient_id, exam_type (BIOPSIA/LABORATORIO/...), description, result, exam_date, ...`

**`treatments`** — tratamientos (N por paciente):
`id, patient_id, name, dose, route, frequency, start_date, end_date, status, notes, ...`

**`procedures`** — procedimientos (N por paciente):
`id, patient_id, name, description, procedure_date, notes, ...`

**`evolutions`** — controles/evoluciones (N por paciente):
`id, patient_id, control_date, clinical_notes, next_control_date, ...`

Reutilizado/reestructurado: `dermatological_evaluations` (Evaluación inicial) y `clinical_sessions` (Controles, manteniendo los campos derm si se decide conservar compatibilidad).

> **Decisión resuelta**: `treatments`, `procedures` y `evolutions` se implementan como **tablas dedicadas**. Son colecciones que crecen en el tiempo (múltiples tratamientos, múltiples procedimientos, múltiples controles), no documentos 1:1 como `dermatological_history`. Esto permite listas, trazabilidad y filtrado por fecha, que es exactamente lo que exige la UI (secciones de lista y timeline).

---

## 4. Contrato REST propuesto

Prefijo versionado para los recursos nuevos, coherente con la convención del proyecto.

| Verbo | Ruta | Descripción |
|---|---|---|
| GET | `/api/v1/patients/{id}/general-history` | Obtener antecedentes generales |
| PUT | `/api/v1/patients/{id}/general-history` | Crear/actualizar (upsert, 1 por paciente) |
| GET/POST/PUT/DELETE | `/api/v1/patients/{id}/allergies[/{allergyId}]` | CRUD alergias |
| GET/POST/PUT/DELETE | `/api/v1/patients/{id}/medications[/{medicationId}]` | CRUD medicamentos |
| GET/POST/PUT/DELETE | `/api/v1/patients/{id}/psychology-evaluations[/{id}]` | CRUD evaluación psicológica |
| GET/POST/PUT/DELETE | `/api/v1/patients/{id}/diagnoses[/{id}]` | CRUD diagnósticos |
| GET/POST/PUT/DELETE | `/api/v1/patients/{id}/therapeutic-plans[/{id}]` | CRUD plan terapéutico |
| GET/PUT | `/api/v1/patients/{id}/dermatological-history` | Upsert antecedentes dermatológicos |
| GET/POST/PUT/DELETE | `/api/v1/patients/{id}/lesions[/{id}]` | CRUD lesiones |
| POST/DELETE | `/api/v1/lesions/{id}/photos` | Subir/eliminar fotos |
| GET/POST/PUT/DELETE | `/api/v1/patients/{id}/auxiliary-exams[/{id}]` | CRUD exámenes auxiliares |
| GET/POST/PUT/DELETE | `/api/v1/patients/{id}/treatments[/{id}]` | CRUD tratamientos |
| GET/POST/PUT/DELETE | `/api/v1/patients/{id}/procedures[/{id}]` | CRUD procedimientos |
| GET/POST/PUT/DELETE | `/api/v1/patients/{id}/evolutions[/{id}]` | CRUD controles/evoluciones |

- **Endpoint agregador opcional**: `GET /api/v1/patients/{id}/clinical-history` que devuelva todo el árbol en una sola respuesta (reduce viajes y alimenta la nueva vista con un único loading).
- Todos filtrados por `specialty` del usuario autenticado (igual que `ClinicalSessionService`/`MedicalRecordService`).
- DTOs en `camelCase`; fechas como `string` en TypeScript; borrado lógico con `204`.

---

## 5. Propuesta visual (cambio a nivel de UI)

Reemplazar la página plana actual (`patient-detail.component.html`, ~644 líneas que apilan secciones sin jerarquía) por una vista de **Historia Clínica** navegable:

1. **Sidebar izquierda fija** (colapsable en móvil) que replica el árbol:
   - Header del paciente arriba (nombre, doc, edad, botones "Agendar cita").
   - Grupo **"General"**: Antecedentes generales, Alergias, Medicamentos.
   - Grupo **"PSICOLOGÍA"** o **"DERMATOLOGÍA"** (según JWT) con sus sub-secciones, cada una con contador (ej. "Alergias (2)").
2. **Panel de contenido** (a la derecha) que renderiza la sección activa:
   - Secciones de lista (alergias, medicamentos, lesiones, diagnósticos, tratamientos, procedimientos, exámenes) → cards + botón "Nuevo" + editar/eliminar.
   - Secciones de documento único (antecedentes generales, evaluación psicológica, antecedentes dermatológicos) → formulario de una sola entidad.
   - **Sesiones/Evoluciones y Controles** → timeline cronológico expandible (mantiene el patrón actual de acordeón pero vertical, como línea de tiempo).
   - **Lesiones** → grid de cards con miniaturas de fotos y lightbox.
3. **Estados vacíos** por sección con CTA contextual.
4. Los modales de formulario se mantienen como patrón actual (`showForm`), o se migran a panel lateral derecho (slider) para no perder contexto del árbol.

Componentes nuevos sugeridos (standalone, estilo actual):
`clinical-history-shell`, `general-history-section`, `allergies-section`, `medications-section`, `psychology-section`, `dermatology-section`, `lesions-section` (+ `lesion-photos`), `timeline` reutilizable.

---

## 6. Plan por fases

### Fase 0 — Contrato y migración base
- [x] Crear `V25__clinical_history_general.sql` (general_history, allergies, medications).
- [x] Crear `V26__clinical_history_psychology.sql` (psychology_evaluations, diagnoses, therapeutic_plans).
- [x] Crear `V27__clinical_history_dermatology.sql` (dermatological_history, lesions, lesion_photos, auxiliary_exams, treatments, procedures, evolutions).
- [x] Migrar datos: `knownAllergies`→`allergies`, `currentMedication`→`medications`, `diagnosis`→`diagnoses`, `treatmentPlan`→`therapeutic_plans`, campos derm de `medical_records`→`dermatological_history`, y campos de `dermatological_evaluations`→`diagnoses/treatments/procedures/evolutions`.
- [x] Validar sintaxis y referencias contra PostgreSQL real (35 sentencias OK, rollback).

### Fase 1 — Bloques generales (backend + frontend)
- [x] Backend: entidades, DTOs, repositorios, servicios y controladores para `GeneralHistory` (upsert), `Allergy` y `Medication` (CRUD paginado), con alcance por especialidad vía `ClinicalAuthorizationService`.
- [x] Backend: pruebas de servicio (`GeneralHistoryServiceTest`, `AllergyServiceTest`).
- [x] Frontend: modelos y servicios (`general-history`, `allergy`, `medication`).
- [x] Frontend: secciones `general-history-section`, `allergies-section`, `medications-section` integradas en `patient-detail` bajo "Historia Clínica — Datos Generales".

### Fase 2 — Psicología
- [x] Backend: `PsychologyEvaluation` (CRUD, `@PreAuthorize` PSICOLOGIA, owner-or-admin), `Diagnosis` y `TherapeuticPlan` (CRUD paginado, alcance por especialidad).
- [x] Backend: pruebas de servicio (`PsychologyEvaluationServiceTest`, `DiagnosisServiceTest`).
- [x] Frontend: modelos y servicios (`psychology-evaluation`, `diagnosis`, `therapeutic-plan`).
- [x] Frontend: secciones `psychology-evaluation-section`, `diagnoses-section`, `therapeutic-plans-section` integradas en `patient-detail` bajo el grupo "Psicología" (`@if (isPsychology)`).
- Se conservan `assessments` (pruebas psicológicas) y `clinical_sessions` (sesiones) existentes.

### Fase 3 — Dermatología + lesiones/fotos
- [x] Backend: `ClinicalFileStorage` (filesystem local `uploads/clinical`, patrón `LocalWebsiteFileStorage`).
- [x] Backend: `DermatologicalHistory` (upsert), `Lesion` + `LesionPhoto` (upload/list/delete/serve), `AuxiliaryExam`, `Treatment`, `Procedure`, `Evolution` (CRUD paginado, `@PreAuthorize` DERMATOLOGIA).
- [x] Backend: pruebas de servicio (`DermatologicalHistoryServiceTest`, `LesionServiceTest`).
- [x] Frontend: modelos y servicios (`dermatological-history`, `lesion`, `lesion-photo`, `auxiliary-exam`, `treatment`, `procedure`, `evolution`).
- [x] Frontend: secciones `dermatological-history-section`, `lesions-section` (con fotos), `auxiliary-exams-section`, `treatments-section`, `procedures-section`, `evolutions-section` + reutiliza `diagnoses-section`; integradas en `patient-detail` bajo el grupo "Dermatología" (`@if (isDermatology)`).

### Fase 4 — Shell visual y navegación
- [x] Refactor de `patient-detail` a shell: header del paciente + sidebar izquierda (árbol agrupado: Paciente / General / Especialidad / Otros) + panel de contenido con `@switch`.
- [x] Migradas todas las secciones al nuevo layout: datos personales, bloques generales, rama Psicología (evaluación inicial, pruebas, diagnósticos, plan, sesiones), rama Dermatología (evaluación inicial, antecedentes, lesiones, diagnósticos, exámenes, tratamientos, procedimientos, controles, sesiones), historial de tratamientos (legacy) y alertas de riesgo.
- [x] Evaluaciones dermatológicas embebidas inline (`dermatological-evaluation-list`) en lugar de enlace a página separada.
- [x] Verificación: `ng build` OK; navegación condicional por especialidad vía `SpecialtyService`.

### Fase 5 — Limpieza y verificación
- [x] Marcar `Historial de Tratamientos` (MedicalRecord) como **legacy** en la UI (etiqueta + aviso), conservando la tabla y endpoints para no perder datos.
- [x] Actualizar `MockDataSeeder` para sembrar las tablas nuevas (general, alergias, medicamentos, psicología, dermatología, lesiones, etc.) en bases limpias.
- [x] Pruebas backend completas: `mvnw test` → 59 tests, 0 fallos.
- [x] Build frontend: `ng build` OK. Tests frontend: 35 OK, 4 fallos preexistentes (MainLayout ×2, LoginComponent, authGuard), ajenos a este cambio.

---

## 7. Riesgos y deuda a considerar

- **Fotos**: resuelto — almacenamiento local (`uploads/clinical/lesions/`) con límite 2MB y validación de tipo; el backup/capacidad queda como responsabilidad operativa del despliegue.
- **Compatibilidad**: `ClinicalSession` y `MedicalRecord` ya mezclan especialidades; la reestructuración debe decidir si se deprecan sus campos derm/SOAP cruzados o se conservan como legacy.
- **Número de tablas**: resuelto — `treatments`/`procedures`/`evolutions` son tablas dedicadas por ser colecciones (listas/timeline), no documentos 1:1.
- **Esfuerzo**: la Fase 3 (lesiones + fotos + exámenes) es la de mayor alcance; el resto es en su mayoría CRUD estándar.

---

## 8. Archivos esperados (resumen)

- **Migraciones Flyway**: ~3 nuevas (general, psicología, dermatología).
- **Backend nuevos**: ~10 entidades, ~10 DTOs, ~10 repositorios, ~10 servicios, ~10 controladores.
- **Frontend nuevos**: ~10 componentes de sección + modelos + servicios por dominio.
- **Frontend a modificar**: `patient-detail`, `app.routes.ts`, servicios/modelos existentes de sesión/registro médico.

---

## 9. Deuda restante (no bloqueante)

1. ~~Deprecación física de campos legacy~~ → **Resuelto**: migración `V28` retira `medical_records` y los campos derm de `clinical_sessions`, y la UI/entidades/DTOs ya no los referencian.
2. ~~Autorización inconsistente~~ → **Resuelto**: todos los recursos de la historia clínica usan owner-or-admin (lectura subdividida por profesional + escritura creador/`ROLE_ADMIN`), alineado con `POLITICA-AUTORIZACION-CLINICA.md`.
3. ~~Migración "mejor esfuerzo" con truncado~~ → **Resuelto**: columnas de texto libre convertidas a `TEXT` y re-migración del texto completo en `V28`. La granularidad (dividir "Penicilina, polen" en filas) sigue siendo manual.
4. ~~Ruta huérfana~~ `/patients/:id/dermatological-evaluations` → **Resuelto**: ruta y componente `dermatological-evaluation-page` retirados; la evaluación dermatológica se usa embebida en `patient-detail`.
5. **Backup/capacidad** de `uploads/clinical/lesions/` queda a cargo del despliegue (sin S3).
6. ~~Tests frontend preexistentes~~ → **Resuelto**: corregidos `authGuard`, `MainLayoutComponent` (mock `AuthService.hasRole`) y `LoginComponent` (proveedores de `Router`/`ActivatedRoute`/`AuthService`). Suite frontend: 39/39 OK.
7. ~~Endpoint agregador~~ `GET /api/v1/patients/{id}/clinical-history` → **Resuelto**: implementado (`ClinicalHistoryDto`, `ClinicalHistoryService`, `ClinicalHistoryController`) devolviendo el árbol completo por especialidad. La UI mantiene carga perezosa por sección; el agregador queda disponible para exportación o vista completa.
8. ~~Contadores por sección en la sidebar~~ → **Resuelto**: la sidebar muestra badges con el número de registros (vía `ClinicalHistoryService` agregador para las secciones nuevas + `sessions`/`activeAlerts` ya cargados); se refrescan al navegar entre secciones.
