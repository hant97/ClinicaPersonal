# Convenciones observadas del backend

## Plataforma

- Java 21.
- Spring Boot 4.1.1 y Maven Wrapper.
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
- Servicios: `@Service`, dependencias `private final`.
- Mapeo Entity↔DTO: interfaces MapStruct (`@Mapper(componentModel = "spring")`) en
  `com.clinica.backend.mapper`, inyectadas en el servicio como cualquier otro bean
  (`XxxMapper`, con método `toDto` y, cuando aplica, `toEntity`). Es el patrón para
  todo servicio nuevo. Cuando el mapeo real incluye campos calculados, listas
  filtradas/ordenadas o reglas de negocio (ver `PaymentMapper`, `CatalogMapper`,
  `PatientMapper`, `ScheduleBlockMapper`, `SupplyMapper`), el mapper cubre solo los
  campos directos (con `@Mapping(target = "...", ignore = true)` para el resto) y el
  service completa esos campos después de llamar al mapper — no meter esa lógica
  dentro de la interfaz del mapper.
  `AppointmentService` es la única excepción deliberada: su "mapeo" hace batching de
  nombres de profesional y lookup de pago (N+1 evitado a propósito), así que se dejó
  con métodos manuales en vez de forzarlo a MapStruct.
  Los tests unitarios que instancian el servicio a mano (sin contexto Spring) deben
  pasar la implementación generada (`XxxMapperImpl`) — o, si usan
  `@ExtendWith(MockitoExtension.class)` con `@InjectMocks`, declarar el mapper como
  `@Spy private XxxMapperImpl xxxMapper = new XxxMapperImpl();` en vez de `@Mock`,
  para que el mapeo real se ejecute y las aserciones sobre campos del DTO no reciban
  `null`.
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
- El esquema tiene migraciones hasta `V17`. Verificar la última versión presente antes de
  nombrar una migración nueva.
- JPA usa `ddl-auto: validate`; Flyway es la fuente de evolución del esquema.

## Contrato y seguridad

- La base pública del entorno Angular termina en `/api`.
- Las rutas de los controladores usan `/api/v1`. No cambiar un contrato existente de forma
  unilateral.
- La autenticación usa `Authorization: Bearer <token>`.
- Mantener públicos únicamente los endpoints que `SecurityConfig` declare como tales.

## Deuda que no debe convertirse en convención

- Algunos módulos antiguos todavía contienen validaciones incompletas o excepciones
  genéricas. Revisar el contrato de errores de `GlobalExceptionHandler` al tocarlos.
- Mantener pruebas de comportamiento para reglas clínicas, seguridad y persistencia;
  no usar una prueba de arranque como sustituto.

Al tocar estas áreas, mejorar de manera acotada y mantener sincronizado el consumidor Angular.
