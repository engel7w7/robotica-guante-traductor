package bo.edu.lsb.quechua.vision

/**
 * Representa una coordenada anatómica normalizada [0.0, 1.0] de la mano.
 */
data class LandmarkPoint(
    val x: Float,
    val y: Float,
    val z: Float = 0f,
    val visibility: Float = 1.0f
)

/**
 * Resultado completo del análisis y seguimiento de landmarks de mano en tiempo real.
 */
data class HandLandmarksResult(
    val landmarks: List<LandmarkPoint> = emptyList(),
    val isHandDetected: Boolean = false,
    val gesture: String = "NEUTRAL",
    val openness: Float = 0.5f,
    val tilt: Float = 0f,
    val isFrontCamera: Boolean = true,
    val confidence: Float = 0f,
    val boundingBox: NormalizedRect? = null,
    val fps: Float = 30f,
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val fingerFlex: List<Int> = listOf(0, 0, 0, 0, 0),
    val handPitch: Float = 0f
) {
    companion object {
        fun default(): HandLandmarksResult = HandLandmarksResult()
    }
}

/**
 * Rectángulo normalizado [0..1] para el área de encuadre de la mano detectada.
 */
data class NormalizedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

/**
 * Topología de las 21 conexiones esqueléticas estándar de la mano humana.
 */
object HandSkeletonTopology {
    // 21 puntos anatómicos estándar (MediaPipe Hand Topology)
    val CONNECTIONS = listOf(
        // Palma y muñeca
        Pair(0, 1),
        Pair(0, 5),
        Pair(5, 9),
        Pair(9, 13),
        Pair(13, 17),
        Pair(0, 17),

        // Pulgar (1..4)
        Pair(1, 2),
        Pair(2, 3),
        Pair(3, 4),

        // Índice (5..8)
        Pair(5, 6),
        Pair(6, 7),
        Pair(7, 8),

        // Dedo Medio (9..12)
        Pair(9, 10),
        Pair(10, 11),
        Pair(11, 12),

        // Anular (13..16)
        Pair(13, 14),
        Pair(14, 15),
        Pair(15, 16),

        // Meñique (17..20)
        Pair(17, 18),
        Pair(18, 19),
        Pair(19, 20)
    )

    val THUMB_INDICES = setOf(1, 2, 3, 4)
    val INDEX_INDICES = setOf(5, 6, 7, 8)
    val MIDDLE_INDICES = setOf(9, 10, 11, 12)
    val RING_INDICES = setOf(13, 14, 15, 16)
    val PINKY_INDICES = setOf(17, 18, 19, 20)
    val WRIST_INDEX = 0

    val FINGERTIP_INDICES = listOf(4, 8, 12, 16, 20)
}
