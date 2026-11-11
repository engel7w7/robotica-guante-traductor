package bo.edu.lsb.quechua.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Matriz de Pruebas Morfosintácticas Bilingües (Español SVO <-> Quechua Boliviano SOV).
 * Verifica la concordancia de persona gramatical, sufijos de declinación andinos y negación.
 */
class BilingualGrammarMatrixTest {

    @Test
    fun testConjugationAcrossAllGrammaticalPersons() {
        // 1ra Persona Singular (1SG): -ni / Yo como
        val p1 = MorphologyEngine.translateSequence(listOf("YO", "PAN", "COMER"))
        assertEquals("Ñuqa t'antata mikhuni", p1.quechuaSentence)
        assertEquals("Yo como pan", p1.spanishSentence)

        // 2da Persona Singular (2SG): -nki / Tú comes
        val p2 = MorphologyEngine.translateSequence(listOf("TU", "PAN", "COMER"))
        assertEquals("Qam t'antata mikhunki", p2.quechuaSentence)
        assertEquals("Tú comes pan", p2.spanishSentence)

        // 3ra Persona Singular (3SG): -n / Él come
        val p3 = MorphologyEngine.translateSequence(listOf("EL", "PAN", "COMER"))
        assertEquals("Pay t'antata mikhun", p3.quechuaSentence)
        assertEquals("Él come pan", p3.spanishSentence)

        // 1ra Persona Plural Inclusiva (1PL_INC): -nchik / Nosotros comemos
        val p4 = MorphologyEngine.translateSequence(listOf("NOSOTROS", "PAN", "COMER"))
        assertEquals("Ñuqanchik t'antata mikhunchik", p4.quechuaSentence)
        assertEquals("Nosotros comemos pan", p4.spanishSentence)

        // 2da Persona Plural (2PL): -nkichik / Ustedes comen
        val p5 = MorphologyEngine.translateSequence(listOf("USTEDES", "PAN", "COMER"))
        assertEquals("Qamkuna t'antata mikhunkichik", p5.quechuaSentence)
        assertEquals("Ustedes comen pan", p5.spanishSentence)

        // 3ra Persona Plural (3PL): -nku / Ellos comen
        val p6 = MorphologyEngine.translateSequence(listOf("ELLOS", "PAN", "COMER"))
        assertEquals("Paykuna t'antata mikhunku", p6.quechuaSentence)
        assertEquals("Ellos comen pan", p6.spanishSentence)
    }

    @Test
    fun testMotionVerbWithAllativeMan() {
        // Destino Casa: wasiman
        val r1 = MorphologyEngine.translateSequence(listOf("YO", "CASA", "IR"))
        assertEquals("Ñuqa wasiman rini", r1.quechuaSentence)
        assertEquals("Yo voy a la casa", r1.spanishSentence)

        // Destino Hospital: hampina wasiman
        val r2 = MorphologyEngine.translateSequence(listOf("TU", "HOSPITAL", "IR"))
        assertEquals("Qam hampina wasiman rinki", r2.quechuaSentence)
        assertEquals("Tú vas al hospital", r2.spanishSentence)
    }

    @Test
    fun testBodilyAffectionDolerFormulas() {
        // Dolor de Cabeza: umay nanawan
        val r1 = MorphologyEngine.translateSequence(listOf("CABEZA", "DOLER"))
        assertEquals("umay nanawan", r1.quechuaSentence)
        assertEquals("Me duele la cabeza", r1.spanishSentence)

        // Dolor de Estómago: wisay nanawan
        val r2 = MorphologyEngine.translateSequence(listOf("ESTOMAGO", "DOLER"))
        assertEquals("wisay nanawan", r2.quechuaSentence)
        assertEquals("Me duele el estómago", r2.spanishSentence)

        // Negación de dolor: umay mana nanawanchu
        val r3 = MorphologyEngine.translateSequence(listOf("NO", "CABEZA", "DOLER"))
        assertEquals("umay mana nanawanchu", r3.quechuaSentence)
        assertEquals("No me duele la cabeza", r3.spanishSentence)
    }

    @Test
    fun testSocialFormulasAndInterrogatives() {
        val hola = MorphologyEngine.translateSequence(listOf("HOLA"))
        assertEquals("Allillanchu", hola.quechuaSentence)
        assertEquals("Hola, ¿cómo estás?", hola.spanishSentence)

        val gracias = MorphologyEngine.translateSequence(listOf("GRACIAS"))
        assertEquals("Yusulpayki", gracias.quechuaSentence)
        assertEquals("Muchas gracias", gracias.spanishSentence)

        val chau = MorphologyEngine.translateSequence(listOf("CHAU"))
        assertEquals("Tupananchiskama", chau.quechuaSentence)
        assertEquals("Hasta luego / Adiós", chau.spanishSentence)

        val si = MorphologyEngine.translateSequence(listOf("SI"))
        assertEquals("Arí", si.quechuaSentence)
        assertEquals("Sí", si.spanishSentence)

        val no = MorphologyEngine.translateSequence(listOf("NO"))
        assertEquals("Mana", no.quechuaSentence)
        assertEquals("No", no.spanishSentence)

        val donde = MorphologyEngine.translateSequence(listOf("DONDE"))
        assertEquals("Maypi", donde.quechuaSentence)
        assertEquals("¿Dónde?", donde.spanishSentence)
    }

    @Test
    fun testNegationRuleManaWithInterrogativeChu() {
        // YO + AGUA + NO + QUERER -> Ñuqa yakuta mana munanichu / Yo no quiero agua
        val r = MorphologyEngine.translateSequence(listOf("YO", "AGUA", "NO", "QUERER"))
        assertEquals("Ñuqa yakuta mana munanichu", r.quechuaSentence)
        assertEquals("Yo no quiero agua", r.spanishSentence)
        assertTrue(r.ruleExplanation.contains("mana ... -chu"))
    }

    @Test
    fun testAffectivePainAcrossGrammaticalPersons() {
        val tuPain = MorphologyEngine.translateSequence(listOf("TU", "CABEZA", "DOLER"))
        assertEquals("umayki nanasunki", tuPain.quechuaSentence)
        assertEquals("Te duele la cabeza", tuPain.spanishSentence)

        val elPain = MorphologyEngine.translateSequence(listOf("EL", "ESTOMAGO", "DOLER"))
        assertEquals("wisan nanan", elPain.quechuaSentence)
        assertEquals("Le duele el estómago", elPain.spanishSentence)

        val nosPain = MorphologyEngine.translateSequence(listOf("NOSOTROS", "CABEZA", "DOLER"))
        assertEquals("umanchik nanawanchik", nosPain.quechuaSentence)
        assertEquals("Nos duele la cabeza", nosPain.spanishSentence)

        val tuNoPain = MorphologyEngine.translateSequence(listOf("NO", "TU", "CABEZA", "DOLER"))
        assertEquals("umayki mana nanasunkichu", tuNoPain.quechuaSentence)
        assertEquals("No te duele la cabeza", tuNoPain.spanishSentence)
    }

    @Test
    fun testModifiersCopulaQuestionsAndInputNormalization() {
        // Modificadores numéricos y calificativos en ambos idiomas
        val twoBread = MorphologyEngine.translateSequence(listOf("YO", "DOS", "PAN", "COMER"))
        assertEquals("Ñuqa iskay t'antata mikhuni", twoBread.quechuaSentence)
        assertEquals("Yo como dos pan", twoBread.spanishSentence)

        val bigBread = MorphologyEngine.translateSequence(listOf("YO", "PAN", "GRANDE", "COMER"))
        assertEquals("Ñuqa hatun t'antata mikhuni", bigBread.quechuaSentence)
        assertEquals("Yo como pan grande", bigBread.spanishSentence)

        // Verbo copulativo ESTAR con emoción / adjetivo y plural
        val happy = MorphologyEngine.translateSequence(listOf("YO", "FELIZ", "ESTAR"))
        assertEquals("Ñuqa kusisqa kani", happy.quechuaSentence)
        assertEquals("Yo estoy feliz", happy.spanishSentence)

        val pluralHappy = MorphologyEngine.translateSequence(listOf("USTEDES", "FELIZ", "ESTAR"))
        assertEquals("Qamkuna kusisqa kankichik", pluralHappy.quechuaSentence)
        assertEquals("Ustedes están feliz", pluralHappy.spanishSentence)

        // Lugares con preposición limpia (al mercado / en el mercado)
        val goMarket = MorphologyEngine.translateSequence(listOf("YO", "MERCADO", "IR"))
        assertEquals("Ñuqa qhatuman rini", goMarket.quechuaSentence)
        assertEquals("Yo voy al mercado", goMarket.spanishSentence)

        val atMarket = MorphologyEngine.translateSequence(listOf("YO", "MERCADO", "ESTAR"))
        assertEquals("Ñuqa qhatupi kani", atMarket.quechuaSentence)
        assertEquals("Yo estoy en el mercado", atMarket.spanishSentence)

        // Raíz ENTENDER corregida (jap'iqa + -ni)
        val understand = MorphologyEngine.translateSequence(listOf("YO", "ENTENDER"))
        assertEquals("Ñuqa jap'iqani", understand.quechuaSentence)
        assertEquals("Yo entiendo", understand.spanishSentence)

        // Preguntas multi-glosa sin signos ? huérfanos en medio
        val whereHosp = MorphologyEngine.translateSequence(listOf("DONDE", "HOSPITAL"))
        assertEquals("Maypi hampina wasi", whereHosp.quechuaSentence)
        assertEquals("¿Dónde hospital?", whereHosp.spanishSentence)

        val whereGo = MorphologyEngine.translateSequence(listOf("DONDE", "TU", "IR"))
        assertEquals("Maypi Qam rinki", whereGo.quechuaSentence)
        assertEquals("¿Dónde Tú vas?", whereGo.spanishSentence)

        // Normalización de entradas minúsculas / espacios / NINGUNA
        val normalized = MorphologyEngine.translateSequence(listOf(" yo ", "NINGUNA", "agua", "", " querer "))
        assertEquals("Ñuqa yakuta munani", normalized.quechuaSentence)
        assertEquals("Yo quiero agua", normalized.spanishSentence)
    }
}
