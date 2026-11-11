package bo.edu.lsb.quechua.model

/**
 * Representa una entrada léxica con sus atributos morfosintácticos
 * para los motores generadores de Español (SVO) y Quechua Boliviano (SOV con declinaciones).
 */
data class MorphologyItem(
    val gloss: String,
    val type: String, // PRON, SUST, CORP, V_TR, V_MV, V_IT, V_AF, SAL, ADV, NUM, ADJ, LOC, NEG, INT
    val quechuaStem: String,
    val spanishDefault: String,
    val spanish1Sg: String? = null,
    val spanish2Sg: String? = null,
    val spanish3Sg: String? = null,
    val spanish1Pl: String? = null,
    val person: String? = null, // "1SG", "2SG", "3SG", "1PL_INC", "1PL_EXC", "2PL", "3PL"
    val categoryLabel: String = "",
    val description: String = "",
    val expectedFlex: List<Int> = listOf(50, 50, 50, 50, 50),
    val expectedImu: List<Double> = listOf(0.0, 0.0),
    val expectedTouch: List<Int> = listOf(0, 0, 0),
    val visionDescriptor: String = "NEUTRAL"
)
