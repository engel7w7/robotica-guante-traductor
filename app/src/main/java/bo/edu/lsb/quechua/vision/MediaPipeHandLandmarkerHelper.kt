package bo.edu.lsb.quechua.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Gestor oficial de Google MediaPipe Hand Landmarker para captura real de los 21 landmarks
 * anatómicos en 3D en tiempo real desde fotogramas de CameraX.
 *
 * Calcula cinemática 3D real por dedo (Pulgar, Índice, Medio, Anular, Meñique en escala 0..100%),
 * inclinación vertical (handPitch) y lateral (tilt), detección de oscilación de saludo y más de 20
 * descriptores visuales compatibles con MultimodalFusionEngine.
 */
class MediaPipeHandLandmarkerHelper(
    private val context: Context,
    var isFrontCamera: Boolean = true,
    var minDetectionConfidence: Float = 0.5f,
    var minTrackingConfidence: Float = 0.5f,
    var minPresenceConfidence: Float = 0.5f,
    private val onResult: (HandLandmarksResult) -> Unit,
    private val onError: (String) -> Unit = {}
) : ImageAnalysis.Analyzer {

    private var handLandmarker: HandLandmarker? = null
    @Volatile
    private var isClosed = false

    private var frameCount = 0
    private var lastFpsTimestamp = SystemClock.uptimeMillis()
    private var currentFps = 30f

    @Volatile
    private var lastRotatedWidth = 0
    @Volatile
    private var lastRotatedHeight = 0

    // Suavizado temporal EMA para flexión real de los 5 dedos y pitch
    private var smoothedFlex: FloatArray? = null
    private var smoothedPitch: Float? = null

    // Historial breve de posición X de muñeca para detectar saludo oscilante (WAVE_HELLO)
    private val wristXHistory = ArrayDeque<Float>(10)

    init {
        setupHandLandmarker()
    }

    fun setupHandLandmarker() {
        try {
            val baseOptionsBuilder = BaseOptions.builder()
                .setModelAssetPath("hand_landmarker.task")
                .setDelegate(Delegate.CPU)

            val optionsBuilder = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptionsBuilder.build())
                .setMinHandDetectionConfidence(minDetectionConfidence)
                .setMinTrackingConfidence(minTrackingConfidence)
                .setMinHandPresenceConfidence(minPresenceConfidence)
                .setNumHands(2)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener { result: HandLandmarkerResult, _: MPImage ->
                    processResult(result)
                }
                .setErrorListener { error: RuntimeException ->
                    onError(error.message ?: "Error en MediaPipe HandLandmarker")
                }

            handLandmarker?.close()
            handLandmarker = HandLandmarker.createFromOptions(context, optionsBuilder.build())
        } catch (e: Exception) {
            onError("No se pudo inicializar MediaPipe: ${e.message}")
        }
    }

    override fun analyze(imageProxy: ImageProxy) {
        if (isClosed || handLandmarker == null) {
            imageProxy.close()
            return
        }

        val frameTime = SystemClock.uptimeMillis()

        frameCount++
        if (frameTime - lastFpsTimestamp >= 1000) {
            currentFps = (frameCount * 1000f) / (frameTime - lastFpsTimestamp)
            frameCount = 0
            lastFpsTimestamp = frameTime
        }

        try {
            val bitmap = imageProxy.toBitmap()
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees

            val matrix = Matrix().apply {
                postRotate(rotationDegrees.toFloat())
                if (isFrontCamera) {
                    postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
                }
            }

            val rotatedBitmap = Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )
            if (rotatedBitmap !== bitmap) {
                bitmap.recycle()
            }

            lastRotatedWidth = rotatedBitmap.width
            lastRotatedHeight = rotatedBitmap.height

            val mpImage = BitmapImageBuilder(rotatedBitmap).build()
            handLandmarker?.detectAsync(mpImage, frameTime)
        } catch (_: Exception) {
        } finally {
            imageProxy.close()
        }
    }

    private fun processResult(result: HandLandmarkerResult) {
        if (isClosed) return

        val landmarksList = result.landmarks()
        if (landmarksList.isNullOrEmpty()) {
            smoothedFlex = null
            smoothedPitch = null
            wristXHistory.clear()
            onResult(
                HandLandmarksResult(
                    landmarks = emptyList(),
                    isHandDetected = false,
                    gesture = "NEUTRAL",
                    openness = 0.5f,
                    tilt = 0f,
                    isFrontCamera = isFrontCamera,
                    confidence = 0f,
                    boundingBox = null,
                    fps = currentFps,
                    imageWidth = lastRotatedWidth,
                    imageHeight = lastRotatedHeight,
                    fingerFlex = listOf(0, 0, 0, 0, 0),
                    handPitch = 0f
                )
            )
            return
        }

        val handLandmarks = landmarksList[0]
        if (handLandmarks.size < 21) return

        var minX = 1.0f
        var maxX = 0.0f
        var minY = 1.0f
        var maxY = 0.0f

        val points = handLandmarks.map { lm ->
            val x = lm.x().coerceIn(0f, 1f)
            val y = lm.y().coerceIn(0f, 1f)
            val z = lm.z()

            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y

            LandmarkPoint(x, y, z)
        }

        // Proyección isotrópica en píxeles reales de la imagen para preservar geometría angular 3D
        val w = lastRotatedWidth.coerceAtLeast(1).toFloat()
        val h = lastRotatedHeight.coerceAtLeast(1).toFloat()

        fun dist3(a: LandmarkPoint, b: LandmarkPoint): Float {
            val dx = (a.x - b.x) * w
            val dy = (a.y - b.y) * h
            val dz = (a.z - b.z) * w
            return sqrt(dx * dx + dy * dy + dz * dz)
        }

        fun angleDeg(a: LandmarkPoint, b: LandmarkPoint, c: LandmarkPoint): Float {
            val v1x = (a.x - b.x) * w
            val v1y = (a.y - b.y) * h
            val v1z = (a.z - b.z) * w

            val v2x = (c.x - b.x) * w
            val v2y = (c.y - b.y) * h
            val v2z = (c.z - b.z) * w

            val mag1 = sqrt(v1x * v1x + v1y * v1y + v1z * v1z)
            val mag2 = sqrt(v2x * v2x + v2y * v2y + v2z * v2z)
            if (mag1 < 1e-4f || mag2 < 1e-4f) return 180f

            val cosTheta = ((v1x * v2x + v1y * v2y + v1z * v2z) / (mag1 * mag2)).coerceIn(-1f, 1f)
            return Math.toDegrees(acos(cosTheta.toDouble())).toFloat()
        }

        val wrist = points[0]
        val palmScale = dist3(wrist, points[9]).coerceAtLeast(1f)

        // Cálculo anatómico 3D de flexión [0..100] para los 4 dedos largos
        fun computeLongFingerFlex(mcpIdx: Int, pipIdx: Int, dipIdx: Int, tipIdx: Int): Float {
            val angMcp = angleDeg(wrist, points[mcpIdx], points[pipIdx])
            val angPip = angleDeg(points[mcpIdx], points[pipIdx], points[dipIdx])
            val angDip = angleDeg(points[pipIdx], points[dipIdx], points[tipIdx])

            val weightedAngle = 0.25f * angMcp + 0.50f * angPip + 0.25f * angDip
            val angleBend = ((168f - weightedAngle) / (168f - 78f) * 100f).coerceIn(0f, 100f)

            val tipWristRatio = dist3(points[tipIdx], wrist) / dist3(points[mcpIdx], wrist).coerceAtLeast(1f)
            val ratioBend = ((1.90f - tipWristRatio) / (1.90f - 0.85f) * 100f).coerceIn(0f, 100f)

            return (angleBend * 0.70f + ratioBend * 0.30f).coerceIn(0f, 100f)
        }

        // Cálculo 3D para Pulgar: mezcla ángulo interfalángico/MCP + abducción hacia base de meñique (17)
        val thumbAng1 = angleDeg(points[1], points[2], points[3])
        val thumbAng2 = angleDeg(points[2], points[3], points[4])
        val thumbAngleFlex = ((168f - (0.45f * thumbAng1 + 0.55f * thumbAng2)) / (168f - 112f) * 100f).coerceIn(0f, 100f)
        val thumbAbductionRatio = dist3(points[4], points[17]) / palmScale
        val thumbAbductionFlex = ((1.42f - thumbAbductionRatio) / (1.42f - 0.58f) * 100f).coerceIn(0f, 100f)
        val rawThumb = (thumbAngleFlex * 0.45f + thumbAbductionFlex * 0.55f).coerceIn(0f, 100f)

        val rawFlex = floatArrayOf(
            rawThumb,
            computeLongFingerFlex(5, 6, 7, 8),
            computeLongFingerFlex(9, 10, 11, 12),
            computeLongFingerFlex(13, 14, 15, 16),
            computeLongFingerFlex(17, 18, 19, 20)
        )

        // Filtro EMA temporal sobre flexión 0..100
        val alpha = 0.45f
        val curSmoothed = smoothedFlex
        val nextSmoothed = if (curSmoothed == null) {
            rawFlex
        } else {
            FloatArray(5) { idx -> alpha * rawFlex[idx] + (1f - alpha) * curSmoothed[idx] }
        }
        smoothedFlex = nextSmoothed
        val fingerFlexInt = nextSmoothed.map { it.roundToInt().coerceIn(0, 100) }

        // Inclinación lateral en plano (tilt) y elevación vertical 3D (handPitch)
        val dxPalm = (points[9].x - wrist.x) * w
        val dyPalmUp = -(points[9].y - wrist.y) * h // Positivo hacia arriba
        val dzPalmCam = -(points[9].z - wrist.z) * w

        val tiltAngle = Math.toDegrees(atan2(dxPalm.toDouble(), dyPalmUp.toDouble()))
            .toFloat().coerceIn(-85f, 85f)

        val rawPitch = Math.toDegrees(
            atan2(dyPalmUp.toDouble(), hypot(dxPalm.toDouble(), dzPalmCam.toDouble() * 0.65))
        ).toFloat().coerceIn(-85f, 85f)
        val nextPitch = smoothedPitch?.let { 0.45f * rawPitch + 0.55f * it } ?: rawPitch
        smoothedPitch = nextPitch

        // Seguimiento de oscilación horizontal para detección de saludo ("WAVE_HELLO")
        if (wristXHistory.size >= 8) wristXHistory.removeFirst()
        wristXHistory.addLast(wrist.x)
        val xOscillation = if (wristXHistory.size >= 5) {
            (wristXHistory.maxOrNull() ?: wrist.x) - (wristXHistory.minOrNull() ?: wrist.x)
        } else 0f

        val opennessRatio = (1.0f - fingerFlexInt.average().toFloat() / 100f).coerceIn(0.05f, 1.0f)
        val pinchThumbIndex = dist3(points[4], points[8]) / palmScale
        val centerY = (minY + maxY) / 2f

        val thumbBent = fingerFlexInt[0] >= 52
        val idxBent = fingerFlexInt[1] >= 52
        val midBent = fingerFlexInt[2] >= 52
        val ringBent = fingerFlexInt[3] >= 52
        val pnkBent = fingerFlexInt[4] >= 52

        val detectedGesture = when {
            xOscillation > 0.085f && !idxBent && !midBent && !ringBent -> "WAVE_HELLO"
            pinchThumbIndex < 0.42f && !midBent && !ringBent && !pnkBent -> "OK_PINCH"
            pinchThumbIndex < 0.42f && ringBent && pnkBent -> "PINCH_SMALL"
            !thumbBent && idxBent && midBent && ringBent && !pnkBent -> "PLAY_SHAKE"
            !thumbBent && !idxBent && !midBent && !ringBent && !pnkBent && nextPitch >= 35f -> "OPEN_HAND_FIVE"
            !thumbBent && !idxBent && !midBent && ringBent && pnkBent -> "THREE_FINGERS"
            thumbBent && !idxBent && !midBent && !ringBent && !pnkBent -> "FOUR_FINGERS"
            thumbBent && !idxBent && !midBent && !ringBent && pnkBent -> "THREE_FINGERS"
            thumbBent && !idxBent && !midBent && ringBent && pnkBent && nextPitch < -25f -> "TWO_FINGERS_WALK"
            thumbBent && !idxBent && !midBent && ringBent && pnkBent -> "TWO_VICTORY"
            !idxBent && midBent && ringBent && pnkBent && nextPitch < -35f -> "POINT_DOWN"
            thumbBent && !idxBent && midBent && ringBent && pnkBent && nextPitch >= 35f -> "INDEX_ONE"
            !thumbBent && !idxBent && midBent && ringBent && pnkBent -> "L_SHAPE"
            !idxBent && midBent && ringBent && !pnkBent -> "HORNS_SIGN"
            thumbBent && idxBent && midBent && ringBent && !pnkBent -> "PINKY_ONLY"
            !idxBent && midBent && ringBent && pnkBent -> "INDEX_POINT"
            !thumbBent && idxBent && midBent && ringBent && pnkBent -> "THUMB_UP"
            thumbBent && idxBent && midBent && ringBent && pnkBent -> "FIST"
            fingerFlexInt[0] in 20..52 && fingerFlexInt[1] in 15..52 && fingerFlexInt[2] in 15..52 && ringBent && pnkBent -> "CUP_HAND"
            minY < 0.22f -> "TOUCH_HEAD"
            centerY in 0.62f..0.90f && (maxX - minX) < 0.36f -> "TOUCH_CHEST"
            !idxBent && !midBent && !ringBent && abs(tiltAngle) >= 35f -> "PALMS_UP_SIDE"
            else -> "OPEN_PALM"
        }

        val bbox = NormalizedRect(minX, minY, maxX, maxY)

        onResult(
            HandLandmarksResult(
                landmarks = points,
                isHandDetected = true,
                gesture = detectedGesture,
                openness = opennessRatio,
                tilt = tiltAngle,
                isFrontCamera = isFrontCamera,
                confidence = 0.98f,
                boundingBox = bbox,
                fps = currentFps,
                imageWidth = lastRotatedWidth,
                imageHeight = lastRotatedHeight,
                fingerFlex = fingerFlexInt,
                handPitch = nextPitch
            )
        )
    }

    fun close() {
        isClosed = true
        handLandmarker?.close()
        handLandmarker = null
    }
}
