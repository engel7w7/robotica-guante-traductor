package bo.edu.lsb.quechua.model

import android.content.Context
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Calibración individual para un sensor de flexión de dedo (Sensor Hall lineal SS49E o flex).
 *
 * @param restRaw Valor ADC analógico en reposo / mano abierta (ej: ~125-135 en PCF8591, ~1850-2050 en ESP32 G34).
 * @param flexRaw Valor ADC analógico con dedo flexionado / puño cerrado (ej: ~210-245 en PCF8591, ~2800-3200 en G34).
 */
data class FingerCalibration(
    val restRaw: Int = 128,
    val flexRaw: Int = 220
) {
    /**
     * Normaliza cualquier valor analógico RAW medido al porcentaje anatómico de flexión de 0% a 100%.
     * Soporta automáticamente:
     * - Polaridad positiva (flexRaw > restRaw: acercar polo sur sube el valor).
     * - Polaridad invertida (flexRaw < restRaw: acercar polo norte baja el valor).
     */
    fun normalize(raw: Int): Int {
        val span = abs(flexRaw - restRaw)
        if (span < 12) {
            // Si el rango es muy estrecho o no ha sido calibrado, aplicar normalización estándar según magnitud
            return when {
                raw > 1000 -> {
                    // Entrada ADC de 12 bits de ESP32 (0..4095) en G34
                    val diff = abs(raw - 1950)
                    ((diff / 900f) * 100f).roundToInt().coerceIn(0, 100)
                }
                raw > 100 -> {
                    // Entrada ADC de 8 bits del PCF8591 (0..255)
                    val diff = abs(raw - 128)
                    ((diff / 90f) * 100f).roundToInt().coerceIn(0, 100)
                }
                else -> raw.coerceIn(0, 100)
            }
        }

        val diff = if (flexRaw >= restRaw) (raw - restRaw) else (restRaw - raw)
        val percent = (diff.toFloat() / span.toFloat()) * 100f
        return percent.roundToInt().coerceIn(0, 100)
    }
}

/**
 * Perfil Integral de Calibración "Paso Cero" del Guante Háptico.
 * Almacena los puntos cero y spans de los 5 dedos y del giróscopo MPU-6050.
 */
data class GloveCalibrationProfile(
    val fingers: List<FingerCalibration> = listOf(
        FingerCalibration(128, 220),  // D0: Pulgar  (PCF8591 AIN2 / CH2)
        FingerCalibration(128, 220),  // D1: Índice  (PCF8591 AIN1 / CH1)
        FingerCalibration(128, 220),  // D2: Medio   (PCF8591 AIN0 / CH0)
        FingerCalibration(128, 220),  // D3: Anular  (PCF8591 AIN3 / CH3)
        FingerCalibration(1950, 2850) // D4: Meñique (ESP32 ADC G34, 12-bit)
    ),
    val zeroPitch: Double = 0.0,
    val zeroRoll: Double = 0.0,
    val isCalibrated: Boolean = false,
    val timestamp: Long = 0L
) {
    /**
     * Normaliza una lista de 5 valores brutos a porcentajes 0..100%.
     * - Si el ESP32 ya envía porcentajes (0..100), re-escala dedo por dedo cuando existe un perfil
     *   guardado en esa misma escala 0..100 (con al menos un dedo con rango >= 12); un dedo rígido
     *   con rango < 12 conserva su lectura sin descartar la calibración de los otros cuatro dedos.
     * - Esto evita también que perfiles antiguos en escala ADC (128..220) deformen lecturas 0..100 del firmware final.
     */
    fun normalizeFlex(rawFlex: List<Int>): List<Int> {
        val allAlreadyPercent = rawFlex.size >= 5 && rawFlex.all { it in 0..100 }
        if (allAlreadyPercent) {
            val calibratedOnPercent = isCalibrated &&
                fingers.size >= 5 &&
                fingers.all { f -> f.restRaw in 0..100 && f.flexRaw in 0..100 } &&
                fingers.any { f -> abs(f.flexRaw - f.restRaw) >= 12 }
            return if (calibratedOnPercent) {
                (0 until 5).map { index ->
                    val f = fingers[index]
                    if (abs(f.flexRaw - f.restRaw) >= 12) {
                        f.normalize(rawFlex[index])
                    } else {
                        rawFlex[index].coerceIn(0, 100)
                    }
                }
            } else {
                rawFlex.take(5).map { it.coerceIn(0, 100) }
            }
        }

        return (0 until 5).map { index ->
            val rawVal = rawFlex.getOrElse(index) { if (index == 4) 1950 else 128 }
            val cal = fingers.getOrElse(index) { FingerCalibration() }
            cal.normalize(rawVal)
        }
    }

    fun normalizePitch(rawPitch: Double): Double = rawPitch - zeroPitch
    fun normalizeRoll(rawRoll: Double): Double = rawRoll - zeroRoll

    companion object {
        private const val PREFS_NAME = "rimay_glove_calibration_prefs"

        fun defaultProfile(): GloveCalibrationProfile = GloveCalibrationProfile()

        fun load(context: Context): GloveCalibrationProfile {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isCal = prefs.getBoolean("is_calibrated", false)
            if (!isCal) return defaultProfile()

            val fingers = (0 until 5).map { i ->
                val defRest = if (i == 4) 1950 else 128
                val defFlex = if (i == 4) 2850 else 220
                val rest = prefs.getInt("rest_$i", defRest)
                val flex = prefs.getInt("flex_$i", defFlex)
                FingerCalibration(rest, flex)
            }
            val pitch = prefs.getFloat("zero_pitch", 0f).toDouble()
            val roll = prefs.getFloat("zero_roll", 0f).toDouble()
            val time = prefs.getLong("timestamp", 0L)

            return GloveCalibrationProfile(
                fingers = fingers,
                zeroPitch = pitch,
                zeroRoll = roll,
                isCalibrated = true,
                timestamp = time
            )
        }

        fun save(context: Context, profile: GloveCalibrationProfile) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().apply {
                putBoolean("is_calibrated", profile.isCalibrated)
                profile.fingers.forEachIndexed { i, f ->
                    putInt("rest_$i", f.restRaw)
                    putInt("flex_$i", f.flexRaw)
                }
                putFloat("zero_pitch", profile.zeroPitch.toFloat())
                putFloat("zero_roll", profile.zeroRoll.toFloat())
                putLong("timestamp", profile.timestamp)
                apply()
            }
        }

        fun clear(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
        }
    }
}

/**
 * Firma de postura personalizada grabada directamente con el guante del usuario.
 * Permite adaptar cualquier glosa a la colocación exacta de imanes/sensores y muñeca del portador.
 */
data class CustomGloveSignature(
    val gloss: String,
    val flex: List<Int>,
    val imuPitch: Double,
    val imuRoll: Double,
    val timestamp: Long = System.currentTimeMillis()
)

object CustomGloveProfileStore {
    private const val PREFS_NAME = "rimay_custom_glove_poses_prefs"

    fun loadAll(context: Context): Map<String, CustomGloveSignature> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val result = mutableMapOf<String, CustomGloveSignature>()
        for ((key, rawValue) in prefs.all) {
            val jsonStr = rawValue as? String ?: continue
            try {
                val obj = org.json.JSONObject(jsonStr)
                val arr = obj.getJSONArray("flex")
                val flex = (0 until 5).map { idx -> arr.optInt(idx, 0).coerceIn(0, 100) }
                val pitch = obj.optDouble("pitch", 0.0)
                val roll = obj.optDouble("roll", 0.0)
                val ts = obj.optLong("ts", 0L)
                val cleanGloss = key.trim().uppercase()
                if (cleanGloss.isNotBlank()) {
                    result[cleanGloss] = CustomGloveSignature(
                        gloss = cleanGloss,
                        flex = flex,
                        imuPitch = pitch,
                        imuRoll = roll,
                        timestamp = ts
                    )
                }
            } catch (_: Exception) {
            }
        }
        return result
    }

    fun save(context: Context, signature: CustomGloveSignature) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cleanGloss = signature.gloss.trim().uppercase()
        val obj = org.json.JSONObject().apply {
            put("flex", org.json.JSONArray(signature.flex.take(5)))
            put("pitch", signature.imuPitch)
            put("roll", signature.imuRoll)
            put("ts", signature.timestamp)
        }
        prefs.edit().putString(cleanGloss, obj.toString()).apply()
    }

    fun remove(context: Context, gloss: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(gloss.trim().uppercase()).apply()
    }

    fun clearAll(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
