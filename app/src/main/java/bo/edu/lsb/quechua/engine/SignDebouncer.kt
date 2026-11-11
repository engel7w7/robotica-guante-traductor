package bo.edu.lsb.quechua.engine

/**
 * Filtro de Estabilización de Señas (Debouncer).
 *
 * Acumula lecturas consecutivas idénticas antes de validar la inserción de una glosa
 * en la oración. Con [maxGlitchFrames] > 0 tolera breves micro-cortes (1 frame de ruido)
 * sin reiniciar la barra de progreso, mientras conserva [maxGlitchFrames] = 0 estricto por defecto.
 */
class SignDebouncer(
    val threshold: Int = 12,
    val maxGlitchFrames: Int = 0,
    private val onSignAccepted: (String) -> Unit
) {
    var candidateGloss: String = "NINGUNA"
        private set

    var stableCount: Int = 0
        private set

    private var glitchCount: Int = 0

    val stabilityProgress: Float
        get() = if (threshold > 0) (stableCount.toFloat() / threshold.toFloat()).coerceIn(0f, 1f) else 0f

    /**
     * Procesa una lectura entrante desde el motor de clasificación multimodal.
     */
    fun processReading(incomingGloss: String) {
        val trimmed = incomingGloss.trim().uppercase()

        if (trimmed.isNotBlank() && trimmed != "NINGUNA") {
            glitchCount = 0
            if (trimmed == candidateGloss) {
                stableCount++
                if (stableCount == threshold) {
                    onSignAccepted(trimmed)
                }
            } else {
                candidateGloss = trimmed
                stableCount = 1
                if (stableCount == threshold) {
                    onSignAccepted(trimmed)
                }
            }
        } else {
            if (maxGlitchFrames > 0 && candidateGloss != "NINGUNA" && glitchCount < maxGlitchFrames) {
                glitchCount++
            } else {
                stableCount = 0
                candidateGloss = "NINGUNA"
                glitchCount = 0
            }
        }
    }

    /**
     * Reinicia el acumulador de estabilidad.
     */
    fun reset() {
        stableCount = 0
        candidateGloss = "NINGUNA"
        glitchCount = 0
    }
}
