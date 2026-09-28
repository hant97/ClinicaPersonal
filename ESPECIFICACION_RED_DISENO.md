# Especificación de módulos y UX para el rediseño

- **Proyecto:** Clínica Personal
- **Versión:** 1.0
- **Fecha de revisión:** 2026-09-28
**Estado:** Inventario de la experiencia actual y criterios base; pendiente de aprobar prioridades y permisos.

## 1. Propósito

Este documento convierte el sistema existente en una base verificable para rediseñar su navegación y sus pantallas. Registra la arquitectura visible hoy, las tareas principales, los permisos declarados, los flujos entre módulos y los criterios que debe cumplir una propuesta visual.

[MANUAL_DE_USUARIO.md](MANUAL_DE_USUARIO.md) conserva las instrucciones de uso y el vocabulario clínico. Esta especificación se usa para planificar y revisar el rediseño. Las rutas y componentes del frontend, junto con las reglas de los servicios backend, son la referencia para describir el comportamiento actual.

## 2. Alcance

Incluye la aplicación pública, el entorno autenticado, los módulos clínicos y operativos, la administración del sistema y las superficies de navegación compartidas. No fija una nueva marca ni reemplaza las reglas clínicas, la API o las políticas de retención de datos.

El diseño objetivo debe mantener separados los permisos por rol y especialidad. Una decisión visual no puede conceder acceso a información o acciones que el backend no autoriza.

## 3. Arquitectura de información actual

### 3.1 Sitio público y acceso

| Ruta | Pantalla | Propósito | Acceso declarado |
| --- | --- | --- | --- |
| `/` | Página pública | Presentar la clínica, sus especialidades y canales de contacto. | Público |
| `/login` | Inicio de sesión | Autenticar al personal. | Público |
| `/confirmar-cita/:token` | Confirmación de cita | Permitir la respuesta pública a una invitación de cita. | Público con token |
| `/verificar-receta/:code` | Verificación de receta | Consultar la validez de una receta mediante su código. | Público con código |
| `/editar-sitio` | Editor del sitio | Administrar el contenido publicado y su vista previa. | `siteAdminGuard`; admite `ROLE_ADMIN` y `ROLE_SITE_ADMIN` |

### 3.2 Entorno autenticado

Las rutas siguientes viven en el shell autenticado y tienen `authGuard`, salvo que indiquen un control adicional.

| Grupo | Rutas | Tareas principales | Control adicional observado |
| --- | --- | --- | --- |
| Panel operativo | `/dashboard` | Revisar citas de hoy, espera/confirmaciones, ingresos, pacientes activos, próxima atención y elementos que requieren seguimiento. | Ninguno en la ruta frontend |
| Agenda | `/agenda` | Consultar calendario semanal o lista, buscar citas, administrar disponibilidad y bloqueos, crear o modificar citas. | Ninguno en la ruta frontend |
| Atenciones | `/attentions` | Registrar encuentros, consultar el flujo por estado, iniciar o finalizar una atención y continuar a la nota, receta o cobro. | Ninguno en la ruta frontend |
| Pacientes | `/patients`, `/patients/new`, `/patients/:identifier/edit`, `/patients/:id` | Buscar y administrar pacientes; consultar su ficha e historia longitudinal. | Ninguno en la ruta frontend; la autorización de datos se aplica también en backend |
| Sesión clínica | `/patients/:id/sessions/new`, `/patients/:id/sessions/:sessionId/edit` | Registrar o editar una sesión clínica asociada al paciente. | `professionalGuard`, además de `authGuard` |
| Cobros | `/billing` | Registrar y consultar pagos, saldos y reportes de caja. | Ninguno en la ruta frontend |
| Servicios clínicos | `/services` | Consultar y administrar el catálogo de prestaciones. | Ninguno en la ruta frontend; acciones administrativas tienen restricciones de rol |
| Pruebas psicométricas | `/tests-catalog`, `/tests-catalog/new`, `/tests-catalog/edit/:id` | Administrar instrumentos psicométricos usados por Psicología. | `specialtyGuard('PSICOLOGIA')` |
| Inventario | `/inventory` | Consultar insumos y registrar movimientos. | Ninguno en la ruta frontend; las operaciones dependen de las reglas backend |
| Catálogos del sistema | `/settings/catalogs` | Administrar categorías y opciones maestras utilizadas por otros módulos. | `adminGuard`; el servicio acepta mutaciones de `ROLE_ADMIN` y `ROLE_SITE_ADMIN` |
| Personal y Cuentas | `/settings/users` | Buscar cuentas, crear o editar usuarios, cambiar su estado y restablecer contraseñas. | `adminGuard`; las operaciones de usuarios requieren `ROLE_ADMIN` en backend |
| Auditoría | `/settings/audit` | Buscar eventos y consultar su detalle. | `adminGuard`; el servicio acepta `ROLE_ADMIN` y `ROLE_SITE_ADMIN` |
| Productividad | `/settings/productivity` | Comparar atenciones, finalización y facturación por profesional en un período; exportar el reporte. | `adminGuard`; el endpoint del reporte requiere `ROLE_ADMIN` |
| Perfil | `/profile` | Consultar y actualizar datos propios y contraseña. | Ninguno adicional en la ruta frontend |

### 3.3 Agrupación actual del menú

- **Principal:** Dashboard, Agenda, Atenciones, Pacientes y Cobros.
- **Operación & Recursos:** Servicios, Pruebas (solo Psicología), Inventario, Catálogos, Personal & Cuentas, Auditoría y Productividad.
- **Sitio Web:** Editar Sitio Web.

El menú cambia por rol y el sidebar puede colapsarse. La visibilidad actual no coincide siempre con la autorización final; las diferencias comprobadas se registran en la sección 8 y deben resolverse antes de cerrar la nueva arquitectura.

## 4. Roles, especialidad y límites de acceso

Los roles existentes son `ROLE_PROFESIONAL`, `ROLE_ASISTENTE`, `ROLE_ADMIN` y `ROLE_SITE_ADMIN`. Pueden combinarse, salvo que Profesional y Asistente no se asignan juntos. `PSICOLOGIA` y `DERMATOLOGIA` son especialidades asociadas a la cuenta, no roles.

| Rol | Uso declarado | Límite de diseño |
| --- | --- | --- |
| `ROLE_PROFESIONAL` | Atención clínica en la especialidad asignada. | Separar la información clínica por especialidad y por las reglas de acceso del backend. |
| `ROLE_ASISTENTE` | Recepción y operación de pacientes, citas y cobros. | No exponer notas ni historia clínica. |
| `ROLE_ADMIN` | Administración de la clínica: usuarios, servicios, inventario, catálogos y reportes. | El rol administrativo por sí solo no equivale a Profesional; la combinación de roles debe mostrarse claramente. |
| `ROLE_SITE_ADMIN` | Administración del sitio público. | El acceso actual también alcanza Catálogos y Auditoría; revisar si esto representa la política deseada. No conceder acceso clínico desde el diseño. |

**Regla para el rediseño:** documentar cada acción sensible con su permiso backend. Ocultar acciones no autorizadas y mostrar una explicación útil cuando una ruta o acción esté restringida.

## 5. Flujos que conectan módulos

### 5.1 Cita a atención y cobro

1. Crear y consultar la cita en **Agenda**.
2. La atención aparece en **Atenciones** y se sigue mediante sus estados: `AGENDADA`, `EN_PROCESO`, `ATENDIDA`, `COBRADA` o `CANCELADA`.
3. Desde una atención en proceso, continuar a la sesión clínica y, cuando corresponda, a una receta.
4. Después de marcar la atención como realizada, continuar a **Cobros** para registrar o consultar el pago.

### 5.2 Atención sin cita previa

Desde **Atenciones**, **Nueva Atención Rápida** registra un encuentro con un paciente y permite seguir el mismo flujo clínico y administrativo.

### 5.3 Expediente del paciente

**Pacientes** es el punto de acceso a datos demográficos y a la ficha longitudinal. La ficha reúne información general y secciones especializadas; Psicología y Dermatología muestran contenido según especialidad y permisos. Sesiones, recetas, documentos y cobros relacionados deben conservar el contexto del paciente.

### 5.4 Administración

**Personal y Cuentas** configura usuarios y roles; **Catálogos** configura opciones usadas por Agenda, historias clínicas, cobros e inventario; **Auditoría** permite revisar operaciones; **Productividad** resume actividad por profesional. El editor del sitio administra contenido que se publica en `/`.

## 6. Inventario de superficies por tipo de diseño

| Tipo de superficie | Módulos | Criterio de rediseño |
| --- | --- | --- |
| Dashboard | Dashboard operativo | Mantener los indicadores y paneles como tarjetas independientes dentro de una cuadrícula. Agruparlos por secciones funcionales. |
| Listado operativo | Pacientes, Atenciones, Cobros, Personal y Cuentas, Auditoría, Productividad, Servicios, Inventario, Pruebas | Usar una estructura coherente de título/acción, filtros y resultados. Agrupar el espacio de trabajo en una tarjeta principal y separar las áreas con divisores o subtítulos. |
| Calendario | Agenda | Mantener sus controles de período y disponibilidad junto al calendario; conservar una superficie amplia para la cuadrícula. |
| Maestro-detalle | Administración de Catálogos | Usar una tarjeta de módulo que agrupe título, indicadores y área de trabajo. Mantener el explorador y el detalle como paneles secundarios ligeros. |
| Expediente clínico | Ficha de Paciente y sus secciones | Mantener visible la identidad del paciente, las alertas pertinentes y la navegación entre secciones sin perder el contexto. |
| Formulario clínico | Sesiones y formularios especializados | Priorizar lectura, orden clínico, validación y guardado; evitar reducir formularios extensos a tablas o tarjetas estadísticas. |
| Editor público | Editor del sitio | Mantener diferenciadas la edición y la vista previa, con controles de borrador/publicación claros. |
| Portal público | Landing, confirmación y verificación | Separar visualmente estas experiencias de la navegación interna autenticada. |

### 6.1 Patrón de tarjeta principal

En pantallas de gestión, la tarjeta principal es la superficie de trabajo donde se colocan los controles y resultados del módulo. Aplicar este orden:

1. **Encabezado de página:** título, descripción breve y acción primaria.
2. **Indicadores:** cuando existan, mostrarlos en tarjetas resumidas independientes, antes del área de trabajo.
3. **Tarjeta principal:** reunir búsqueda, filtros, selector de vista y contenido central (tabla, tarjetas, lista o panel de detalle). Los estados de carga, error y vacío ocupan esta misma superficie.
4. **Acciones por elemento:** mantenerlas cerca del registro al que afectan y distinguirlas de la acción primaria de página.

Aplicación a los casos revisados:

| Módulo | Distribución recomendada |
| --- | --- |
| Atenciones | Mantener los indicadores del día sobre la tarjeta principal. Colocar búsqueda, rango de fechas, filtros de estado y resultados dentro de ella; presentar el estado vacío en el centro de la misma tarjeta. |
| Personal y Cuentas, Auditoría, Productividad | Reunir filtros y resultados en una tarjeta de trabajo. Mantener acciones como crear usuario o exportar en el encabezado de página o junto a los resultados. |
| Catálogos | Agrupar el explorador de catálogos y el detalle de opciones dentro de un área principal común; mantener los indicadores resumidos como tarjetas separadas. |
| Agenda | Usar una superficie amplia para calendario y disponibilidad. Mantener el encabezado y acciones de agenda por encima de ella. |
| Dashboard | Conservar las tarjetas independientes para indicadores, agenda y alertas; no añadir una envoltura general alrededor de todos los paneles. |

## 7. Requisitos de UX y criterios de aceptación

Cada pantalla rediseñada debe cumplir lo siguiente antes de considerarse terminada:

1. **Propósito y acción principal:** el título, la descripción y la acción prioritaria dejan claro qué tarea resuelve la pantalla.
2. **Navegación:** nombres de menú, título de página y destino de enlaces son coherentes. Una ruta restringida no debe aparecer como una acción normal para quien no puede usarla.
3. **Permisos:** el frontend y el backend aplican la misma matriz aprobada para rol, especialidad, lectura y escritura.
4. **Estados:** incluye carga, error, lista vacía, búsqueda sin resultados, contenido y acceso restringido cuando correspondan.
5. **Filtros:** los filtros activos son visibles, se pueden limpiar y no cambian silenciosamente el alcance de indicadores independientes.
6. **Diseño adaptable:** comprobar anchos de 360, 768 y 1280 píxeles como mínimo. Tablas anchas conservan desplazamiento horizontal local y no ensanchan toda la aplicación.
7. **Accesibilidad:** jerarquía de encabezados, etiquetas asociadas a controles, estados seleccionados anunciables, uso por teclado y foco visible. El color no es la única forma de distinguir estados.
8. **Datos clínicos:** no mostrar información sensible en pantallas públicas, resultados de búsqueda o indicadores fuera del permiso vigente.
9. **Acciones irreversibles:** confirmar antes de eliminar o cancelar y explicar el resultado de la acción.

## 8. Diferencias actuales que deben resolverse

Estas diferencias se observaron al contrastar `app.routes.ts`, `main-layout.component.html`, los guards Angular y las reglas de los controladores backend. Son decisiones de producto/seguridad; la especificación las deja visibles sin resolverlas por cuenta propia.

| Caso | Estado actual observado | Decisión requerida |
| --- | --- | --- |
| Catálogos en el menú | El enlace aparece para cuentas del grupo operativo; la ruta usa `adminGuard`, que permite `ROLE_ADMIN` y `ROLE_SITE_ADMIN`. Las mutaciones backend permiten ambos roles. | Definir qué roles ven y administran catálogos; alinear menú, guard y API. |
| Personal y Cuentas | El menú muestra la sección a `ROLE_ADMIN` y `ROLE_SITE_ADMIN`; la ruta acepta ambos por `adminGuard`, mientras las operaciones de usuarios requieren `ROLE_ADMIN` en backend. | Ocultar la ruta a `ROLE_SITE_ADMIN` o ampliar los permisos backend deliberadamente. |
| Auditoría | La ruta frontend y el endpoint backend admiten `ROLE_ADMIN` y `ROLE_SITE_ADMIN`. | Confirmar que el administrador del sitio debe ver auditoría, que puede incluir detalles de operaciones internas. |
| Productividad | El menú solo muestra el enlace a `ROLE_ADMIN`; `adminGuard` permite también `ROLE_SITE_ADMIN`, pero el endpoint de productividad requiere `ROLE_ADMIN`. | Alinear guard frontend con el permiso del endpoint. |
| Roles en el manual | La versión anterior nombraba `ROLE_USER`; los roles del código son `ROLE_PROFESIONAL`, `ROLE_ASISTENTE`, `ROLE_ADMIN` y `ROLE_SITE_ADMIN`. | Mantener un único vocabulario aprobado en manual, interfaz y backend. |
| Indicador del Dashboard | El manual anterior decía «Pacientes Atendidos»; la tarjeta actual dice «Pacientes Activos». | Usar el nombre y definición que entrega realmente la métrica. |

Resolver los cuatro primeros casos antes de producir prototipos con estados o menús definitivos. El rediseño debe reflejar la política aprobada, no perpetuar accidentalmente las discrepancias de guardado actuales.

## 9. Ficha para especificar cada pantalla

Completar esta ficha antes de diseñar o implementar una pantalla:

```text
Módulo y nombre de pantalla:
Ruta(s):
Objetivo del usuario:
Rol(es) y especialidad(es):
Punto de entrada y destinos relacionados:
Datos principales que consulta o modifica:
Acción primaria y acciones secundarias:
Filtros, ordenamiento y paginación:
Estados de carga, error, vacío y éxito:
Reglas sensibles o de auditoría:
Comportamiento en móvil/tablet/escritorio:
Componentes compartidos a reutilizar:
Criterios de aceptación:
Decisiones todavía pendientes:
```

## 10. Secuencia de trabajo sugerida

1. Aprobar la matriz de acceso de la sección 8.
2. Confirmar la jerarquía de navegación para Profesional, Asistente, Administrador y Administrador del Sitio.
3. Priorizar los flujos clínicos diarios: Dashboard, Agenda, Atenciones, Pacientes y Cobros.
4. Diseñar después los espacios administrativos, los formularios y el editor público.
5. Revisar prototipos con criterios de aceptación de la sección 7 y actualizar este documento cuando cambie una ruta o permiso.

## 11. Fuentes verificadas

- `frontend/src/app/app.routes.ts`
- `frontend/src/app/layout/main-layout/main-layout.component.html`
- `frontend/src/app/core/guards/`
- `frontend/src/app/core/models/roles.ts`
- `backend/src/main/java/com/clinica/backend/controller/`
- `frontend/src/app/features/`
- `MANUAL_DE_USUARIO.md`
