---
name: clinica-backend-feature
description: Implementar, extender o revisar funcionalidades del backend de ClinicaPersonal con Java 17, Spring Boot 4, Maven, Spring MVC, JPA, PostgreSQL, Flyway y JWT. Usar cuando se creen o modifiquen entidades, DTOs, repositorios, servicios, controladores REST, reglas clínicas, seguridad, paginación, borrado lógico, migraciones o pruebas backend dentro de backend/.
---

# Implementar funcionalidades backend

## Preparar el cambio

1. Leer los archivos del módulo relacionado en `backend/src/main/java/com/clinica/backend`.
2. Leer `references/backend-conventions.md`.
3. Localizar el cliente Angular correspondiente antes de cambiar un endpoint o DTO.
4. Mantener el paquete base `com.clinica.backend` y la organización horizontal existente.
5. No introducir una librería o patrón arquitectónico nuevo sin una necesidad explícita.

## Implementar por capas

1. Modelar persistencia en `model/` con JPA y nombres Java en inglés.
2. Exponer datos mediante DTOs en `dto/`; no devolver entidades desde controladores.
3. Crear consultas en una interfaz `repository/` que extienda `JpaRepository`.
4. Mantener reglas, transacciones y mapeo manual entidad/DTO en `service/`.
5. Mantener el controlador delgado en `controller/`, devolver `ResponseEntity` y delegar la lógica.
6. Usar inyección por constructor con `@RequiredArgsConstructor` y campos `private final`.
7. Añadir una migración Flyway incremental cuando cambie el esquema; no editar migraciones aplicadas.

## Preservar comportamiento

- Usar `Page<T>` y `Pageable` para listados potencialmente grandes.
- Excluir registros borrados en lecturas cuando la entidad tenga `deleted`.
- Aplicar borrado lógico solo si el agregado ya lo soporta o si se incorpora de extremo a extremo.
- Proteger operaciones compuestas con `@Transactional`; usar `readOnly = true` en consultas complejas.
- Mantener las rutas existentes de un módulo. Para un módulo nuevo, preferir `/api/v1/{resources}` y reflejar exactamente esa ruta en Angular.
- No copiar inconsistencias existentes como validación ausente, borrado físico accidental o rutas mezcladas.

## Verificar

1. Añadir pruebas enfocadas a reglas de negocio, consultas, seguridad o contrato modificado.
2. Ejecutar desde `backend/`:

```powershell
.\mvnw.cmd test
```

3. Revisar que cualquier cambio de DTO, ruta, verbo, parámetro o paginación tenga su ajuste equivalente en el frontend.
4. Invocar también `$clinica-fullstack-contract` cuando el cambio cruce la API.
