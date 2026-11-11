package bo.edu.lsb.quechua.engine

import bo.edu.lsb.quechua.model.FingerCalibration
import bo.edu.lsb.quechua.model.GloveCalibrationProfile
import bo.edu.lsb.quechua.model.GloveTelemetry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Suite Exhaustiva de Verificación de Integridad:
 * 1. Léxico completo (100+ términos en Quechua y Español)
 * 2. Fonética y Pronunciación TTS Andina
 * 3. Morfosintaxis Bilingüe (SOV, Sufijos Gramaticales, Casos)
 * 4. Calibración de Sensores Hall e Inversión de Polaridad
 * 5. Fusión Multimodal (Guante Háptico + Cámara)
 */
class ComprehensiveExhaustiveVerificationTest {

    @Test
    fun testAllLexiconItemsIntegrity() {
        val lexicon = MorphologyEngine.lexicon
        assertTrue("El léxico debe contener al menos 100 palabras", lexicon.size >= 100)

        for ((gloss, item) in lexicon) {
            // Glosa y raíz
            assertFalse("La glosa no debe ser vacía", gloss.isBlank())
            assertFalse("El stem quechua no debe ser vacío para $gloss", item.quechuaStem.isBlank())
            assertFalse("La traducción en español no debe ser vacía para $gloss", item.spanishDefault.isBlank())
            assertFalse("La etiqueta de categoría no debe ser vacía para $gloss", item.categoryLabel.isBlank())

            // Vectores de hardware del guante
            assertEquals("Cada seña debe definir exactamente 5 sensores de flexión para $gloss", 5, item.expectedFlex.size)
            for (f in item.expectedFlex) {
                assertTrue("Flexión fuera de rango 0..100 en $gloss: $f", f in 0..100)
            }

            assertTrue("IMU debe definir al menos [pitch, roll] para $gloss", item.expectedImu.size >= 2)
            assertTrue("Touch debe definir al menos 3 pines para $gloss", item.expectedTouch.size >= 3)
            for (t in item.expectedTouch) {
                assertTrue("Touch debe ser 0 o 1 en $gloss: $t", t == 0 || t == 1)
            }

            // Descriptor visual MediaPipe
            assertFalse("El descriptor visual no debe ser vacío para $gloss", item.visionDescriptor.isBlank())

            // Fonética para síntesis de voz (TTS)
            val phonetic = MorphologyEngine.adaptQuechuaPhonetics(item.quechuaStem)
            assertNotNull("La fonética no debe ser nula para $gloss", phonetic)
            assertFalse("La fonética no debe ser vacía para $gloss", phonetic.isBlank())
        }
    }

    @Test
    fun testSubjectVerbAgreementAcrossPronouns() {
        // Matriz de personas gramaticales con el verbo QUERER (muna-)
        val yoQuerer = MorphologyEngine.translateSequence(listOf("YO", "AGUA", "QUERER"))
        assertEquals("Ñuqa yakuta munani", yoQuerer.quechuaSentence)
        assertEquals("Yo quiero agua", yoQuerer.spanishSentence)

        val tuQuerer = MorphologyEngine.translateSequence(listOf("TU", "AGUA", "QUERER"))
        assertEquals("Qam yakuta munanki", tuQuerer.quechuaSentence)
        assertEquals("Tú quieres agua", tuQuerer.spanishSentence)

        val elQuerer = MorphologyEngine.translateSequence(listOf("EL", "AGUA", "QUERER"))
        assertEquals("Pay yakuta munan", elQuerer.quechuaSentence)
        assertEquals("Él quiere agua", elQuerer.spanishSentence)

        val nosotrosQuerer = MorphologyEngine.translateSequence(listOf("NOSOTROS", "AGUA", "QUERER"))
        assertEquals("Ñuqanchik yakuta munanchik", nosotrosQuerer.quechuaSentence)
        assertEquals("Nosotros queremos agua", nosotrosQuerer.spanishSentence)
    }

    @Test
    fun testLocativeAndAllativeCaseSuffixes() {
        // Caso Alativo (-man) con verbos de movimiento (IR)
        val destinoEscuela = MorphologyEngine.translateSequence(listOf("YO", "ESCUELA", "IR"))
        assertEquals("Ñuqa yachay wasiman rini", destinoEscuela.quechuaSentence)
        assertEquals("Yo voy a la escuela", destinoEscuela.spanishSentence)

        // Caso Locativo (-pi) con verbos de estado (ESTAR)
        val ubicacionCasa = MorphologyEngine.translateSequence(listOf("YO", "CASA", "ESTAR"))
        assertEquals("Ñuqa wasipi kani", ubicacionCasa.quechuaSentence)
        assertEquals("Yo estoy en la casa", ubicacionCasa.spanishSentence)
    }

    @Test
    fun testAffectivePainFormulas() {
        // Dolor de cabeza: umay nanawan
        val dolorCabeza = MorphologyEngine.translateSequence(listOf("CABEZA", "DOLER"))
        assertEquals("umay nanawan", dolorCabeza.quechuaSentence)
        assertEquals("Me duele la cabeza", dolorCabeza.spanishSentence)

        // Dolor de estómago: wisay nanawan
        val dolorEstomago = MorphologyEngine.translateSequence(listOf("ESTOMAGO", "DOLER"))
        assertEquals("wisay nanawan", dolorEstomago.quechuaSentence)
        assertEquals("Me duele el estómago", dolorEstomago.spanishSentence)
    }

    @Test
    fun testNegationRuleWithManaAndSuffixChu() {
        // Oración negativa: Ñuqa yakuta mana munanichu
        val noAgua = MorphologyEngine.translateSequence(listOf("YO", "AGUA", "NO", "QUERER"))
        assertEquals("Ñuqa yakuta mana munanichu", noAgua.quechuaSentence)
        assertEquals("Yo no quiero agua", noAgua.spanishSentence)
        assertTrue(noAgua.ruleExplanation.contains("mana"))
    }

    @Test
    fun testQuechuaPhoneticsComprehensiveTransformation() {
        // 1. Eyectivas
        assertEquals("tjanta", MorphologyEngine.adaptQuechuaPhonetics("t'anta"))
        assertEquals("kjacha", MorphologyEngine.adaptQuechuaPhonetics("k'acha"))
        assertEquals("chjarki", MorphologyEngine.adaptQuechuaPhonetics("ch'arki"))

        // 2. Aspiradas
        assertEquals("cjhagua", MorphologyEngine.adaptQuechuaPhonetics("qhawa"))
        assertEquals("micjuna", MorphologyEngine.adaptQuechuaPhonetics("mikhuna"))

        // 3. Alofonía uvular post-velar
        assertEquals("soncjo", MorphologyEngine.adaptQuechuaPhonetics("sunqu"))
        assertEquals("cjocha", MorphologyEngine.adaptQuechuaPhonetics("qucha"))
        assertEquals("guajqe", MorphologyEngine.adaptQuechuaPhonetics("wawqi"))

        // 4. Semivocal bilabial /w/
        assertEquals("guasi", MorphologyEngine.adaptQuechuaPhonetics("wasi"))
        assertEquals("guagua", MorphologyEngine.adaptQuechuaPhonetics("wawa"))

        // 5. Saludo interrogativo
        assertEquals("Allillancho", MorphologyEngine.adaptQuechuaPhonetics("Allillanchu"))
    }

    @Test
    fun testGloveCalibrationInversionAndAdcScaling() {
        // Polaridad magnética directa (el valor sube con la flexión)
        val directCal = FingerCalibration(restRaw = 120, flexRaw = 220)
        assertEquals(0, directCal.normalize(120))
        assertEquals(50, directCal.normalize(170))
        assertEquals(100, directCal.normalize(220))
        // Clamping
        assertEquals(0, directCal.normalize(80))
        assertEquals(100, directCal.normalize(250))

        // Polaridad magnética invertida (el imán se acerca al polo sur, el valor baja con la flexión)
        val invertedCal = FingerCalibration(restRaw = 230, flexRaw = 130)
        assertEquals(0, invertedCal.normalize(230))
        assertEquals(50, invertedCal.normalize(180))
        assertEquals(100, invertedCal.normalize(130))
        // Clamping en invertido
        assertEquals(0, invertedCal.normalize(250))
        assertEquals(100, invertedCal.normalize(100))

        // Sensor del meñique (G34 en ESP32: resolución de 12 bits, valores 1800..2900)
        val adc12BitCal = FingerCalibration(restRaw = 2800, flexRaw = 1900)
        assertEquals(0, adc12BitCal.normalize(2800))
        assertEquals(100, adc12BitCal.normalize(1900))
        assertEquals(50, adc12BitCal.normalize(2350))

        // Fallback inteligente cuando span < 12 (sin calibrar aún): aplica perfiles estándar de magnitud sin crasheo
        val zeroSpanCal = FingerCalibration(restRaw = 128, flexRaw = 128)
        assertEquals(0, zeroSpanCal.normalize(128)) // En reposo estándar PCF8591
        assertEquals(24, zeroSpanCal.normalize(150)) // 24% flexión estimada estándar
    }

    @Test
    fun testMultimodalFusionHybridAgreementAndNeutralVeto() {
        val waterItem = MorphologyEngine.lexicon["AGUA"]!!
        val waterTelemetry = GloveTelemetry(
            flex = waterItem.expectedFlex,
            imuPitch = waterItem.expectedImu[0],
            imuRoll = waterItem.expectedImu[1],
            touch = waterItem.expectedTouch,
            gloss = "AGUA"
        )

        // 1. Ambos coinciden -> Confianza superior a 95%
        val agreementResult = MultimodalFusionEngine.processMultimodal(
            mode = MultimodalMode.HYBRID_FUSION,
            telemetry = waterTelemetry,
            visualDescriptor = "CUP_HAND",
            handOpenness = 0.5f,
            handTilt = 10f
        )
        assertEquals("AGUA", agreementResult.gloss)
        assertTrue(agreementResult.confidence >= 0.95f)
        assertTrue(agreementResult.sourceDescription.contains("FUSIÓN HÍBRIDA"))

        // 2. Mano en reposo neutro -> La cámara veta cualquier falso positivo
        val neutralTelemetry = GloveTelemetry(
            flex = listOf(10, 10, 10, 10, 10),
            imuPitch = 0.0,
            imuRoll = 0.0,
            touch = listOf(0, 0, 0)
        )
        val neutralResult = MultimodalFusionEngine.processMultimodal(
            mode = MultimodalMode.HYBRID_FUSION,
            telemetry = neutralTelemetry,
            visualDescriptor = "NEUTRAL",
            handOpenness = 0.5f,
            handTilt = 0f
        )
        assertEquals("NINGUNA", neutralResult.gloss)
        assertEquals(0f, neutralResult.confidence, 0.001f)
    }
}
