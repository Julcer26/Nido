# 📊 Estrategia de Rentabilidad: Base de Datos, Modelo de Suscripciones y Publicación de Nido

> **Objetivo fundamental:** Convertir a **Nido** en un negocio rentable y autosostenible que genere ingresos mensuales recurrentes mediante suscripciones digitales y que mantenga los costes operativos (base de datos, almacenamiento e IA) al mínimo absoluto.

---

## 1. 🗄️ Análisis Exhaustivo de Base de Datos: ¿Firebase o Supabase?

Una de las preguntas más críticas para no "desangrarse" en costes fijos es elegir la infraestructura adecuada. A continuación tienes la comparativa real y técnica basada en las necesidades de Nido:

### Tabla Comparativa de Costos Reales

| Concepto | Firebase (Google Cloud) | Supabase (PostgreSQL) | Veredicto para Nido |
| :--- | :--- | :--- | :--- |
| **Plan Gratuito (Free Tier)** | • **Auth:** 50,000 usuarios activos/mes GRATIS.<br>• **Realtime DB:** 1 GB almacenamiento + 10 GB descarga/mes GRATIS.<br>• **Storage (Fotos):** **5 GB GRATIS** (hasta 40,000 fotos comprimidas).<br>• **Servicio continuo:** Nunca se apaga ni se pausa. | • **Auth:** 50,000 usuarios activos/mes GRATIS.<br>• **Postgres DB:** 500 MB.<br>• **Storage (Fotos):** **Solo 1 GB GRATIS** (~5,000 fotos).<br>• ⚠️ **Riesgo:** Si el proyecto pasa 7 días sin actividad en Free, **se pausa automáticamente**. | **Ganador Free: Firebase.**<br>Ofrece 5 veces más almacenamiento de fotos gratuito y no se pausa. |
| **Salto al Plan de Pago** | **Plan Blaze (Pago por uso real, centavo a centavo):**<br>• Si gastas $0.15 USD en el mes, **pagas solo $0.15 USD**.<br>• Storage extra: $0.026 por GB al mes.<br>• Descarga extra: $0.12 por GB. | **Plan Pro:**<br>• **$25.00 USD/mes FIJOS obligatorios** en cuanto superas 500 MB de base de datos o 1 GB de storage.<br>• Aunque solo tengas 1 usuario de más, pagas $300 USD al año. | **Ganador en costes: Firebase.**<br>Supabase te obliga a pagar $25/mes fijos, mientras que Firebase te permite escalar desde $0.01 sin cargos fijos. |
| **Sincronización Móvil en Tiempo Real** | Nativo para Android (SDK de Kotlin oficial con persistencia offline `setPersistenceEnabled(true)`). Diseñado para móviles. | Requiere WebSockets permanentes (Realtime channels), lo cual consume más batería y datos en redes móviles. | **Ganador técnico: Firebase.** |
| **Costo de Migración** | **$0 USD y 0 horas:** Ya está 100% programado e integrado en `FamilyRepository.kt`. | **40 a 60 horas de refactorización:** Reescribir esquemas SQL, políticas RLS, cambiar más de 550 líneas de código y volver a probar toda la app. | **Mantener Firebase.** |

### 💡 Conclusión sobre la Base de Datos
**Mantenerse en Firebase es la decisión más inteligente y económica.**
1. Nido guarda información contable en texto JSON ligero. **1 GB de Realtime Database almacena más de 2.5 millones de transacciones familiares.**
2. En Firebase Storage tienes **5 GB gratuitos**. Comprimiendo las fotos de los recibos a ~120 KB antes de subirlas (como ya hace la app), tienes espacio para más de **40,000 comprobantes** sin pagar un solo dólar.
3. Si el día de mañana superas el free tier, en Firebase pagarás centavos ($1 o $2 dólares al mes), mientras que en Supabase tendrías una factura fija de $25 USD/mes ($300 al año).

---

## 2. 🧠 Costos de Inteligencia Artificial (Google Gemini) y Regla Antigasto

Para que la app no genere pérdidas por consumo de IA, aplicamos la regla de costes unitarios:

* **Modelo utilizado:** `gemini-2.5-flash` / `gemini-flash-lite`.
* **Costo oficial de Google Gemini:**
  - Entrada: $0.075 por cada 1,000,000 de tokens.
  - Salida: $0.30 por cada 1,000,000 de tokens.
* **Consumo por cada factura escaneada:**
  - 1 fotografía optimizada (1024 px) + prompt de extracción + JSON de salida = ~1,200 tokens.
  - **Costo directo por factura = $0.00012 USD** (es decir, **menos de dos décimas de centavo de dólar por factura**; escanear 1,000 facturas cuesta apenas $0.12 USD).

### 🛡️ Regla de Oro para Rentabilidad Máxima
1. **Usuarios Free:** Limitar el escaneo a **3 recibos mensuales de prueba**. Esto genera un coste máximo de **$0.00036 USD por usuario al mes** (completamente despreciable).
2. **Usuarios Pro ($2.99 USD/mes):** Tienen escaneos ilimitados. Incluso si un usuario muy activo escanea 60 facturas en un mes, su coste directo en IA es de **$0.007 USD**.
   - Ingreso por usuario: $2.99
   - Comisión de Google Play (15%): -$0.45
   - Coste de IA promedio: -$0.01
   - **BENEFICIO NETO LIMPIO: ~$2.53 USD por usuario cada mes (>85% de margen).**

---

## 3. 💳 Plan de Monetización y Facturación Real

Para que la aplicación genere facturación real, proponemos un esquema de 3 niveles con pasarela de pagos oficial:

### A. Estructura de Precios Comercial

| Plan | Precio al Usuario | Propósito y Psicología de Precios |
| :--- | :---: | :--- |
| **Nido Gratuito** | **$0.00** | Adquisición masiva de usuarios. Permite registro manual ilimitado, control de gastos, tareas y **3 escaneos IA de prueba**. |
| **Nido Pro Mensual** | **$2.99 USD / mes** | Opción accesible de baja resistencia para usuarios que quieren probar un mes. |
| **Nido Pro Anual (DESTACADO)** | **$19.99 USD / año** *(Equivale a $1.66/mes)* | **La opción más rentable para ti.** Descuento del 44% que incentiva el pago anual, generándote **flujo de caja inmediato** desde el primer día. |
| **Nido Familiar Vitalicio (Lifetime)** | **$39.99 USD (Pago único)** | Ideal para campañas de lanzamiento y conseguir capital rápido de los primeros 100 usuarios. |

### B. Funciones Bloqueadas (Gating / Paywall)
¿Qué hace que el usuario decida pagar?
1. **Lector IA Ilimitado:** Al intentar realizar el 4to escaneo del mes, la app muestra una pantalla de suscripción (*Paywall*) con los planes Pro.
2. **Asesor Financiero Predictivo IA:** Diagnósticos inteligentes de ahorro a fin de mes, gráficos comparativos y alertas de gastos hormiga.
3. **Exportación a Excel (.csv) y PDF:** Para quienes necesitan presentar cuentas o declaraciones de impuestos del hogar.
4. **Miembros Familiares Ilimitados:** El plan Free incluye hasta 2 miembros (pareja); el plan Pro permite toda la familia extendida.

### C. Herramienta de Cobros Recomendada: RevenueCat
En lugar de programar la API nativa de Google Play Billing desde cero (lo que suele llevar semanas para manejar renovaciones, cancelaciones, cambios de tarjeta y reembolsos):
- **RevenueCat** es la plataforma estándar líder en la industria.
- Es gratuita hasta que tu aplicación facture más de **$2,500 USD al mes**.
- Proporciona componentes visuales de Paywall listos para Jetpack Compose.
- Gestiona automáticamente las suscripciones en Google Play Store y actualiza el campo `isPro = true` en Firebase.

---

## 4. 🚀 Plan de Lanzamiento en Google Play Store (Checklist Definitivo)

Sigue estos pasos en orden para publicar la aplicación sin contratiempos:

### Fase 1: Despliegue de los Documentos Legales (Completado)
Ya creamos los documentos legales en `public/`:
- `public/privacy.html`
- `public/terms.html`
- `public/delete-account.html`

Para publicarlos en tu web de Firebase Hosting, ejecuta en tu terminal:
```bash
firebase deploy --only hosting
```
Tus enlaces quedarán automáticamente activos en:
* `https://hogar-finanzas-2026.web.app/privacy.html`
* `https://hogar-finanzas-2026.web.app/terms.html`
* `https://hogar-finanzas-2026.web.app/delete-account.html`

---

### Fase 2: Configuración en Google Play Console
1. **Crear cuenta de Desarrollador Google Play:**
   - Pago único de $25 USD en [play.google.com/console](https://play.google.com/console).
   - Verificar identidad con DNI o Pasaporte.
2. **Crear Aplicación:**
   - Nombre: `Nido: Finanzas Familiares IA`
   - Idioma: Español
   - Tipo: Aplicación > Gratuita (con compras integradas).
3. **Sección "Contenido de la aplicación":**
   - **Política de privacidad:** Pega `https://hogar-finanzas-2026.web.app/privacy.html`.
   - **Eliminación de datos:** Pega `https://hogar-finanzas-2026.web.app/delete-account.html`.
   - **Seguridad de los datos:** Rellena las preguntas siguiendo la guía exacta de [GUIA_DATA_SAFETY_GOOGLE_PLAY.md](file:///home/antigravity/Julcer26/Nido/docs/legal/GUIA_DATA_SAFETY_GOOGLE_PLAY.md).
   - **Acceso a la app:** Crear un usuario de prueba (ej: `tester@nidoapp.com` / clave: `Tester2026!`) para que los revisores de Google puedan entrar sin problemas.

---

### Fase 3: Compilación y Subida del Paquete (.aab)
Google Play ya no acepta archivos `.apk`. Debes generar el **Android App Bundle (.aab)**:
```bash
./gradlew bundleRelease
```
El archivo generado se ubicará en:
`app/build/outputs/bundle/release/app-release.aab`

---

### Fase 4: Requisito de Prueba Cerrada (Closed Testing - 20 Testers)
Si tu cuenta de Google Play Console fue creada después de noviembre de 2023, Google exige:
1. Crear un canal de **Prueba Cerrada (Closed Track)**.
2. Invitar a **20 evaluadores** (amigos, familiares o grupos de testers de Android) mediante sus correos de Google Play.
3. Mantenerlos registrados durante **14 días continuos**.
4. Cumplidos los 14 días, se activa el botón para **solicitar el lanzamiento a Producción**. La revisión de Google toma de 2 a 4 días hábiles y la app quedará visible para millones de personas en Google Play Store.
