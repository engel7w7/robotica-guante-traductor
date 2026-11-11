package bo.edu.lsb.quechua.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MorphologyEngineTest {

    @Test
    fun testGreetingSingleWord() {
        val result = MorphologyEngine.translateSequence(listOf("HOLA"))
        assertEquals("Allillanchu", result.quechuaSentence)
        assertEquals("Hola, ¿cómo estás?", result.spanishSentence)
    }

    @Test
    fun testTransitiveVerbWithAccusativeTa() {
        // YO + AGUA + QUERER -> Ñuqa yakuta munani / Yo quiero agua
        val result = MorphologyEngine.translateSequence(listOf("YO", "AGUA", "QUERER"))
        assertEquals("Ñuqa yakuta munani", result.quechuaSentence)
        assertEquals("Yo quiero agua", result.spanishSentence)
        assertEquals("Ñocja yacuta munani", result.quechuaPhonetics)
    }

    @Test
    fun testSecondPersonTransitiveVerb() {
        // TU + PAN + QUERER -> Qam t'antata munanki / Tú quieres pan
        val result = MorphologyEngine.translateSequence(listOf("TU", "PAN", "QUERER"))
        assertEquals("Qam t'antata munanki", result.quechuaSentence)
        assertEquals("Tú quieres pan", result.spanishSentence)
        assertEquals("Cjam tjantata munanki", result.quechuaPhonetics)
    }

    @Test
    fun testMovementVerbWithAllativeMan() {
        // YO + MEDICO + IR -> Ñuqa hampiqman rini / Yo voy al médico
        val result = MorphologyEngine.translateSequence(listOf("YO", "MEDICO", "IR"))
        assertEquals("Ñuqa hampiqman rini", result.quechuaSentence)
        assertEquals("Yo voy al médico", result.spanishSentence)
        assertEquals("Ñocja jampicjman rini", result.quechuaPhonetics)
    }

    @Test
    fun testAffectiveVerbBodyAffectionDoler() {
        // CABEZA + DOLER -> Umay nanawan / Me duele la cabeza
        val result = MorphologyEngine.translateSequence(listOf("CABEZA", "DOLER"))
        assertEquals("umay nanawan", result.quechuaSentence)
        assertEquals("Me duele la cabeza", result.spanishSentence)
    }

    @Test
    fun testTemporalAdverbWithSentence() {
        // MANANA + YO + AGUA + QUERER -> Q'aya Ñuqa yakuta munani / Mañana Yo quiero agua
        val result = MorphologyEngine.translateSequence(listOf("MANANA", "YO", "AGUA", "QUERER"))
        assertEquals("Q'aya Ñuqa yakuta munani", result.quechuaSentence)
        assertEquals("Mañana Yo quiero agua", result.spanishSentence)
        assertEquals("Cjaya Ñocja yacuta munani", result.quechuaPhonetics)
    }

    @Test
    fun testNegationWithVerb() {
        // NO + YO + PAN + COMER -> Ñuqa t'antata mana mikhunichu / Yo no como pan
        val result = MorphologyEngine.translateSequence(listOf("NO", "YO", "PAN", "COMER"))
        assertEquals("Ñuqa t'antata mana mikhunichu", result.quechuaSentence)
        assertEquals("Yo no como pan", result.spanishSentence)
    }

    @Test
    fun testAllativeHospitalDestination() {
        // TU + HOSPITAL + IR -> Qam hampina wasiman rinki / Tú vas al hospital
        val result = MorphologyEngine.translateSequence(listOf("TU", "HOSPITAL", "IR"))
        assertEquals("Qam hampina wasiman rinki", result.quechuaSentence)
        assertEquals("Tú vas al hospital", result.spanishSentence)
    }

    @Test
    fun testMultimodalFusionEngineHighAccuracy() {
        // Probar el motor de fusión multimodal para AGUA con firma de guante y cámara
        val waterItem = MorphologyEngine.lexicon["AGUA"]!!
        val telemetry = bo.edu.lsb.quechua.model.GloveTelemetry(
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
        assertTrue("La confianza de fusión híbrida debe superar el 95%", result.confidence >= 0.95f)
    }

    @Test
    fun testHallEffectGloveTelemetryJsonParsing() {
        // Formato real enviado por el ESP32 con sensores Hall lineales SS49E y MPU-6050
        val esp32Json = """{"pulgar":12,"indice":85,"medio":92,"anular":90,"menique":88,"pitch":14.5,"roll":-3.2}"""
        val telemetry = bo.edu.lsb.quechua.model.GloveTelemetry.parseFromJson(esp32Json)

        org.junit.Assert.assertNotNull("La telemetría Hall no debe ser nula", telemetry)
        telemetry!!
        assertEquals(12, telemetry.thumbFlex)
        assertEquals(85, telemetry.indexFlex)
        assertEquals(92, telemetry.middleFlex)
        assertEquals(90, telemetry.ringFlex)
        assertEquals(88, telemetry.pinkyFlex)
        assertEquals(14.5, telemetry.imuPitch, 0.01)
        assertEquals(-3.2, telemetry.imuRoll, 0.01)
    }

    @Test
    fun testQuechuaPhoneticDialectalRules() {
        // 1. Saludo tradicional con sufijo interrogativo -chu -> -cho
        val greetingPhonetic = MorphologyEngine.adaptQuechuaPhonetics("Allillanchu")
        assertEquals("Allillancho", greetingPhonetic)

        // 2. Aspiradas 'h' al inicio de palabra convertidas a 'j' audible
        val hamuyPhonetic = MorphologyEngine.adaptQuechuaPhonetics("Hamuy wasiman")
        assertEquals("Jamuy guasiman", hamuyPhonetic)

        // 3. Eyectivas y post-velares uvulares
        val kachaPhonetic = MorphologyEngine.adaptQuechuaPhonetics("k'acha ch'arki")
        assertEquals("kjacha chjarki", kachaPhonetic)

        // 4. Alofonía uvular post-velar y lateral palatal
        val qullqiPhonetic = MorphologyEngine.adaptQuechuaPhonetics("Qullqi qucha")
        assertEquals("Cjolyqe cjocha", qullqiPhonetic)
    }

    @Test
    fun testExpandedLexiconWords() {
        val lexicon = MorphologyEngine.lexicon
        assertTrue("El léxico debe contener más de 100 señas", lexicon.size >= 100)
        assertTrue("Debe contener MAIZ (sara)", lexicon.containsKey("MAIZ"))
        assertTrue("Debe contener CHARQUE (ch'arki)", lexicon.containsKey("CHARQUE"))
        assertTrue("Debe contener MATE (hampi yaku)", lexicon.containsKey("MATE"))
        assertTrue("Debe contener ENFERMERA (hampiq warmi)", lexicon.containsKey("ENFERMERA"))
        assertTrue("Debe contener DORMIR (puñu)", lexicon.containsKey("DORMIR"))
        assertTrue("Debe contener SOL (inti)", lexicon.containsKey("SOL"))
        assertTrue("Debe contener ROJO (puka)", lexicon.containsKey("ROJO"))
    }

    @Test
    fun testExpandedSentenceComposition() {
        // YO + MAIZ + COMER -> Ñuqa sarata mikhuni / Yo como maíz / choclo
        val resultCorn = MorphologyEngine.translateSequence(listOf("YO", "MAIZ", "COMER"))
        assertEquals("Ñuqa sarata mikhuni", resultCorn.quechuaSentence)
        assertEquals("Yo como maíz / choclo", resultCorn.spanishSentence)

        // ELLA + DORMIR -> Pay puñun / Ella duerme
        val resultSleep = MorphologyEngine.translateSequence(listOf("ELLA", "DORMIR"))
        assertEquals("Pay puñun", resultSleep.quechuaSentence)
        assertEquals("Ella duerme", resultSleep.spanishSentence)
    }
}

