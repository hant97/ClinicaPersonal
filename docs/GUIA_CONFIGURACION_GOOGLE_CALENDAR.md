# Guía de Configuración: Integración con Google Calendar

Esta guía detalla los pasos para conectar **ClinicaPersonal** con **Google Calendar** para registrar automáticamente las citas cuando pasen a estado **CONFIRMADA**.

---

## 📌 Resumen del Funcionamiento

1. El sistema utiliza una **Cuenta de Servicio (Service Account)** oficial de Google Cloud para autenticarse sin necesidad de login manual.
2. Tu cuenta de Gmail (la de prueba en local/pre o la oficial en producción) **comparte su calendario** con el correo de la cuenta de servicio otorgándole permisos para "Hacer cambios en eventos".
3. Al confirmar una cita en la Agenda:
   - El sistema crea el evento con: nombre del paciente, servicio médico, fecha, hora, modalidad, enlace de videollamada o consultorio, y notas.
   - Si la cita se reprograma, el evento en Google Calendar se actualiza.
   - Si la cita se cancela, el evento se elimina del calendario.

---

## 🚀 Paso 1: Crear la Cuenta de Servicio en Google Cloud (Gratis)

1. Ingresa a la consola de Google: [https://console.cloud.google.com/](https://console.cloud.google.com/) con cualquier cuenta de Google.
2. En la parte superior, haz clic en el selector de proyectos y luego en **"Nuevo proyecto"** (puedes nombrarlo `Clinica-Personal`).
3. En el menú lateral izquierdo, ve a **APIs y servicios** > **Biblioteca**.
4. Busca **"Google Calendar API"**, entra en el resultado y haz clic en **"Habilitar"**.
5. Ve a **APIs y servicios** > **Credenciales**.
6. Haz clic en **"+ Crear credenciales"** > **"Cuenta de servicio" (Service account)**:
   - **Nombre**: `clinica-calendar-sync`
   - Haz clic en **"Crear y continuar"** y luego en **"Listo"** (no requiere roles adicionales en GCP).
7. En la lista de Cuentas de Servicio, haz clic sobre la cuenta recién creada.
8. Ve a la pestaña **"Claves" (Keys)** > **"Agregar clave"** > **"Crear clave nueva"** > Selecciona **JSON** y presiona **Crear**.
   - Se descargará un archivo `.json` en tu computadora (por ejemplo: `clinica-personal-123456.json`).
9. **Copia el correo de la cuenta de servicio** que aparece en pantalla (tiene formato: `clinica-calendar-sync@tu-proyecto.iam.gserviceaccount.com`). Lo usaremos en el Paso 2.

---

## 📅 Paso 2: Compartir tu Calendario de Gmail con la Cuenta de Servicio

1. Abre Google Calendar con tu correo de prueba (o el de la clínica): [https://calendar.google.com/](https://calendar.google.com/).
2. En el menú lateral izquierdo, ubica tu calendario bajo **"Mis calendarios"**, pasa el cursor y haz clic en los **tres puntos verticales (⋮)** > **"Configuración y uso compartido"**.
3. Baja hasta la sección **"Compartir con personas o grupos específicos"** y haz clic en **"+ Agregar personas y grupos"**.
4. Pega el correo de la cuenta de servicio que copiaste en el Paso 1 (`...@tu-proyecto.iam.gserviceaccount.com`).
5. En el menú desplegable de permisos, selecciona:
   👉 **"Hacer cambios en eventos"** (Make changes to events).
6. Haz clic en **Enviar** / **Guardar**.

---

## ⚙️ Paso 3: Configuración en tu Entorno Local

1. Mueve el archivo JSON descargado a la carpeta `backend/` de tu proyecto y renómbralo a:
   `google-credentials.json`
2. Abre tu archivo `backend/.env` (o créalo a partir de `backend/.env.example`) y configura:

```env
# Integración con Google Calendar
GOOGLE_CALENDAR_ENABLED=true
GOOGLE_CALENDAR_ID=tu-correo-de-prueba@gmail.com
GOOGLE_CALENDAR_CREDENTIALS_PATH=./backend/google-credentials.json
APP_TIMEZONE=America/Lima
GOOGLE_CALENDAR_SEND_NOTIFICATIONS=false
```

> **Nota sobre `GOOGLE_CALENDAR_ID`**: Si deseas que se registre en el calendario principal de tu cuenta de Gmail, coloca tu correo (ej. `micuenta@gmail.com`) o la palabra `primary`.

---

## 🌐 Paso 4: Pasar a Producción (Sin tocar código)

Cuando despliegues en producción (ej. Render, Railway, Docker, AWS):
1. Comparte el calendario oficial de la clínica en Google Calendar con el mismo correo de la cuenta de servicio (con permisos de "Hacer cambios en eventos").
2. En las Variables de Entorno del servidor de producción, solo necesitas configurar:
   - `GOOGLE_CALENDAR_ENABLED=true`
   - `GOOGLE_CALENDAR_ID=correo-clinica-oficial@gmail.com`
   - `GOOGLE_CALENDAR_CREDENTIALS_JSON={"type":"service_account",...}` *(el contenido completo del archivo JSON copiado y pegado en la variable)*
   - `APP_TIMEZONE=America/Lima`
   - `GOOGLE_CALENDAR_SEND_NOTIFICATIONS=true`
