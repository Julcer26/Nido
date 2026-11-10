# Política de Privacidad de Nido (Presupuesto Familiar IA)

**Última actualización:** 10 de noviembre de 2026  
**Fecha de entrada en vigor:** 10 de noviembre de 2026  
**Aplicación:** Nido - Finanzas y Presupuesto Familiar IA  
**Contacto de privacidad:** soporte.nidoapp@gmail.com  

---

## 1. Introducción y Alcance
En **Nido** ("nosotros", "nuestro" o "la Aplicación"), nos tomamos muy en serio la privacidad y seguridad de la información de tu hogar. Esta Política de Privacidad explica de forma transparente cómo recopilamos, utilizamos, almacenamos, protegemos y eliminamos tus datos personales y financieros cuando utilizas nuestra aplicación móvil Android y los servicios asociados.

Al descargar, instalar o utilizar Nido, aceptas las prácticas descritas en esta política. Si no estás de acuerdo con estos términos, por favor no utilices la Aplicación.

---

## 2. Información que Recopilamos

### A. Información que proporcionas voluntariamente
1. **Datos de Registro y Cuenta:**
   - Dirección de correo electrónico y contraseña (gestionados a través de autenticación cifrada con Google Firebase Auth).
   - Nombre de perfil o alias familiar (ej. "Papá", "Mamá", "Julio").
   - Identificador familiar (`familyId`) generado para compartir presupuestos dentro de tu grupo del hogar.

2. **Datos Financieros y Transaccionales:**
   - Registro de ingresos, gastos, transacciones manuales, montos, descripciones, fechas y categorías asociadas.
   - Divisas utilizadas (ej. USD, VES) y tasas de cambio de referencia aplicadas (ej. Banco Central de Venezuela - BCV).
   - Metas de ahorro, límites de presupuesto mensual por categoría y recordatorios de pagos recurrentes o servicios.

3. **Imágenes de Recibos y Facturas (Cámara y Galería):**
   - Fotografías de comprobantes de pago, tickets de compra o facturas que decides escanear o adjuntar voluntariamente para el control de gastos.

4. **Gestión Familiar y del Hogar:**
   - Listas de compras compartidas, notas de gastos y asignación de tareas del hogar.

### B. Información recopilada automáticamente
1. **Datos Técnicos y de Diagnóstico:**
   - Versión del sistema operativo Android, modelo de dispositivo, versión instalada de la aplicación y registros de errores o rendimiento técnico (vía Firebase Crashlytics / Analytics).
   - Dirección IP (utilizada exclusivamente con fines de seguridad de red, prevención de abusos y geolocalización regional para tasas de cambio).

---

## 3. Permisos del Dispositivo y su Finalidad

Nido solicita únicamente los permisos indispensables para su funcionamiento esencial:

| Permiso Android | Finalidad Específica |
| :--- | :--- |
| `android.permission.INTERNET` | Sincronización en tiempo real con Firebase (base de datos de tu familia) y consulta de tasas de cambio oficiales. |
| `android.permission.CAMERA` | Permitir tomar fotos de facturas o comprobantes para su extracción automática mediante IA. **Nunca accedemos a la cámara sin tu acción directa.** |
| `android.permission.USE_BIOMETRIC` | Permitir el desbloqueo rápido y seguro mediante huella dactilar o reconocimiento facial. **Los datos biométricos permanecen en el hardware seguro de tu dispositivo y jamás salen de él ni son transmitidos a nuestros servidores.** |
| `android.permission.POST_NOTIFICATIONS` | Enviar recordatorios opcionales sobre vencimientos de pagos recurrentes o sincronizaciones familiares (solo en Android 13 o superior). |

---

## 4. Uso de Inteligencia Artificial (Google Gemini)

Nido integra capacidades de Inteligencia Artificial mediante los modelos de **Google Gemini** para facilitar la gestión financiera:
- **Lectura Inteligente de Recibos (OCR + AI):** Al subir una foto de un recibo, la imagen o el texto extraído se procesa temporalmente mediante la API de Google Gemini para identificar ítems, cantidades, montos y categorías sugeridas.
- **Asistente y Análisis Financiero:** Analiza resúmenes numéricos de tus gastos mensuales para generar consejos personalizados de ahorro y alertas de desviaciones presupuestarias.
- **Compromiso de Privacidad IA:** Tus datos financieros no son vendidos ni utilizados para entrenar modelos públicos de terceros sin anonimización previa.

---

## 5. Almacenamiento y Seguridad de los Datos

1. **Cifrado en Tránsito y Reposo:** Toda comunicación entre tu teléfono y nuestros servidores se realiza mediante protocolos seguros `HTTPS / TLS 1.3`. Los datos alojados en **Google Cloud / Firebase Realtime Database** y **Firebase Storage** se almacenan con cifrado de nivel bancario (AES-256).
2. **Aislamiento por Familia:** La arquitectura de base de datos implementa Reglas de Seguridad (*Firebase Security Rules*) estrictas. Solo los miembros autenticados que poseen el código de familia correspondiente pueden leer o modificar los datos financieros de ese grupo.
3. **No Comercialización de Datos:** **Nido no vende, no alquila y no comparte tus datos personales o financieros con corredores de datos ni con redes publicitarias de terceros.**

---

## 6. Proveedores de Servicios Terceros

Para operar la plataforma, nos apoyamos en los siguientes proveedores tecnológicos líderes:
- **Google LLC (Firebase & Google Cloud):** Autenticación de usuarios, base de datos en la nube y almacenamiento seguro de imágenes.
- **Google AI (Gemini API):** Procesamiento de lenguaje natural y visión computacional para recibos.
- **Google Play Services:** Distribución de la aplicación y procesamiento de compras/suscripciones bajo Google Play Billing.

---

## 7. Retención y Eliminación de Datos

Conservamos tu información únicamente mientras mantengas una cuenta activa en Nido. En cualquier momento puedes solicitar la eliminación total de tus datos:
1. **Desde la Aplicación:** Accede a `Ajustes / Perfil` -> `Zona de Peligro` -> `Eliminar mi Cuenta`.
2. **Vía Web o Correo Electrónico:** Puedes solicitar el borrado permanente completando el formulario en `https://hogar-finanzas-2026.web.app/delete-account.html` o escribiendo directamente a `soporte.nidoapp@gmail.com`.
3. Al procesar la solicitud, se eliminarán de forma irrevocable tu usuario de autenticación, tu perfil, los comprobantes almacenados y las referencias de tus transacciones en un plazo no mayor a 30 días hábiles.

---

## 8. Privacidad de Menores

Nido no está dirigida a menores de 13 años (o la edad mínima legal en tu jurisdicción). No recopilamos intencionalmente datos personales de niños. Si detectamos que un menor nos ha proporcionado datos sin el consentimiento de sus tutores, los eliminaremos de inmediato.

---

## 9. Cambios a esta Política de Privacidad

Podemos actualizar esta política periódicamente para reflejar mejoras en la app o requisitos regulatorios. Te notificaremos sobre cambios significativos a través de un aviso en la aplicación o mediante correo electrónico.

---

## 10. Contacto y Derechos del Usuario (ARCO / RGPD)

Tienes derecho a acceder, rectificar, cancelar u oponerte al tratamiento de tus datos personales. Para ejercer estos derechos o realizar consultas sobre privacidad, contáctanos en:

* **Responsable:** Equipo de Desarrollo Nido  
* **Correo de Soporte:** `soporte.nidoapp@gmail.com`  
* **Sitio Web:** `https://hogar-finanzas-2026.web.app`  
