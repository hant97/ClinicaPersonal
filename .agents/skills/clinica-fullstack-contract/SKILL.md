---
name: clinica-fullstack-contract
description: Diseñar, implementar o revisar cambios full-stack de ClinicaPersonal manteniendo sincronizados el contrato REST de Spring Boot y su consumidor Angular. Usar cuando una funcionalidad afecte simultáneamente rutas, verbos, DTOs, modelos TypeScript, paginación, filtros, fechas, autenticación, validación, errores, formularios o flujos CRUD entre backend/ y frontend/.
---

# Mantener el contrato full-stack

## Trazar primero el flujo

1. Leer `references/contract-checklist.md`.
2. Identificar en backend controlador, DTO, servicio, entidad y repositorio.
3. Identificar en frontend ruta, componente, servicio HTTP y modelo.
4. Escribir una tabla de contrato breve con verbo, URL, parámetros, request y response.
5. Resolver diferencias existentes antes de ampliar el flujo.

## Implementar de extremo a extremo

1. Definir primero el contrato externo con nombres JSON en `camelCase`.
2. Implementar la regla de negocio y persistencia con `$clinica-backend-feature`.
3. Implementar el modelo, cliente y UI con `$clinica-frontend-feature`.
4. Mantener fechas como tipos `java.time` en Java y `string` en TypeScript.
5. Mantener paginación base cero y el formato `PageResponse<T>` que espera Angular.
6. Mantener los catálogos como códigos estables y etiquetas de presentación separadas.
7. No exponer campos internos como contraseñas, flags técnicos o relaciones JPA completas.

## Verificar el hilo completo

- Comparar propiedad por propiedad el DTO Java y la interfaz TypeScript.
- Comparar verbo, ruta, query params, path params y cuerpo.
- Verificar autenticación y autorización del endpoint.
- Probar éxito, validación o regla inválida y recurso inexistente.
- Verificar carga, vacío, error, creación/edición y refresco de UI.
- Ejecutar pruebas backend, pruebas frontend y build Angular.

No considerar completo un cambio de API mientras solo uno de los dos lados compile.
