# Lista de control del contrato

## Tabla mínima

Antes de editar, completar mentalmente o en notas:

| Elemento | Backend | Frontend |
|---|---|---|
| Método | `@GetMapping`, `@PostMapping`, etc. | `http.get`, `post`, etc. |
| Ruta | `@RequestMapping` + mapping del método | `environment.apiUrl` + segmento |
| Entrada | path, query y DTO | interpolación, `HttpParams` y body |
| Salida | DTO, `Page<DTO>` o `Void` | interfaz, `PageResponse<T>` o `void` |
| Seguridad | pública o JWT | guard/interceptor y flujo de sesión |

## Convenciones de serialización

- JSON en `camelCase`.
- IDs Java `Long` ↔ TypeScript `number`.
- `BigDecimal` ↔ `number`, conservando redondeo monetario en backend.
- `LocalDate`, `LocalTime`, `LocalDateTime` ↔ `string`.
- Respuesta paginada de Spring Boot 4:

```json
{
  "content": [],
  "page": {
    "size": 10,
    "number": 0,
    "totalElements": 0,
    "totalPages": 0
  }
}
```

- DELETE exitoso usado por la UI: `204 No Content` y `Observable<void>`.
- Token: cabecera `Authorization: Bearer <token>`.

## Rutas actuales

La API mezcla familias versionadas y heredadas:

- Versionadas: auth, users, patients, appointments y payments.
- Sin versión: catalogs, clinical-services, supplies, medical-records, clinical-sessions,
  assessments, tests, alerts, inventory-transactions y clinic settings.

No normalizar una sola familia dentro de un cambio funcional sin migrar también todos sus
consumidores. Para recursos nuevos, preferir `/api/v1/{resources}`.

## Validación de cambios

### Crear o editar campos

- Actualizar migración, entidad, DTO, mapper, interfaz TypeScript y formulario.
- Decidir nulabilidad una sola vez y reflejarla en SQL, Java, validación y TypeScript.
- No permitir que el cliente establezca campos calculados o de auditoría.

### Cambiar filtros o paginación

- Conservar nombres exactos de parámetros.
- Conservar índice de página base cero.
- Reiniciar la página a cero al cambiar búsqueda o filtros.
- Reutilizar `PageResponse<T>` y `PaginationComponent`.

### Cambiar errores

- Evitar que el frontend dependa de mensajes accidentales de excepciones Java.
- Mostrar al usuario mensajes en español y conservar detalles técnicos fuera de la UI.
- Añadir una prueba que demuestre la regla o validación que originó el cambio.
