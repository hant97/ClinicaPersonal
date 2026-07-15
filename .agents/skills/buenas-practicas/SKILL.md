---
name: buenas-practicas-proyecto-personalizado
description: Reglas de arquitectura basadas en el estilo actual del proyecto, optimizadas para Java 18 (Spring Boot) y Angular 18.
tags: [java18, spring-boot, angular18, signals, clean-code, custom-rules]
---

# Reglas de Arquitectura y Buenas Prácticas del Proyecto

Este proyecto sigue una serie de convenciones estrictas que deben ser respetadas en todo momento para mantener la coherencia con el código existente.

## Reglas Generales de Estilo
- **Clean Code**: Todo el código (nombres de variables, métodos, clases) debe estar en inglés para mantener la consistencia con el código existente.
- **Legibilidad**: Mantén el código simple y fácil de leer. Sigue los principios de diseño SOLID y DRY.
- **Formato**: Respeta la indentación, espaciado y estructura de carpetas de los archivos existentes.
- **Importaciones explícitas**: NUNCA utilices el nombre de paquete completo (Fully Qualified Name) de una clase o interfaz directamente como tipo de variable o parámetro de un método (por ejemplo, `java.time.LocalDateTime start`). En su lugar, debes **importar** la clase en la cabecera del archivo y generar el objeto usando únicamente el nombre simple de la clase (por ejemplo, `LocalDateTime start`).

## Reglas de Backend (Java 17/18 + Spring Boot + Maven)
- **Arquitectura de Capas**:
  - `Controller`: Define los endpoints REST usando anotaciones estándar (`@RestController`, `@RequestMapping`). Los endpoints deben envolver la respuesta en `ResponseEntity`.
  - `Service`: Contiene toda la lógica de negocio, anotado con `@Service`.
  - `Repository`: Interfaces que extienden de Spring Data JPA.
  - `DTOs`: Utiliza objetos DTO (ej. `PatientDto`) para la transferencia de datos de cara a la API.
- **Inyección de Dependencias**: Utiliza inyección de dependencias por constructor proporcionada por Lombok mediante la anotación `@RequiredArgsConstructor`, declarando los servicios inyectados como `private final`.
- **Mapeo de Entidades**: El mapeo entre Entidades y DTOs se hace de forma manual dentro de los servicios (ej. métodos privados `mapToDto` y `mapToEntity`). **No incorpores MapStruct** ni otras librerías de mapeo que no estén ya en el `pom.xml`.
- **Lombok y Java**: El proyecto utiliza Lombok. El POM está configurado para Java 17 (`<java.version>17</java.version>`). Puedes aprovechar características modernas como `Records` para nuevos DTOs si lo consideras adecuado, asegurando compatibilidad.
- **Eliminación Lógica**: El proyecto maneja el borrado mediante un flag booleano (*Soft Delete*: `setDeleted(true)`), no uses borrado físico (`deleteById`).
- **Manejo de Excepciones**: Para búsquedas, utiliza `orElseThrow()` al buscar por ID y delega en el manejo global de excepciones.

## Reglas de Frontend (Angular 18)
- **Componentes Standalone**: Todos los componentes nuevos deben ser *Standalone Components* (`standalone: true`). Deben importar explícitamente en el decorador `@Component` lo que necesiten (`CommonModule`, `RouterLink`, componentes anidados e iconos).
- **Estado Reactivo**:
  - El proyecto actual interactúa con los servicios consumiendo APIs a través de **RxJS** (`Observables` y `.subscribe()`). Mantén este patrón de servicios inyectables para la obtención de datos y el estado de los componentes.
  - No introduzcas `Signals` a menos que se requiera refactorizar explícitamente una parte del estado local reactivo.
- **Flujo de Control Declarativo (Angular 18)**:
  - Para las nuevas vistas y componentes HTML, **debes utilizar** la nueva sintaxis de control de flujo declarativo de Angular 18: `@if`, `@else`, y `@for` (con `track`), abandonando progresivamente las directivas estructurales tradicionales (`*ngIf`, `*ngFor`) presentes en el código antiguo.
