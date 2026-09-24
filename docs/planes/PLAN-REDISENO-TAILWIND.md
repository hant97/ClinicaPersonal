# Plan de rediseño minimalista y migración a Tailwind

## Objetivo

Convertir la interfaz de ClinicaPersonal en una experiencia clínica minimalista,
serena y fácil de usar. El diseño debe priorizar las tareas diarias: atender la
agenda, consultar pacientes, registrar información clínica y gestionar cobros.

La implementación sustituirá progresivamente los estilos CSS nativos por
utilidades y componentes de Tailwind CSS, sin alterar los contratos de la API
ni los flujos funcionales existentes.

## Estado inicial

- [x] Se identificó CSS nativo global y encapsulado por componente.
- [x] Se instaló Tailwind CSS, PostCSS y Autoprefixer.
- [x] Se configuraron tokens de marca, tipografías y sombras en `tailwind.config.js`.
- [x] Se migraron los estilos compartidos globales: tarjetas, botones, campos y tablas.
- [x] Actualizar Node.js a la versión 18.19 o superior para poder validar Angular 18.
- [ ] Migrar los estilos CSS restantes de cada componente.

> Los tokens CSS existentes se conservarán solo durante la transición. Se eliminarán
> cuando ningún componente los utilice.

## Dirección visual

| Elemento | Decisión |
| --- | --- |
| Fondo | Gris verdoso muy claro (`canvas`) para reducir fatiga visual. |
| Superficies | Tarjetas blancas, borde sutil y sombra suave. |
| Color principal | Verde azulado clínico para navegación, foco y acciones principales. |
| Tipografía | Inter para lectura; Space Grotesk para títulos y datos destacados. |
| Acciones de riesgo | Rojo reservado exclusivamente para errores, alertas y eliminaciones. |
| Densidad | Espacios generosos, formularios por grupos y una acción primaria por contexto. |

## Fase 0 — Preparación técnica

- [x] Añadir dependencias de Tailwind al frontend.
- [x] Crear `tailwind.config.js` con la ruta de análisis `src/**/*.{html,ts}`.
- [x] Crear configuración PostCSS.
- [x] Añadir directivas de Tailwind a `src/styles.css`.
- [x] Definir colores, fuentes y sombras del sistema visual.
- [x] Actualizar el entorno local a Node.js 18.19+.
- [x] Ejecutar `npm install` con la versión actualizada de Node.js.
- [x] Ejecutar `npm run build` como validación inicial.
- [x] Registrar y resolver solo errores introducidos por Tailwind.

**Criterio de terminado:** Tailwind genera estilos en desarrollo y producción sin
errores, y el frontend conserva sus flujos funcionales.

## Fase 1 — Base y navegación

### Layout principal

- [x] Convertir `main-layout.component.html` a clases Tailwind.
- [x] Sustituir estilos inline del logo por utilidades Tailwind.
- [x] Eliminar `main-layout.component.css` y su referencia en el componente.
- [x] Mantener el menú lateral fijo en escritorio y deslizable en móvil.
- [x] Comprobar que el overlay móvil cierre el menú correctamente.
- [x] Mantener el foco visible y tamaños táctiles de al menos 44 px.

### Arquitectura de navegación

- [x] Ordenar opciones: Inicio, Agenda, Pacientes, Atención clínica, Cobros y Configuración.
- [x] Agrupar Servicios, Pruebas e Inventario como opciones operativas secundarias.
- [x] Dar un único estado activo claro a la opción actual.
- [x] Mantener “Cerrar sesión” separado de la navegación clínica.

**Criterio de terminado:** la navegación es comprensible en menos de un vistazo y
funciona de forma completa entre 320 px y escritorio.

## Fase 2 — Dashboard y agenda

### Dashboard

- [x] Convertir tarjetas KPI y acciones rápidas a Tailwind.
- [x] Priorizar citas del día, pendientes, pacientes y cobros relevantes.
- [x] Reducir información visual secundaria y decoraciones sin valor operativo.
- [x] Definir estados de carga, vacío, éxito y error.
- [x] Eliminar `dashboard.component.css` al finalizar la conversión.

### Agenda

- [x] Migrar filtros, vista de lista, calendario y menú de acciones.
- [x] Resaltar fecha, hora, paciente y estado de cada cita.
- [x] Mantener color como refuerzo; nunca como único indicador de estado.
- [x] Garantizar desplazamiento horizontal o alternativa legible en móvil.
- [x] Eliminar `agenda.component.css` y `appointment-form.component.css` tras migrarlos.

**Criterio de terminado:** una persona puede identificar y abrir la siguiente cita
con rapidez, tanto en lista como en calendario.

## Fase 3 — Pacientes y atención clínica

- [x] Migrar listado de pacientes, búsqueda y acciones de fila.
- [x] Migrar formulario de paciente y sus mensajes de validación.
- [x] Migrar ficha del paciente, historial, alertas y línea de tiempo.
- [x] Migrar formularios de historia clínica, sesiones, evaluaciones y alertas de riesgo.
- [x] Agrupar campos por tema y mostrar ayudas breves donde reduzcan errores.
- [x] Mantener etiquetas asociadas a cada control y errores descriptivos en español.
- [x] Eliminar los CSS de los componentes migrados.

**Criterio de terminado:** registrar o consultar un paciente requiere una jerarquía
clara, sin campos ni acciones visualmente ambiguas.

## Fase 4 — Cobros, servicios e inventario

- [x] Migrar listado y detalle de pagos.
- [x] Migrar formulario de pagos, servicios e insumos.
- [x] Migrar catálogos de servicios clínicos y pruebas.
- [x] Migrar listado y formulario de inventario.
- [x] Normalizar tablas, filtros, botones de exportación y confirmaciones de borrado.
- [x] Reservar `danger` para eliminación y alertas de stock o errores.
- [x] Eliminar CSS de cada componente al completar su pantalla.

**Criterio de terminado:** las tareas administrativas conservan consistencia visual
con los flujos clínicos y son utilizables en móvil.

## Fase 5 — Perfil, configuración y componentes compartidos

- [x] Migrar perfil de usuario y configuración de clínica.
- [x] Migrar gestión de catálogos.
- [x] Migrar paginación, autocompletado de paciente y notificaciones.
- [x] Estandarizar modales, encabezados de pantalla y estados vacíos.
- [x] Eliminar estilos inline que puedan expresarse con utilidades Tailwind.
- [x] Eliminar los CSS encapsulados ya migrados y sus referencias `styleUrl` o `styleUrls`.

**Criterio de terminado:** no existen variaciones innecesarias entre componentes
que cumplen la misma función.

## Fase 6 — Retiro de compatibilidad y calidad

- [x] Buscar referencias a `var(--` dentro de `frontend/src/app`.
- [x] Convertir las últimas referencias encontradas a clases Tailwind.
- [x] Eliminar tokens CSS de transición en `src/styles.css`.
- [x] Confirmar que no quedan archivos `*.component.css` usados por pantallas migradas.
- [x] Revisar contraste de texto, enlaces, estados deshabilitados y errores.
- [x] Comprobar navegación con teclado y foco visible.
- [x] Comprobar preferencias de reducción de movimiento.
- [x] Verificar vista móvil, tableta y escritorio.
- [x] Ejecutar `npm run build`.
- [x] Ejecutar `npm test -- --watch=false`.

**Criterio de terminado:** el proyecto compila, las pruebas relevantes pasan y no
quedan estilos CSS nativos de la interfaz anterior.

## Orden recomendado de implementación

1. Fase 0 y actualización de Node.js.
2. Fase 1 para establecer la navegación y el marco visual.
3. Fase 2 para mejorar la jornada operativa principal.
4. Fase 3 para completar el flujo de atención clínica.
5. Fases 4 y 5 para el resto de procesos y componentes compartidos.
6. Fase 6 como cierre obligatorio de calidad y eliminación de compatibilidad.

## Registro de validación

| Fecha | Área validada | Responsable | Resultado | Observaciones |
| --- | --- | --- | --- | --- |
| 2026-07-30 | Fases 0 y 1 | Codex | Correcto | Node 18.20.8, `npm install` y compilación de producción realizados; Tailwind genera estilos sin errores. |
| 2026-07-30 | Fase 2 | Codex | Correcto | Dashboard, agenda y modal de citas migrados a Tailwind; `npm run build` correcto con Node 20.20.2. |
| 2026-07-30 | Fase 3 | Codex | Correcto | Pacientes y atención clínica migrados a Tailwind; CSS encapsulados retirados y `npm run build` correcto con Node 20.20.2. |
| 2026-07-30 | Fase 4 | Codex | Correcto | Cobros, catálogos de servicios y pruebas, e inventario migrados a Tailwind; CSS encapsulados retirados y `npm run build` correcto con Node 20.20.2. |
| 2026-07-30 | Fase 5 | Codex | Correcto | Perfil, configuración clínica, catálogos y componentes compartidos migrados a Tailwind; estilos encapsulados retirados y `npm run build` correcto con Node 20.20.2. |
| 2026-07-30 | Fase 6 | Codex | Correcto | Sin variables CSS, estilos inline ni archivos `*.component.css`; se añadieron preferencias de reducción de movimiento, foco visible y utilidades responsivas. `npm run build` y 16 pruebas pasaron con Node 20.20.2. |
