# Convenciones observadas del backend

## Plataforma

- Java 17.
- Spring Boot 4.1.0 y Maven Wrapper.
- Spring MVC, Spring Data JPA, Spring Security, Bean Validation, PostgreSQL y Flyway.
- JWT con `jjwt` y filtro `JwtAuthenticationFilter`.
- Paquete base: `com.clinica.backend`.

## Estructura

```text
backend/src/main/java/com/clinica/backend/
├── config/
├── controller/
├── dto/
├── model/
├── repository/
├── security/
└── service/
```

Usar clases por capa y por concepto, por ejemplo `PatientController`, `PatientService`,
`PatientRepository`, `Patient` y `PatientDto`.

## Patrones de código

- Controladores: `@RestController`, `@RequestMapping`, `@RequiredArgsConstructor` y
  `ResponseEntity`.
- Servicios: `@Service`, dependencias `private final`, mapeadores privados manuales
  `mapToDto` y `mapToEntity`.
- Repositorios: `JpaRepository<Entity, Long>`, métodos derivados o `@Query` con parámetros.
- Entidades: JPA con Lombok; columnas SQL en `snake_case` y propiedades Java en `camelCase`.
- Fechas: `LocalDate`, `LocalTime` o `LocalDateTime`, según el dominio.
- Paginación: Spring `Page` serializado con `content` y metadatos bajo `page`.
- Escrituras compuestas: `@Transactional`; lecturas que lo requieran:
  `@Transactional(readOnly = true)`.

## Integridad y evolución

- Pacientes, pagos, insumos y servicios clínicos usan `deleted`; filtrar con consultas
  como `findByDeletedFalse`.
- Alertas se resuelven cambiando `active` y registrando `resolvedAt`.
- Algunos módulos antiguos todavía borran físicamente. No usar eso como precedente para
  nuevos agregados sin analizar relaciones, auditoría y reglas del dominio.
- El esquema tiene migraciones `V1` a `V5`. Crear siempre la siguiente versión libre y
  verificar el número presente antes de nombrarla.
- Aunque JPA está configurado actualmente con `ddl-auto: update`, tratar Flyway como fuente
  explícita de evolución del esquema.

## Contrato y seguridad

- La base pública del entorno Angular termina en `/api`.
- Existen rutas versionadas (`/api/v1/patients`, `/api/v1/appointments`, pagos, auth y
  usuarios) y rutas heredadas sin versión. No cambiar una familia existente de forma
  unilateral.
- La autenticación usa `Authorization: Bearer <token>`.
- Mantener públicos únicamente los endpoints que `SecurityConfig` declare como tales.

## Deuda que no debe convertirse en convención

- DTOs y controladores tienen poca Bean Validation.
- Se usa `orElseThrow()` o `RuntimeException` sin un contrato uniforme de errores.
- La cobertura backend se limita casi por completo al arranque del contexto.
- Hay rutas versionadas y no versionadas.

Al tocar estas áreas, mejorar de manera acotada y mantener sincronizado el consumidor Angular.
