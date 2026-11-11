package bo.edu.lsb.quechua.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GloveCalibrationTest {

    @Test
    fun testNormalPolarityCalibration() {
        val finger = FingerCalibration(restRaw = 128, flexRaw = 220)
        assertEquals(0, finger.normalize(128))
        assertEquals(100, finger.normalize(220))
        assertEquals(50, finger.normalize(174))
        assertEquals(0, finger.normalize(110)) // Menor que reposo no genera negativo
        assertEquals(100, finger.normalize(245)) // Mayor que máximo se topa en 100
    }

    @Test
    fun testInvertedPolarityCalibration() {
        // En caso de que el imán esté colocado con el polo inverso y al flexionar el valor baje
        val finger = FingerCalibration(restRaw = 128, flexRaw = 36)
        assertEquals(0, finger.normalize(128))
        assertEquals(100, finger.normalize(36))
        assertEquals(50, finger.normalize(82))
        assertEquals(0, finger.normalize(140))
        assertEquals(100, finger.normalize(20))
    }

    @Test
    fun testEsp32Adc12BitCalibration() {
        val pinky = FingerCalibration(restRaw = 1950, flexRaw = 2850)
        assertEquals(0, pinky.normalize(1950))
        assertEquals(100, pinky.normalize(2850))
        assertEquals(50, pinky.normalize(2400))
    }

    @Test
    fun testZeroSpanSafetyFallback() {
        // Si el sensor está desconectado o entrega el mismo valor para reposo y flexión
        val badFinger = FingerCalibration(restRaw = 128, flexRaw = 128)
        // No debe lanzar división entre cero, debe normalizar de forma segura
        val norm = badFinger.normalize(128)
        assertTrue(norm in 0..100)
    }

    @Test
    fun testGloveProfileNormalizesFullHand() {
        val profile = GloveCalibrationProfile(
            fingers = listOf(
                FingerCalibration(128, 220),  // Pulgar
                FingerCalibration(128, 220),  // Índice
                FingerCalibration(128, 220),  // Medio
                FingerCalibration(128, 220),  // Anular
                FingerCalibration(1950, 2850) // Meñique
            ),
            zeroPitch = 5.0,
            zeroRoll = -2.0,
            isCalibrated = true
        )

        // Simular mano extendida en reposo
        val restTelemetry = GloveTelemetry(
            rawFlex = listOf(128, 128, 128, 128, 1950),
            imuPitch = 5.0,
            imuRoll = -2.0
        ).withCalibration(profile)

        assertEquals(listOf(0, 0, 0, 0, 0), restTelemetry.flex)
        assertEquals(0.0, restTelemetry.imuPitch, 0.001)
        assertEquals(0.0, restTelemetry.imuRoll, 0.001)

        // Simular puño cerrado
        val flexTelemetry = GloveTelemetry(
            rawFlex = listOf(220, 220, 220, 220, 2850),
            imuPitch = 25.0,
            imuRoll = 8.0
        ).withCalibration(profile)

        assertEquals(listOf(100, 100, 100, 100, 100), flexTelemetry.flex)
        assertEquals(20.0, flexTelemetry.imuPitch, 0.001)
        assertEquals(10.0, flexTelemetry.imuRoll, 0.001)
    }

    @Test
    fun testParsingIncomingJsonWithCalibration() {
        val profile = GloveCalibrationProfile(
            fingers = listOf(
                FingerCalibration(128, 228),
                FingerCalibration(128, 228),
                FingerCalibration(128, 228),
                FingerCalibration(128, 228),
                FingerCalibration(1950, 2950)
            ),
            isCalibrated = true
        )

        val json = """{"pulgar":128,"indice":228,"medio":178,"anular":128,"menique":2950,"pitch":12.5,"roll":-3.0}"""
        val telemetry = GloveTelemetry.parseFromJson(json, profile)

        assertNotNull(telemetry)
        assertEquals(0, telemetry!!.thumbFlex)
        assertEquals(100, telemetry.indexFlex)
        assertEquals(50, telemetry.middleFlex)
        assertEquals(0, telemetry.ringFlex)
        assertEquals(100, telemetry.pinkyFlex)
    }

    @Test
    fun testParsingIncomingJsonWithEnglishKeys() {
        val profile = GloveCalibrationProfile(
            fingers = listOf(
                FingerCalibration(128, 228),
                FingerCalibration(128, 228),
                FingerCalibration(128, 228),
                FingerCalibration(128, 228),
                FingerCalibration(1950, 2950)
            ),
            isCalibrated = true
        )

        val json = """{"thumb":128,"index":228,"middle":128,"ring":228,"pinky":2950,"pitch":0.0,"roll":0.0}"""
        val telemetry = GloveTelemetry.parseFromJson(json, profile)

        assertNotNull(telemetry)
        assertEquals(0, telemetry!!.thumbFlex)
        assertEquals(100, telemetry.indexFlex)
        assertEquals(0, telemetry.middleFlex)
        assertEquals(100, telemetry.ringFlex)
        assertEquals(100, telemetry.pinkyFlex)
    }

    @Test
    fun testParsingIncomingCsvWithCalibration() {
        val profile = GloveCalibrationProfile(
            fingers = listOf(
                FingerCalibration(128, 220),
                FingerCalibration(128, 220),
                FingerCalibration(128, 220),
                FingerCalibration(128, 220),
                FingerCalibration(1950, 2850)
            ),
            isCalibrated = true
        )
        val csv = "128,220,128,220,2850,15.0,-4.5"
        val telemetry = GloveTelemetry.parseFromJson(csv, profile)

        assertNotNull(telemetry)
        assertEquals(0, telemetry!!.thumbFlex)
        assertEquals(100, telemetry.indexFlex)
        assertEquals(0, telemetry.middleFlex)
        assertEquals(100, telemetry.ringFlex)
        assertEquals(100, telemetry.pinkyFlex)
        assertEquals(15.0, telemetry.imuPitch, 0.001)
        assertEquals(-4.5, telemetry.imuRoll, 0.001)
    }

    @Test
    fun testPercentTelemetryPreservedWithLegacyOrUncalibratedProfile() {
        // Perfil antiguo en escala ADC (128..220 y 1950..2850) NO debe deformar tramas 0..100% del ESP32 BLE final
        val legacyAdcProfile = GloveCalibrationProfile(
            fingers = listOf(
                FingerCalibration(128, 220),
                FingerCalibration(128, 220),
                FingerCalibration(128, 220),
                FingerCalibration(128, 220),
                FingerCalibration(1950, 2850)
            ),
            isCalibrated = true
        )
        val bleJson = """{"pulgar":8,"indice":94,"medio":52,"anular":89,"menique":15,"pitch":22.5,"roll":-5.0}"""
        val telemetry = GloveTelemetry.parseFromJson(bleJson, legacyAdcProfile)
        assertNotNull(telemetry)
        assertEquals(listOf(8, 94, 52, 89, 15), telemetry!!.flex)

        // Perfil calibrado en escala porcentaje (con rango >= 18) sí re-escala correctamente
        val percentProfile = GloveCalibrationProfile(
            fingers = listOf(
                FingerCalibration(5, 85),
                FingerCalibration(10, 90),
                FingerCalibration(5, 85),
                FingerCalibration(10, 90),
                FingerCalibration(10, 90)
            ),
            isCalibrated = true
        )
        val rescaled = percentProfile.normalizeFlex(listOf(45, 50, 5, 90, 50))
        assertEquals(listOf(50, 50, 0, 100, 50), rescaled)
    }

    @Test
    fun testPartialPercentCalibrationPreservesCalibratedFingersAndStiffFinger() {
        // 4 dedos con recorrido amplio (>= 12) y 1 dedo con rango estrecho (< 12, p. ej. anular 12..18):
        // Los 4 calibrados deben normalizarse y el dedo de rango estrecho conserva su lectura sin descartar el perfil.
        val partialProfile = GloveCalibrationProfile(
            fingers = listOf(
                FingerCalibration(5, 85),
                FingerCalibration(10, 90),
                FingerCalibration(5, 85),
                FingerCalibration(12, 18), // span = 6 < 12
                FingerCalibration(10, 90)
            ),
            zeroPitch = 3.5,
            zeroRoll = -1.5,
            isCalibrated = true
        )

        val normalized = partialProfile.normalizeFlex(listOf(45, 50, 5, 37, 90))
        assertEquals(listOf(50, 50, 0, 37, 100), normalized)

        val telemetry = GloveTelemetry(
            rawFlex = listOf(45, 50, 5, 37, 90),
            imuPitch = 28.5,
            imuRoll = -11.5
        ).withCalibration(partialProfile)

        assertEquals(listOf(50, 50, 0, 37, 100), telemetry.flex)
        assertEquals(25.0, telemetry.imuPitch, 0.001)
        assertEquals(-10.0, telemetry.imuRoll, 0.001)
    }
}
