# Firmware ESP32 - Guante Háptico Traductor LSB

Este módulo contiene el firmware en C++/Arduino para la placa **ESP32**, responsable de captar las señales físicas de los sensores del guante y transmitirlas a la aplicación móvil Android a través de **Bluetooth Low Energy (BLE)**.

> ⚠️ **Importante**: Sin este código compilado y cargado en el ESP32, el guante físico no puede sensar los movimientos de la mano ni enviar las señales/tramas necesarias para que la app móvil realice la traducción.

---

## 📋 Características del Firmware

- **Lectura de 5 Dedos (0 a 100%)**:
  - Dedos Pulgar, Índice, Medio y Anular a través del conversor ADC I2C **PCF8591**.
  - Dedo Meñique a través del pin analógico directo del ESP32 (**GPIO 34**).
  - Mapeo y calibración porcentual individual con holguras `PULGAR_*`, `INDICE_*`, etc.
- **Unidad de Medición Inercial (IMU MPU-6050)**:
  - Comunicación I2C compartida con pines SDA (GPIO 21) y SCL (GPIO 22).
  - Cálculo de inclinación en grados: **Pitch** (eje X) y **Roll** (eje Y).
  - Calibración automática de offsets en el arranque (`mpu.calcOffsets()`).
- **Servidor BLE (Bluetooth Low Energy)**:
  - Nombre del dispositivo BLE: `Guante_ESP32`
  - Reanunciación automática en caso de desconexión.
  - Frecuencia de envío: cada **100 ms** (10 Hz).

---

## ⚙️ Conexión de Pines y Hardware

| Componente | Señal / Canal | Pin ESP32 / Módulo | Descripción |
| :--- | :--- | :--- | :--- |
| **I2C Bus** | SDA | `GPIO 21` | Línea de datos I2C (PCF8591 y MPU-6050) |
| **I2C Bus** | SCL | `GPIO 22` | Línea de reloj I2C (PCF8591 y MPU-6050) |
| **Meñique** | Señal analógica | `GPIO 34` (ADC1) | Lectura ADC directa 0-4095 |
| **PCF8591** | I2C Addr `0x48` | `G21` / `G22` | Quitar jumpers del módulo para liberar entradas |
| ↳ Pulgar | AIN2 (Canal 2) | PCF8591 | Sensor flex / potenciómetro del Pulgar |
| ↳ Índice | AIN1 (Canal 1) | PCF8591 | Sensor flex / potenciómetro del Índice |
| ↳ Medio | AIN0 (Canal 0) | PCF8591 | Sensor flex / potenciómetro del Medio |
| ↳ Anular | AIN3 (Canal 3) | PCF8591 | Sensor flex / potenciómetro del Anular |
| **MPU-6050** | I2C Addr `0x68` | `G21` / `G22` | Acelerómetro + Giroscopio |

---

## 📡 Protocolo y UUIDs BLE

La app Android busca exactamente estos identificadores:

- **Nombre BLE**: `Guante_ESP32`
- **Service UUID**: `4fafc201-1fb5-459e-8fcc-c5c9c331914b`
- **Characteristic UUID**: `beb5483e-36e1-4688-b7f5-ea07361b26a8` (READ + NOTIFY)

### Formato de la Trama JSON emitida

```json
{"pulgar":15,"indice":80,"medio":85,"anular":82,"menique":20,"pitch":12.4,"roll":-3.1}
```

---

## 🛠️ Instrucciones de Carga (Arduino IDE)

1. Instala el paquete de placas **esp32** en Arduino IDE:
   - *Ajustes* > *Gestor de URLs adicionales de tarjetas*: `https://raw.githubusercontent.com/espressif/arduino-esp32/gh-pages/package_esp32_index.json`
   - *Herramientas* > *Placa* > *Gestor de tarjetas* > Busca e instala **esp32**.
2. Instala la librería:
   - **`MPU6050_light`** por *rfetick* (desde el Gestor de Librerías).
3. Abre el archivo:
   - `firmware/guante_esp32/guante_esp32.ino`
4. Selecciona tu placa (ej. *ESP32 Dev Module*) y el puerto COM asignado.
5. Presiona **Subir (Upload)**.
6. Abre el Monitor Serie a **115200 baudios** manteniendo el guante inmóvil durante los primeros 2 segundos para calibrar el giroscopio.

