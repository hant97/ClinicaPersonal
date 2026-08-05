# Convenciones observadas del frontend

## Plataforma

- Angular 18.2, TypeScript 5.5, RxJS 7.8 y Zone.js.
- Configuración standalone mediante `bootstrapApplication` y `ApplicationConfig`.
- TypeScript estricto y plantillas estrictas.
- Locale global `es-PE`.
- Dependencias visuales existentes: Lucide Angular, Chart.js y SweetAlert2.

## Estructura

```text
frontend/src/app/
├── core/
│   ├── guards/
│   ├── interceptors/
│   ├── models/
│   └── services/
├── features/
├── layout/
├── shared/
│   ├── components/
│   └── services/
├── app.config.ts
└── app.routes.ts
```

Los features actuales cubren pacientes, agenda, facturación, inventario, servicios clínicos,
catálogos, perfil, autenticación y dashboard.

## Componentes y estado

- Todos los componentes observados son standalone.
- Predominan formularios reactivos, inyección por constructor, propiedades locales y
  `Observable.subscribe`.
- No existe un store global ni una adopción consistente de signals. No introducirlos para un
  cambio aislado.
- Importar en cada componente `CommonModule`, `ReactiveFormsModule`, `FormsModule`,
  `RouterLink`, iconos o componentes hijos que realmente use.
- Preferir la sintaxis Angular moderna `@if` y `@for` con una expresión `track` estable.

## API

- `environment.apiUrl` vale `/api` sobre el host correspondiente.
- Cada servicio agrega la ruta restante, por ejemplo `/v1/patients` o `/catalogs`.
- El interceptor funcional agrega el bearer token.
- Los servicios retornan `Observable` tipado.
- `PageResponse<T>` usa:

```ts
interface PageResponse<T> {
  content: T[];
  page: {
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
  };
}
```

- La paginación es base cero tanto en Spring como en Angular.

## Experiencia y estilo

- Reutilizar `PaginationComponent`, `PatientAutocompleteComponent`, `ToastService` y los
  servicios compartidos antes de duplicar soluciones.
- Mantener los estilos dentro del CSS del componente y respetar las variables/clases globales
  presentes en `src/styles.css`.
- Mantener etiquetas y mensajes para el usuario en español.
- Representar `LocalDate`, `LocalTime` y `LocalDateTime` del backend como cadenas JSON.

## Deuda que no debe convertirse en convención

- Hay dos servicios de autenticación y dos interceptores en rutas diferentes.
- Varias plantillas mezclan `*ngIf`/`*ngFor` con `@if`/`@for`.
- Muchas pruebas solo verifican que el componente o servicio se cree.
- Las rutas se importan de forma eager aunque Angular soporte carga diferida.

En código nuevo, reutilizar la ruta activa configurada en `app.config.ts`, usar control flow
moderno y escribir al menos una prueba de comportamiento relevante.
