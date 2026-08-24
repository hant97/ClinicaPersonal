# Plan de corrección de deuda técnica

> **Estado:** ✅ Entregas A, B y C implementadas  
> **Última actualización:** 2026-08-23  
> **Base:** auditoría técnica de backend Java 17/Spring Boot 4.1 y frontend Angular 18  
> **Regla de trabajo:** completar una fase, ejecutar su verificación y revisar el diff antes de iniciar la siguiente.

## Registro de implementación — Entrega A

Implementada el 2026-08-23:

- Línea base estabilizada: la prueba de Google Calendar usa un archivo temporal y la prueba
  de cierre de sesión ya no deja una navegación Angular activa tras destruir `TestBed`.
- Inventario protegido con bloqueo pesimista, respuesta `409 Conflict` y restricción SQL de
  stock no negativo.
- Agenda protegida con `pg_advisory_xact_lock` por especialidad y fecha, adquirido en orden
  estable para recurrencias antes de consultar solapamientos.
- Confirmación pública separada en `GET` de consulta y `POST` idempotente, con confirmación
  explícita en Angular y bloqueo de doble clic.
- Migración incremental añadida: `V10__protect_inventory_and_appointment_concurrency.sql`.

Verificación local:

- Backend: 244 pruebas exitosas y 2 pruebas PostgreSQL omitidas por no estar definida la
  conexión de integración.
- Frontend: 150 pruebas exitosas, sin `NG0205`.
- Build Angular de producción: exitoso.

Las pruebas concurrentes reales para PostgreSQL quedan incluidas en
`PostgresCriticalConcurrencyIntegrationTest` y se activan definiendo `POSTGRES_TEST_URL`,
`POSTGRES_TEST_USER` y `POSTGRES_TEST_PASSWORD` contra una base exclusiva de pruebas.

## Registro de implementación — Entrega C

Implementada el 2026-08-23:

- OSIV desactivado y lecturas transaccionales explícitas; listados de citas y bloqueos cargan
  profesionales por lote y los repositorios críticos usan grafos de entidad para evitar N+1.
- Alias REST sin versión retirados. La migración `V11__canonicalize_api_urls.sql` corrige URLs
  persistidas y una prueba de integración impide reintroducir rutas fuera de `/api/v1`.
- Reportes de pagos y validación del borrador web separados en colaboradores dedicados; la
  coordinación de inventario continúa encapsulada y probada.
- Cabeceras de Agenda y detalle de paciente extraídas como componentes de presentación; se
  reforzaron tipos, ciclo de vida RxJS y errores visibles mediante `ToastService`.
- ESLint, SpotBugs, JaCoCo, cobertura Angular y auditoría de producción añadidos como puertas
  obligatorias de CI con umbrales de línea base.

Verificación local:

- Backend: 263 pruebas ejecutadas, 0 fallos y 2 pruebas PostgreSQL omitidas sin conexión externa;
  JaCoCo superó los mínimos y SpotBugs terminó con 0 hallazgos.
- Frontend: 156 pruebas exitosas; cobertura 49.95 % statements, 27.42 % branches, 42.30 %
  functions y 51.55 % lines; lint sin errores y build de producción exitoso.
- Dependencias frontend de producción: 0 vulnerabilidades reportadas por `npm audit --omit=dev`.

## Registro de implementación — Entrega B

Implementada el 2026-08-23:

- El alcance de catálogos se deriva del usuario autenticado; solo `ROLE_SITE_ADMIN` puede
  solicitar `ALL` u otra especialidad y los intentos de acceso cruzado responden `403`.
- La paginación usa un límite global de 100 y valida `page >= 0` y `1 <= size <= 100`; los
  consumidores que necesitan todos los resultados recorren páginas sucesivas.
- Angular se actualizó de forma secuencial hasta 21.2.21, la última versión compatible con
  Node 20.19 del proyecto, conservando alineados CLI, Core, TypeScript, RxJS y Zone.js.
- Se retiró `xlsx` y las exportaciones simples ahora generan CSV UTF-8, con pruebas para
  caracteres españoles, fechas, montos, nombres de columnas y protección de fórmulas.
- Las rutas principales usan `loadComponent`, el shell queda eager y el presupuesto inicial
  se ajustó a 750 kB de advertencia y 1 MB de error.
- Las suscripciones de larga duración intervenidas usan `takeUntilDestroyed`; las vistas
  previas blob de inventario revocan la URL anterior y la pendiente al destruirse.

Verificación local:

- Backend: 256 pruebas ejecutadas, 0 fallos y 2 pruebas PostgreSQL omitidas cuando no están
  definidas las variables de conexión de integración.
- Frontend: 156 pruebas exitosas, sin `NG0205`.
- Build Angular de producción: exitoso; bundle inicial de 596.01 kB raw.
- `npm audit --omit=dev`: 0 vulnerabilidades de producción.

No se creó una rama separada porque la entrega se aplicó sobre el workspace solicitado, que
ya contenía cambios locales; esos cambios se conservaron sin descartarlos.

---

## 1. Resumen ejecutivo

El sistema cuenta con una base funcional sólida: DTOs, Bean Validation, borrado lógico,
autorización clínica, JWT con refresh rotatorio, Flyway, pruebas backend y pruebas Angular.
La auditoría identificó, sin embargo, riesgos vigentes de concurrencia, semántica HTTP,
autorización por especialidad, dependencias vulnerables, rendimiento y mantenibilidad.

La ejecución se organiza en tres entregas:

1. **Entrega A — Integridad y seguridad funcional:** estabilizar pruebas, proteger stock y
   agenda frente a concurrencia, y corregir la confirmación pública de citas.
2. **Entrega B — Dependencias, autorización y rendimiento frontend:** limitar alcance y
   paginación, actualizar dependencias vulnerables, aplicar lazy loading y cerrar fugas de
   recursos.
3. **Entrega C — Rendimiento interno y mantenibilidad:** eliminar consultas N+1, desactivar
   Open Session in View, retirar rutas heredadas, dividir clases grandes y añadir puertas de
   calidad.

### Exclusión expresa

Este plan **no incluye**:

- Modificación de los usuarios sembrados en la migración inicial.
- Rotación, eliminación o cambio obligatorio de sus contraseñas.
- Cambios en la prueba que documenta las contraseñas iniciales.
- Aprovisionamiento inicial de cuentas o credenciales de acceso al sistema.

La estabilización del test de Google Calendar sí está incluida: se corregirá la dependencia
de un archivo local externo, sin incorporar credenciales reales al repositorio.

---

## 2. Línea base verificada

| Verificación | Resultado actual |
|---|---|
| Backend `./mvnw.cmd test` | 238 pruebas ejecutadas; 1 fallo en `GoogleCalendarServiceTest`. |
| Frontend `npm test -- --watch=false --browsers=ChromeHeadless` | 145 pruebas exitosas; aparece un error asíncrono `NG0205` que no hace fallar Karma. |
| Frontend `npm run build` | Exitoso; bundle inicial de 2.09 MB. |
| `npm audit --omit=dev` | 9 vulnerabilidades altas en dependencias de producción. |
| Rutas Angular | Una sola ruta usa `loadComponent`; el resto se carga de forma eager. |
| Persistencia | Flyway activo y `ddl-auto: validate`; la última migración observada es `V9`. |
| Estado del workspace | Existen cambios frontend locales que deben conservarse y separarse de los PR de este plan. |

Antes de implementar una migración se deberá volver a comprobar la última versión disponible;
no se editará una migración ya aplicada.

---

## 3. Prioridades y resultados esperados

| Prioridad | Área | Resultado esperado |
|---|---|---|
| P0 | Concurrencia de inventario | Ninguna actualización perdida ni stock negativo. |
| P0 | Concurrencia de agenda | Una sola reserva válida por intervalo incompatible. |
| P0 | Confirmación pública | Abrir un enlace no modifica datos; confirmar requiere una acción explícita. |
| P1 | Dependencias frontend | Sin vulnerabilidades altas conocidas en producción o con excepción formalmente mitigada. |
| P1 | Autorización y paginación | Especialidad derivada del usuario y consultas limitadas globalmente. |
| P1 | CI y pruebas | Suites verdes, reproducibles y sin errores asíncronos ocultos. |
| P2 | Rendimiento | Menor bundle inicial, sin N+1 y sin sesión JPA abierta durante la respuesta. |
| P3 | Mantenibilidad | Servicios y componentes cohesivos, rutas canónicas y análisis estático obligatorio. |

---

## 4. Decisiones de diseño

| Tema | Decisión |
|---|---|
| Stock concurrente | Bloqueo pesimista de la fila del insumo dentro de la transacción y restricción SQL que impida stock negativo. |
| Agenda concurrente | Bloqueo transaccional PostgreSQL por especialidad y fecha antes de validar solapamientos. |
| Confirmación pública | `GET` de consulta sin efectos y `POST` idempotente para confirmar. |
| Paginación | Índice base cero, `PageResponse<T>` existente, tamaño máximo global de 100. |
| Catálogos | Solo `ROLE_SITE_ADMIN` puede solicitar `ALL` u otra especialidad; el resto usa la especialidad autenticada. |
| Angular | Actualización secuencial entre versiones mayores, ejecutando tests y build en cada salto. |
| Exportación | Sustituir `xlsx`, usar CSV cuando sea suficiente o trasladar exportación compleja al backend. |
| Carga frontend | Componentes standalone con `loadComponent`, sin introducir NgModules ni un store global. |
| JPA | DTOs mapeados dentro de transacciones; `spring.jpa.open-in-view=false`. |
| API heredada | Deprecar primero y retirar después de comprobar consumidores. |
| Refactor | Mantener la organización horizontal y extraer únicamente responsabilidades demostrablemente distintas. |

---

## 5. Contratos full-stack que cambiarán

### 5.1 Confirmación pública de citas

| Operación | Método actual | Método objetivo | Ruta | Efecto |
|---|---|---|---|---|
| Consultar token | `GET` | `GET` | `/api/v1/public/appointments/confirm/{token}` | Solo lectura. |
| Confirmar cita | No existe | `POST` | `/api/v1/public/appointments/confirm/{token}` | Cambia el estado a `CONFIRMADA`. |

Reglas:

- [ ] El `GET` no guarda la cita ni cambia `confirmedAt`.
- [ ] El `POST` es idempotente.
- [ ] Las citas canceladas, completadas o con inasistencia no se confirman.
- [ ] El frontend muestra detalles y solicita confirmación explícita.
- [ ] Los tokens no aparecen en logs ni mensajes de auditoría.

### 5.2 Paginación

| Elemento | Contrato objetivo |
|---|---|
| Parámetros | `page >= 0`; `1 <= size <= 100`. |
| Índice | Base cero. |
| Respuesta | `PageResponse<T>` con `content` y metadatos bajo `page`. |
| Error | Parámetros inválidos devuelven `400 Bad Request`. |
| Frontend | Reinicia `page` a cero al cambiar filtros o búsqueda. |

### 5.3 Alcance de catálogos

| Usuario | Parámetro permitido | Alcance efectivo |
|---|---|---|
| Profesional | Omitido o su especialidad | General + especialidad autenticada. |
| `ROLE_ADMIN` | Omitido o su especialidad | General + especialidad autenticada. |
| `ROLE_SITE_ADMIN` | `ALL` o especialidad explícita | Alcance solicitado. |

Una solicitud fuera del alcance permitido devolverá `403 Forbidden` y no revelará datos de
la otra especialidad.

---

## 6. Entrega A — Integridad y seguridad funcional

### Fase 0 — Estabilizar la línea base (P1)

**Objetivo:** asegurar que los cambios posteriores partan de un CI confiable.

#### Backend

- [ ] Separar la resolución de credenciales de Google de la construcción del cliente externo.
- [ ] Reescribir `GoogleCalendarServiceTest.testResolvingCredentialsPathWhenFileExists` con
  `@TempDir` o un colaborador simulado.
- [ ] Evitar dependencias de `google-credentials.json`, red o servicios de Google.
- [ ] Confirmar que el test verifica comportamiento y no la existencia de un archivo local.

#### Frontend

- [ ] Identificar la prueba que deja navegación, peticiones o observables activos después de
  destruir `TestBed`.
- [ ] Cerrar correctamente operaciones asíncronas con `fakeAsync`/`flush`, `firstValueFrom` o
  `HttpTestingController.verify()`.
- [ ] Hacer que los errores asíncronos no controlados fallen en CI en vez de quedar solo en la
  consola.
- [ ] Conservar las pruebas E2E fuera de esta fase cuando requieran credenciales de acceso.

**Criterio de cierre**

- [ ] Las 238 pruebas backend pasan sin archivos externos.
- [ ] Las 145 pruebas Angular pasan sin `NG0205` ni otros errores de consola.
- [ ] El build Angular pasa.
- [ ] El diff no contiene secretos ni archivos generados.

---

### Fase 1 — Integridad concurrente del inventario (P0)

**Objetivo:** impedir actualizaciones perdidas y stock negativo bajo solicitudes simultáneas.

#### Persistencia y servicio

- [ ] Añadir en `SupplyRepository` una consulta de insumo activo con
  `@Lock(LockModeType.PESSIMISTIC_WRITE)`.
- [ ] Recuperar el insumo bloqueado antes de calcular el nuevo stock.
- [ ] Mantener actualización de `Supply` y creación de `InventoryTransaction` dentro de la
  misma operación `@Transactional`.
- [ ] Confirmar que cobros, edición de cobros y restauración de stock usan exclusivamente
  `InventoryTransactionService`.
- [ ] Añadir una migración Flyway incremental con `CHECK (current_stock >= 0)` e índices
  necesarios.
- [ ] Traducir el conflicto de stock a `409 Conflict` mediante el contrato uniforme de errores.

#### Pruebas

- [ ] Ejecutar dos salidas simultáneas cuando el stock solo alcanza para una.
- [ ] Verificar que exactamente una solicitud termina con éxito.
- [ ] Probar entradas y salidas simultáneas sin actualización perdida.
- [ ] Probar que una transacción fallida no deja movimiento ni stock parcial.
- [ ] Probar que eliminar o editar un cobro restaura el stock exactamente una vez.
- [ ] Añadir al menos una prueba de integración concurrente sobre PostgreSQL, no solo H2.

**Criterio de cierre**

- [ ] Ningún escenario concurrente produce stock negativo.
- [ ] El stock final coincide con la suma de movimientos confirmados.
- [ ] El frontend conserva mensajes de error en español y permite reintentar.

---

### Fase 2 — Integridad concurrente de agenda (P0)

**Objetivo:** impedir que dos solicitudes creen citas incompatibles en el mismo horario.

#### Persistencia y servicio

- [ ] Crear una operación nativa que adquiera un bloqueo transaccional PostgreSQL por
  `specialty + appointmentDate`.
- [ ] Adquirir el bloqueo antes de ejecutar `findOverlappingAppointments`.
- [ ] Aplicar el mismo flujo en creación, edición, reprogramación y recurrencias.
- [ ] Adquirir bloqueos de recurrencias en orden estable de fecha para prevenir deadlocks.
- [ ] Añadir un índice compuesto para especialidad, fecha, profesional, estado y horario.
- [ ] Mantener intervalos semiabiertos: una cita que termina a las 10:00 no bloquea otra que
  comienza a las 10:00.
- [ ] Responder `409 Conflict` ante solapamiento.

#### Pruebas

- [ ] Dos reservas simultáneas del mismo intervalo guardan una sola cita.
- [ ] Intervalos adyacentes son válidos.
- [ ] Profesionales diferentes pueden compartir horario cuando la regla actual lo permite.
- [ ] Una cita sin profesional conserva su semántica global.
- [ ] Una recurrencia con conflicto intermedio revierte toda la serie.
- [ ] La prueba concurrente se ejecuta contra PostgreSQL.

**Criterio de cierre**

- [ ] La base nunca contiene dos citas que violen la regla de solapamiento.
- [ ] No aparecen bloqueos permanentes ni deadlocks en recurrencias.

---

### Fase 3 — Confirmación pública explícita (P0)

**Objetivo:** evitar que previsualizadores, antivirus o crawlers confirmen citas al abrir un
enlace.

#### Backend

- [ ] Separar `getConfirmationByToken` y `confirmByToken` en
  `AppointmentConfirmationService`.
- [ ] Mantener `GET /api/v1/public/appointments/confirm/{token}` como consulta sin escritura.
- [ ] Añadir `POST /api/v1/public/appointments/confirm/{token}` para confirmar.
- [ ] Hacer el `POST` idempotente.
- [ ] Mantener ambos endpoints bajo `PublicRateLimitFilter`.
- [ ] Añadir errores uniformes para token inexistente y estado no confirmable.

#### Frontend

- [ ] Sustituir `confirmByToken()` por `getConfirmation()` y `confirm()` en
  `AppointmentService`.
- [ ] Cargar la información al abrir `/confirmar-cita/:token` sin cambiar el estado.
- [ ] Mostrar botón explícito **Confirmar cita**.
- [ ] Implementar estados de carga, token inválido, ya confirmada, éxito y error.
- [ ] Bloquear dobles clics mientras la solicitud está en curso.

#### Pruebas

- [ ] Controller y servicio backend: el `GET` no modifica la entidad.
- [ ] Controller y servicio backend: el `POST` confirma y es idempotente.
- [ ] Servicio Angular: verbo, ruta y respuesta correctos.
- [ ] Componente Angular: abrir la pantalla no confirma.
- [ ] Prueba de contrato full-stack para ambos verbos.

**Criterio de cierre**

- [ ] Ninguna petición `GET` cambia el estado de una cita.
- [ ] Backend y frontend se despliegan juntos con el nuevo contrato.

---

## 7. Entrega B — Dependencias, autorización y rendimiento frontend

### Fase 4 — Alcance de catálogos y paginación central (P1)

#### 4.1 Autorización de catálogos

- [x] Derivar la especialidad efectiva exclusivamente del usuario autenticado.
- [x] Permitir `ALL` u otra especialidad únicamente a `ROLE_SITE_ADMIN`.
- [x] Rechazar con `403` los intentos de ampliar el alcance.
- [x] Actualizar `CatalogManagementComponent` para omitir `specialty` en usuarios clínicos.
- [x] Enviar `ALL` únicamente cuando el usuario sea `ROLE_SITE_ADMIN`.
- [x] Proteger `/settings/catalogs` con `adminGuard`.
- [x] Probar profesional, administrador clínico, administrador global y acceso cruzado.

#### 4.2 Paginación centralizada

- [x] Migrar controladores paginados a `Pageable` y `@PageableDefault` de forma incremental.
- [x] Configurar un tamaño máximo global de 100.
- [x] Validar `page >= 0` y `1 <= size <= 100`.
- [x] Conservar nombres `page` y `size`, índice base cero y `PageResponse<T>`.
- [x] Eliminar solicitudes frontend con `size=1000`.
- [x] Consumir páginas sucesivas o crear endpoints resumen cuando la UI necesite datos
  agregados.
- [x] Reiniciar a página cero al cambiar búsqueda o filtros.

**Criterio de cierre**

- [x] Un usuario no puede leer catálogos de otra especialidad.
- [x] Ningún endpoint paginado acepta respuestas masivas sin límite.
- [x] Los consumidores Angular conservan el contrato paginado.

---

### Fase 5 — Dependencias frontend vulnerables (P1)

#### 5.1 Actualización Angular

- [ ] Crear una rama o serie de PR dedicada a la actualización.
- [x] Actualizar una versión mayor por vez mediante las migraciones oficiales.
- [x] Mantener alineadas las versiones de Angular Core, CLI, compiler, router, forms,
  TypeScript y Zone.js.
- [x] Resolver APIs obsoletas antes de avanzar al siguiente salto mayor.
- [x] Ejecutar las 145 pruebas y el build en cada versión intermedia.
- [x] Ejecutar `npm audit --omit=dev` al finalizar la actualización.

#### 5.2 Sustitución de `xlsx`

- [x] Inventariar todas las exportaciones y características realmente utilizadas.
- [x] Elegir entre una biblioteca mantenida, CSV para casos simples o generación en backend
  para archivos complejos.
- [x] Retirar `xlsx` y actualizar `ExportService`.
- [x] Cargar la implementación de exportación bajo demanda.
- [x] Probar caracteres españoles, fechas, montos, columnas y nombres de archivo.

**Criterio de cierre**

- [x] `npm audit --omit=dev` no reporta vulnerabilidades altas de producción, o cualquier
  excepción restante tiene riesgo y mitigación documentados.
- [x] Las exportaciones mantienen el comportamiento esperado.
- [x] Tests y build permanecen en verde.

---

### Fase 6 — Lazy loading y ciclo de vida frontend (P2)

#### 6.1 Carga diferida

- [x] Mantener eager únicamente el shell mínimo necesario.
- [x] Convertir a `loadComponent` dashboard, pacientes, agenda, atenciones, cobros,
  inventario, servicios, catálogos, perfil y administración.
- [x] Mantener componentes standalone; no introducir NgModules.
- [x] Cargar Chart.js, exportación y otras dependencias pesadas solo en el feature que las usa.
- [x] Ajustar los presupuestos del build después de medir la nueva línea base.

#### 6.2 Suscripciones y URLs temporales

- [x] Usar `takeUntilDestroyed` o `async` pipe en suscripciones a streams de larga duración.
- [x] Corregir las suscripciones a `ClinicSettingsService.settings$` que sobreviven al
  componente.
- [x] Revocar la URL anterior al reemplazar la vista previa del inventario.
- [x] Revocar toda URL blob durante `ngOnDestroy`.
- [x] Revisar suscripciones a `BehaviorSubject`, router y servicios compartidos.
- [x] No añadir una infraestructura global de estado para resolver estos casos locales.

**Criterio de cierre**

- [x] Bundle inicial objetivo menor de 1.5 MB raw.
- [x] Cada feature principal produce un chunk independiente.
- [x] Navegar repetidamente no acumula suscriptores ni URLs blob.
- [x] No se reproducen errores de inyector destruido.

---

## 8. Entrega C — Rendimiento interno y mantenibilidad

### Fase 7 — JPA, consultas N+1 y transacciones (P2)

**Objetivo:** hacer explícito el límite transaccional y evitar consultas por cada fila.

- [x] Configurar `spring.jpa.open-in-view: false`.
- [x] Corregir todos los accesos lazy que aparezcan fuera de transacción.
- [x] Cargar profesionales de citas en bloque mediante IDs o proyección.
- [x] Cargar profesionales de bloqueos de agenda en bloque.
- [x] Mantener la carga agrupada de pagos por citas.
- [x] Revisar dashboard, pagos y reportes para detectar consultas dentro de bucles.
- [x] Añadir `@Transactional(readOnly = true)` en lecturas que lo requieran.
- [x] Añadir pruebas que verifiquen un número acotado de consultas para listados principales.

**Criterio de cierre**

- [x] El número de consultas no crece linealmente por cada fila mapeada.
- [x] Ningún controlador depende de una sesión JPA abierta.
- [x] Los DTOs y respuestas JSON no cambian.

---

### Fase 8 — Consolidación de rutas API (P3)

**Objetivo:** conservar una sola ruta canónica versionada por recurso.

#### Paso 1 — Deprecación

- [x] Inventariar las rutas dobles `/api/v1/...` y `/api/...`.
- [x] Confirmar que Angular, scripts y pruebas usan rutas versionadas.
- [x] Registrar temporalmente el uso de rutas heredadas.
- [x] Documentar la fecha o versión prevista de retirada.

#### Paso 2 — Retirada

- [x] Eliminar los alias no versionados cuando no existan consumidores conocidos.
- [x] Actualizar pruebas de contrato y documentación.
- [x] No cambiar DTOs, parámetros ni comportamiento durante esta limpieza.
- [x] Confirmar que `SecurityConfig` protege las rutas canónicas de la misma forma.

**Criterio de cierre**

- [x] Existe una única ruta canónica `/api/v1/...` por recurso.
- [x] Todos los consumidores conocidos usan la ruta versionada.

---

### Fase 9 — Refactorización y puertas de calidad (P3)

#### 9.1 Backend

Prioridad de refactor:

1. `PaymentService`.
2. `WebsiteSettingsService`.
3. `AttentionService`.
4. `AppointmentService`.

Tareas:

- [x] Separar reportes de pagos de las operaciones CRUD.
- [x] Mantener la coordinación de inventario detrás de un colaborador explícito y probado.
- [x] Separar publicación, validación y persistencia del editor web.
- [x] Mantener los controladores delgados y el mapeo manual de DTOs.
- [x] No introducir una arquitectura nueva ni un framework de mapeo.

#### 9.2 Frontend

Prioridad de refactor:

1. `AgendaComponent`.
2. `PatientDetailComponent`.
3. `CatalogManagementComponent`.
4. Formularios de cobro y atención.

Tareas:

- [x] Extraer componentes de presentación y secciones de formulario cohesivas.
- [x] Mantener la coordinación HTTP en el componente contenedor.
- [x] Reemplazar `any` por tipos de formularios, eventos, gráficos e iconos.
- [x] Unificar errores visibles con `ToastService`.
- [x] Añadir estados de carga, vacío, error y éxito donde correspondan.

#### 9.3 Calidad automática

- [x] Incorporar ESLint para Angular y TypeScript.
- [x] Incorporar Checkstyle o SpotBugs en Maven.
- [x] Añadir JaCoCo y cobertura frontend.
- [x] Establecer inicialmente umbrales iguales a la cobertura real para impedir regresiones.
- [x] Incrementar los umbrales de forma gradual.
- [x] Añadir `npm audit --omit=dev` al CI.
- [x] Mantener tests backend, tests frontend y build como verificaciones obligatorias.

**Criterio de cierre**

- [x] Las clases priorizadas tienen responsabilidades claramente separadas.
- [x] No se degrada la cobertura.
- [x] El CI rechaza lint, análisis estático, tests, build o auditoría de producción fallidos.

---

## 9. Orden recomendado de PR

1. Estabilización de pruebas.
2. Bloqueo de stock y prueba concurrente.
3. Serialización de citas y prueba concurrente.
4. Confirmación pública `GET`/`POST`, backend y frontend juntos.
5. Alcance de catálogos.
6. Paginación centralizada.
7. Actualización Angular.
8. Sustitución de `xlsx`.
9. Lazy loading y ciclo de vida frontend.
10. `open-in-view: false` y eliminación de N+1.
11. Deprecación y retirada de rutas heredadas.
12. Refactor backend.
13. Refactor frontend y puertas de calidad.

No se deben mezclar actualizaciones mayores de dependencias con cambios de reglas clínicas o
de concurrencia en el mismo PR.

---

## 10. Definition of Done global

Cada fase o PR debe cumplir:

- [ ] Pruebas backend completas y exitosas.
- [ ] Pruebas frontend completas, sin errores no controlados en consola.
- [ ] Build Angular de producción exitoso.
- [ ] Contrato REST y modelos TypeScript sincronizados.
- [ ] Fechas Java `java.time` representadas como `string` en TypeScript.
- [ ] Paginación base cero y formato `PageResponse<T>` conservados.
- [ ] Migración Flyway incremental cuando corresponda; nunca modificar una aplicada.
- [ ] Pruebas concurrentes para agenda e inventario.
- [ ] Sin vulnerabilidades altas nuevas en dependencias de producción.
- [ ] Sin secretos, archivos generados ni cambios accidentales sobre trabajo local existente.
- [ ] Diff completo revisado antes de integrar.

## 11. Criterio de cierre del plan

El plan se considerará completado cuando:

- [ ] Inventario y agenda estén protegidos frente a concurrencia real.
- [ ] Abrir un enlace público de cita no modifique el estado.
- [ ] La autorización de catálogos no permita ampliar especialidad desde el cliente.
- [ ] Todos los listados tengan paginación limitada.
- [ ] La auditoría de dependencias no tenga vulnerabilidades altas sin mitigación aprobada.
- [ ] El bundle inicial esté por debajo del objetivo acordado.
- [x] `open-in-view` esté desactivado y no queden N+1 conocidos en los flujos principales.
- [x] La API activa use rutas `/api/v1` canónicas.
- [x] CI ejecute análisis estático, pruebas, build y auditoría de dependencias.
- [ ] La deuda de credenciales permanezca explícitamente fuera del alcance de este documento.
