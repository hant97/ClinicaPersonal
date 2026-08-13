# Fase 1: seguridad de configuración y sesiones

## Configuración requerida

El backend no arranca sin `JWT_SECRET`, `DB_PASSWORD`, `AUTH_COOKIE_SECURE` y
`CORS_ALLOWED_ORIGINS`. `JWT_SECRET` debe ser Base64 y representar al menos 32 bytes.
`CORS_ALLOWED_ORIGINS` es una lista de URLs concretas separadas por comas; se rechazan
comodines.

El despliegue Render declara `AUTH_COOKIE_SECURE=true`. El ejemplo de desarrollo local usa
`false` únicamente para HTTP local. La plantilla [`backend/.env.example`](../backend/.env.example)
no contiene secretos; el archivo local `backend/.env` se carga automáticamente y está ignorado
por Git.

## Rotación de credenciales y sesiones

La migración `V19` invalida los access tokens previos, revoca refresh tokens existentes y exige
cambio de contraseña al próximo acceso. Antes de desplegarla, el responsable del entorno debe:

1. Generar y cargar un `JWT_SECRET` nuevo en el gestor de secretos.
2. Rotar la contraseña de la base de datos en el proveedor y actualizar `DB_PASSWORD`.
3. Configurar solamente los dominios HTTPS del frontend en `CORS_ALLOWED_ORIGINS`.
4. Comunicar a los usuarios que deben establecer una contraseña nueva.

## Política de sesiones

Cada usuario puede conservar hasta cinco refresh tokens activos (`AUTH_MAX_ACTIVE_REFRESH_TOKENS`).
Al iniciar una sesión que supera el límite se revoca la más antigua. La rotación bloquea la fila
del token presentado: la primera solicitud concurrente recibe una sesión nueva y cualquier
reintento con el token anterior recibe `401`. Los tokens vencidos se eliminan cada hora.
