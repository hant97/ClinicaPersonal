# Fase 0: línea base y contratos actuales

> Documento de referencia creado el 2026-08-13. No contiene secretos ni valores de producción.

## Estado del árbol de trabajo

- Rama de trabajo: `chore/phase-0-baseline`.
- Al crear la rama ya existían cambios funcionales sin confirmar. Se conservaron intactos y esta fase no los descarta, restablece ni confirma.
- Los cambios posteriores de esta fase se limitan a documentación y aislamiento del contexto de pruebas backend.

## Inventario de configuración sensible

Los valores de producción deben mantenerse exclusivamente en el gestor de secretos o en la plataforma de despliegue. Esta tabla registra qué valores se requieren, no sus contenidos.

| Variable | Uso | Requisito de producción |
|---|---|---|
| `SPRING_DATASOURCE_URL` | URL de PostgreSQL | Base de datos de producción; nunca la de desarrollo o pruebas. |
| `SPRING_DATASOURCE_USERNAME` | Usuario de PostgreSQL | Cuenta con privilegios mínimos necesarios. |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de PostgreSQL | Secreto rotado y no versionado. |
| `JWT_SECRET` | Firma de JWT HS256 | Secreto Base64 de al menos 32 bytes; distinto por entorno. |
| `AUTH_COOKIE_SECURE` | Atributo `Secure` de refresh cookie | `true` en HTTPS/producción. |
| `CORS_ALLOWED_ORIGINS` | Orígenes permitidos | Lista explícita de dominios del frontend. |

## Contrato actual de autenticación

| Operación | Método y ruta | Entrada | Salida | Seguridad |
|---|---|---|---|---|
| Iniciar sesión | `POST /api/v1/auth/login` | JSON: `username`, `password` | `200` con `token`, `specialty`, `roles`; emite cookie `refresh_token` | Pública; validación de credenciales y bloqueos. |
| Renovar sesión | `POST /api/v1/auth/refresh` | Cookie HTTP-only `refresh_token` | `200` con nuevo access token y cookie rotada | Pública; token de refresh válido y no revocado. |
| Cerrar sesión | `POST /api/v1/auth/logout` | Cookie opcional y header `Authorization: Bearer <token>` | `204 No Content`; revoca los tokens recibidos y expira la cookie | Requiere autenticación. |

- La cookie se limita a `/api/v1/auth`, es `HttpOnly` y configura `SameSite` por entorno (`None` con `Secure` cuando frontend y API usan dominios distintos).
- Los errores de validación de login responden `400`; las credenciales inválidas y refresh inválido responden `401`.
- El access token se transmite con `Authorization: Bearer <token>` y no se persiste en almacenamiento del navegador.

## Contrato actual de evaluaciones dermatológicas

| Operación | Método y ruta | Entrada | Salida | Seguridad |
|---|---|---|---|---|
| Listar por paciente | `GET /api/v1/patients/{patientId}/dermatological-evaluations` | Query `page` (base cero), `size` | `200` con `Page<DermatologicalEvaluationDto>`: `content` y metadata bajo `page` | JWT; especialidad `DERMATOLOGIA`. |
| Crear | `POST /api/v1/patients/{patientId}/dermatological-evaluations` | `DermatologicalEvaluationDto` | `200` con la evaluación creada | JWT; especialidad `DERMATOLOGIA`. |
| Consultar | `GET /api/v1/dermatological-evaluations/{id}` | Path `id` | `200` con `DermatologicalEvaluationDto` | JWT; especialidad `DERMATOLOGIA`. |
| Actualizar | `PUT /api/v1/dermatological-evaluations/{id}` | `DermatologicalEvaluationDto` | `200` con la evaluación actualizada | JWT; especialidad `DERMATOLOGIA`. |
| Eliminar | `DELETE /api/v1/dermatological-evaluations/{id}` | Path `id` | `204 No Content` | JWT; especialidad `DERMATOLOGIA`. |

`DermatologicalEvaluationDto` usa IDs numéricos y fechas JSON como cadenas (`LocalDate`/`LocalDateTime` en backend). El cliente debe representar el listado con `PageResponse<DermatologicalEvaluation>` y leer los metadatos desde `page`.

## Línea base de verificación

| Comprobación | Resultado inicial |
|---|---|
| `backend: ./mvnw.cmd clean test` | Correcto: 16 pruebas. Antes de esta fase usaba PostgreSQL local. |
| `frontend: npm run build` con Node 14 predeterminado | Fallaba: Angular 18 requiere Node 18.19 o posterior. |
| `frontend: npm run build` con Node 20 local | Correcto, con advertencia de presupuesto CSS en la landing. |
| `frontend: npm test -- --watch=false --browsers=ChromeHeadless` con Node 20 | 22 correctas y 4 fallidas por falta de `HttpClient` en pruebas de layout/dashboard. |

## Aislamiento de pruebas backend

- `BackendApplicationTests` activa ahora el perfil `test`.
- `backend/src/test/resources/application-test.yml` configura una H2 en memoria llamada `clinica_test`; no se conecta a PostgreSQL de desarrollo.
- Flyway se desactiva solo para este test de contexto y JPA crea/elimina el esquema en memoria. La validación de migraciones PostgreSQL se añadirá como prueba de integración en la Fase 5.
