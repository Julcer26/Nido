# Guía Oficial para Completar el Formulario "Seguridad de los Datos" (Data Safety) en Google Play Console

Al enviar **Nido** a revisión en Google Play Console, debes rellenar obligatoriamente la sección **Seguridad de los datos** (Data Safety). Si las respuestas no concuerdan con los permisos declarados en el código, Google rechazará la aplicación. 

A continuación tienes la respuesta exacta para cada pregunta del formulario:

---

## 1. Visión General (Overview)

1. **¿Tu aplicación recopila o comparte alguno de los tipos de datos de usuario obligatorios?**
   - **Respuesta:** **SÍ**.
2. **¿Se cifran en tránsito todos los datos de usuario que recopila tu aplicación?**
   - **Respuesta:** **SÍ** (Todos los datos viajan mediante HTTPS/TLS 1.3 hacia Firebase y Google APIs).
3. **¿Proporcionas una forma para que los usuarios soliciten que se eliminen sus datos?**
   - **Respuesta:** **SÍ** (Desde la app y mediante URL web).
4. **Agrega el enlace web para la solicitud de eliminación:**
   - **URL a pegar:** `https://hogar-finanzas-2026.web.app/delete-account.html`

---

## 2. Tipos de Datos Específicos que Recopila Nido

Marca únicamente las siguientes categorías en el cuestionario:

### A. Información Personal (Personal Info)
* **Dirección de correo electrónico (Email address):**
  - ¿Se recopila? **SÍ**.
  - ¿Se comparte con terceros? **NO**.
  - ¿El procesamiento es efímero? **NO** (se almacena para autenticación).
  - ¿Es obligatoria para usar la app? **SÍ**.
  - **Finalidades a marcar:**
    - `Funcionalidad de la aplicación (App functionality)`
    - `Gestión de cuentas (Account management)`

* **Nombre o Alias de Usuario (Name):**
  - ¿Se recopila? **SÍ** (Nombre para el perfil familiar).
  - ¿Se comparte? **NO**.
  - ¿Es efímero? **NO**.
  - ¿Es obligatorio? **SÍ**.
  - **Finalidades a marcar:**
    - `Funcionalidad de la aplicación (App functionality)`
    - `Personalización (Personalization)`

---

### B. Información Financiera (Financial Info)
* **Historial de compras o transacciones (User payment info / Purchase history):**
  - *Nota:* Nido no guarda números de tarjetas de crédito (los pagos de suscripción los maneja Google Play directamente). Sin embargo, se guardan transacciones de gastos del hogar ingresadas por el usuario.
  - Marca **Otra información financiera (Other financial info)**:
    - ¿Se recopila? **SÍ**.
    - ¿Se comparte con terceros? **NO**.
    - ¿Es efímero? **NO** (se guarda en Firebase para el balance familiar).
    - ¿Es obligatoria? **SÍ** (es la función principal de la app).
    - **Finalidades a marcar:**
      - `Funcionalidad de la aplicación (App functionality)`

---

### C. Fotos y Videos (Photos and Videos)
* **Fotos (Photos):**
  - ¿Se recopila? **SÍ** (Fotos de facturas y tickets de compra tomadas con la cámara o cargadas de la galería).
  - ¿Se comparte con terceros? **NO**.
  - ¿Es efímero? **NO** (se guarda en Firebase Storage para que la familia pueda consultar el comprobante).
  - ¿Es obligatoria? **NO** (Opcional, el usuario decide si toma la foto o registra el gasto manualmente).
  - **Finalidades a marcar:**
    - `Funcionalidad de la aplicación (App functionality)`

---

### D. Dispositivo u otros identificadores (Device or other IDs)
* **IDs del dispositivo o de la app:**
  - ¿Se recopila? **SÍ** (Identificador de Firebase Installation y token de notificaciones FCM).
  - ¿Se comparte? **NO**.
  - ¿Es efímero? **NO**.
  - ¿Es obligatorio? **SÍ**.
  - **Finalidades a marcar:**
    - `Funcionalidad de la aplicación (App functionality)`
    - `Estadísticas / Analítica (Analytics)`
    - `Prevención de fraudes y seguridad (Fraud prevention, security, and compliance)`

---

## 3. Datos que NO debes marcar (Marcar NO)
- **Ubicación (Location):** NO (no se solicita `ACCESS_FINE_LOCATION`).
- **Mensajes o SMS:** NO.
- **Contactos de la agenda:** NO.
- **Archivos de audio:** NO.
- **Salud y acondicionamiento físico:** NO.
- **Datos biométricos:** NO (el sensor de huella dactilar se valida a través de la API `BiometricPrompt` del sistema Android; la app nunca tiene acceso ni almacena los datos de la huella, por lo que según las pautas de Google **no cuenta como datos recopilados por la app**).

---

## 4. URL de la Política de Privacidad para pegar en Google Play Console
Pega exactamente este enlace en el apartado **Contenido de la aplicación > Política de privacidad**:

👉 `https://hogar-finanzas-2026.web.app/privacy.html`
