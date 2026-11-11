# Intérprete LSB: Quechua & Español (Native Android Kotlin)

Aplicación móvil nativa en **Kotlin + Jetpack Compose + Material 3** para el guante háptico inteligente y visión artificial de la **Lengua de Señas Boliviana (LSB)** con traducción simultánea a **Español (Sintaxis SVO)** y **Quechua Boliviano (Sintaxis SOV con sufijos morfológicos)** y síntesis de voz en tiempo real.

---

## 🏛️ Características Principales

1. **Diseño Formal, Elegante y Académico (Colores Pasteles)**:
   - Paleta sobria basada en Azul Pizarra Pastel (`#385A75`), Verde Salvia Andino (`#4A7C59`), Terracota Suave (`#A06C55`) y superficies limpias (`#F7F9FB`).
   - Sin estridencias ni efectos neón; uso exclusivo de iconografía vectorial oficial Material Icons (sin emojis).

2. **Pantallas y Flujos de Usuario**:
   - **Pantalla de Carga y Diagnóstico (Splash Screen)**: Validación de subsistemas BLE, motor morfológico y Text-To-Speech con barra de progreso animada.
   - **Pantalla de Inicio (Dashboard)**: Tarjetas modulares de navegación formal, estado del enlace en tiempo real y conmutador rápido de hardware.
   - **Pantalla de Traducción & Telemetría BLE**:
     - Visualización en vivo de barras de progreso diferenciadas para los 5 dedos: Pulgar, Índice, Medio, Anular y Meñique (0% a 100%).
     - Inclinación IMU (Pitch y Roll en grados).
     - Indicadores de los 3 pines táctiles capacitivos (T0, T1, T2).
     - **Estabilizador Debouncer**: 12 muestras consecutivas a 25 Hz (~480 ms) con barra de progreso visual de estabilización.
     - Botones tipo chip para pruebas rápidas de glosas: `YO`, `TU`, `AGUA`, `PAN`, `MEDICO`, `CABEZA`, `QUERER`, `IR`, `DOLER`, `HOLA`, `GRACIAS`, `AYUDA`, `MANANA`.
     - Tarjeta bilingüe de alto contraste con desglose de la regla morfosintáctica aplicada.
     - Controles de selección de idioma de voz (Quechua vs Español), botón de limpieza y botón **HABLAR**.
   - **Visión por Cámara LSB**:
     - Integración con **CameraX** con selector de cámara frontal/trasera.
     - Cuadrícula de alineación anatómica manual para postura y señas.
     - Superposición inferior con traducción en vivo y botón de escucha rápida.
   - **Diccionario Morfológico**:
     - Catálogo de las 13 glosas LSB con sus clasificaciones gramaticales y función en la oración.
     - Guía detallada de sufijos quechuas: Acusativo (`-ta`), Alativo (`-man`), Verbalizador 1SG (`-ni`) y 2SG (`-nki`), Posesivo y Afectivo (`-y ... nanawan`).
   - **Protocolo de Pruebas (Fase 7)**:
     - Guía interactiva de 4 pasos de taller para comprobación de hardware antes de cerrar cables:
       - *Prueba 1*: Escáner I2C (PCF8591 en `0x48`, MPU-6050 en `0x68`).
       - *Prueba 2*: Calibración de 5 dedos (mano abierta vs puño cerrado).
       - *Prueba 3*: Toque capacitivo (T0, T1, T2).
       - *Prueba 4*: Traducción a voz completa.
     - Consola de eventos y tramas RAW JSON en tiempo real (`{"f":[...],"imu":[...],"t":[...],"g":"..."}`).

3. **Motor Morfológico Bilingüe (`MorphologyEngine.kt`)**:
   - **Español (SVO)**: Sujeto + Verbo conjugado + Objeto (e.g. *"Yo quiero agua"*, *"Tú quieres pan"*, *"Me duele la cabeza"*).
   - **Quechua Boliviano (SOV con declinaciones)**:
     - Objeto transitivo: sufijo acusativo **`-ta`** (*"Ñuqa yakuta munani"*).
     - Objeto de desplazamiento: sufijo alativo **`-man`** (*"Ñuqa hampiqman rini"*).
     - Afección corporal: posesivo **`-y`** + **`nanawan`** (*"Umay nanawan"*).
     - Conjugación de persona: 1SG **`-ni`**, 2SG **`-nki`**.
   - **Adaptador Fonético para TTS Quechua**:
     - Transforma las grafías quechuas a aproximaciones fonéticas precisas para motores de síntesis en español boliviano (`es-BO`):
       - `Ñuqa` → `Ñocja`
       - `Qam` → `Cjam`
       - `Q'aya` → `Cjaya`
       - `t'anta` → `tjanta`
       - `hampiq` → `jampicj`
       - `ll` → `ly`
       - `w` → `gu`

4. **Simulador Háptico de Hardware Integrado**:
   - Si no tienes el ESP32 encendido, pulsa **"Modo Simulación"** para generar telemetría realista a 25 Hz y probar el 100% de la funcionalidad de la app sin hardware físico conectado.

---

## 🚀 Instalación y Ejecución

### Opción A: Instalar el APK ya Compilado en tu Celular Android

El archivo APK debug ya se encuentra generado y listo para instalar en:
```
app/build/outputs/apk/debug/app-debug.apk
```

Para instalarlo por cable USB mediante ADB:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
*(O simplemente copia el archivo `app-debug.apk` a tu teléfono y ábrelo para instalar).*

### Opción B: Abrir y Modificar en Android Studio

1. Abre **Android Studio**.
2. Selecciona **File > Open** y abre la carpeta del proyecto:
   ```
   D:\septimo_semestre\robotica-vision-app
   ```
3. Android Studio sincronizará el proyecto con Gradle 9.1 automáticamente.
4. Presiona **Run (Shift + F10)** para ejecutar en tu celular físico o emulador.

### Opción C: Compilar y Ejecutar Tests desde la Terminal

```bash
# Ejecutar todas las pruebas unitarias
gradlew test

# Compilar el APK debug
gradlew assembleDebug
```
