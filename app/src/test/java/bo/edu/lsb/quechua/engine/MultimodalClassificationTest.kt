package bo.edu.lsb.quechua.engine

import bo.edu.lsb.quechua.model.CustomGloveSignature
import bo.edu.lsb.quechua.model.FingerCalibration
import bo.edu.lsb.quechua.model.GloveCalibrationProfile
import bo.edu.lsb.quechua.model.GloveTelemetry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * Suite de Pruebas del Clasificador y Motor de Fusión Multimodal (Guante Háptico + Cámara MediaPipe).
 */
class MultimodalClassificationTest {

    @Test
    fun testGloveOnlyEvaluation() {
        val waterItem = MorphologyEngine.lexicon["AGUA"]!!
        val telemetry = GloveTelemetry(
            flex = waterItem.expectedFlex,
            imuPitch = waterItem.expectedImu[0],
            imuRoll = waterItem.expectedImu[1],
            touch = waterItem.expectedTouch,
            gloss = "AGUA"
        )

        val result = MultimodalFusionEngine.processMultimodal(
            mode = MultimodalMode.GLOVE_ONLY,
            telemetry = telemetry,
            visualDescriptor = "NEUTRAL",
            handOpenness = 0.5f,
            handTilt = 0f
        )

        assertEquals("AGUA", result.gloss)
        assertTrue("La confianza del guante debe ser mayor al 80%", result.gloveConfidence >= 0.80f)
        assertEquals(0f, result.cameraConfidence, 0.001f)
    }

    @Test
    fun testCameraOnlyEvaluation() {
        val telemetryNeutral = GloveTelemetry.default()

        val result = MultimodalFusionEngine.processMultimodal(
            mode = MultimodalMode.CAMERA_ONLY,
            telemetry = telemetryNeutral,
            visualDescriptor = "CUP_HAND",
            handOpenness = 0.5f,
            handTilt = 10f
        )

        assertEquals("AGUA", result.gloss)
        assertTrue("La confianza de la cámara debe ser mayor al 75%", result.cameraConfidence >= 0.75f)
        assertEquals(0f, result.gloveConfidence, 0.001f)
    }

    @Test
    fun testHybridFusionAgreementBoostsConfidence() {
        val waterItem = MorphologyEngine.lexicon["AGUA"]!!
        val telemetry = GloveTelemetry(
            flex = waterItem.expectedFlex,
            imuPitch = waterItem.expectedImu[0],
            imuRoll = waterItem.expectedImu[1],
            touch = waterItem.expectedTouch,
            gloss = "AGUA"
        )

        val result = MultimodalFusionEngine.processMultimodal(
            mode = MultimodalMode.HYBRID_FUSION,
            telemetry = telemetry,
            visualDescriptor = "CUP_HAND",
            handOpenness = 0.5f,
            handTilt = 10f
        )

        assertEquals("AGUA", result.gloss)
        assertTrue("La confianza de fusión híbrida debe superar el 95% al coincidir ambos sensores", result.confidence >= 0.95f)
        assertTrue(result.sourceDescription.contains("FUSIÓN HÍBRIDA"))
    }

    @Test
    fun testNeutralPostureReturnsNinguna() {
        val telemetryNeutral = GloveTelemetry(
            flex = listOf(10, 10, 10, 10, 10),
            imuPitch = 0.0,
            imuRoll = 0.0,
            touch = listOf(0, 0, 0)
        )

        val result = MultimodalFusionEngine.processMultimodal(
            mode = MultimodalMode.HYBRID_FUSION,
            telemetry = telemetryNeutral,
            visualDescriptor = "NEUTRAL",
            handOpenness = 0.5f,
            handTilt = 0f
        )

        assertEquals("NINGUNA", result.gloss)
        assertEquals(0f, result.confidence, 0.001f)
    }

    @Test
    fun testEsp32FinalBleJsonWithoutTouchClassifiesKeySigns() {
        val targetGlosses = listOf(
            "HOLA", "YO", "TU", "AGUA",
            "UNO", "DOS", "TRES", "CUATRO", "CINCO",
            "QUERER", "IR", "DOLER", "CABEZA"
        )

        val percentProfile = GloveCalibrationProfile(
            fingers = List(5) { FingerCalibration(0, 100) },
            zeroPitch = 0.0,
            zeroRoll = 0.0,
            isCalibrated = true
        )

        for (gloss in targetGlosses) {
            val item = MorphologyEngine.lexicon[gloss]!!
            // Formato JSON exacto del firmware final ESP32 BLE (sin touch ni glosa pre-etiquetada)
            val json = String.format(
                Locale.US,
                """{"pulgar":%d,"indice":%d,"medio":%d,"anular":%d,"menique":%d,"pitch":%.1f,"roll":%.1f}""",
                item.expectedFlex[0],
                item.expectedFlex[1],
                item.expectedFlex[2],
                item.expectedFlex[3],
                item.expectedFlex[4],
                item.expectedImu[0],
                item.expectedImu[1]
            )

            val parsed = GloveTelemetry.parseFromJson(json, percentProfile)
            assertNotNull("Debe parsear JSON BLE para $gloss", parsed)

            val res = MultimodalFusionEngine.processMultimodal(
                mode = MultimodalMode.GLOVE_ONLY,
                telemetry = parsed!!,
                visualDescriptor = "NEUTRAL",
                handOpenness = 0.5f,
                handTilt = 0f
            )

            assertEquals("La seña BLE '$gloss' debe reconocerse sin sensores touch", gloss, res.gloss)
            assertTrue("Confianza insuficiente en '$gloss': ${res.gloveConfidence}", res.gloveConfidence >= 0.90f)
        }
    }

    @Test
    fun testNeutralRestReturnsNingunaWhileCincoPitchIsRecognized() {
        // Mano abierta en reposo (pitch=0, roll=0, sin touch) debe dar NINGUNA
        val restTelemetry = GloveTelemetry(
            flex = listOf(10, 10, 10, 10, 10),
            rawFlex = listOf(10, 10, 10, 10, 10),
            imuPitch = 0.0,
            imuRoll = 0.0,
            touch = listOf(0, 0, 0)
        )
        val restRes = MultimodalFusionEngine.evaluateGlove(restTelemetry)
        assertEquals("NINGUNA", restRes.first)
        assertEquals(0f, restRes.second, 0.001f)

        // Mano abierta levantada al frente (CINCO, pitch ~ 58°) sí debe clasificarse como CINCO
        val cincoItem = MorphologyEngine.lexicon["CINCO"]!!
        val cincoTelemetry = GloveTelemetry(
            flex = cincoItem.expectedFlex,
            rawFlex = cincoItem.expectedFlex,
            imuPitch = cincoItem.expectedImu[0],
            imuRoll = cincoItem.expectedImu[1],
            touch = listOf(0, 0, 0)
        )
        val cincoRes = MultimodalFusionEngine.evaluateGlove(cincoTelemetry)
        assertEquals("CINCO", cincoRes.first)
        assertTrue("CINCO debe tener alta confianza: ${cincoRes.second}", cincoRes.second >= 0.90f)
    }

    @Test
    fun testAll146LexiconItemsSelfClassifyWithoutTouch() {
        val lexicon = MorphologyEngine.lexicon
        assertEquals("El léxico deduplicado debe tener exactamente 146 glosas únicas", 146, lexicon.size)

        for ((gloss, item) in lexicon) {
            val telemetry = GloveTelemetry(
                flex = item.expectedFlex,
                rawFlex = item.expectedFlex,
                imuPitch = item.expectedImu.getOrElse(0) { 0.0 },
                imuRoll = item.expectedImu.getOrElse(1) { 0.0 },
                touch = listOf(0, 0, 0),
                gloss = "NINGUNA"
            )
            val (predicted, conf) = MultimodalFusionEngine.evaluateGlove(telemetry)
            assertEquals("Auto-clasificación sin touch falló para $gloss", gloss, predicted)
            assertTrue("Confianza baja en $gloss: $conf", conf >= 0.90f)
        }
    }

    @Test
    fun testCamera3DKinematicsEvaluationRecognizesSignsAndRest() {
        // Reposo neutral 3D -> NINGUNA
        val (restGloss, restConf) = MultimodalFusionEngine.evaluateCamera(
            visualDescriptor = "OPEN_PALM",
            handOpenness = 0.9f,
            handTilt = 2f,
            cameraFlex = listOf(8, 8, 8, 8, 8),
            handPitch = 0f
        )
        assertEquals("NINGUNA", restGloss)
        assertEquals(0f, restConf, 0.001f)

        val targetSigns = listOf(
            "HOLA", "CINCO", "UNO", "DOS", "TRES", "CUATRO",
            "AGUA", "BIEN", "AYUDA", "YO", "CABEZA"
        )
        for (gloss in targetSigns) {
            val item = MorphologyEngine.lexicon[gloss]!!
            val avgFlex = item.expectedFlex.average().toFloat() / 100f
            val (predicted, conf) = MultimodalFusionEngine.evaluateCamera(
                visualDescriptor = item.visionDescriptor,
                handOpenness = (1f - avgFlex).coerceIn(0f, 1f),
                handTilt = item.expectedImu.getOrElse(1) { 0.0 }.toFloat(),
                cameraFlex = item.expectedFlex,
                handPitch = item.expectedImu.getOrElse(0) { 0.0 }.toFloat()
            )
            assertEquals("Cámara 3D falló al clasificar '$gloss'", gloss, predicted)
            assertTrue("Confianza de cámara 3D baja en '$gloss': $conf", conf >= 0.85f)
        }
    }

    @Test
    fun testSignDebouncerWithGlitchTolerance() {
        val accepted = mutableListOf<String>()
        val debouncer = SignDebouncer(threshold = 5, maxGlitchFrames = 1) { accepted.add(it) }

        // 3 frames estables + 1 glitch transitorio + 2 frames estables = acepta en el 5to frame válido
        debouncer.processReading("HOLA")
        debouncer.processReading("HOLA")
        debouncer.processReading("HOLA")
        debouncer.processReading("NINGUNA") // 1 glitch tolerado
        assertEquals("HOLA", debouncer.candidateGloss)
        assertEquals(3, debouncer.stableCount)

        debouncer.processReading("HOLA")
        debouncer.processReading("HOLA")
        assertEquals(listOf("HOLA"), accepted)

        // 2 glitches consecutivos deben reiniciar al candidato
        debouncer.processReading("AGUA")
        debouncer.processReading("AGUA")
        debouncer.processReading("NINGUNA") // glitch 1 (tolerado)
        assertEquals("AGUA", debouncer.candidateGloss)
        debouncer.processReading("NINGUNA") // glitch 2 (reinicia)
        assertEquals("NINGUNA", debouncer.candidateGloss)
        assertEquals(0, debouncer.stableCount)
    }

    @Test
    fun testEssentialGlossesScopeSeparatesWithRealisticSensorNoise() {
        val essential = MultimodalFusionEngine.ESSENTIAL_GLOSSES
        assertEquals(29, essential.size)

        for (gloss in essential) {
            val item = MorphologyEngine.lexicon[gloss]
            assertNotNull("Falta la glosa esencial $gloss en MorphologyEngine.lexicon", item)
            item!!

            // Simular variación realista del guante: ±8% flex y ±8° IMU alrededor de la postura esperada
            val noisyFlex = item.expectedFlex.mapIndexed { idx, v ->
                val delta = if (idx % 2 == 0) 8 else -8
                (v + delta).coerceIn(0, 100)
            }
            val noisyPitch = (item.expectedImu.getOrElse(0) { 0.0 } + 6.0).coerceIn(-90.0, 90.0)
            val noisyRoll = (item.expectedImu.getOrElse(1) { 0.0 } - 6.0).coerceIn(-180.0, 180.0)

            val noisyTelemetry = GloveTelemetry(
                flex = noisyFlex,
                rawFlex = noisyFlex,
                imuPitch = noisyPitch,
                imuRoll = noisyRoll,
                touch = listOf(0, 0, 0)
            )
            val (predicted, conf) = MultimodalFusionEngine.evaluateGlove(
                telemetry = noisyTelemetry,
                allowedGlosses = essential
            )
            assertEquals("Con ruido ±8%/±6°, $gloss debe seguir reconociéndose en modo Esencial", gloss, predicted)
            assertTrue("Confianza baja con ruido para $gloss: $conf", conf >= 0.75f)
        }
    }

    @Test
    fun testOscillatingWaveDistinguishesHolaFromMonotonicCincoLift() {
        // 1. Subir la mano abierta desde la mesa (0° -> 58°) es monotónico: NO es agitación (debe dar CINCO)
        val monotonicLift = listOf(
            0.0 to 0.0,
            10.0 to 1.0,
            22.0 to 2.0,
            35.0 to 1.0,
            48.0 to 0.0,
            58.0 to 0.0
        )
        assertFalse(
            "Elevar la mano suavemente hacia CINCO no debe disparar detectOscillatingWave",
            MultimodalFusionEngine.detectOscillatingWave(monotonicLift)
        )

        val cincoItem = MorphologyEngine.lexicon["CINCO"]!!
        val cincoTelemetry = GloveTelemetry(
            flex = cincoItem.expectedFlex,
            rawFlex = cincoItem.expectedFlex,
            imuPitch = 58.0,
            imuRoll = 0.0,
            touch = listOf(0, 0, 0)
        )
        val (cincoPred, _) = MultimodalFusionEngine.evaluateGlove(
            telemetry = cincoTelemetry,
            allowedGlosses = MultimodalFusionEngine.ESSENTIAL_GLOSSES,
            isWavingGlove = MultimodalFusionEngine.detectOscillatingWave(monotonicLift)
        )
        assertEquals("CINCO", cincoPred)

        // 2. Agitación lateral real de muñeca (roll -30 -> +25 -> -25 con pitch ~25°): SÍ es HOLA
        val lateralWave = listOf(
            25.0 to -30.0,
            26.0 to -5.0,
            25.0 to 25.0,
            24.0 to -2.0,
            25.0 to -25.0
        )
        assertTrue(
            "La oscilación lateral de muñeca debe detectarse como saludo",
            MultimodalFusionEngine.detectOscillatingWave(lateralWave)
        )

        val waveTelemetry = GloveTelemetry(
            flex = listOf(12, 12, 12, 12, 12),
            rawFlex = listOf(12, 12, 12, 12, 12),
            imuPitch = 25.0,
            imuRoll = 25.0,
            touch = listOf(0, 0, 0)
        )
        val (holaPred, holaConf) = MultimodalFusionEngine.evaluateGlove(
            telemetry = waveTelemetry,
            allowedGlosses = MultimodalFusionEngine.ESSENTIAL_GLOSSES,
            isWavingGlove = MultimodalFusionEngine.detectOscillatingWave(lateralWave)
        )
        assertEquals("HOLA", holaPred)
        assertTrue("HOLA dinámico debe tener alta confianza: $holaConf", holaConf >= 0.90f)
    }

    @Test
    fun testCustomGloveSignatureOverridesAdaptationForUserHand() {
        val customHola = CustomGloveSignature(
            gloss = "HOLA",
            flex = listOf(22, 34, 30, 28, 35),
            imuPitch = 32.0,
            imuRoll = -12.0,
            timestamp = 123456L
        )
        val userTelemetry = GloveTelemetry(
            flex = listOf(23, 33, 31, 27, 36),
            rawFlex = listOf(23, 33, 31, 27, 36),
            imuPitch = 31.0,
            imuRoll = -11.0,
            touch = listOf(0, 0, 0)
        )

        val (predicted, conf) = MultimodalFusionEngine.evaluateGlove(
            telemetry = userTelemetry,
            customSignatures = mapOf("HOLA" to customHola),
            allowedGlosses = MultimodalFusionEngine.ESSENTIAL_GLOSSES
        )
        assertEquals("HOLA", predicted)
        assertTrue("Firma personalizada grabada debe superar 0.95: $conf", conf >= 0.95f)
    }

    @Test
    fun testDescribeGlovePostureReturnsClearSpanishGuide() {
        val cincoDesc = MultimodalFusionEngine.describeGlovePosture(MorphologyEngine.lexicon["CINCO"]!!)
        assertTrue(cincoDesc.contains("Mano abierta"))
        assertTrue(cincoDesc.contains("arriba"))

        val holaDesc = MultimodalFusionEngine.describeGlovePosture(MorphologyEngine.lexicon["HOLA"]!!)
        assertTrue(holaDesc.contains("Mano abierta"))
        assertTrue(holaDesc.contains("agitar"))

        val siDesc = MultimodalFusionEngine.describeGlovePosture(MorphologyEngine.lexicon["SI"]!!)
        assertTrue(siDesc.contains("Puño cerrado"))
        assertTrue(siDesc.contains("abajo"))
    }
}

