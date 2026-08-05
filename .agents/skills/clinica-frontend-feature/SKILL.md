---
name: clinica-frontend-feature
description: Implementar, extender o revisar funcionalidades del frontend de ClinicaPersonal con Angular 18, TypeScript estricto, componentes standalone, formularios reactivos, RxJS y clientes HttpClient. Usar cuando se creen o modifiquen páginas, componentes, rutas, modelos, servicios API, guards, interceptores, formularios, catálogos, paginación, estilos o pruebas dentro de frontend/.
---

# Implementar funcionalidades frontend

## Preparar el cambio

1. Leer el feature relacionado y su servicio/modelo en `frontend/src/app`.
2. Leer `references/frontend-conventions.md`.
3. Revisar el controlador y DTO backend correspondientes antes de cambiar una llamada HTTP.
4. Mantener TypeScript y plantillas compatibles con los modos estrictos del proyecto.

## Ubicar responsabilidades

- Colocar pantallas y componentes específicos en `features/<dominio>/`.
- Colocar modelos, servicios API, guards e interceptores transversales en `core/`.
- Colocar componentes y servicios reutilizables sin conocimiento de dominio en `shared/`.
- Colocar shells de navegación en `layout/`.
- Registrar rutas en `app.routes.ts` y proteger las privadas con `authGuard`.

## Implementar

1. Crear componentes standalone e importar explícitamente sus dependencias.
2. Usar formularios reactivos y validadores para captura de datos.
3. Usar `HttpClient` en servicios `providedIn: 'root'` y devolver `Observable<T>`.
4. Mantener la suscripción y la coordinación de UI en el componente.
5. Modelar el JSON real con interfaces TypeScript; representar fechas como `string`.
6. Construir URLs desde `environment.apiUrl`; no fijar hosts ni duplicar `/api`.
7. Usar `PageResponse<T>` y el componente compartido de paginación para respuestas paginadas.
8. Usar `@if` y `@for (...; track ...)` en plantillas nuevas o tocadas.
9. Mantener textos visibles en español y nombres de código en inglés.

## Verificar

1. Añadir pruebas útiles de servicios HTTP, validadores, estados y comportamiento del componente.
2. Ejecutar desde `frontend/`:

```powershell
npm test -- --watch=false
npm run build
```

3. Confirmar estados de carga, vacío, error y éxito cuando apliquen.
4. Confirmar que ruta, verbo, parámetros, cuerpo y respuesta coincidan con Spring.
5. Invocar también `$clinica-fullstack-contract` cuando el cambio cruce la API.
