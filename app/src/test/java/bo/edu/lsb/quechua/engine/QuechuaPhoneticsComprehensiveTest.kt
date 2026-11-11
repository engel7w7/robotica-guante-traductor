package bo.edu.lsb.quechua.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Suite Exhaustiva de Validación Fonética y Pronunciación para Quechua Boliviano (Collao/Sureño).
 * Verifica que las transformaciones fonológicas sean acústicamente naturales en motores TTS Android.
 */
class QuechuaPhoneticsComprehensiveTest {

    @Test
    fun testUvularVowelLoweringAllophony() {
        // En Quechua Sureño/Collao, las vocales altas /i, u/ en contacto con la post-velar /q/
        // se abren acústicamente a [e, o] (alofonía uvular).
        val sunqu = MorphologyEngine.adaptQuechuaPhonetics("sunqu")
        assertEquals("soncjo", sunqu)

        val qucha = MorphologyEngine.adaptQuechuaPhonetics("qucha")
        assertEquals("cjocha", qucha)

        val uqyana = MorphologyEngine.adaptQuechuaPhonetics("uqyana")
        assertEquals("ocjyana", uqyana)

        val allillanchu = MorphologyEngine.adaptQuechuaPhonetics("Allillanchu")
        assertEquals("Allillancho", allillanchu)

        val manachu = MorphologyEngine.adaptQuechuaPhonetics("manachu")
        assertEquals("manacho", manachu)
    }

    @Test
    fun testInitialAspiratedHToJ() {
        // La 'h' quechua es una fricativa glotal sorda [h] (como en inglés 'house').
        // Para que el sintetizador español (es-BO) no la ignore como 'h' muda, se transcribe a 'j'.
        val hamuy = MorphologyEngine.adaptQuechuaPhonetics("Hamuy")
        assertEquals("Jamuy", hamuy)

        val hampi = MorphologyEngine.adaptQuechuaPhonetics("hampi")
        assertEquals("jampi", hampi)

        val hatun = MorphologyEngine.adaptQuechuaPhonetics("hatun")
        assertEquals("jatun", hatun)

        val hampiq = MorphologyEngine.adaptQuechuaPhonetics("hampiq")
        assertEquals("jampicj", hampiq)
    }

    @Test
    fun testEjectivesAndAspiratesAcousticBurst() {
        // Eyectivas con estallido plosivo: t' -> tj, k' -> kj, p' -> pj, ch' -> chj
        assertEquals("tjanta", MorphologyEngine.adaptQuechuaPhonetics("t'anta"))
        assertEquals("kjacha", MorphologyEngine.adaptQuechuaPhonetics("k'acha"))
        assertEquals("chjarki", MorphologyEngine.adaptQuechuaPhonetics("ch'arki"))
        assertEquals("pjacha", MorphologyEngine.adaptQuechuaPhonetics("p'acha"))

        // Oclusivas aspiradas: qh -> cjh, kh -> cjh, th -> tj, ph -> pj, chh -> chjh
        assertEquals("cjhagua", MorphologyEngine.adaptQuechuaPhonetics("qhawa"))
        assertEquals("micjuna", MorphologyEngine.adaptQuechuaPhonetics("mikhuna"))
        assertEquals("tjani", MorphologyEngine.adaptQuechuaPhonetics("thani"))
    }

    @Test
    fun testBilabialSemivowelWToGu() {
        // La semiconsonante /w/ antes de vocal se pronuncia [w], transcrita como 'gu' para es-BO
        assertEquals("guasi", MorphologyEngine.adaptQuechuaPhonetics("wasi"))
        assertEquals("guagua", MorphologyEngine.adaptQuechuaPhonetics("wawa"))
        assertEquals("guajqe", MorphologyEngine.adaptQuechuaPhonetics("wawqi"))
    }

    @Test
    fun testEveryWordInLexiconHasValidPhoneticOutput() {
        val lexicon = MorphologyEngine.lexicon
        assertTrue("El léxico debe contener al menos 100 palabras", lexicon.size >= 100)

        for ((gloss, item) in lexicon) {
            val stem = item.quechuaStem
            assertFalse("El stem no puede estar vacío para la glosa $gloss", stem.isBlank())

            val phonetic = MorphologyEngine.adaptQuechuaPhonetics(stem)
            assertNotNull("La fonética no debe ser nula para $gloss", phonetic)
            assertFalse("La fonética no debe ser vacía para $gloss", phonetic.isBlank())

            // Verificar que no queden caracteres de escape residuales
            assertFalse("No debe contener comillas dobles residuales: $phonetic", phonetic.contains("\""))
            assertFalse("No debe contener caracteres nulos: $phonetic", phonetic.contains("\u0000"))
        }
    }

    @Test
    fun testBilingualFullSentencesPhonetics() {
        // Frase 1: Yo quiero agua
        val r1 = MorphologyEngine.translateSequence(listOf("YO", "AGUA", "QUERER"))
        assertEquals("Ñocja yacuta munani", r1.quechuaPhonetics)

        // Frase 2: Tú quieres pan
        val r2 = MorphologyEngine.translateSequence(listOf("TU", "PAN", "QUERER"))
        assertEquals("Cjam tjantata munanki", r2.quechuaPhonetics)

        // Frase 3: Yo voy al médico
        val r3 = MorphologyEngine.translateSequence(listOf("YO", "MEDICO", "IR"))
        assertEquals("Ñocja jampicjman rini", r3.quechuaPhonetics)

        // Frase 4: Mañana yo quiero agua
        val r4 = MorphologyEngine.translateSequence(listOf("MANANA", "YO", "AGUA", "QUERER"))
        assertEquals("Cjaya Ñocja yacuta munani", r4.quechuaPhonetics)
    }

    @Test
    fun testTtsPhoneticSpeechNaturalAndeanPronunciation() {
        // Acentuación paroxítona automática y ortografía española 100% pronunciable sin dígrafos cortados
        assertEquals("Jámuy", MorphologyEngine.toTtsPhoneticSpeech("Hamuy"))
        assertEquals("Yanapáguay", MorphologyEngine.toTtsPhoneticSpeech("Yanapaway"))
        assertEquals("Pampacháguay", MorphologyEngine.toTtsPhoneticSpeech("Pampachaway"))
        assertEquals("Ñokánchik", MorphologyEngine.toTtsPhoneticSpeech("Ñuqanchik"))
        assertEquals("Júchuy", MorphologyEngine.toTtsPhoneticSpeech("Huch'uy"))
        assertEquals("Jámpij", MorphologyEngine.toTtsPhoneticSpeech("Hampiq"))
        assertEquals("Jampijman rini", MorphologyEngine.toTtsPhoneticSpeech("Hampiqman rini"))
        assertEquals("Púnchau", MorphologyEngine.toTtsPhoneticSpeech("P'unchaw"))
        assertEquals("Punchaupi", MorphologyEngine.toTtsPhoneticSpeech("P'unchawpi"))
        assertEquals("Arí", MorphologyEngine.toTtsPhoneticSpeech("Arí"))
        assertEquals("güisa", MorphologyEngine.toTtsPhoneticSpeech("wisa"))
        assertEquals("güísay", MorphologyEngine.toTtsPhoneticSpeech("wisay"))
        assertEquals("onkoska", MorphologyEngine.toTtsPhoneticSpeech("onqosqa"))
        assertEquals("¿Allillancho?", MorphologyEngine.toTtsPhoneticSpeech("Allillanchu"))
        assertEquals("Allillanmi", MorphologyEngine.toTtsPhoneticSpeech("Allillanmi"))
        assertEquals("Yusulpáyki", MorphologyEngine.toTtsPhoneticSpeech("Yusulpayki"))
    }

    @Test
    fun testTtsPhoneticSpeechEveryLexiconStemHasNaturalSpanishOrthography() {
        val forbiddenTtsDigraphs = listOf("cj", "tj", "pj", "kj", "ly")
        for ((gloss, item) in MorphologyEngine.lexicon) {
            val tts = MorphologyEngine.toTtsPhoneticSpeech(item.quechuaStem)
            assertFalse("TTS vacío para $gloss", tts.isBlank())
            val lower = tts.lowercase()
            for (digraph in forbiddenTtsDigraphs) {
                assertFalse(
                    "TTS de $gloss ('${item.quechuaStem}' -> '$tts') no debe contener dígrafo artificial '$digraph'",
                    lower.contains(digraph)
                )
            }
        }
    }

    @Test
    fun testNaturalTtsPronunciationForCorrectedStemsAndBodyParts() {
        assertEquals("Pókoy", MorphologyEngine.toTtsPhoneticSpeech("Puquy"))
        assertEquals("pókoy", MorphologyEngine.toTtsPhoneticSpeech("puquy"))
        assertEquals("pokoyta", MorphologyEngine.toTtsPhoneticSpeech("puquyta"))

        assertEquals("japékay", MorphologyEngine.toTtsPhoneticSpeech("jap'iqay"))
        assertEquals("Ñoka japekani", MorphologyEngine.toTtsPhoneticSpeech("Ñuqa jap'iqani"))
        assertEquals("japekánchik", MorphologyEngine.toTtsPhoneticSpeech("jap'iqanchik"))

        assertEquals("úmay", MorphologyEngine.toTtsPhoneticSpeech("umay"))
        assertEquals("yáchay guasi", MorphologyEngine.toTtsPhoneticSpeech("yachay wasi"))
        assertEquals("ískay", MorphologyEngine.toTtsPhoneticSpeech("iskay"))
    }

    @Test
    fun testTtsPhoneticSpeechInflectedFormsAcrossAll146Stems() {
        val forbiddenDigraphs = listOf("cj", "tj", "pj", "kj", "ly")
        for ((gloss, item) in MorphologyEngine.lexicon) {
            val forms = when (item.type) {
                "V_TR", "V_MV", "V_IT" -> listOf(
                    item.quechuaStem,
                    "${item.quechuaStem}ni",
                    "${item.quechuaStem}nki",
                    "${item.quechuaStem}n",
                    "${item.quechuaStem}nchik"
                )
                "SUST" -> listOf(item.quechuaStem, "${item.quechuaStem}ta")
                "LOC" -> listOf(item.quechuaStem, "${item.quechuaStem}man", "${item.quechuaStem}pi")
                "CORP" -> listOf(item.quechuaStem, "${item.quechuaStem}y", "${item.quechuaStem}yki")
                else -> listOf(item.quechuaStem)
            }

            for (form in forms) {
                val tts = MorphologyEngine.toTtsPhoneticSpeech(form)
                assertFalse("TTS vacío para $gloss ('$form')", tts.isBlank())
                val lower = tts.lowercase()
                assertFalse("No debe quedar 'q' cruda en TTS de $gloss ('$form' -> '$tts')", lower.contains("q"))
                assertFalse("No debe quedar apóstrofe en TTS de $gloss ('$form' -> '$tts')", tts.contains("'") || tts.contains("’"))
                assertFalse(
                    "No debe quedar 'h' muda inicial tras espacio/inicio en TTS de $gloss ('$form' -> '$tts')",
                    Regex("\\bh[aeiouáéíóú]").containsMatchIn(lower)
                )
                for (d in forbiddenDigraphs) {
                    assertFalse(
                        "No debe quedar dígrafo '$d' en TTS de $gloss ('$form' -> '$tts')",
                        lower.contains(d)
                    )
                }
            }
        }
    }
}
