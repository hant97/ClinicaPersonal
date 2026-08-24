# Retirada de rutas REST heredadas — Entrega C

Fecha efectiva de retirada: **2026-08-23**.

La auditoría de consumidores confirmó que Angular, pruebas y scripts del repositorio usan rutas
versionadas. Por ello se retiraron los alias sin versión y se conservaron únicamente estas rutas
canónicas:

| Recurso | Alias retirado | Ruta canónica |
| --- | --- | --- |
| Evaluaciones | `/api/assessments` | `/api/v1/assessments` |
| Catálogos | `/api/catalogs` | `/api/v1/catalogs` |
| Sesiones clínicas | `/api/clinical-sessions` | `/api/v1/clinical-sessions` |
| Configuración de clínica | `/api/settings/clinic` | `/api/v1/settings/clinic` |
| Alertas globales | `/api/alerts` | `/api/v1/alerts` |
| Alertas por paciente | `/api/patients/{patientId}/alerts` | `/api/v1/patients/{patientId}/alerts` |
| Movimientos de inventario | `/api/inventory-transactions` | `/api/v1/inventory-transactions` |
| Pruebas psicométricas | `/api/tests` | `/api/v1/tests` |
| Especialidades | `/api/specialties` | `/api/v1/specialties` |
| Insumos | `/api/supplies` | `/api/v1/supplies` |

No se modificaron verbos, parámetros, DTOs ni reglas de autorización. La migración Flyway
`V11__canonicalize_api_urls.sql` actualiza las URLs de logotipos persistidas con el prefijo
heredado. `ApiRouteConsolidationIntegrationTest` inspecciona los mappings registrados por Spring
y falla si vuelve a aparecer una ruta `/api/...` fuera de `/api/v1/...`.
