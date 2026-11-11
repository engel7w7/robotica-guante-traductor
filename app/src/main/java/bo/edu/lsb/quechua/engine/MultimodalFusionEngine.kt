package bo.edu.lsb.quechua.engine

import bo.edu.lsb.quechua.model.CustomGloveSignature
import bo.edu.lsb.quechua.model.GloveTelemetry
import bo.edu.lsb.quechua.model.MorphologyItem
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Modo de Reconocimiento Sensorial Activo.
 */
enum class MultimodalMode(val label: String, val badge: String) {
    GLOVE_ONLY("Solo Guante Háptico", "BLE 10Hz · 100ms"),
    CAMERA_ONLY("Solo Cámara de Visión", "MediaPipe 3D"),
    HYBRID_FUSION("Fusión Híbrida Sensorial", "Guante + Cámara")
}

/**
 * Alcance del vocabulario evaluado por el reconocedor del guante.
 * En modo [ESSENTIAL] se evalúan 29 señas cotidianas perfectamente separadas para conversación rápida;
 * en modo [FULL] se evalúan las 146 glosas del diccionario completo.
 */
enum class GloveVocabScope(val label: String, val shortLabel: String) {
    ESSENTIAL("Vocabulario Esencial (29 señas)", "Esencial (29)"),
    FULL("Léxico Completo (146 señas)", "Completo (146)")
}

/**
 * Resultado del clasificador multimodal.
 */
data class RecognitionResult(
    val gloss: String,
    val gloveConfidence: Float,
    val cameraConfidence: Float,
    val fusionConfidence: Float,
    val sourceDescription: String
) {
    val confidence: Float
        get() = fusionConfidence
}

/**
 * Motor de Fusión Multimodal (Guante Háptico + Visión por Cámara).
 *
 * Implementa el clasificador de señas de alta fidelidad:
 * - Distancia anatómica combinada (RMS + pico de dedo) en los 5 sensores Hall SS49E + orientación IMU Pitch/Roll
 *   sin depender de sensores capacitivos touch (inexistentes en el hardware final del guante).
 * - Evaluación visual 3D en tiempo real de cámara usando los 5 porcentajes de flexión MediaPipe, inclinación,
 *   orientación y descriptores geométricos.
 * - Fusión ponderada para máxima inmunidad al ruido.
 */
object MultimodalFusionEngine {

    val ESSENTIAL_GLOSSES: Set<String> = setOf(
        "HOLA", "CHAU", "YO", "TU",
        "AGUA", "PAN", "COMIDA",
        "QUERER", "COMER", "BEBER",
        "IR", "VENIR",
        "CABEZA", "ESTOMAGO", "DOLER",
        "CASA", "HOSPITAL", "MEDICO",
        "SI", "NO", "GRACIAS", "POR_FAVOR", "BIEN", "AYUDA",
        "UNO", "DOS", "TRES", "CUATRO", "CINCO"
    )

    /**
     * Detecta agitación real de la mano (al menos una inversión de dirección con amplitud >= 20°),
     * distinguiendo el saludo dinámico (HOLA) de una elevación simple desde reposo hacia una seña estática (CINCO).
     */
    fun detectOscillatingWave(imuWindow: List<Pair<Double, Double>>): Boolean {
        if (imuWindow.size < 4) return false
        val pitches = imuWindow.map { it.first }
        val rolls = imuWindow.map { it.second }
        val pitchSpan = (pitches.maxOrNull() ?: 0.0) - (pitches.minOrNull() ?: 0.0)
        val rollSpan = (rolls.maxOrNull() ?: 0.0) - (rolls.minOrNull() ?: 0.0)

        fun hasReversal(values: List<Double>): Boolean {
            var prevSign = 0
            var reversals = 0
            for (i in 1 until values.size) {
                val delta = values[i] - values[i - 1]
                if (abs(delta) >= 5.0) {
                    val sign = if (delta > 0.0) 1 else -1
                    if (prevSign != 0 && sign != prevSign) {
                        reversals++
                    }
                    prevSign = sign
                }
            }
            return reversals >= 1
        }

        return (rollSpan >= 20.0 && hasReversal(rolls)) ||
            (pitchSpan >= 22.0 && hasReversal(pitches))
    }

    /**
     * Evalúa la telemetría del guante frente al léxico activo y firmas personalizadas del usuario.
     * Retorna el mejor par (glosa, confianza 0..1).
     */
    fun evaluateGlove(
        telemetry: GloveTelemetry,
        customSignatures: Map<String, CustomGloveSignature> = emptyMap(),
        allowedGlosses: Set<String>? = null,
        isWavingGlove: Boolean = false
    ): Pair<String, Float> {
        val normalizedGloss = telemetry.gloss.trim().uppercase()
        if (normalizedGloss.isNotBlank() && normalizedGloss != "NINGUNA" && MorphologyEngine.lexicon.containsKey(normalizedGloss)) {
            return Pair(normalizedGloss, 0.96f)
        }

        val flex = telemetry.flex
        val pitch = telemetry.imuPitch
        val roll = telemetry.imuRoll
        val touch = telemetry.touch
        val hasTouchHardware = touch.any { it != 0 }

        // Postura neutral de reposo: todos los dedos relajados y muñeca plana sin inclinación ni agitación intencional
        val isRestPosture = flex.all { it <= 20 } && !hasTouchHardware && !isWavingGlove &&
            abs(pitch) < 20.0 && abs(roll) < 20.0
        if (isRestPosture) {
            return Pair("NINGUNA", 0f)
        }

        // Gesto dinámico prioritario: agitar la mano abierta levantada -> HOLA
        val openFingerCount = flex.take(5).count { it <= 35 }
        val holaAllowed = allowedGlosses == null || allowedGlosses.contains("HOLA")
        if (isWavingGlove && openFingerCount >= 4 && pitch >= 15.0 && holaAllowed) {
            return Pair("HOLA", 0.94f)
        }

        var bestGloss = "NINGUNA"
        var bestScore = 0f

        for ((gloss, item) in MorphologyEngine.lexicon) {
            if (allowedGlosses != null && !allowedGlosses.contains(gloss) && !customSignatures.containsKey(gloss)) {
                continue
            }

            val baseSimilarity = computeSignatureSimilarity(
                flex = flex,
                pitch = pitch,
                roll = roll,
                touch = touch,
                hasTouchHardware = hasTouchHardware,
                expFlex = item.expectedFlex,
                expImu = item.expectedImu,
                expTouch = item.expectedTouch
            )

            val custom = customSignatures[gloss]
            val similarity = if (custom != null) {
                val customSimilarity = computeSignatureSimilarity(
                    flex = flex,
                    pitch = pitch,
                    roll = roll,
                    touch = touch,
                    hasTouchHardware = false,
                    expFlex = custom.flex,
                    expImu = listOf(custom.imuPitch, custom.imuRoll),
                    expTouch = emptyList()
                )
                val boostedCustom = (customSimilarity * 1.03f).coerceIn(0f, 0.99f)
                maxOf(baseSimilarity, boostedCustom)
            } else {
                baseSimilarity
            }

            if (similarity > bestScore) {
                bestScore = similarity
                bestGloss = gloss
            }
        }

        val acceptanceThreshold = if (allowedGlosses != null) 0.64f else 0.66f
        return if (bestScore >= acceptanceThreshold) Pair(bestGloss, bestScore) else Pair("NINGUNA", 0f)
    }

    private fun computeSignatureSimilarity(
        flex: List<Int>,
        pitch: Double,
        roll: Double,
        touch: List<Int>,
        hasTouchHardware: Boolean,
        expFlex: List<Int>,
        expImu: List<Double>,
        expTouch: List<Int>
    ): Float {
        var flexDistSq = 0.0
        var maxFingerDiff = 0.0
        for (i in 0 until 5) {
            val fActual = flex.getOrElse(i) { 50 }
            val fExpected = expFlex.getOrElse(i) { 50 }
            val absDiff = abs(fActual - fExpected) / 100.0
            if (absDiff > maxFingerDiff) maxFingerDiff = absDiff
            flexDistSq += absDiff * absDiff
        }
        val rmsFlexDist = sqrt(flexDistSq / 5.0)
        val flexDist = (rmsFlexDist * 0.72 + maxFingerDiff * 0.28).coerceIn(0.0, 1.0)

        val pDiff = (abs(pitch - expImu.getOrElse(0) { 0.0 }) / 90.0).coerceIn(0.0, 1.0)
        val rDiff = (abs(roll - expImu.getOrElse(1) { 0.0 }) / 110.0).coerceIn(0.0, 1.0)
        val imuDist = (pDiff * 0.55 + rDiff * 0.45).coerceIn(0.0, 1.0)

        return if (hasTouchHardware && expTouch.size >= 3) {
            var touchMatch = 0
            for (t in 0 until 3) {
                if (touch.getOrElse(t) { 0 } == expTouch.getOrElse(t) { 0 }) {
                    touchMatch++
                }
            }
            val touchScore = touchMatch / 3.0
            ((1.0 - flexDist) * 0.56 + (1.0 - imuDist) * 0.27 + touchScore * 0.17).toFloat()
        } else {
            ((1.0 - flexDist) * 0.72 + (1.0 - imuDist) * 0.28).toFloat()
        }.coerceIn(0f, 1f)
    }

    /**
     * Describe en español claro cómo colocar los dedos y la muñeca para hacer una seña con el guante.
     */
    fun describeGlovePosture(item: MorphologyItem): String {
        val names = listOf("Pulgar", "Índice", "Medio", "Anular", "Meñique")
        val extended = mutableListOf<String>()
        val curved = mutableListOf<String>()
        val closed = mutableListOf<String>()

        for (i in 0 until 5) {
            val valPct = item.expectedFlex.getOrElse(i) { 50 }
            when {
                valPct <= 25 -> extended.add(names[i])
                valPct <= 68 -> curved.add(names[i])
                else -> closed.add(names[i])
            }
        }

        val fingerSummary = when {
            extended.size == 5 -> "Mano abierta (5 dedos)"
            closed.size == 5 -> "Puño cerrado (5 dedos)"
            extended.isNotEmpty() && curved.isEmpty() -> "Extender ${extended.joinToString(", ")}"
            curved.size >= 4 -> "Dedos semicurvos en garra"
            extended.isNotEmpty() -> "Extender ${extended.joinToString(", ")}"
            else -> "Curvar ${curved.joinToString(", ")}"
        }

        val pitch = item.expectedImu.getOrElse(0) { 0.0 }
        val roll = item.expectedImu.getOrElse(1) { 0.0 }
        val wristParts = mutableListOf<String>()
        if (pitch >= 35.0) wristParts.add("arriba")
        else if (pitch <= -30.0) wristParts.add("abajo")
        if (roll <= -35.0) wristParts.add("giro izq.")
        else if (roll >= 35.0) wristParts.add("giro der.")

        val wristText = if (wristParts.isEmpty()) "muñeca neutra" else "muñeca ${wristParts.joinToString(" + ")}"
        val extra = if (item.gloss == "HOLA") " (o agitar)" else ""
        return "$fingerSummary · $wristText$extra"
    }

    /**
     * Evalúa el descriptor visual y la cinemática 3D extraída por MediaPipe.
     * Si [cameraFlex] se proporciona (5 valores 0..100%), clasifica directamente por postura real
     * de los 5 dedos + inclinación/orientación + descriptor visual; si es nulo (pruebas manuales),
     * evalúa por descriptor simbólico y apertura global.
     */
    fun evaluateCamera(
        visualDescriptor: String,
        handOpenness: Float,
        handTilt: Float,
        cameraFlex: List<Int>? = null,
        handPitch: Float = 0f
    ): Pair<String, Float> {
        if (visualDescriptor.isBlank()) {
            return Pair("NINGUNA", 0f)
        }

        // Modo cinemático real 3D (MediaPipe 21 landmarks -> 5 flexiones + pitch + tilt)
        if (cameraFlex != null && cameraFlex.size >= 5) {
            val isNeutralRest = cameraFlex.all { it <= 20 } &&
                abs(handPitch) < 20f &&
                abs(handTilt) < 20f &&
                (visualDescriptor.equals("NEUTRAL", ignoreCase = true) ||
                    visualDescriptor.equals("OPEN_PALM", ignoreCase = true))
            if (isNeutralRest) {
                return Pair("NINGUNA", 0f)
            }

            var bestGloss = "NINGUNA"
            var bestScore = 0f
            val vdPrefix = visualDescriptor.substringBefore('_')

            for ((gloss, item) in MorphologyEngine.lexicon) {
                val expFlex = item.expectedFlex
                val expImu = item.expectedImu

                var flexDistSq = 0.0
                var maxFingerDiff = 0.0
                for (i in 0 until 5) {
                    val fActual = cameraFlex[i]
                    val fExpected = expFlex.getOrElse(i) { 50 }
                    val absDiff = abs(fActual - fExpected) / 100.0
                    if (absDiff > maxFingerDiff) maxFingerDiff = absDiff
                    flexDistSq += absDiff * absDiff
                }
                val rmsFlexDist = sqrt(flexDistSq / 5.0)
                val flexDist = (rmsFlexDist * 0.72 + maxFingerDiff * 0.28).coerceIn(0.0, 1.0)

                val pDiff = (abs(handPitch.toDouble() - expImu.getOrElse(0) { 0.0 }) / 90.0).coerceIn(0.0, 1.0)
                val rDiff = (abs(handTilt.toDouble() - expImu.getOrElse(1) { 0.0 }) / 140.0).coerceIn(0.0, 1.0)

                val descriptorBonus = when {
                    item.visionDescriptor.equals(visualDescriptor, ignoreCase = true) -> 0.18
                    vdPrefix.length >= 3 && item.visionDescriptor.substringBefore('_').equals(vdPrefix, ignoreCase = true) -> 0.08
                    else -> 0.0
                }

                val score = (
                    (1.0 - flexDist) * 0.58 +
                        (1.0 - pDiff) * 0.18 +
                        (1.0 - rDiff) * 0.06 +
                        descriptorBonus
                    ).toFloat().coerceIn(0f, 1f)

                if (score > bestScore) {
                    bestScore = score
                    bestGloss = gloss
                }
            }

            return if (bestScore >= 0.65f) Pair(bestGloss, bestScore) else Pair("NINGUNA", 0f)
        }

        // Modo descriptor directo (para botones de prueba manual o pruebas unitarias sin landmarks)
        if (visualDescriptor == "NEUTRAL") {
            return Pair("NINGUNA", 0f)
        }

        var bestGloss = "NINGUNA"
        var bestScore = 0f

        for ((gloss, item) in MorphologyEngine.lexicon) {
            var score = 0f
            if (item.visionDescriptor.equals(visualDescriptor, ignoreCase = true)) {
                score += 0.85f
                val avgExpFlex = item.expectedFlex.average().toFloat() / 100f
                val opennessMatch = 1.0f - abs((1.0f - avgExpFlex) - handOpenness).coerceIn(0f, 1f)
                score += opennessMatch * 0.15f
            }

            if (score > bestScore) {
                bestScore = score
                bestGloss = gloss
            }
        }

        return if (bestScore >= 0.70f) Pair(bestGloss, bestScore.coerceIn(0f, 1f)) else Pair("NINGUNA", 0f)
    }

    /**
     * Ejecuta la FUSIÓN MULTIMODAL de acuerdo al modo activo.
     */
    fun processMultimodal(
        mode: MultimodalMode,
        telemetry: GloveTelemetry,
        visualDescriptor: String,
        handOpenness: Float,
        handTilt: Float,
        cameraFlex: List<Int>? = null,
        handPitch: Float = 0f,
        customSignatures: Map<String, CustomGloveSignature> = emptyMap(),
        allowedGlosses: Set<String>? = null,
        isWavingGlove: Boolean = false
    ): RecognitionResult {
        val (gloveGloss, gloveConf) = evaluateGlove(
            telemetry = telemetry,
            customSignatures = customSignatures,
            allowedGlosses = allowedGlosses,
            isWavingGlove = isWavingGlove
        )
        val (camGloss, camConf) = evaluateCamera(visualDescriptor, handOpenness, handTilt, cameraFlex, handPitch)

        return when (mode) {
            MultimodalMode.GLOVE_ONLY -> {
                RecognitionResult(
                    gloss = gloveGloss,
                    gloveConfidence = gloveConf,
                    cameraConfidence = 0f,
                    fusionConfidence = gloveConf,
                    sourceDescription = if (gloveGloss != "NINGUNA") "Guante Háptico BLE (${(gloveConf * 100).toInt()}%)" else "Sin seña activa"
                )
            }
            MultimodalMode.CAMERA_ONLY -> {
                RecognitionResult(
                    gloss = camGloss,
                    gloveConfidence = 0f,
                    cameraConfidence = camConf,
                    fusionConfidence = camConf,
                    sourceDescription = if (camGloss != "NINGUNA") "Cámara MediaPipe (${(camConf * 100).toInt()}%)" else "Mano en espera"
                )
            }
            MultimodalMode.HYBRID_FUSION -> {
                if (visualDescriptor == "NEUTRAL" || visualDescriptor.isBlank()) {
                    if (gloveConf < 0.72f || gloveGloss == "NINGUNA") {
                        RecognitionResult(
                            gloss = "NINGUNA",
                            gloveConfidence = gloveConf,
                            cameraConfidence = 0f,
                            fusionConfidence = 0f,
                            sourceDescription = "Postura neutral en reposo"
                        )
                    } else {
                        RecognitionResult(
                            gloss = gloveGloss,
                            gloveConfidence = gloveConf,
                            cameraConfidence = 0f,
                            fusionConfidence = gloveConf * 0.94f,
                            sourceDescription = "Guante BLE Háptico (${(gloveConf * 100).toInt()}%)"
                        )
                    }
                } else if (gloveGloss != "NINGUNA" && gloveGloss == camGloss) {
                    val fusedConf = (0.50f * gloveConf + 0.45f * camConf + 0.05f).coerceIn(0f, 0.998f)
                    RecognitionResult(
                        gloss = gloveGloss,
                        gloveConfidence = gloveConf,
                        cameraConfidence = camConf,
                        fusionConfidence = fusedConf,
                        sourceDescription = "FUSIÓN HÍBRIDA: Guante + Cámara [${(fusedConf * 100).toInt()}% PRECISIÓN]"
                    )
                } else if (gloveConf >= 0.84f) {
                    RecognitionResult(
                        gloss = gloveGloss,
                        gloveConfidence = gloveConf,
                        cameraConfidence = camConf,
                        fusionConfidence = gloveConf * 0.95f,
                        sourceDescription = "Fusión: Prioridad Guante (${(gloveConf * 100).toInt()}%)"
                    )
                } else if (camConf >= 0.85f) {
                    RecognitionResult(
                        gloss = camGloss,
                        gloveConfidence = gloveConf,
                        cameraConfidence = camConf,
                        fusionConfidence = camConf * 0.95f,
                        sourceDescription = "Fusión: Prioridad Cámara (${(camConf * 100).toInt()}%)"
                    )
                } else if (gloveGloss != "NINGUNA" && gloveConf >= 0.75f && camGloss == "NINGUNA") {
                    RecognitionResult(
                        gloss = gloveGloss,
                        gloveConfidence = gloveConf,
                        cameraConfidence = 0f,
                        fusionConfidence = gloveConf * 0.90f,
                        sourceDescription = "Guante BLE Háptico (${(gloveConf * 100).toInt()}%)"
                    )
                } else {
                    RecognitionResult(
                        gloss = "NINGUNA",
                        gloveConfidence = gloveConf,
                        cameraConfidence = camConf,
                        fusionConfidence = 0f,
                        sourceDescription = "Esperando postura o movimiento de seña..."
                    )
                }
            }
        }
    }
}
