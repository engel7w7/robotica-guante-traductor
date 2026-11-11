package bo.edu.lsb.quechua.model

import org.json.JSONObject

/**
 * Representa la telemetría en tiempo real recibida desde el microcontrolador ESP32
 * del Guante Háptico LSB a través de Bluetooth (SPP Serial o BLE GATT).
 *
 * @param flex Porcentaje anatómico de flexión calibrado (0% = extendido/reposo, 100% = flexionado/puño).
 * @param rawFlex Valores brutos directos del convertidor ADC (PCF8591 0..255 y ESP32 G34 0..4095).
 * @param imuPitch Inclinación frontal (-90° a +90°).
 * @param imuRoll Rotación lateral (-180° a +180°).
 * @param imuYaw Giro azimutal.
 * @param touch Estado de los electrodos táctiles capacitivos (T0, T1, T2).
 * @param gloss Glosa pre-reconocida si el firmware la incluye.
 */
data class GloveTelemetry(
    val flex: List<Int> = listOf(0, 0, 0, 0, 0),
    val rawFlex: List<Int> = listOf(128, 128, 128, 128, 1950),
    val imuPitch: Double = 0.0,
    val imuRoll: Double = 0.0,
    val imuYaw: Double = 0.0,
    val touch: List<Int> = listOf(0, 0, 0),
    val gloss: String = "NINGUNA",
    val rawJson: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    val thumbFlex: Int get() = flex.getOrElse(0) { 0 }
    val indexFlex: Int get() = flex.getOrElse(1) { 0 }
    val middleFlex: Int get() = flex.getOrElse(2) { 0 }
    val ringFlex: Int get() = flex.getOrElse(3) { 0 }
    val pinkyFlex: Int get() = flex.getOrElse(4) { 0 }

    val thumbRaw: Int get() = rawFlex.getOrElse(0) { 128 }
    val indexRaw: Int get() = rawFlex.getOrElse(1) { 128 }
    val middleRaw: Int get() = rawFlex.getOrElse(2) { 128 }
    val ringRaw: Int get() = rawFlex.getOrElse(3) { 128 }
    val pinkyRaw: Int get() = rawFlex.getOrElse(4) { 1950 }

    val isTouch0Active: Boolean get() = touch.getOrElse(0) { 0 } == 1
    val isTouch1Active: Boolean get() = touch.getOrElse(1) { 0 } == 1
    val isTouch2Active: Boolean get() = touch.getOrElse(2) { 0 } == 1

    /**
     * Aplica el perfil de calibración "Paso Cero" para normalizar los valores analógicos RAW a 0..100%
     * y corregir la deriva del giróscopo MPU-6050.
     */
    fun withCalibration(profile: GloveCalibrationProfile): GloveTelemetry {
        val calibratedFlex = profile.normalizeFlex(rawFlex)
        val calibratedPitch = profile.normalizePitch(imuPitch)
        val calibratedRoll = profile.normalizeRoll(imuRoll)
        return copy(
            flex = calibratedFlex,
            imuPitch = calibratedPitch,
            imuRoll = calibratedRoll
        )
    }

    companion object {
        fun default(): GloveTelemetry = GloveTelemetry()

        /**
         * Parsea tramas entrantes desde el ESP32 en cualquier formato admitido:
         * 1. JSON con nombres: {"pulgar":128,"indice":210,"medio":220,"anular":215,"menique":2800,"pitch":12.5,"roll":-3.2}
         * 2. JSON abreviado: {"p":128,"i":210,"m":220,"a":215,"me":2800}
         * 3. JSON compacto: {"f":[128,210,220,215,2800],"imu":[12.5,-3.2],"t":[0,0,0]}
         * 4. CSV Serial plano: 128,210,220,215,2800 o 128,210,220,215,2800,12.5,-3.2
         * 5. Clave-Valor Serial: AIN3:128,AIN2:210,AIN1:220,AIN0:215,G34:2800
         */
        fun parseFromJson(
            incomingStr: String,
            calibration: GloveCalibrationProfile? = null
        ): GloveTelemetry? {
            val trimmed = incomingStr.trim()
            if (trimmed.isEmpty()) return null

            // Intentar parser JSON estructurado
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                try {
                    val json = JSONObject(trimmed)
                    val rawList = mutableListOf<Int>()

                    // 1. Detección por nombres explícitos de dedos (Español, Inglés o abreviaturas)
                    if (json.has("pulgar") || json.has("indice") || json.has("medio") ||
                        json.has("anular") || json.has("menique") || json.has("meñique") ||
                        json.has("thumb") || json.has("index") || json.has("middle") ||
                        json.has("ring") || json.has("pinky") ||
                        json.has("p") || json.has("i") || json.has("m") || json.has("a") || json.has("me")
                    ) {
                        val p = json.optInt("pulgar", json.optInt("thumb", json.optInt("p", 128)))
                        val i = json.optInt("indice", json.optInt("index", json.optInt("i", 128)))
                        val m = json.optInt("medio", json.optInt("middle", json.optInt("m", 128)))
                        val a = json.optInt("anular", json.optInt("ring", json.optInt("a", 128)))
                        val me = json.optInt("menique", json.optInt("meñique", json.optInt("pinky", json.optInt("me", 1950))))

                        rawList.add(p)
                        rawList.add(i)
                        rawList.add(m)
                        rawList.add(a)
                        rawList.add(me)
                    } else if (json.has("f") || json.has("flex")) {
                        val arr = if (json.has("f")) json.getJSONArray("f") else json.getJSONArray("flex")
                        for (idx in 0 until arr.length()) {
                            rawList.add(arr.optInt(idx, 128))
                        }
                    }

                    while (rawList.size < 4) rawList.add(128)
                    if (rawList.size < 5) rawList.add(1950)

                    // 2. Extraer orientación MPU-6050
                    var pitch = 0.0
                    var roll = 0.0
                    var yaw = 0.0

                    if (json.has("pitch") || json.has("roll")) {
                        pitch = json.optDouble("pitch", 0.0)
                        roll = json.optDouble("roll", 0.0)
                        yaw = json.optDouble("yaw", 0.0)
                    } else if (json.has("imu")) {
                        val arr = json.getJSONArray("imu")
                        pitch = arr.optDouble(0, 0.0)
                        roll = arr.optDouble(1, 0.0)
                        yaw = arr.optDouble(2, 0.0)
                    }

                    // 3. Extraer pines touch
                    val touchList = mutableListOf<Int>()
                    if (json.has("t") || json.has("touch")) {
                        val arr = if (json.has("t")) json.getJSONArray("t") else json.getJSONArray("touch")
                        for (idx in 0 until arr.length()) {
                            touchList.add(arr.optInt(idx, 0).coerceIn(0, 1))
                        }
                    }
                    while (touchList.size < 3) touchList.add(0)

                    val gloss = json.optString("g", json.optString("gloss", json.optString("glosa", "NINGUNA")))

                    val activeProfile = calibration ?: GloveCalibrationProfile.defaultProfile()
                    val calibratedFlex = activeProfile.normalizeFlex(rawList.take(5))

                    return GloveTelemetry(
                        flex = calibratedFlex,
                        rawFlex = rawList.take(5),
                        imuPitch = activeProfile.normalizePitch(pitch),
                        imuRoll = activeProfile.normalizeRoll(roll),
                        imuYaw = yaw,
                        touch = touchList.take(3),
                        gloss = gloss.ifBlank { "NINGUNA" },
                        rawJson = trimmed,
                        timestamp = System.currentTimeMillis()
                    )
                } catch (_: Exception) {
                    // Fallback a regex o CSV
                }
            }

            // Intentar parser CSV plano (ej: "128,135,130,126,1980" o "128,135,130,126,1980,12.5,-3.2")
            val csvParsed = parseFromCsv(trimmed, calibration)
            if (csvParsed != null) return csvParsed

            // Intentar parser Regex clave-valor
            return parseViaRegex(trimmed, calibration)
        }

        private fun parseFromCsv(
            csvStr: String,
            calibration: GloveCalibrationProfile?
        ): GloveTelemetry? {
            val parts = csvStr.split(',', ';').map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.size < 5) return null

            val p0 = parts.getOrNull(0)?.toIntOrNull() ?: return null
            val p1 = parts.getOrNull(1)?.toIntOrNull() ?: return null
            val p2 = parts.getOrNull(2)?.toIntOrNull() ?: return null
            val p3 = parts.getOrNull(3)?.toIntOrNull() ?: return null
            val p4 = parts.getOrNull(4)?.toIntOrNull() ?: return null

            val rawList = listOf(p0, p1, p2, p3, p4)
            val pitch = parts.getOrNull(5)?.toDoubleOrNull() ?: 0.0
            val roll = parts.getOrNull(6)?.toDoubleOrNull() ?: 0.0

            val activeProfile = calibration ?: GloveCalibrationProfile.defaultProfile()
            return GloveTelemetry(
                flex = activeProfile.normalizeFlex(rawList),
                rawFlex = rawList,
                imuPitch = activeProfile.normalizePitch(pitch),
                imuRoll = activeProfile.normalizeRoll(roll),
                imuYaw = 0.0,
                touch = listOf(0, 0, 0),
                gloss = "NINGUNA",
                rawJson = csvStr,
                timestamp = System.currentTimeMillis()
            )
        }

        private fun parseViaRegex(
            rawStr: String,
            calibration: GloveCalibrationProfile?
        ): GloveTelemetry? {
            return try {
                fun findInt(key: String, default: Int): Int {
                    val m = Regex("(?i)[\"']?$key[\"']?\\s*[:=]\\s*(-?\\d+)").find(rawStr)
                    return m?.groupValues?.get(1)?.toIntOrNull() ?: default
                }
                fun findDouble(key: String, default: Double): Double {
                    val m = Regex("(?i)[\"']?$key[\"']?\\s*[:=]\\s*(-?[0-9.]+)").find(rawStr)
                    return m?.groupValues?.get(1)?.toDoubleOrNull() ?: default
                }

                val p = findInt("pulgar", findInt("thumb", findInt("p", findInt("AIN2", findInt("CH2", 128)))))
                val i = findInt("indice", findInt("index", findInt("i", findInt("AIN1", findInt("CH1", 128)))))
                val m = findInt("medio", findInt("middle", findInt("m", findInt("AIN0", findInt("CH0", 128)))))
                val a = findInt("anular", findInt("ring", findInt("a", findInt("AIN3", findInt("CH3", 128)))))
                val me = findInt("menique", findInt("meñique", findInt("pinky", findInt("me", findInt("G34", 1950)))))

                val pitch = findDouble("pitch", 0.0)
                val roll = findDouble("roll", 0.0)
                val yaw = findDouble("yaw", 0.0)

                val rawList = listOf(p, i, m, a, me)
                val activeProfile = calibration ?: GloveCalibrationProfile.defaultProfile()

                GloveTelemetry(
                    flex = activeProfile.normalizeFlex(rawList),
                    rawFlex = rawList,
                    imuPitch = activeProfile.normalizePitch(pitch),
                    imuRoll = activeProfile.normalizeRoll(roll),
                    imuYaw = yaw,
                    touch = listOf(0, 0, 0),
                    gloss = "NINGUNA",
                    rawJson = rawStr,
                    timestamp = System.currentTimeMillis()
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
