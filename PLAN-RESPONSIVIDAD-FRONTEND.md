# Plan de implementación de responsividad para tablet y escritorio

> **Estado:** Fases 1–4 completadas  
> **Base:** auditoría responsive realizada el 2026-08-24  
> **Alcance:** frontend Angular; no se modifican contratos REST ni reglas de negocio  
> **Regla de trabajo:** completar una fase, ejecutar sus verificaciones y revisar el diff antes de iniciar la siguiente.

## Objetivo

Conseguir que ClinicaPersonal sea cómodo y predecible en tablet y PC, con prioridad en
768×1024, 1024×768 y 1366×768, sin degradar la experiencia en pantallas amplias ni en
móvil.

La implementación debe corregir los saltos de layout en 768 y 1024 px, reducir la
dependencia del desplazamiento horizontal, mejorar la interacción táctil y proteger el
resultado con pruebas automatizadas.

## Estado inicial

- [x] El documento completo no presenta overflow horizontal en las resoluciones auditadas.
- [x] Landing, login, dashboard, formularios y modales principales tienen una base fluida.
- [x] El sidebar conserva un ancho compacto en tablet horizontal.
- [x] Agenda, cobros e inventario seleccionan una vista apropiada para tablet.
- [x] Los KPI y layouts maestro-detalle responden al espacio real de su contenedor.
- [x] Auditoría es legible en un PC de 1366 px sin depender de scroll horizontal.
- [x] Los controles frecuentes tienen un área táctil apropiada para tablet.
- [x] Playwright valida resoluciones de tablet y PC estándar.

### Evidencia de la auditoría

| Resolución | Área útil aproximada | Hallazgo principal |
| --- | ---: | --- |
| 768×1024 | 604 px | Sidebar compacto correcto, pero agenda y tablas se abren en modo escritorio. |
| 1024×768 | 668 px | El sidebar se expande a 256 px y activa simultáneamente layouts `lg`. |
| 1366×768 | 1012 px | Las pantallas principales caben; auditoría conserva una tabla de unos 1294 px. |
| 1920×1080 | 1566 px o más | No se observaron recortes relevantes. |

## Resoluciones objetivo

| Perfil | Viewport | Comportamiento esperado |
| --- | --- | --- |
| Móvil de control | 390×844 | Menú superpuesto, vistas compactas y una columna. |
| Tablet vertical | 768×1024 | Rail lateral compacto, tarjetas o lista por defecto y controles táctiles. |
| Tablet horizontal | 1024×768 | Rail compacto y espacio suficiente para calendario o tablas priorizadas. |
| PC estándar | 1366×768 | Sidebar configurable y flujos principales sin información esencial oculta. |
| PC amplio | 1920×1080 | Aprovechamiento del ancho sin estirar excesivamente formularios o texto. |

## Principios de implementación

- No añadir librerías de UI, breakpoints o estado para resolver este plan.
- Mantener los componentes standalone y las convenciones actuales de Tailwind.
- Preferir CSS Container Queries cuando una composición dependa del ancho del contenido y
  no del viewport completo.
- Mantener `overflow-x-auto` solo como mecanismo deliberado, con indicación visual cuando
  exista contenido fuera de la vista.
- No ocultar datos clínicos esenciales; mover información secundaria a detalles expandibles,
  drawers o modales.
- Preservar estados de carga, vacío, error y éxito.
- Mantener textos visibles en español y nombres de código en inglés.
- Revisar las vistas de impresión después de cambiar grids, tablas o contenedores.

---

## Fase 1 — Estructura responsive y política de breakpoints (P0)

**Objetivo:** eliminar la reducción inesperada del área de trabajo en 1024 px y establecer
una política única para móvil, tablet y escritorio.

### 1.1 Política de navegación

- [x] Mantener el menú superpuesto para viewports menores de 768 px.
- [x] Mantener un rail de 64 px entre 768 y 1279 px, tanto en orientación vertical como
  horizontal.
- [x] Permitir el sidebar completo de 256 px a partir de 1280 px.
- [x] Cambiar las clases de expansión del sidebar de `lg:*` a `xl:*` en
  `main-layout.component.html`.
- [x] Ocultar o deshabilitar el control de expansión cuando el viewport no tenga espacio
  suficiente.
- [x] Conservar `sidebar_expanded` para escritorio, sin permitir que una preferencia guardada
  fuerce 256 px en tablet.
- [x] Confirmar que cambiar la orientación no deja el body bloqueado ni el sidebar en un
  estado intermedio.
- [x] Mantener foco, `inert`, `aria-hidden`, Escape y restauración del foco del menú móvil.

### 1.2 Clasificación de viewport

- [x] Mantener `isMobile()` exclusivamente para el comportamiento menor de 768 px.
- [x] Incorporar en `ViewPreferenceService` una clasificación compacta para anchos menores
  de 1024 px.
- [x] Evitar comparaciones de ancho duplicadas en componentes; centralizar la decisión de
  vista inicial en el servicio compartido.
- [x] Definir el comportamiento exacto en 767, 768, 1023, 1024, 1279 y 1280 px.
- [x] Actualizar las pruebas de `ViewPreferenceService` para cubrir todos los límites.

### 1.3 Contenedor principal y cabecera

- [x] Confirmar que `main`, header y `router-outlet` mantienen `min-width: 0`.
- [x] Evitar que breadcrumb, buscador, atajos y avatar compitan por ancho en tablet.
- [x] Ocultar primero los textos secundarios de la cabecera y mantener visibles las acciones
  esenciales.
- [x] Verificar nombres de ruta y nombres de clínica largos.

### Archivos principales

- `frontend/src/app/layout/main-layout/main-layout.component.html`
- `frontend/src/app/layout/main-layout/main-layout.component.ts`
- `frontend/src/app/layout/main-layout/main-layout.component.spec.ts`
- `frontend/src/app/shared/services/view-preference/view-preference.service.ts`
- `frontend/src/app/shared/services/view-preference/view-preference.service.spec.ts`
- `frontend/src/styles.css`

### Criterio de cierre

- [x] En 768×1024 y 1024×768 el sidebar ocupa como máximo 64 px.
- [x] En 1280 px o más la preferencia expandida/colapsada funciona y persiste.
- [x] Ningún cambio de orientación produce overflow global o bloqueo del scroll.
- [x] Las pruebas unitarias de límites de viewport pasan.

---

## Fase 2 — Adaptación de pantallas críticas al espacio disponible (P1)

**Objetivo:** conseguir que agenda, cobros, inventario, catálogos y auditoría sean legibles
sin activar composiciones de escritorio dentro de contenedores estrechos.

### 2.1 Agenda

- [x] Usar lista como vista inicial en anchos menores de 1024 px.
- [x] Mantener el calendario como opción manual en tablet vertical.
- [x] Confirmar que el calendario semanal cabe en 1024×768 con el rail compacto o, si aún
  requiere scroll, mostrar una pista visual de desplazamiento.
- [x] Hacer sticky la columna de hora cuando exista scroll horizontal.
- [x] Mantener visibles los controles de semana anterior, hoy y semana siguiente.
- [x] Reorganizar filtros y acciones en dos filas estables, sin truncar profesionales o
  estados.
- [x] Verificar paneles de disponibilidad, bloqueos y formulario de cita a 768 y 1024 px.

### 2.2 Cobros e inventario

- [x] Usar tarjetas como vista inicial en anchos menores de 1024 px.
- [x] Mantener la tabla disponible cuando el usuario la seleccione expresamente.
- [x] Convertir KPI y resúmenes a composiciones dependientes del contenedor.
- [x] Mostrar cinco KPI solo cuando cada tarjeta disponga del ancho mínimo necesario.
- [x] Evitar `truncate` en importes; permitir ajuste tipográfico o cambio de columnas.
- [x] Convertir filtros a un grid progresivo de una, dos y cuatro columnas.
- [x] Mantener búsqueda y acción primaria con un ancho útil en tablet.
- [x] Priorizar columnas en tablas y mover datos secundarios a la vista de detalle o menú de
  acciones.
- [x] Verificar formularios, movimientos de stock, gráficos y detalle de pago.

### 2.3 Catálogos

- [x] Activar el maestro-detalle de dos columnas según el ancho del contenedor, no solo por
  `lg` del viewport.
- [x] Mantener el flujo apilado cuando el panel de detalle no disponga de unos 600 px.
- [x] Evitar que el panel izquierdo de 320 px deje una tabla de 560 px dentro de un contenedor
  de aproximadamente 332 px.
- [x] Conservar pestañas de especialidad desplazables y accesibles mediante teclado.
- [x] Verificar creación y edición de catálogos y opciones.

### 2.4 Auditoría y tablas administrativas

- [x] Definir columnas prioritarias: fecha, usuario, acción, entidad y especialidad.
- [x] Mover IP, detalle completo y otros datos secundarios al modal o drawer existente.
- [x] Truncar el resumen de detalle con acceso claro al contenido completo.
- [x] Conseguir que las columnas prioritarias sean visibles a 1366×768 sin scroll horizontal.
- [x] Aplicar la misma revisión a Personal y Cuentas y a tablas administrativas similares.

### 2.5 Revisión transversal

- [x] Comprobar directorio y formulario de pacientes, atenciones, servicios clínicos,
  pruebas psicométricas, perfil y ficha del paciente.
- [x] Confirmar que pestañas, subpestañas y menús de acciones no generan overflow global.
- [x] Confirmar que charts usan el ancho del contenedor y no conservan dimensiones obsoletas
  al cambiar la orientación.
- [x] Verificar modales y drawers con alturas de 768 y 1024 px.
- [x] Revisar landing, login, editor visual y vistas de impresión como regresión.

### Archivos principales

- `frontend/src/app/features/agenda/agenda/agenda.component.html`
- `frontend/src/app/features/agenda/agenda-toolbar/agenda-toolbar.component.html`
- `frontend/src/app/features/agenda/agenda/agenda.component.ts`
- `frontend/src/app/features/billing/billing/billing.component.html`
- `frontend/src/app/features/billing/billing-summary/billing-summary.component.html`
- `frontend/src/app/features/inventory/inventory-list/inventory-list.component.html`
- `frontend/src/app/features/settings/catalog-management/catalog-management.component.html`
- `frontend/src/app/features/settings/audit-log/audit-log.component.html`
- `frontend/src/app/features/settings/user-management/user-management.component.html`
- `frontend/src/styles.css`

### Criterio de cierre

- [x] Agenda abre en lista a 768 px y puede abrir calendario sin romper el documento.
- [x] Cobros e inventario abren en tarjetas a 768 px.
- [x] Ningún importe KPI aparece truncado a 768, 1024 o 1366 px.
- [x] Catálogos no activa maestro-detalle cuando el panel derecho resulta insuficiente.
- [x] Auditoría muestra sus columnas prioritarias sin scroll horizontal a 1366 px.
- [x] El scroll horizontal restante está justificado, es visible y funciona con teclado y
  touch.

---

## Fase 3 — Interacción táctil, accesibilidad y densidad (P1)

**Objetivo:** hacer que la interfaz sea cómoda para tablets táctiles sin perder la densidad
adecuada para usuarios de ratón y teclado.

### 3.1 Tamaños interactivos

- [x] Definir una utilidad compartida para acciones táctiles frecuentes.
- [x] Aplicar una altura mínima de 40–44 px a acciones primarias, campos y filtros cuando
  `pointer: coarse`.
- [x] Llevar botones de icono frecuentes a un área mínima de 36–40 px.
- [x] Mantener separación suficiente entre editar, eliminar, desactivar y otras acciones
  sensibles.
- [x] Evitar aumentar iconos innecesariamente; ampliar el área interactiva mediante padding.

### 3.2 Scroll, foco y navegación

- [x] Añadir una indicación visual cuando una tabla, pestaña o calendario tenga contenido
  horizontal oculto.
- [x] Permitir foco visible sobre contenedores desplazables cuando necesiten interacción por
  teclado.
- [x] Verificar que las columnas sticky no oculten contenido ni produzcan solapamientos.
- [x] Confirmar que drawers, modales, command palette y menús restauran correctamente el
  foco.
- [x] Mantener el soporte de `prefers-reduced-motion`.

### 3.3 Contenido real y casos extremos

- [x] Probar nombres de pacientes, profesionales, servicios y clínicas largos.
- [x] Probar importes de seis o más dígitos, correos largos y estados extensos.
- [x] Probar zoom del navegador al 125 % y 150 % en PC.
- [x] Probar tablet con teclado conectado y tablet en modo táctil.
- [x] Confirmar que la información no depende exclusivamente de hover.

### Archivos principales

- `frontend/src/styles.css`
- `frontend/src/app/shared/components/drawer-sheet/drawer-sheet.component.ts`
- `frontend/src/app/shared/components/command-palette/command-palette.component.ts`
- `frontend/src/app/shared/components/pagination/pagination.component.html`
- Templates de features intervenidos en la Fase 2.

### Criterio de cierre

- [x] Las acciones primarias y destructivas son utilizables con touch sin pulsaciones
  accidentales frecuentes.
- [x] Todos los controles conservan foco visible y nombre accesible.
- [x] No hay funciones importantes disponibles únicamente mediante hover.
- [x] La aplicación sigue siendo utilizable con zoom al 150 % en 1366×768.

---

## Fase 4 — Pruebas responsive y protección contra regresiones (P1)

**Objetivo:** convertir los criterios de responsividad en verificaciones automáticas
reproducibles localmente y en CI.

### 4.1 Proyectos Playwright

- [x] Mantener las pruebas cross-browser de escritorio existentes.
- [x] Añadir un proyecto Chromium `tablet-portrait` de 768×1024.
- [x] Añadir un proyecto Chromium `tablet-landscape` de 1024×768.
- [x] Añadir un proyecto Chromium `desktop-standard` de 1366×768.
- [x] Añadir un proyecto Chromium `desktop-wide` de 1920×1080 si su coste en CI es
  aceptable.
- [x] Configurar credenciales y datos E2E mediante un entorno de pruebas reproducible, sin
  incluir secretos reales en el repositorio.

### 4.2 Smoke test por ruta

- [x] Recorrer dashboard, agenda, atenciones, pacientes, cobros, servicios, inventario,
  pruebas, catálogos, usuarios, auditoría y perfil.
- [x] Validar que `document.documentElement.scrollWidth <= window.innerWidth` con una
  tolerancia máxima de 1 px.
- [x] Verificar que el título y la acción primaria de cada pantalla sean visibles.
- [x] Verificar el ancho del sidebar esperado para cada perfil.
- [x] Mantener una lista explícita de contenedores autorizados a tener scroll horizontal.
- [x] Fallar si aparece un nuevo scroll horizontal fuera de esa lista.

### 4.3 Pruebas de comportamiento responsive

- [x] Confirmar lista de agenda a 768 px y calendario a 1024 px cuando corresponda.
- [x] Confirmar tarjetas de cobros e inventario a 768 px.
- [x] Confirmar que los KPI no tienen `scrollWidth` mayor que `clientWidth`.
- [x] Confirmar layout apilado de catálogos en tablet y maestro-detalle en escritorio.
- [x] Confirmar columnas prioritarias de auditoría a 1366 px.
- [x] Abrir los modales principales y comprobar que cabecera, cuerpo y acciones permanecen
  accesibles.
- [x] Cambiar de orientación durante una sesión y comprobar sidebar, scroll y vista activa.
- [x] Añadir capturas de fallo y trazas para facilitar el diagnóstico.

### 4.4 Pruebas unitarias y cierre técnico

- [x] Probar los límites de `ViewPreferenceService` y la preferencia guardada.
- [x] Probar el comportamiento expandido/compacto de `MainLayoutComponent`.
- [x] Probar que agenda, cobros e inventario seleccionan el modo inicial correcto.
- [x] Ejecutar lint, pruebas Angular, E2E responsive y build de producción.
- [x] Revisar el diff completo y confirmar que no se modificaron contratos REST, secretos ni
  archivos generados.

### Archivos principales

- `frontend/playwright.config.ts`
- `frontend/e2e/responsive-layout.spec.ts`
- `frontend/e2e/responsive-modals.spec.ts`
- Specs unitarios de los componentes y servicios modificados.
- `.github/workflows/ci.yml`, solo si se integra la suite responsive en CI.

### Comandos de verificación

Ejecutar con la versión de Node declarada en `.nvmrc`:

```powershell
Set-Location frontend
npm run lint
npm test -- --watch=false --browsers=ChromeHeadless
npm run build
npm run test:e2e:responsive
npm run test:e2e:ci
```

### Criterio de cierre

- [x] La suite responsive pasa en las tres resoluciones prioritarias.
- [x] No existe overflow horizontal global en ninguna ruta cubierta.
- [x] No hay KPI, acciones primarias ni filtros esenciales recortados.
- [x] Los modales principales funcionan a 768×1024 y 1024×768.
- [x] `npm run lint`, pruebas Angular y build finalizan correctamente.

---

## Orden recomendado de implementación

1. Completar la Fase 1 y validar los seis puntos de ruptura antes de tocar pantallas.
2. Implementar la Fase 2 por grupos: agenda; cobros/inventario; catálogos/auditoría;
   regresión transversal.
3. Aplicar la Fase 3 sobre los componentes ya estabilizados.
4. Incorporar la Fase 4 durante las fases anteriores, dejando su ejecución completa como
   cierre obligatorio.

## Estrategia de entregas

| Entrega | Contenido | Validación mínima |
| --- | --- | --- |
| 1 | Sidebar, breakpoints y preferencias | Unit tests + build + capturas 768/1024/1280. |
| 2 | Agenda, cobros e inventario | Pruebas de componente + smoke E2E tablet. |
| 3 | Catálogos, auditoría y tablas administrativas | Smoke E2E 1024/1366 + revisión de datos largos. |
| 4 | Touch, accesibilidad y suite completa | Lint + unit + E2E responsive + build. |

## Riesgos y mitigaciones

| Riesgo | Mitigación |
| --- | --- |
| Preferencias antiguas fuerzan una tabla estrecha | Aplicar la preferencia solo cuando la vista sea compatible y permitir restablecerla. |
| Un breakpoint arregla tablet y rompe PC | Validar 767/768/1023/1024/1279/1280 en cada entrega. |
| Ocultar columnas elimina información necesaria | Clasificar prioridad y mantener acceso al dato completo mediante detalle. |
| Container Queries no se aplican por falta de contenedor | Declarar y probar explícitamente `container-type: inline-size` en cada shell. |
| Capturas visuales producen falsos positivos | Priorizar aserciones de geometría; limitar snapshots a vistas estables. |
| Cambios de layout afectan impresión | Ejecutar regresión de receta e historia clínica impresa. |
| La suite E2E incrementa demasiado el tiempo de CI | Ejecutar responsive en Chromium y conservar cross-browser para flujos esenciales. |

## Estimación

| Fase | Esfuerzo estimado |
| --- | ---: |
| Fase 1 — Estructura y breakpoints | 1–2 jornadas |
| Fase 2 — Pantallas críticas | 2–3 jornadas |
| Fase 3 — Touch y accesibilidad | 1 jornada |
| Fase 4 — Automatización y cierre | 1–2 jornadas |
| **Total** | **5–8 jornadas** |

## Criterio de cierre global

- [x] Tablet vertical y horizontal conservan un rail lateral compacto.
- [x] PC de 1366 px muestra toda la información operativa prioritaria.
- [x] No existe overflow horizontal a nivel de documento.
- [x] Todo overflow interno restante es intencional, visible y accesible.
- [x] Importes, títulos y acciones primarias no aparecen truncados.
- [x] La interacción táctil cumple los tamaños definidos.
- [x] Landing, login, editor, modales e impresión no presentan regresiones.
- [x] Lint, pruebas unitarias, pruebas responsive y build pasan.
- [x] El diff final no contiene secretos, archivos generados ni cambios de API accidentales.

## Registro de validación

| Fecha | Fase | Resoluciones verificadas | Responsable | Resultado | Observaciones |
| --- | --- | --- | --- | --- | --- |
| 2026-08-24 | Fase 1 | 767, 768, 1023, 1024, 1279 y 1280 px | Codex | Correcto | Rail de 64 px entre 768 y 1279; sidebar expandible desde 1280; sin overflow global; preferencia persistente; 161 pruebas Angular y build exitosos. |
| 2026-08-24 | Fase 2 | 768×1024, 1024×768 y 1366×768 | Codex | Correcto | Agenda en lista y cobros/inventario en tarjetas a 768; KPI de 2/3/5 columnas; catálogos apilado/apilado/maestro-detalle; auditoría priorizada; sin overflow global en rutas críticas y transversales; 163 pruebas Angular y build exitosos. |
| 2026-08-24 | Fase 3 | 768×1024 touch; 1366×768 con zoom efectivo 125 % y 150 % | Codex | Correcto | Objetivos táctiles de 44 px e iconos de 40 px; separación sensible de 8 px; scroll visible y enfocable; foco restaurado en menú, modales y command palette; alternativa a hover; contenido extremo sin overflow global; pruebas Angular y build exitosos. |
| 2026-08-24 | Fase 4 | 768×1024, 1024×768, 1366×768 y 1920×1080 | Codex | Correcto | 46 E2E de CI aprobadas y 2 omisiones condicionales esperadas; 9 pruebas responsive aprobadas en escritorio amplio y 1 omisión condicional esperada; 165 pruebas Angular; lint sin errores y build de producción exitoso. |
