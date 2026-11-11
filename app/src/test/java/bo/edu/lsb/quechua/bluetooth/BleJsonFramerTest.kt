package bo.edu.lsb.quechua.bluetooth

import bo.edu.lsb.quechua.engine.MorphologyEngine
import bo.edu.lsb.quechua.engine.MultimodalFusionEngine
import bo.edu.lsb.quechua.engine.SignDebouncer
import bo.edu.lsb.quechua.model.FingerCalibration
import bo.edu.lsb.quechua.model.GloveCalibrationProfile
import bo.edu.lsb.quechua.model.GloveTelemetry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * Pruebas unitarias de reensamblado de paquetes BLE ATT MTU ([BleJsonFramer])
 * y verificación extremo a extremo desde bytes fragmentados del ESP32 hasta
 * la salida bilingüe y fonética TTS Quechua.
 */
class BleJsonFramerTest {

    @Test
    fun testReassembles20ByteAttMtuFragments() {
        val framer = BleJsonFramer()
        val fullJson =
            """{"pulgar":12,"indice":85,"medio":92,"anular":90,"menique":88,"pitch":14.5,"roll":-3.2}"""
        val bytes = fullJson.toByteArray(StandardCharsets.UTF_8)

        val chunks = mutableListOf<ByteArray>()
        var offset = 0
        while (offset < bytes.size) {
            val end = minOf(offset + 20, bytes.size)
            chunks.add(bytes.copyOfRange(offset, end))
            offset = end
        }
        assertTrue("La trama debe partirse en al menos 4 fragmentos BLE de 20B", chunks.size >= 4)

        val emitted = mutableListOf<String>()
        for (i in 0 until chunks.size - 1) {
            val res = framer.appendBytes(chunks[i])
            assertTrue("No debe emitir JSON incompleto en el fragmento $i", res.isEmpty())
            emitted.addAll(res)
        }
        emitted.addAll(framer.appendBytes(chunks.last()))

        assertEquals(listOf(fullJson), emitted)
        val parsed = GloveTelemetry.parseFromJson(emitted.single())
        assertNotNull(parsed)
        assertEquals(listOf(12, 85, 92, 90, 88), parsed!!.flex)
        assertEquals(14.5, parsed.imuPitch, 0.01)
        assertEquals(-3.2, parsed.imuRoll, 0.01)
    }

    @Test
    fun testSplitsStickyPacketsAndDiscardsLeadingGarbage() {
        val framer = BleJsonFramer()
        val frame1 = """{"pulgar":10,"indice":10,"medio":10,"anular":10,"menique":10,"pitch":0.0,"roll":0.0}"""
        val frame2 = """{"pulgar":52,"indice":12,"medio":88,"anular":92,"menique":92,"pitch":15.0,"roll":0.0}"""

        val stickyPart1 = "\r\nESP32_BOOT_NOISE_123" + frame1 + frame2.substring(0, 28)
        val firstBatch = framer.appendBytes(stickyPart1.toByteArray(StandardCharsets.UTF_8))
        assertEquals(listOf(frame1), firstBatch)

        val secondBatch = framer.appendBytes(frame2.substring(28).toByteArray(StandardCharsets.UTF_8))
        assertEquals(listOf(frame2), secondBatch)
    }

    @Test
    fun testHandlesEscapedQuotesAndBracesInsideJsonString() {
        val framer = BleJsonFramer()
        val trickyJson =
            """{"meta":"noise\"inside{braces}","pulgar":15,"indice":20,"medio":25,"anular":80,"menique":85,"pitch":5.0,"roll":-2.0}"""

        val half = trickyJson.length / 2
        val part1 = framer.appendChunk(trickyJson.substring(0, half))
        assertTrue(part1.isEmpty())

        val part2 = framer.appendChunk(trickyJson.substring(half))
        assertEquals(listOf(trickyJson), part2)
    }

    @Test
    fun testRecoversFromTruncatedOverflowFrame() {
        val framer = BleJsonFramer(maxBufferChars = 128)
        val corruptTruncated = """{"pulgar":99,"corrupt":"${"X".repeat(140)}"""
        val res1 = framer.appendChunk(corruptTruncated)
        assertTrue(res1.isEmpty())

        val validFrame = """{"pulgar":30,"indice":25,"medio":20,"anular":75,"menique":80,"pitch":15.0,"roll":10.0}"""
        val res2 = framer.appendChunk(validFrame)
        assertEquals(listOf(validFrame), res2)
    }

    @Test
    fun testEndToEndBleBytesThroughPipelineToQuechuaTts() {
        // Perfil calibrado Paso Cero realista en escala 0..100% (reposo 10% -> flexionado 90%, cero IMU (+4°, -2°))
        val profile = GloveCalibrationProfile(
            fingers = List(5) { FingerCalibration(restRaw = 10, flexRaw = 90) },
            zeroPitch = 4.0,
            zeroRoll = -2.0,
            isCalibrated = true
        )

        // Invertimos el perfil para simular lecturas brutas realistas del ESP32 antes de aplicar Paso Cero
        fun toSensorPercent(targetPct: Int): Int =
            (10.0 + (targetPct / 100.0) * 80.0).toInt().coerceIn(0, 100)

        fun build20ByteBleChunksForGloss(gloss: String): List<ByteArray> {
            val item = MorphologyEngine.lexicon[gloss]!!
            val rawJson = String.format(
                Locale.US,
                """{"pulgar":%d,"indice":%d,"medio":%d,"anular":%d,"menique":%d,"pitch":%.1f,"roll":%.1f}""",
                toSensorPercent(item.expectedFlex[0]),
                toSensorPercent(item.expectedFlex[1]),
                toSensorPercent(item.expectedFlex[2]),
                toSensorPercent(item.expectedFlex[3]),
                toSensorPercent(item.expectedFlex[4]),
                item.expectedImu[0] + profile.zeroPitch,
                item.expectedImu[1] + profile.zeroRoll
            )
            val bytes = rawJson.toByteArray(StandardCharsets.UTF_8)
            val chunks = mutableListOf<ByteArray>()
            var idx = 0
            while (idx < bytes.size) {
                chunks.add(bytes.copyOfRange(idx, minOf(idx + 20, bytes.size)))
                idx += 20
            }
            return chunks
        }

        val restChunks = {
            val restJson = """{"pulgar":10,"indice":10,"medio":10,"anular":10,"menique":10,"pitch":4.0,"roll":-2.0}"""
            val bytes = restJson.toByteArray(StandardCharsets.UTF_8)
            listOf(bytes.copyOfRange(0, 20), bytes.copyOfRange(20, bytes.size))
        }

        val framer = BleJsonFramer()
        val acceptedSequence = mutableListOf<String>()
        val debouncer = SignDebouncer(threshold = 5, maxGlitchFrames = 1) { accepted ->
            acceptedSequence.add(accepted)
        }

        val stream = listOf("YO", "AGUA", "QUERER")
        for (targetGloss in stream) {
            // 5 tramas de la seña fragmentadas a 20 bytes
            repeat(5) {
                for (chunk in build20ByteBleChunksForGloss(targetGloss)) {
                    for (json in framer.appendBytes(chunk)) {
                        val telemetry = GloveTelemetry.parseFromJson(json, profile)!!
                        val (predicted, _) = MultimodalFusionEngine.evaluateGlove(
                            telemetry = telemetry,
                            allowedGlosses = MultimodalFusionEngine.ESSENTIAL_GLOSSES
                        )
                        debouncer.processReading(predicted)
                    }
                }
            }
            // Pausa de reposo entre señas
            repeat(2) {
                for (chunk in restChunks()) {
                    for (json in framer.appendBytes(chunk)) {
                        val telemetry = GloveTelemetry.parseFromJson(json, profile)!!
                        val (predicted, _) = MultimodalFusionEngine.evaluateGlove(
                            telemetry = telemetry,
                            allowedGlosses = MultimodalFusionEngine.ESSENTIAL_GLOSSES
                        )
                        debouncer.processReading(predicted)
                    }
                }
            }
        }

        assertEquals(listOf("YO", "AGUA", "QUERER"), acceptedSequence)

        val translation = MorphologyEngine.translateSequence(acceptedSequence)
        assertEquals("Yo quiero agua", translation.spanishSentence)
        assertEquals("Ñuqa yakuta munani", translation.quechuaSentence)
        assertEquals("Ñocja yacuta munani", translation.quechuaPhonetics)

        val ttsSpeech = MorphologyEngine.toTtsPhoneticSpeech(translation.quechuaSentence)
        assertEquals("Ñoka yakuta munani", ttsSpeech)
    }

    @Test
    fun testEndToEndWavingGreetingAndFullPipelineThroughFramer() {
        val framer = BleJsonFramer()
        val profile = GloveCalibrationProfile(
            fingers = List(5) { FingerCalibration(0, 100) },
            isCalibrated = true
        )

        // Oscilación lateral real de muñeca con mano abierta (~900 ms a 10 Hz) enviada en fragmentos BLE de 20B
        val rollWave = listOf(-30.0, -5.0, 25.0, -2.0, -25.0, 20.0, -20.0, 22.0, -18.0)
        val imuWindow = ArrayDeque<Pair<Double, Double>>(8)
        val accepted = mutableListOf<String>()
        val debouncer = SignDebouncer(threshold = 5, maxGlitchFrames = 1) { accepted.add(it) }

        for (rVal in rollWave) {
            val rawJson = String.format(
                Locale.US,
                """{"pulgar":12,"indice":12,"medio":12,"anular":12,"menique":12,"pitch":25.0,"roll":%.1f}""",
                rVal
            )
            val bytes = rawJson.toByteArray(StandardCharsets.UTF_8)
            var idx = 0
            while (idx < bytes.size) {
                val chunk = bytes.copyOfRange(idx, minOf(idx + 20, bytes.size))
                idx += 20
                for (json in framer.appendBytes(chunk)) {
                    val telemetry = GloveTelemetry.parseFromJson(json, profile)!!
                    if (imuWindow.size >= 8) imuWindow.removeFirst()
                    imuWindow.addLast(telemetry.imuPitch to telemetry.imuRoll)
                    val isWaving = MultimodalFusionEngine.detectOscillatingWave(imuWindow.toList())
                    val (pred, _) = MultimodalFusionEngine.evaluateGlove(
                        telemetry = telemetry,
                        allowedGlosses = MultimodalFusionEngine.ESSENTIAL_GLOSSES,
                        isWavingGlove = isWaving
                    )
                    debouncer.processReading(pred)
                }
            }
        }

        assertEquals(listOf("HOLA"), accepted)
        val res = MorphologyEngine.translateSequence(accepted)
        assertEquals("Hola, ¿cómo estás?", res.spanishSentence)
        assertEquals("Allillanchu", res.quechuaSentence)
        assertEquals("¿Allillancho?", MorphologyEngine.toTtsPhoneticSpeech(res.quechuaSentence))
    }
}

