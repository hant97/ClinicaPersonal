# Plan de implementación del editor visual de la landing

## Objetivo

Reemplazar progresivamente el formulario aislado de “Configuración del sitio web” por un
editor visual que permita al usuario con `ROLE_SITE_ADMIN` modificar el contenido sobre la
misma presentación de la landing, previsualizar el resultado en contexto y publicarlo de
forma explícita.

El editor será controlado: permitirá cambiar contenido, imágenes, visibilidad y orden de
elementos previstos por el diseño, pero no insertar HTML arbitrario ni alterar libremente la
estructura visual de la página.

## Estado inicial

- [x] La landing pública se sirve en `/` mediante `LandingComponent`.
- [x] El contenido público se obtiene con `GET /api/v1/public/landing`.
- [x] Existe un módulo administrativo en `/settings/website`, protegido por
  `siteAdminGuard` y `ROLE_SITE_ADMIN` en backend.
- [x] El backend ya persiste configuración general, especialidades, beneficios, pasos,
  profesionales e imágenes.
- [x] El guardado actual reemplaza la configuración publicada directamente.
- [x] Las cargas y eliminaciones de imágenes actuales también afectan inmediatamente el
  contenido publicado.
- [ ] Implementar un estado de borrador independiente del contenido publicado.
- [ ] Incorporar edición contextual sobre la presentación real de la landing.
- [ ] Retirar el formulario administrativo anterior después de validar la migración.

> El principal cambio técnico no es el estilo del formulario, sino separar con claridad el
> borrador de trabajo de la versión pública. Ninguna edición o carga de imagen debe hacerse
> visible para visitantes hasta ejecutar la acción “Publicar”.

## Decisiones de alcance

| Tema | Decisión |
| --- | --- |
| Experiencia | Editar sobre una representación idéntica a la landing pública. |
| Acceso | Solo usuarios con `ROLE_SITE_ADMIN`. La API seguirá siendo la autoridad final. |
| Entrada al editor | Botón “Editar sitio” para el administrador y ruta protegida `/editar-sitio`. |
| URL pública | `/` continúa siendo pública y nunca depende de permisos administrativos. |
| Guardado | “Guardar borrador” conserva cambios sin afectar la landing pública. |
| Publicación | “Publicar” reemplaza la versión pública dentro de una transacción. |
| Edición de textos | Selección del bloque y formulario contextual; no usar `contenteditable` libre. |
| Imágenes | Carga al borrador, previsualización inmediata y publicación posterior. |
| Colecciones | Altas, bajas, visibilidad y orden controlado de beneficios, pasos y profesionales. |
| Servicios clínicos | Continúan provenientes del catálogo de servicios clínicos; el editor solo modifica la presentación de cada especialidad. |
| Diseño libre | Fuera de alcance: HTML, CSS, scripts, nuevas columnas o maquetación arbitraria. |
| Historial completo | Fuera del MVP; se conservará auditoría mínima y la última versión publicada. |

## Flujo esperado

1. El administrador abre la landing pública y selecciona “Editar sitio”.
2. La aplicación navega a `/editar-sitio`, valida `ROLE_SITE_ADMIN` y carga el borrador.
3. El administrador selecciona un texto, imagen o colección de la página.
4. Un panel contextual muestra únicamente los campos válidos para ese bloque.
5. Cada cambio se refleja de inmediato en la vista previa local.
6. “Guardar borrador” persiste el trabajo sin modificar la landing pública.
7. “Vista pública” permite comparar el borrador con la versión publicada.
8. “Publicar” valida el documento completo y actualiza la versión pública de forma atómica.
9. La landing vuelve a solicitar `GET /api/v1/public/landing` y muestra la nueva versión.

## Contrato REST propuesto

Todos los nombres JSON permanecerán en `camelCase`. Los endpoints administrativos requieren
JWT con `ROLE_SITE_ADMIN`.

| Método | Ruta | Entrada | Respuesta | Seguridad |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/public/landing` | Sin entrada | `PublicLandingDto` publicado | Pública |
| `GET` | `/api/v1/admin/website/editor` | Sin entrada | `WebsiteEditorDto` | `ROLE_SITE_ADMIN` |
| `PUT` | `/api/v1/admin/website/draft` | `WebsiteDraftDto` + `revision` | Borrador actualizado | `ROLE_SITE_ADMIN` |
| `POST` | `/api/v1/admin/website/publish` | `revision` | `WebsiteEditorDto` publicado | `ROLE_SITE_ADMIN` |
| `POST` | `/api/v1/admin/website/draft/assets/{category}` | `multipart/form-data` | Referencia de imagen del borrador | `ROLE_SITE_ADMIN` |
| `DELETE` | `/api/v1/admin/website/draft/assets/{category}` | Sin body | Borrador actualizado | `ROLE_SITE_ADMIN` |
| `POST` | `/api/v1/admin/website/draft/professionals/{draftKey}/photo` | `multipart/form-data` | Referencia de foto del borrador | `ROLE_SITE_ADMIN` |
| `POST` | `/api/v1/admin/website/draft/reset` | `revision` | Borrador restaurado desde publicación | `ROLE_SITE_ADMIN` |

`WebsiteEditorDto` incluirá como mínimo:

- `draft`: documento editable completo.
- `revision`: número para control de concurrencia optimista.
- `hasUnpublishedChanges`: indicador para la barra del editor.
- `draftUpdatedAt` y `draftUpdatedBy`.
- `publishedAt` y `publishedBy`.

Una revisión desactualizada devolverá `409 Conflict`. Los errores de validación devolverán
mensajes de campo estables que Angular pueda presentar en español sin depender del texto de
una excepción Java.

## Fase 1 — Base de datos y modelo de publicación

### Persistencia del borrador

- [ ] Crear la migración `V22__add_website_landing_draft.sql`.
- [ ] Crear una tabla singleton `website_landing_drafts` con:
  - `singleton_key`.
  - `content JSONB`.
  - `revision BIGINT`.
  - `updated_at` y `updated_by`.
- [ ] Agregar a `website_settings` los metadatos `published_at`, `published_by` y
  `published_revision`.
- [ ] Inicializar el primer borrador a partir de la configuración publicada existente.
- [ ] Mantener las tablas normalizadas actuales como fuente de la versión pública.

### Integridad y concurrencia

- [ ] Guardar el borrador y aumentar `revision` dentro de una transacción.
- [ ] Rechazar actualizaciones basadas en una revisión anterior con `409 Conflict`.
- [ ] Publicar configuración general y colecciones dentro de una única transacción.
- [ ] Evitar el patrón actual de borrar colecciones antes de completar todas las validaciones.
- [ ] Registrar usuario y fecha de cada guardado y publicación.

**Criterio de terminado:** guardar o cargar recursos en el borrador no modifica la respuesta
de `GET /api/v1/public/landing`; publicar actualiza todo el contenido o no actualiza nada.

## Fase 2 — Backend del editor

### DTO y validación

- [ ] Crear `WebsiteDraftDto` con los límites y nulabilidad actuales del dominio.
- [ ] Crear `WebsiteEditorDto` con borrador, revisión y metadatos.
- [ ] Mantener códigos de iconos como valores permitidos y etiquetas visibles separadas.
- [ ] Validar campos obligatorios como `commercialName` y `heroTitle`.
- [ ] Validar longitudes, URL `http/https`, correo, teléfonos y límites de colecciones.
- [ ] Asignar un `draftKey` UUID a elementos nuevos para editar imágenes antes de publicar.
- [ ] Normalizar `displayOrder` en backend; no confiar únicamente en el orden enviado.

### Servicio y controlador

- [ ] Extraer el mapeo entre documento editable, entidades publicadas y `PublicLandingDto`.
- [ ] Implementar `getEditor()`, `saveDraft()`, `publish()` y `resetDraft()`.
- [ ] Conservar temporalmente los endpoints administrativos actuales para compatibilidad.
- [ ] Marcar el `PUT /api/v1/admin/website` anterior como ruta de transición y dejar de
  consumirlo desde Angular cuando el editor esté validado.
- [ ] Mantener `@PreAuthorize("hasRole('SITE_ADMIN')")` en todas las operaciones del editor.
- [ ] Confirmar que un usuario autenticado sin ese rol recibe `403 Forbidden`.

### Imágenes de borrador

- [ ] Guardar imágenes de borrador bajo claves separadas de los recursos publicados.
- [ ] Validar en backend extensión real, MIME, tamaño máximo y categoría permitida.
- [ ] No eliminar un archivo todavía referenciado por la versión publicada.
- [ ] Al publicar, promover o referenciar el archivo de borrador de forma atómica.
- [ ] Limpiar archivos huérfanos después de un periodo seguro, nunca durante la transacción
  que reemplaza contenido publicado.

**Criterio de terminado:** el backend ofrece un ciclo completo de cargar, guardar, restaurar
y publicar, protegido por rol y con conflicto de revisión controlado.

## Fase 3 — Presentación reutilizable de la landing

- [ ] Separar la presentación actual en un componente reutilizable, por ejemplo
  `LandingViewComponent`.
- [ ] Mantener `LandingComponent` como contenedor público que obtiene `PublicLanding`.
- [ ] Crear `LandingEditorComponent` como contenedor protegido que trabaja con el borrador.
- [ ] Hacer que ambos contenedores rendericen `LandingViewComponent` para evitar dos diseños
  que puedan divergir.
- [ ] Definir eventos de selección por bloque sin introducir lógica administrativa en la
  experiencia pública.
- [ ] Mantener enlaces, navegación por anclas, menú móvil e imágenes de respaldo en ambos
  modos.
- [ ] Verificar que la vista pública no descargue código administrativo innecesario; cargar
  el editor de forma diferida.

**Criterio de terminado:** la landing pública y la vista del editor comparten la misma
estructura visual, y un cambio de diseño se realiza en un solo componente.

## Fase 4 — Experiencia de edición visual

### Barra del editor

- [ ] Añadir una barra fija con “Salir”, “Vista pública”, “Guardar borrador” y “Publicar”.
- [ ] Mostrar claramente los estados: guardado, cambios pendientes, guardando y error.
- [ ] Deshabilitar publicación mientras existan errores de validación o una operación activa.
- [ ] Advertir antes de salir o recargar cuando haya cambios locales sin guardar.
- [ ] Mostrar autor y fecha de la última publicación.

### Edición contextual

- [ ] Resaltar bloques editables solo en modo edición, sin alterar el layout público.
- [ ] Abrir un panel lateral al seleccionar hero, enfoque, contacto, SEO o redes sociales.
- [ ] Reflejar cada cambio válido inmediatamente en la vista previa local.
- [ ] Mostrar contador y límite de caracteres en textos sensibles al diseño.
- [ ] Permitir cancelar los cambios locales del bloque antes de guardar el borrador.
- [ ] Mantener etiquetas, foco visible, navegación por teclado y objetivos táctiles de 44 px.

### Colecciones

- [ ] Permitir crear, editar, ocultar y ordenar beneficios.
- [ ] Permitir crear, editar, ocultar y ordenar pasos del proceso.
- [ ] Permitir crear, editar, ocultar y ordenar profesionales y sus fotografías.
- [ ] Permitir editar título, subtítulo, icono y visibilidad de especialidades.
- [ ] Mostrar los servicios clínicos como datos derivados de su módulo; incluir un enlace
  administrativo para gestionarlos sin duplicar su fuente de verdad.
- [ ] Preferir controles “Subir/Bajar” en el MVP; incorporar arrastrar y soltar solo si se
  demuestra accesible también mediante teclado.

### Vista adaptable

- [ ] Añadir controles de previsualización para escritorio, tableta y móvil.
- [ ] Hacer que el panel sea lateral en escritorio y modal inferior en móvil.
- [ ] Comprobar que el editor no tape el contenido seleccionado.
- [ ] Mantener zoom y tamaño de texto del navegador sin desbordamientos críticos.

**Criterio de terminado:** un administrador puede modificar todos los campos disponibles en
el módulo anterior viendo el resultado real, usando ratón, teclado o pantalla táctil.

## Fase 5 — Navegación y migración del módulo anterior

- [ ] Añadir la ruta lazy `/editar-sitio` con `siteAdminGuard`.
- [ ] Mostrar “Editar sitio” en la landing únicamente cuando la sesión tenga
  `ROLE_SITE_ADMIN`.
- [ ] Mantener la seguridad aunque se ingrese directamente por URL o se manipule la UI.
- [ ] Cambiar el enlace lateral “Configuración del sitio web” para abrir `/editar-sitio`.
- [ ] Convertir `/settings/website` en redirección temporal hacia el editor.
- [ ] Mantener el componente anterior durante una versión como respaldo controlado.
- [ ] Eliminar el componente, estilos y ruta anterior cuando exista paridad funcional y las
  pruebas de aceptación hayan finalizado.

**Criterio de terminado:** no existen dos interfaces activas capaces de editar el mismo
contenido, y los accesos anteriores conducen al editor visual.

## Fase 6 — Pruebas y validación

### Backend

- [ ] Probar que los endpoints administrativos requieren `ROLE_SITE_ADMIN`.
- [ ] Probar guardado de borrador sin cambios en la respuesta pública.
- [ ] Probar publicación correcta de configuración y todas las colecciones.
- [ ] Probar rollback completo ante un elemento inválido durante la publicación.
- [ ] Probar conflicto `409` con dos revisiones concurrentes.
- [ ] Probar validaciones de URL, icono, archivo, tamaño y campos obligatorios.
- [ ] Probar que reemplazar una imagen de borrador no elimina la imagen publicada.
- [ ] Probar restauración del borrador desde la versión pública.

### Frontend

- [ ] Probar el guard y la visibilidad del acceso según el rol.
- [ ] Probar carga, vacío, error, cambios pendientes y conflicto de revisión.
- [ ] Probar que editar un campo actualiza la vista previa sin publicar.
- [ ] Probar guardar, publicar, restaurar y advertencia al salir.
- [ ] Probar altas, bajas, visibilidad y reordenamiento de colecciones.
- [ ] Probar cargas y reemplazos de imágenes.
- [ ] Probar navegación por teclado y foco al abrir/cerrar el panel contextual.

### Verificación integral

- [ ] Ejecutar pruebas backend con Maven.
- [ ] Ejecutar pruebas frontend.
- [ ] Ejecutar el build de producción de Angular.
- [ ] Verificar manualmente la landing en 320 px, tableta y escritorio.
- [ ] Comparar en dos sesiones: administrador con borrador y visitante con versión pública.
- [ ] Confirmar que no se modificaron secretos, archivos generados ni datos clínicos.

**Criterio de terminado:** todas las aplicaciones afectadas compilan, las pruebas críticas
pasan y un visitante nunca observa contenido parcial o no publicado.

## Orden recomendado de entrega

### Entrega 1 — Borrador seguro

- Persistencia de borrador, revisión optimista y publicación transaccional.
- Nuevos endpoints y pruebas backend.
- Sin retirar todavía el módulo actual.

### Entrega 2 — Editor visual mínimo

- Presentación compartida, ruta protegida, barra y edición de textos e imágenes principales.
- Guardar, restaurar y publicar.
- Pruebas de estado y permisos.

### Entrega 3 — Paridad funcional

- Beneficios, pasos, profesionales, especialidades, SEO y redes.
- Orden, visibilidad, responsive y accesibilidad.
- Redirección del módulo anterior.

### Entrega 4 — Retiro y limpieza

- Periodo de validación con usuarios.
- Eliminación del componente y endpoints heredados.
- Limpieza segura de recursos huérfanos y actualización de documentación.

## Archivos principales previstos

### Backend — nuevos

| Archivo | Responsabilidad |
| --- | --- |
| `V22__add_website_landing_draft.sql` | Borrador, revisión y auditoría de publicación. |
| `WebsiteLandingDraft.java` | Entidad singleton del borrador. |
| `WebsiteLandingDraftRepository.java` | Persistencia del documento editable. |
| `WebsiteDraftDto.java` | Contrato del contenido editable. |
| `WebsiteEditorDto.java` | Borrador, revisión y metadatos del editor. |

### Backend — a modificar

| Archivo | Cambio principal |
| --- | --- |
| `WebsiteAdminController.java` | Endpoints de borrador, publicación y recursos. |
| `WebsiteSettingsService.java` | Separar edición, publicación y lectura pública. |
| `WebsiteFileStorage.java` | Ciclo de vida de archivos de borrador y publicados. |
| `LocalWebsiteFileStorage.java` | Almacenamiento y limpieza segura de recursos. |
| `WebsiteSettings.java` | Metadatos de la última publicación. |
| `GlobalExceptionHandler.java` | Error estable para conflicto de revisión. |

### Frontend — nuevos o extraídos

| Archivo o carpeta | Responsabilidad |
| --- | --- |
| `features/public/landing-view/` | Presentación compartida de la landing. |
| `features/settings/landing-editor/` | Contenedor y estado del editor visual. |
| `features/settings/landing-editor/editor-toolbar/` | Acciones y estado del borrador. |
| `features/settings/landing-editor/editor-panel/` | Formularios contextuales. |
| `core/models/website-editor.model.ts` | Contratos del editor y revisión. |

### Frontend — a modificar

| Archivo | Cambio principal |
| --- | --- |
| `landing.component.*` | Convertirse en contenedor público de la vista compartida. |
| `website-settings.service.ts` | Consumir borrador, publicación y cargas preparadas. |
| `app.routes.ts` | Ruta lazy protegida y redirección temporal. |
| `main-layout.component.html` | Enlace hacia el editor visual. |
| `website-settings.component.*` | Retiro después de lograr paridad funcional. |

## Riesgos y mitigaciones

| Riesgo | Mitigación |
| --- | --- |
| Publicar cambios parciales | Transacción única y validación completa antes de escribir tablas públicas. |
| Perder el trabajo de otro administrador | Revisión optimista y respuesta `409 Conflict`. |
| Divergencia entre preview y landing | Un único componente de presentación y un único modelo visual. |
| Eliminar una imagen todavía publicada | Separar claves de borrador y comprobar referencias antes de borrar. |
| Duplicar servicios clínicos | Mantener el catálogo clínico como única fuente y hacerlo explícito en la UI. |
| Romper el diseño con textos extensos | Límites por campo, contadores y prueba responsive. |
| Exponer controles a visitantes | Ruta protegida, renderizado condicional y autorización obligatoria en backend. |
| Incrementar demasiado el alcance | Entregas graduales y editor de bloques controlados, sin maquetación libre. |

## Definición final de terminado

- [ ] Un visitante solo ve la última versión publicada.
- [ ] Un administrador edita y previsualiza sobre el diseño real de la landing.
- [ ] Guardar borrador y publicar son acciones distintas y comprensibles.
- [ ] Textos, imágenes, especialidades, beneficios, pasos, profesionales, contacto, redes y
  SEO tienen paridad con el módulo anterior.
- [ ] La seguridad se aplica tanto en Angular como en Spring Boot.
- [ ] La publicación es transaccional y controla ediciones concurrentes.
- [ ] El editor funciona en móvil y escritorio y es operable con teclado.
- [ ] El módulo anterior se retira sin dejar rutas o contratos duplicados.
- [ ] Pruebas backend, pruebas frontend y build Angular finalizan correctamente.
