# Convenciones observadas del frontend

## Plataforma

- Angular 21.2, TypeScript 5.9, RxJS 7.8 y Zone.js 0.15.
- Node 24.21.0 y npm 11 para el entorno reproducible (`.nvmrc` y `package.json`).
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
- Predominan formularios reactivos, estado local y `Observable.subscribe`.
- No existe un store global ni una adopción consistente de signals. No introducirlos para un
  cambio aislado.
- Importar en cada componente `CommonModule`, `ReactiveFormsModule`, `FormsModule`,
  `RouterLink`, iconos o componentes hijos que realmente use.
- Preferir la sintaxis Angular moderna `@if` y `@for` con una expresión `track` estable.

## API

- `environment.apiUrl` vale `/api` sobre el host correspondiente.
- Cada servicio agrega la ruta versionada restante, por ejemplo `/v1/patients`.
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

- Varias plantillas mezclan `*ngIf`/`*ngFor` con `@if`/`@for`.
- Los componentes más grandes necesitan dividirse con pruebas de comportamiento como apoyo.

En código nuevo, usar las rutas con `loadComponent` de `app.routes.ts`, el interceptor de
`app.config.ts`, control flow moderno y pruebas de comportamiento relevantes.
