# Parlante Cobros (Estilo Apple) 📱🔊

Aplicativo móvil para Android que convierte tu celular en un **parlante inteligente de cobros** (similar al parlante QR de Izipay / Yape POS). 

La app escucha en segundo plano las notificaciones entrantes de **Yape, Plin, BCP, Interbank, BBVA, Scotiabank y Banco de la Nación**, extrae el monto y el nombre de quien paga, y los anuncia en voz alta por el altavoz con un tono de campanilla previo.

---

## 🎨 Diseño Visual (Filosofía Apple)
Diseñado siguiendo los principios de la estética de Apple:
* **Minimalismo profesional:** Mucho espacio en blanco, composición limpia y equilibrada.
* **Jerarquía visual muy clara:** Métricas en grande (`S/ 0.00`), subtítulos en gris neutro (`#86868B`), contrastes nítidos.
* **Tarjetas limpias:** Bordes suaves (`#E5E5EA`), esquinas redondeadas (`20.dp`), sombras sutiles.
* **Paleta de colores funcional:** Fondo blanco cálido / gris muy claro (`#F5F5F7`), texto pizarra oscuro (`#1D1D1F`), acento azul Apple (`#0071E3`), verde de estado (`#34C759`) y distintivos suaves para cada banco (Yape morado, Plin celeste, BCP azul).

---

## 🚀 Estructura del Proyecto

```text
ParlanteCobrosApp/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml          # Registro del servicio de escucha y permisos
│   │   ├── java/com/parlantecobros/app/
│   │   │   ├── MainActivity.kt          # Pantalla principal en Jetpack Compose
│   │   │   ├── model/
│   │   │   │   └── PaymentNotification.kt # Modelos de datos y configuración
│   │   │   ├── service/
│   │   │   │   ├── CobrosNotificationListener.kt # NotificationListenerService en segundo plano
│   │   │   │   ├── PaymentParser.kt     # Motor Regex inteligente (Yape, Plin, bancos)
│   │   │   │   └── SpeechManager.kt     # Síntesis de voz Text-To-Speech y tono chime
│   │   │   ├── data/
│   │   │   │   └── PaymentRepository.kt # Estado en memoria, historial y deduplicación
│   │   │   └── ui/
│   │   │       ├── theme/Theme.kt       # Paleta y tokens de diseño Apple
│   │   │       └── screens/HomeScreen.kt# Interfaz de usuario minimalista y simulador
│   │   └── res/                         # Strings, colores y estilos
│   └── build.gradle.kts                 # Configuración de compilación Android
├── web_simulator/
│   └── index.html                       # Simulador interactivo en navegador con voz real
├── build.gradle.kts                     # Gradle raíz
├── settings.gradle.kts
└── README.md
```

---

## 💻 Cómo Abrir el Proyecto

### Opción 1: En Android Studio (Recomendado para generar el APK)
1. Abre **Android Studio**.
2. Selecciona **File > Open** (Abrir proyecto).
3. Selecciona la carpeta:
   `C:\Users\User\.gemini\antigravity\scratch\ParlanteCobrosApp`
4. Android Studio sincronizará las dependencias automáticamente con Gradle.

### Opción 2: Probar el Simulador Interactivo al Instante (Sin compilar nada)
Puedes abrir el archivo en cualquier navegador (Chrome, Edge):
`C:\Users\User\.gemini\antigravity\scratch\ParlanteCobrosApp\web_simulator\index.html`
* Este simulador tiene la **misma interfaz Apple** y utiliza la API de voz de tu computadora o celular para hablar en tiempo real al hacer clic en los botones de simulación.

---

## 📦 Cómo Generar el archivo `.apk` (Sin pagar Google Play)

1. En **Android Studio**, ve al menú superior:
   **Build > Build Bundle(s) / APK(s) > Build APK(s)**
2. En unos segundos verás una notificación en la esquina inferior derecha:
   `APK(s) generated successfully for 1 module. [locate]`
3. Haz clic en **locate**. Tu archivo instalable estará listo:
   `app-debug.apk` (o `app-release.apk`).
4. **Para instalarlo en tu celular:**
   * Sube el archivo `.apk` a tu **Google Drive**.
   * Abre Google Drive desde tu celular Android y pulsa sobre el archivo para instalarlo.
   * Si el celular te pregunta, activa la opción **"Permitir la instalación de aplicaciones desconocidas"**.

---

## ⚙️ Pasos importantes al abrir la app en el celular

1. **Permitir acceso a notificaciones:**
   * La app te mostrará un aviso. Toca en **Activar** o ve a:
     *Ajustes del celular > Aplicaciones > Acceso especial a aplicaciones > Acceso a notificaciones* y activa **Parlante Cobros**.
2. **Desactivar optimización de batería (Para que no se apague):**
   * En tu celular ve a: *Ajustes > Aplicaciones > Parlante Cobros > Batería > Sin restricciones*.
   * Esto garantiza que el celular siga anunciando los cobros aunque la pantalla esté bloqueada.
