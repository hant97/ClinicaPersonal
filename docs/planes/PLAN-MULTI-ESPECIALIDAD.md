# Plan de Implementación: Sistema Multi-Especialidad (Psicología + Dermatología)

> **Estado**: ⏳ Pendiente de autorización para iniciar ejecución
> **Última actualización**: 2026-08-12

---

## Decisiones de Diseño Confirmadas

| Pregunta | Decisión |
|----------|----------|
| ¿Multi-especialidad por usuario? | **No** — cada usuario tiene una sola especialidad fija |
| ¿Pacientes compartidos? | **Sí** — un paciente puede ser atendido por ambas especialidades |
| ¿Subida de fotos clínicas? | **No** — no se implementa por ahora |
| ¿Más especialidades a futuro? | **No** — solo Psicología y Dermatología |
| ¿Consultorio compartido? | **No** — cada especialidad tiene su propia clínica independiente |
| ¿Catálogos de sesión/modalidad compartidos? | **No** — independientes por especialidad |

---

## Fase 1: Infraestructura de Especialidad

> Objetivo: Establecer el campo `specialty` en todo el stack, desde la base de datos hasta el JWT y el frontend. Al finalizar esta fase, el sistema reconoce la especialidad del usuario autenticado.

### 1.1 Backend — Base de datos y entidad User

- [x] **1.1.1** Crear migración `V6__add_specialty_to_users.sql`
  - Agregar columna `specialty VARCHAR(50) NOT NULL DEFAULT 'PSICOLOGIA'` a tabla `users`
- [x] **1.1.2** Modificar `User.java`
  - Agregar campo `private String specialty`
  - Agregar anotación `@Column(name = "specialty", nullable = false)`
- [x] **1.1.3** Modificar `CreateUserRequest.java`
  - Agregar campo `specialty` con validación `@NotBlank`
- [x] **1.1.4** Modificar `UserProfileDTO.java`
  - Agregar campo `specialty`
- [x] **1.1.5** Modificar `UserService.java`
  - Mapear `specialty` en la creación de usuarios y en el perfil
- [x] **1.1.6** Modificar `UserController.java`
  - Asegurar que `specialty` se incluya en las respuestas del perfil

### 1.2 Backend — JWT y Autenticación

- [x] **1.2.1** Modificar `JwtService.java`
  - Incluir claim `specialty` al generar el token (`extraClaims.put("specialty", ...)`)
- [x] **1.2.2** Modificar `AuthResponse.java`
  - Agregar campo `specialty` para que el frontend lo reciba directamente en el login

### 1.3 Backend — Datos iniciales

- [x] **1.3.1** Modificar `DataInitializer.java`
  - Asignar `specialty = "PSICOLOGIA"` al usuario admin por defecto
  - Crear un segundo usuario admin para dermatología: `admin_derm` / `admin123` con `specialty = "DERMATOLOGIA"`

### 1.4 Backend — ClinicSettings independientes por especialidad

- [x] **1.4.1** Crear migración `V6b__add_specialty_to_clinic_settings.sql`
  - Agregar columna `specialty VARCHAR(50)` a tabla `clinic_settings`
  - Actualizar registros existentes con `specialty = 'PSICOLOGIA'`
- [x] **1.4.2** Modificar `ClinicSettings.java`
  - Agregar campo `specialty`
- [x] **1.4.3** Modificar `ClinicSettingsDto.java`
  - Agregar campo `specialty`
- [x] **1.4.4** Modificar `ClinicSettingsService.java`
  - Filtrar configuración de clínica por la especialidad del usuario autenticado
- [x] **1.4.5** Modificar `ClinicSettingsController.java`
  - Obtener la especialidad del usuario autenticado y pasarla al servicio

### 1.5 Frontend — Servicio de Especialidad

- [x] **1.5.1** Crear `core/services/specialty.service.ts`
  - Decodificar JWT para extraer claim `specialty`
  - Exponer métodos: `getSpecialty()`, `isPsychology()`, `isDermatology()`
- [x] **1.5.2** Modificar `auth.service.ts`
  - Almacenar `specialty` del login response en localStorage
- [x] **1.5.3** Modificar `user-profile.model.ts`
  - Agregar campo `specialty?: string` a `UserProfile`

### 1.6 Verificación de Fase 1

- [x] **1.6.1** Compilar backend sin errores
- [x] **1.6.2** Ejecutar migración Flyway exitosamente
- [x] **1.6.3** Login con usuario psicología → JWT contiene `specialty: "PSICOLOGIA"`
- [x] **1.6.4** Login con usuario dermatología → JWT contiene `specialty: "DERMATOLOGIA"`
- [x] **1.6.5** Cada especialidad ve su propia configuración de clínica

---

## Fase 2: Menú Lateral y Navegación Condicional

> Objetivo: El menú de la aplicación se adapta dinámicamente según la especialidad. El psicólogo ve sus módulos exclusivos y el dermatólogo los suyos.

### 2.1 Frontend — Menú lateral dinámico

- [x] **2.1.1** Modificar `main-layout.component.ts`
  - Inyectar `SpecialtyService`
  - Exponer propiedades `isPsychology` e `isDermatology` al template
- [x] **2.1.2** Modificar `main-layout.component.html`
  - Envolver enlace "Pruebas" con `@if (isPsychology)`
  - Agregar enlace "Evaluaciones Derm." con `@if (isDermatology)`
  - Mantener enlaces compartidos sin condicional: Inicio, Agenda, Pacientes, Atención clínica, Cobros, Servicios, Inventario, Configuración

### 2.2 Frontend — Rutas condicionales

- [x] **2.2.1** Modificar `app.routes.ts`
  - Agregar ruta `dermatological-evaluations` (lazy o directa)
  - Mantener ruta `tests-catalog` existente
- [x] **2.2.2** Crear guard `specialty.guard.ts` (opcional)
  - Proteger rutas exclusivas de cada especialidad para evitar acceso por URL directa

### 2.3 Verificación de Fase 2

- [x] **2.3.1** Login como psicólogo → menú muestra "Pruebas", NO muestra "Evaluaciones Derm."
- [x] **2.3.2** Login como dermatólogo → menú muestra "Evaluaciones Derm.", NO muestra "Pruebas"
- [x] **2.3.3** Módulos compartidos visibles para ambos perfiles

---

## Fase 3: Historial Clínico Adaptado por Especialidad

> Objetivo: Las sesiones clínicas y registros médicos se adaptan según la especialidad. El formulario de sesión muestra campos SOAP para psicología y campos dermatológicos para dermatología. Los catálogos de tipos de sesión y modalidad son independientes.

### 3.1 Backend — Sesiones clínicas con especialidad

- [x] **3.1.1** Crear migración `V7__add_dermatology_fields_clinical_sessions.sql`
  - Agregar `specialty VARCHAR(50) NOT NULL DEFAULT 'PSICOLOGIA'`
  - Agregar `skin_exam_findings TEXT` (hallazgos examen de piel)
  - Agregar `dermatological_diagnosis TEXT`
  - Agregar `procedures_performed TEXT`
  - Agregar `prescriptions TEXT`
- [x] **3.1.2** Modificar `ClinicalSession.java`
  - Agregar campo `specialty` y campos dermatológicos
- [x] **3.1.3** Modificar `ClinicalSessionDto.java`
  - Agregar campos dermatológicos al DTO
- [x] **3.1.4** Modificar `ClinicalSessionService.java`
  - Asignar `specialty` automáticamente desde el usuario autenticado al crear sesión
  - Filtrar sesiones por especialidad del usuario
- [x] **3.1.5** Modificar `ClinicalSessionController.java`
  - Pasar el usuario autenticado al servicio para determinar especialidad

### 3.2 Backend — Registros médicos con especialidad

- [x] **3.2.1** Crear migración `V8__add_dermatology_fields_medical_records.sql`
  - Agregar `specialty VARCHAR(50) DEFAULT 'PSICOLOGIA'`
  - Agregar `skin_type VARCHAR(50)`
  - Agregar `known_allergies TEXT`
  - Agregar `chronic_conditions TEXT`
  - Agregar `sun_exposure_habits TEXT`
- [x] **3.2.2** Modificar `MedicalRecord.java`
  - Agregar campo `specialty` y campos dermatológicos
- [x] **3.2.3** Modificar `MedicalRecordDto.java`
  - Agregar campos dermatológicos al DTO
- [x] **3.2.4** Modificar `MedicalRecordService.java`
  - Asignar `specialty` al crear registro
  - Filtrar registros por especialidad del usuario
- [x] **3.2.5** Modificar `MedicalRecordController.java`
  - Pasar usuario autenticado al servicio

### 3.3 Backend — Catálogos independientes por especialidad

- [x] **3.3.1** Crear migración `V9__add_specialty_to_catalogs.sql`
  - Agregar columna `specialty VARCHAR(50)` a tabla `catalogs`
  - Actualizar catálogos existentes con `specialty = 'PSICOLOGIA'`
- [x] **3.3.2** Modificar `Catalog.java` — Agregar campo `specialty`
- [x] **3.3.3** Modificar `CatalogService.java` — Filtrar catálogos por especialidad
- [x] **3.3.4** Modificar `CatalogController.java` — Pasar especialidad
- [x] **3.3.5** Crear migración `V10__seed_dermatology_catalogs.sql`
  - Insertar catálogos: `SKIN_TYPE`, `LESION_TYPE`, `BODY_AREA`, `DERM_PROCEDURE`, `DERM_SESSION_TYPE`, `RISK_ALERT_TYPE_DERM`
  - Insertar catálogos de tipos de sesión y modalidad para dermatología (independientes de psicología)
- [x] **3.3.6** Modificar `CatalogDataInitializer.java`
  - Asegurar que los catálogos iniciales tengan `specialty` asignada

### 3.4 Frontend — Modelos actualizados

- [x] **3.4.1** Modificar `clinical-session.model.ts`
  - Agregar `specialty?: string` y campos dermatológicos opcionales
- [x] **3.4.2** Modificar `medical-record.model.ts`
  - Agregar `specialty?: string` y campos dermatológicos opcionales

### 3.5 Frontend — Formularios condicionales

- [x] **3.5.1** Modificar formulario de sesión clínica (`clinical-session-form`)
  - Si `isPsychology()`: mostrar campos SOAP (Subjetivo, Objetivo, Análisis, Plan) — **sin cambios**
  - Si `isDermatology()`: mostrar campos dermatológicos (Hallazgos, Diagnóstico, Procedimientos, Prescripciones)
  - Inyectar `SpecialtyService`
- [x] **3.5.2** Modificar formulario de registro médico (`medical-record-form`)
  - Si `isPsychology()`: campos actuales (Diagnóstico, Medicación, Plan, Estado) — **sin cambios**
  - Si `isDermatology()`: campos adicionales (Tipo de piel, Alergias, Condiciones crónicas, Hábitos solares)
  - Inyectar `SpecialtyService`

### 3.6 Frontend — Vista de detalle del paciente

- [x] **3.6.1** Modificar `patient-detail.component.ts`
  - Inyectar `SpecialtyService`
  - Exponer `isPsychology` e `isDermatology`
- [x] **3.6.2** Modificar `patient-detail.component.html`
  - Sección "Evaluaciones Psicométricas": envolver con `@if (isPsychology)`
  - Sección "Historia Clínica (Sesiones)": adaptar vista expandida según especialidad
  - Sección "Historial de Tratamientos": adaptar cards según especialidad (mostrar campos dermatológicos cuando aplique)

### 3.7 Verificación de Fase 3

- [x] **3.7.1** Compilar backend sin errores
- [x] **3.7.2** Migraciones V7, V8, V9, V10 ejecutan correctamente
- [x] **3.7.3** Psicólogo crea sesión → campos SOAP se guardan; campos derm son null
- [x] **3.7.4** Dermatólogo crea sesión → campos derm se guardan; campos SOAP son null
- [x] **3.7.5** Cada especialidad ve solo sus catálogos
- [x] **3.7.6** Registro médico de psicología muestra diagnóstico/medicación/plan
- [x] **3.7.7** Registro médico de dermatología muestra tipo piel/alergias/condiciones

---

## Fase 4: Evaluaciones Dermatológicas

> Objetivo: Crear el módulo completo de evaluaciones dermatológicas (equivalente a las pruebas psicométricas pero orientado a dermatología). Sin funcionalidad de fotos por ahora.

### 4.1 Backend — Entidad y API

- [x] **4.1.1** Crear migración `V11__create_dermatological_evaluations.sql`
  - Tabla `dermatological_evaluations` con campos: `id`, `patient_id`, `evaluation_date`, `skin_type`, `affected_area`, `lesion_type`, `lesion_size`, `dermatological_diagnosis`, `treatment_indicated`, `procedure_performed`, `evolution_notes`, `next_review_date`, `professional_id`, `created_at`, `updated_at`
- [x] **4.1.2** Crear `model/DermatologicalEvaluation.java` — Entidad JPA
- [x] **4.1.3** Crear `dto/DermatologicalEvaluationDto.java` — DTO
- [x] **4.1.4** Crear `repository/DermatologicalEvaluationRepository.java` — Repositorio JPA
- [x] **4.1.5** Crear `service/DermatologicalEvaluationService.java` — Servicio con lógica CRUD
- [x] **4.1.6** Crear `controller/DermatologicalEvaluationController.java` — Endpoints REST
  - `GET /api/v1/patients/{patientId}/dermatological-evaluations` — listar por paciente
  - `POST /api/v1/patients/{patientId}/dermatological-evaluations` — crear
  - `PUT /api/v1/dermatological-evaluations/{id}` — actualizar
  - `DELETE /api/v1/dermatological-evaluations/{id}` — eliminar

### 4.2 Frontend — Modelo y servicio

- [x] **4.2.1** Crear `core/models/dermatological-evaluation.model.ts`
  - Interface `DermatologicalEvaluation` con todos los campos
- [x] **4.2.2** Crear `core/services/dermatological-evaluation.service.ts`
  - Métodos HTTP: `getByPatientId()`, `create()`, `update()`, `delete()`

### 4.3 Frontend — Componentes

- [x] **4.3.1** Crear `features/patients/dermatological-evaluation-list/` — Componente lista
  - Mostrar evaluaciones del paciente en cards
  - Botón "Nueva Evaluación"
  - Opciones editar/eliminar por evaluación
- [x] **4.3.2** Crear `features/patients/dermatological-evaluation-form/` — Componente formulario
  - Formulario reactivo con campos: tipo piel, zona afectada, tipo lesión, tamaño, diagnóstico, tratamiento, procedimiento, notas de evolución, próxima revisión
  - Selectores con catálogos (`SKIN_TYPE`, `LESION_TYPE`, `BODY_AREA`, `DERM_PROCEDURE`)
  - Modo crear y modo editar

### 4.4 Frontend — Integración en vista del paciente

- [x] **4.4.1** Modificar `patient-detail.component.ts`
  - Importar componente de evaluaciones dermatológicas
  - Cargar evaluaciones cuando `isDermatology()`
- [x] **4.4.2** Modificar `patient-detail.component.html`
  - Agregar sección "Evaluaciones Dermatológicas" con `@if (isDermatology)`
  - Incluir `<app-dermatological-evaluation-list>`

### 4.5 Verificación de Fase 4

- [x] **4.5.1** Migración V11 ejecuta correctamente
- [x] **4.5.2** API CRUD de evaluaciones dermatológicas funciona (probar con Postman/curl)
- [x] **4.5.3** Dermatólogo puede crear, ver, editar y eliminar evaluaciones desde la UI
- [x] **4.5.4** Psicólogo NO ve la sección de evaluaciones dermatológicas
- [x] **4.5.5** Catálogos dermatológicos se cargan correctamente en los selectores

---

## Fase 5: Dashboard Adaptado y Pulido Final

> Objetivo: Adaptar el dashboard para mostrar KPIs relevantes según la especialidad y realizar ajustes finales de UX y consistencia.

### 5.1 Backend — Dashboard filtrado por especialidad

- [x] **5.1.1** Modificar `DashboardService.java`
  - Recibir parámetro `specialty` para filtrar estadísticas
  - Filtrar alertas de riesgo por catálogos de la especialidad
  - Contabilizar sesiones/citas según la especialidad del profesional
- [x] **5.1.2** Modificar `DashboardController.java`
  - Obtener especialidad del usuario autenticado y pasarla al servicio

### 5.2 Frontend — Dashboard condicional

- [x] **5.2.1** Modificar componente Dashboard
  - Inyectar `SpecialtyService`
  - Mostrar/ocultar sección "Alertas de Riesgo" con tipos según especialidad
  - Título contextual: "Dashboard — Psicología" o "Dashboard — Dermatología"
- [x] **5.2.2** Adaptar tarjetas de KPI según relevancia por especialidad
  - Psicología: mantener KPIs actuales (incluir "Evaluaciones psicométricas del mes")
  - Dermatología: adaptar KPIs (incluir "Evaluaciones dermatológicas del mes", "Procedimientos realizados")

### 5.3 Frontend — Pulido de UX

- [x] **5.3.1** Revisar que los textos/labels sean genéricos o contextuales según especialidad
  - Ejemplo: "Historia Clínica (Sesiones)" → mantener genérico
  - Ejemplo: "Evaluaciones Psicométricas" → solo visible para psicología
- [x] **5.3.2** Revisar que los formularios no muestren campos vacíos irrelevantes
- [x] **5.3.3** Verificar comportamiento responsive en ambos perfiles
- [x] **5.3.4** Verificar que los catálogos de tipos de sesión y modalidad se filtren correctamente en los formularios de agenda y sesiones

### 5.4 Backend — Datos de prueba

- [x] **5.4.1** Modificar `MockDataSeeder.java` (si aplica)
  - Agregar datos de prueba para dermatología: pacientes, sesiones, evaluaciones, registros médicos
- [x] **5.4.2** Modificar `DataSeeder.java`
  - Mantener seed de pruebas psicométricas (BDI-II) solo para psicología
  - No crear datos de pruebas psicométricas para el perfil dermatología

### 5.5 Verificación Final

- [x] **5.5.1** Compilar backend sin errores ni warnings
- [x] **5.5.2** Compilar frontend sin errores ni warnings
- [x] **5.5.3** Flujo completo psicólogo: login → dashboard → crear paciente → agendar cita → crear sesión SOAP → crear registro médico → aplicar test psicométrico → crear alerta → cobrar
- [x] **5.5.4** Flujo completo dermatólogo: login → dashboard → crear paciente → agendar cita → crear sesión dermatológica → crear registro médico con ficha de piel → crear evaluación dermatológica → crear alerta → cobrar
- [x] **5.5.5** Un mismo paciente aparece en ambos perfiles con historiales clínicos independientes
- [x] **5.5.6** Cada especialidad ve su propia configuración de clínica (nombre, logo, contacto)
- [x] **5.5.7** Los catálogos de sesión y modalidad son independientes por especialidad

---

## Resumen de Archivos por Fase

### Migraciones Flyway (7 archivos nuevos)

| Migración | Fase | Descripción |
|-----------|------|-------------|
| `V6__add_specialty_to_users.sql` | 1 | Campo `specialty` en `users` |
| `V6b__add_specialty_to_clinic_settings.sql` | 1 | Campo `specialty` en `clinic_settings` |
| `V7__add_dermatology_fields_clinical_sessions.sql` | 3 | Campos derm en `clinical_sessions` |
| `V8__add_dermatology_fields_medical_records.sql` | 3 | Campos derm en `medical_records` |
| `V9__add_specialty_to_catalogs.sql` | 3 | Campo `specialty` en `catalogs` |
| `V10__seed_dermatology_catalogs.sql` | 3 | Catálogos de dermatología |
| `V11__create_dermatological_evaluations.sql` | 4 | Tabla `dermatological_evaluations` |

### Backend — Archivos nuevos (5)

| Archivo | Fase |
|---------|------|
| `model/DermatologicalEvaluation.java` | 4 |
| `dto/DermatologicalEvaluationDto.java` | 4 |
| `repository/DermatologicalEvaluationRepository.java` | 4 |
| `service/DermatologicalEvaluationService.java` | 4 |
| `controller/DermatologicalEvaluationController.java` | 4 |

### Backend — Archivos a modificar (~20)

| Archivo | Fase |
|---------|------|
| `User.java` | 1 |
| `CreateUserRequest.java` | 1 |
| `UserProfileDTO.java` | 1 |
| `UserService.java` | 1 |
| `UserController.java` | 1 |
| `JwtService.java` | 1 |
| `AuthResponse.java` | 1 |
| `DataInitializer.java` | 1 |
| `ClinicSettings.java` | 1 |
| `ClinicSettingsDto.java` | 1 |
| `ClinicSettingsService.java` | 1 |
| `ClinicSettingsController.java` | 1 |
| `ClinicalSession.java` | 3 |
| `ClinicalSessionDto.java` | 3 |
| `ClinicalSessionService.java` | 3 |
| `ClinicalSessionController.java` | 3 |
| `MedicalRecord.java` | 3 |
| `MedicalRecordDto.java` | 3 |
| `MedicalRecordService.java` | 3 |
| `MedicalRecordController.java` | 3 |
| `Catalog.java` | 3 |
| `CatalogService.java` | 3 |
| `CatalogController.java` | 3 |
| `CatalogDataInitializer.java` | 3 |
| `DashboardService.java` | 5 |
| `DashboardController.java` | 5 |
| `MockDataSeeder.java` | 5 |
| `DataSeeder.java` | 5 |

### Frontend — Archivos nuevos (6)

| Archivo | Fase |
|---------|------|
| `core/services/specialty.service.ts` | 1 |
| `core/guards/specialty.guard.ts` (opcional) | 2 |
| `core/models/dermatological-evaluation.model.ts` | 4 |
| `core/services/dermatological-evaluation.service.ts` | 4 |
| `features/patients/dermatological-evaluation-list/` | 4 |
| `features/patients/dermatological-evaluation-form/` | 4 |

### Frontend — Archivos a modificar (~10)

| Archivo | Fase |
|---------|------|
| `auth.service.ts` | 1 |
| `user-profile.model.ts` | 1 |
| `main-layout.component.ts` | 2 |
| `main-layout.component.html` | 2 |
| `app.routes.ts` | 2 |
| `clinical-session.model.ts` | 3 |
| `medical-record.model.ts` | 3 |
| `clinical-session-form` (ts + html) | 3 |
| `medical-record-form` (ts + html) | 3 |
| `patient-detail.component.ts` | 3, 4 |
| `patient-detail.component.html` | 3, 4 |
| Dashboard component (ts + html) | 5 |
