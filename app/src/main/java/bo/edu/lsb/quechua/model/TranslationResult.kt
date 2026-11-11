package bo.edu.lsb.quechua.model

/**
 * Representa el resultado generado por el motor morfosintáctico
 * a partir de la secuencia acumulada de glosas reconocidas.
 */
data class TranslationResult(
    val sequenceGlosses: List<String> = emptyList(),
    val spanishSentence: String = "Esperando señas...",
    val quechuaSentence: String = "Suyachkan...",
    val quechuaPhonetics: String = "",
    val ruleExplanation: String = "Sintaxis: Esperando detección de sujeto, objeto y verbo"
) {
    val isEmpty: Boolean get() = sequenceGlosses.isEmpty()
    val formattedGlossSequence: String get() = if (sequenceGlosses.isEmpty()) "NINGUNA" else sequenceGlosses.joinToString(" + ")
}
