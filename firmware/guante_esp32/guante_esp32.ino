#include <Wire.h>
#include <MPU6050_light.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

// Configuración de pines y direcciones
#define PCF8591_ADDR 0x48
#define PIN_MENIQUE  34
#define PIN_SDA      21
#define PIN_SCL      22

// =========================================================================
// UUIDs para BLE (Tu app móvil debe buscar estos mismos UUIDs)
// =========================================================================
#define SERVICE_UUID        "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define CHARACTERISTIC_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a8"

BLEServer* pServer = NULL;
BLECharacteristic* pCharacteristic = NULL;
bool deviceConnected = false;
bool oldDeviceConnected = false;

MPU6050 mpu(Wire);

// TABLA DE CALIBRACIÓN (Actualiza con tus valores de holgura)
const int PULGAR_ESTIRADO  = 131;  const int PULGAR_DOBLADO  = 215;
const int INDICE_ESTIRADO  = 131;  const int INDICE_DOBLADO  = 215;
const int MEDIO_ESTIRADO   = 131;  const int MEDIO_DOBLADO   = 215;
const int ANULAR_ESTIRADO  = 131;  const int ANULAR_DOBLADO  = 215;
const int MENIQUE_ESTIRADO = 1920; const int MENIQUE_DOBLADO = 3100;

unsigned long ultimoEnvio = 0;
const int INTERVALO_MS = 100; 

// Callbacks para detectar si la app se conecta o desconecta
class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
    };
    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
    }
};

int leerCanalPCF(byte canal) {
  Wire.beginTransmission(PCF8591_ADDR);
  Wire.write(canal);
  if (Wire.endTransmission() != 0) return -1;
  Wire.requestFrom(PCF8591_ADDR, (uint8_t)2);
  if (Wire.available() >= 2) {
    Wire.read();
    return Wire.read();
  }
  return -1;
}

int calcularPorcentaje(int lectura, int valEstirado, int valDoblado) {
  if (lectura < 0) return 0;
  int porcentaje = map(lectura, valEstirado, valDoblado, 0, 100);
  return constrain(porcentaje, 0, 100);
}

void setup() {
  Serial.begin(115200);
  Wire.begin(PIN_SDA, PIN_SCL);

  // 1. Configurar BLE
  BLEDevice::init("Guante_ESP32");
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  BLEService *pService = pServer->createService(SERVICE_UUID);
  pCharacteristic = pService->createCharacteristic(
                      CHARACTERISTIC_UUID,
                      BLECharacteristic::PROPERTY_READ   |
                      BLECharacteristic::PROPERTY_NOTIFY
                    );
  pCharacteristic->addDescriptor(new BLE2902());
  pService->start();

  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(false);
  pAdvertising->setMinPreferred(0x0);
  BLEDevice::startAdvertising();
  Serial.println("BLE iniciado. Esperando conexion de la App...");

  // 2. Iniciar y calibrar MPU-6050
  byte status = mpu.begin();
  if (status != 0) {
    Serial.print("Error MPU-6050: ");
    Serial.println(status);
  } else {
    Serial.println("Calibrando MPU-6050... Manten la mano quieta 2 segundos.");
    delay(1000);
    mpu.calcOffsets();
    Serial.println("¡Calibrado!");
  }
}

void loop() {
  mpu.update();

  if (millis() - ultimoEnvio >= INTERVALO_MS) {
    ultimoEnvio = millis();

    // Canales del PCF8591 corregidos según el orden físico de tus dedos
    int rawPulgar  = leerCanalPCF(2);
    int rawIndice  = leerCanalPCF(1);
    int rawMedio   = leerCanalPCF(0);
    int rawAnular  = leerCanalPCF(3);
    int rawMenique = analogRead(PIN_MENIQUE);

    int pPulgar  = calcularPorcentaje(rawPulgar, PULGAR_ESTIRADO, PULGAR_DOBLADO);
    int pIndice  = calcularPorcentaje(rawIndice, INDICE_ESTIRADO, INDICE_DOBLADO);
    int pMedio   = calcularPorcentaje(rawMedio, MEDIO_ESTIRADO, MEDIO_DOBLADO);
    int pAnular  = calcularPorcentaje(rawAnular, ANULAR_ESTIRADO, ANULAR_DOBLADO);
    int pMenique = calcularPorcentaje(rawMenique, MENIQUE_ESTIRADO, MENIQUE_DOBLADO);

    float pitch = mpu.getAngleX();
    float roll  = mpu.getAngleY();

    String tramaJSON = "{";
    tramaJSON += "\"pulgar\":"  + String(pPulgar)  + ",";
    tramaJSON += "\"indice\":"  + String(pIndice)  + ",";
    tramaJSON += "\"medio\":"   + String(pMedio)   + ",";
    tramaJSON += "\"anular\":"  + String(pAnular)  + ",";
    tramaJSON += "\"menique\":" + String(pMenique) + ",";
    tramaJSON += "\"pitch\":"   + String(pitch, 1) + ",";
    tramaJSON += "\"roll\":"    + String(roll, 1);
    tramaJSON += "}";

    Serial.println(tramaJSON);

    // Si la app está conectada, enviar por BLE
    if (deviceConnected) {
      pCharacteristic->setValue(tramaJSON.c_str());
      pCharacteristic->notify();
    }
  }

  // Manejar desconexión para volver a ser visible
  if (!deviceConnected && oldDeviceConnected) {
      delay(500); 
      pServer->startAdvertising(); 
      Serial.println("App desconectada. BLE visible nuevamente.");
      oldDeviceConnected = deviceConnected;
  }
  // Manejar nueva conexión
  if (deviceConnected && !oldDeviceConnected) {
      oldDeviceConnected = deviceConnected;
      Serial.println("¡App conectada!");
  }
}

