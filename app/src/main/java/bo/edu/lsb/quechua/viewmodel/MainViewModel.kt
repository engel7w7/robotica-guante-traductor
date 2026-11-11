package bo.edu.lsb.quechua.viewmodel

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import bo.edu.lsb.quechua.audio.TextToSpeechManager
import bo.edu.lsb.quechua.bluetooth.BleGloveManager
import bo.edu.lsb.quechua.bluetooth.ScannedBleDevice
import bo.edu.lsb.quechua.engine.GloveVocabScope
import bo.edu.lsb.quechua.engine.MorphologyEngine
import bo.edu.lsb.quechua.engine.MultimodalFusionEngine
import bo.edu.lsb.quechua.engine.MultimodalMode
import bo.edu.lsb.quechua.engine.RecognitionResult
import bo.edu.lsb.quechua.engine.SignDebouncer
import bo.edu.lsb.quechua.model.ConnectionState
import bo.edu.lsb.quechua.model.CustomGloveProfileStore
import bo.edu.lsb.quechua.model.CustomGloveSignature
import bo.edu.lsb.quechua.model.FingerCalibration
import bo.edu.lsb.quechua.model.GloveCalibrationProfile
import bo.edu.lsb.quechua.model.GloveTelemetry
import bo.edu.lsb.quechua.model.TranslationResult
import bo.edu.lsb.quechua.theme.AppThemeMode
import bo.edu.lsb.quechua.vision.HandLandmarksResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Pasos del Asistente Guiado de Calibración "Paso Cero".
 */
enum class CalibrationStep(val title: String, val instruction: String) {
    IDLE("Paso Cero: Calibración", "Presiona 'Iniciar' para sincronizar los sensores a la postura de tu mano."),
    STEP_REST("Paso 0A: Mano Abierta", "Mantén la mano extendida y relajada durante 2 segundos."),
    STEP_FLEX("Paso 0B: Cierra el Puño", "Cierra todos los dedos con firmeza durante 2 segundos."),
    COMPLETED("¡Calibrado Exitosamente!", "Tu guante ha sido adaptado a tu anatomía. Ya puedes traducir sin errores.")
}

/**
 * Modo de Síntesis de Voz.
 */
enum class SpeechMode(val label: String, val badge: String) {
    QUECHUA("Quechua", "QU Fonético"),
    SPANISH("Español", "ES Estándar"),
    BILINGUAL("Bilingüe (ES + QU)", "Simultáneo")
}

/**
 * Preferencia persistente de arranque de la aplicación.
 */
enum class AppStartMode(val label: String, val shortLabel: String, val description: String) {
    ASK_ON_STARTUP("Preguntar al iniciar", "Preguntar", "Muestra el panel principal para elegir modo"),
    GLOVE("Guante BLE directo", "Guante", "Abre directo el intérprete del guante"),
    CAMERA("Cámara IA directa", "Cámara", "Abre directo la cámara MediaPipe"),
    HYBRID("Fusión Híbrida directa", "Híbrida", "Combina Guante BLE + Cámara simultáneamente")
}

/**
 * ViewModel central de la aplicación Rimay LSB.
 * Gestiona el ciclo de vida de la telemetría BLE, el reconocedor multimodal (Guante, Cámara, Fusión Híbrida),
 * el motor morfológico SVO/SOV, la síntesis de voz bilingüe, la preferencia de inicio y el tema visual.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    val bleManager = BleGloveManager(application.applicationContext)
    val ttsManager = TextToSpeechManager(application.applicationContext)

    private val appPrefs = application.getSharedPreferences("rimay_app_prefs", android.content.Context.MODE_PRIVATE)

    private fun loadStartMode(): AppStartMode {
        val saved = appPrefs.getString("app_start_mode", AppStartMode.ASK_ON_STARTUP.name)
        return AppStartMode.values().firstOrNull { it.name == saved } ?: AppStartMode.ASK_ON_STARTUP
    }

    private fun loadThemeMode(): AppThemeMode {
        val saved = appPrefs.getString("theme_mode", AppThemeMode.DARK.name)
        return AppThemeMode.values().firstOrNull { it.name == saved } ?: AppThemeMode.DARK
    }

    private fun loadGloveVocabScope(): GloveVocabScope {
        val saved = appPrefs.getString("glove_vocab_scope", GloveVocabScope.ESSENTIAL.name)
        return GloveVocabScope.values().firstOrNull { it.name == saved } ?: GloveVocabScope.ESSENTIAL
    }

    private fun loadAutoSpeakOnPause(): Boolean = appPrefs.getBoolean("auto_speak_on_pause", false)

    // -------------------------------------------------------------
    // PREFERENCIA DE MODO DE INICIO
    // -------------------------------------------------------------
    private val _appStartMode = MutableStateFlow(loadStartMode())
    val appStartMode: StateFlow<AppStartMode> = _appStartMode.asStateFlow()

    fun setStartMode(mode: AppStartMode) {
        _appStartMode.value = mode
        appPrefs.edit().putString("app_start_mode", mode.name).apply()
        when (mode) {
            AppStartMode.GLOVE -> setMultimodalMode(MultimodalMode.GLOVE_ONLY)
            AppStartMode.CAMERA -> setMultimodalMode(MultimodalMode.CAMERA_ONLY)
            AppStartMode.HYBRID -> setMultimodalMode(MultimodalMode.HYBRID_FUSION)
            AppStartMode.ASK_ON_STARTUP -> Unit
        }
        logSystem("[INICIO] Preferencia al abrir la app: ${mode.label}")
    }

    // -------------------------------------------------------------
    // TEMA VISUAL (MODO OSCURO / MODO CLARO)
    // -------------------------------------------------------------
    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    fun toggleThemeMode() {
        val next = when (_themeMode.value) {
            AppThemeMode.DARK -> AppThemeMode.LIGHT
            AppThemeMode.LIGHT -> AppThemeMode.DARK
            AppThemeMode.SYSTEM -> AppThemeMode.LIGHT
        }
        setThemeMode(next)
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        appPrefs.edit().putString("theme_mode", mode.name).apply()
        logSystem("[TEMA] Modo visual establecido en: ${mode.name}")
    }

    // -------------------------------------------------------------
    // MODOS MULTIMODALES (GUANTE, CÁMARA, FUSIÓN HÍBRIDA)
    // -------------------------------------------------------------
    private val _multimodalMode = MutableStateFlow(
        when (_appStartMode.value) {
            AppStartMode.GLOVE -> MultimodalMode.GLOVE_ONLY
            AppStartMode.CAMERA -> MultimodalMode.CAMERA_ONLY
            AppStartMode.HYBRID -> MultimodalMode.HYBRID_FUSION
            AppStartMode.ASK_ON_STARTUP -> MultimodalMode.GLOVE_ONLY
        }
    )
    val multimodalMode: StateFlow<MultimodalMode> = _multimodalMode.asStateFlow()

    fun setMultimodalMode(mode: MultimodalMode) {
        if (_multimodalMode.value != mode) {
            _multimodalMode.value = mode
            debouncer.reset()
            _debouncerProgress.value = 0f
            _candidateGloss.value = "NINGUNA"
            logSystem("[MULTIMODAL] Modo de captura cambiado a: ${mode.label} (BLE y secuencia conservados)")
        }
    }

    // Alcance de vocabulario del guante (Esencial 29 señas vs Léxico completo 146)
    private val _gloveVocabScope = MutableStateFlow(loadGloveVocabScope())
    val gloveVocabScope: StateFlow<GloveVocabScope> = _gloveVocabScope.asStateFlow()

    fun setGloveVocabScope(scope: GloveVocabScope) {
        if (_gloveVocabScope.value != scope) {
            _gloveVocabScope.value = scope
            appPrefs.edit().putString("glove_vocab_scope", scope.name).apply()
            debouncer.reset()
            _debouncerProgress.value = 0f
            _candidateGloss.value = "NINGUNA"
            logSystem("[GUANTE] Alcance de reconocimiento: ${scope.label}")
        }
    }

    // Posturas personalizadas grabadas con el guante del usuario
    private val _customSignatures = MutableStateFlow(CustomGloveProfileStore.loadAll(application))
    val customSignatures: StateFlow<Map<String, CustomGloveSignature>> = _customSignatures.asStateFlow()

    private val _recordingCustomGloss = MutableStateFlow<String?>(null)
    val recordingCustomGloss: StateFlow<String?> = _recordingCustomGloss.asStateFlow()
    private var recordPoseJob: Job? = null

    // Locución manos libres al volver a reposo
    private val _autoSpeakOnPause = MutableStateFlow(loadAutoSpeakOnPause())
    val autoSpeakOnPause: StateFlow<Boolean> = _autoSpeakOnPause.asStateFlow()

    fun setAutoSpeakOnPause(enabled: Boolean) {
        _autoSpeakOnPause.value = enabled
        appPrefs.edit().putBoolean("auto_speak_on_pause", enabled).apply()
        if (!enabled) {
            autoSpeakJob?.cancel()
            autoSpeakJob = null
        }
        logSystem("[AUDIO] Auto-voz en pausa: ${if (enabled) "ACTIVADA" else "DESACTIVADA"}")
    }

    private var pendingAutoSpeakAfterAdd = false
    private var autoSpeakJob: Job? = null

    // Suavizado adaptativo EMA + ventana IMU para agitación HOLA
    private var hasEmaBaseline = false
    private val emaFlex = DoubleArray(5)
    private var emaPitch = 0.0
    private var emaRoll = 0.0
    private val recentImuWindow = ArrayDeque<Pair<Double, Double>>(8)
    @Volatile private var lastIsWavingGlove = false

    // Estado del clasificador multimodal en tiempo real
    private val _recognitionResult = MutableStateFlow(
        RecognitionResult("NINGUNA", 0f, 0f, 0f, "Sistema listo")
    )
    val recognitionResult: StateFlow<RecognitionResult> = _recognitionResult.asStateFlow()

    // Estado de landmarks anatómicos de la mano en tiempo real (21 puntos)
    private val _handLandmarksResult = MutableStateFlow(HandLandmarksResult.default())
    val handLandmarksResult: StateFlow<HandLandmarksResult> = _handLandmarksResult.asStateFlow()

    // Parámetros visuales de cámara en vivo
    private var lastCameraDescriptor = "NEUTRAL"
    private var lastHandOpenness = 0.5f
    private var lastHandTilt = 0f
    private var lastCameraFlex: List<Int>? = null
    private var lastHandPitch = 0f
    private var lastCameraDebounceMs = 0L

    // -------------------------------------------------------------
    // TELEMETRÍA Y CONEXIÓN
    // -------------------------------------------------------------
    private val _telemetry = MutableStateFlow(GloveTelemetry.default())
    val telemetry: StateFlow<GloveTelemetry> = _telemetry.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = bleManager.connectionState
    val scannedBleDevices: StateFlow<List<ScannedBleDevice>> = bleManager.scannedDevices

    private val _systemLogs = MutableStateFlow<List<String>>(emptyList())
    val systemLogs: StateFlow<List<String>> = _systemLogs.asStateFlow()

    // -------------------------------------------------------------
    // PERFIL DE CALIBRACIÓN PASO CERO
    // -------------------------------------------------------------
    private val _calibrationProfile = MutableStateFlow(GloveCalibrationProfile.load(application))
    val calibrationProfile: StateFlow<GloveCalibrationProfile> = _calibrationProfile.asStateFlow()

    private val _isCalibrationDialogOpen = MutableStateFlow(false)
    val isCalibrationDialogOpen: StateFlow<Boolean> = _isCalibrationDialogOpen.asStateFlow()

    private val _calibrationStep = MutableStateFlow(CalibrationStep.IDLE)
    val calibrationStep: StateFlow<CalibrationStep> = _calibrationStep.asStateFlow()

    private val _calibrationCountdown = MutableStateFlow(2.0f)
    val calibrationCountdown: StateFlow<Float> = _calibrationCountdown.asStateFlow()

    private var calibrationJob: Job? = null

    // -------------------------------------------------------------
    // DEBOUNCER Y SECUENCIA DE GLOSAS
    // -------------------------------------------------------------
    val debouncerThreshold: Int = 5

    private val _debouncerProgress = MutableStateFlow(0f)
    val debouncerProgress: StateFlow<Float> = _debouncerProgress.asStateFlow()

    private val _candidateGloss = MutableStateFlow("NINGUNA")
    val candidateGloss: StateFlow<String> = _candidateGloss.asStateFlow()

    private val _glossSequence = MutableStateFlow<List<String>>(emptyList())
    val glossSequence: StateFlow<List<String>> = _glossSequence.asStateFlow()

    private val _translation = MutableStateFlow(TranslationResult())
    val translation: StateFlow<TranslationResult> = _translation.asStateFlow()

    // -------------------------------------------------------------
    // SÍNTESIS DE VOZ Y TTS BILINGÜE
    // -------------------------------------------------------------
    private val _speechMode = MutableStateFlow(SpeechMode.BILINGUAL)
    val speechMode: StateFlow<SpeechMode> = _speechMode.asStateFlow()

    fun setSpeechMode(mode: SpeechMode) {
        _speechMode.value = mode
        logSystem("[AUDIO] Modo de habla configurado: ${mode.label}")
    }

    val isTtsReady: StateFlow<Boolean> = ttsManager.isReady
    val isSpeaking: StateFlow<Boolean> = ttsManager.isSpeaking
    val activeTtsVoiceLabel: StateFlow<String> = ttsManager.activeVoiceLabel

    // Estabilizador Debouncer (5 muestras a 10 Hz BLE o ticks de 95 ms en cámara ~= 500 ms sostenidos)
    private val debouncer = SignDebouncer(threshold = debouncerThreshold, maxGlitchFrames = 1) { acceptedGloss ->
        addGloss(acceptedGloss)
    }

    init {
        // Enlazar perfil de calibración activo con el gestor de hardware
        bleManager.setCalibrationProfile(_calibrationProfile.value)

        // Escucha del estado de conexión Bluetooth: al conectar, si no está calibrado, abrir Paso Cero obligatorio
        // y reiniciar filtro adaptativo al cambiar de enlace
        viewModelScope.launch {
            bleManager.connectionState.collect { state ->
                if (state is ConnectionState.Disconnected ||
                    state is ConnectionState.Scanning ||
                    state is ConnectionState.Connecting ||
                    state is ConnectionState.SimulationMode
                ) {
                    hasEmaBaseline = false
                    recentImuWindow.clear()
                    lastIsWavingGlove = false
                }
                if (state is ConnectionState.Connected && !_calibrationProfile.value.isCalibrated) {
                    _isCalibrationDialogOpen.value = true
                    logSystem("[PASO CERO] Guante conectado sin calibrar. Desplegando asistente obligatorio.")
                }
            }
        }

        // Escucha del flujo de telemetría proveniente del Guante BLE o simulador (10 Hz / 100 ms)
        viewModelScope.launch {
            bleManager.telemetryFlow.collect { frame ->
                _telemetry.value = frame

                val smoothedFrame = if (!hasEmaBaseline) {
                    hasEmaBaseline = true
                    for (i in 0 until 5) {
                        emaFlex[i] = frame.flex.getOrElse(i) { 0 }.toDouble()
                    }
                    emaPitch = frame.imuPitch
                    emaRoll = frame.imuRoll
                    frame
                } else {
                    val rawFlexVals = (0 until 5).map { frame.flex.getOrElse(it) { 0 } }
                    val hasStepJump = (0 until 5).any { abs(rawFlexVals[it] - emaFlex[it]) >= 25.0 } ||
                        abs(frame.imuPitch - emaPitch) >= 25.0 ||
                        abs(frame.imuRoll - emaRoll) >= 25.0
                    val alpha = if (hasStepJump) 0.80 else 0.50
                    val nextFlex = (0 until 5).map { i ->
                        emaFlex[i] = emaFlex[i] + alpha * (rawFlexVals[i] - emaFlex[i])
                        emaFlex[i].roundToInt().coerceIn(0, 100)
                    }
                    emaPitch += alpha * (frame.imuPitch - emaPitch)
                    emaRoll += alpha * (frame.imuRoll - emaRoll)
                    frame.copy(flex = nextFlex, imuPitch = emaPitch, imuRoll = emaRoll)
                }

                if (recentImuWindow.size >= 8) recentImuWindow.removeFirst()
                recentImuWindow.addLast(frame.imuPitch to frame.imuRoll)
                val isWaving = MultimodalFusionEngine.detectOscillatingWave(recentImuWindow.toList())
                lastIsWavingGlove = isWaving

                if (_multimodalMode.value != MultimodalMode.CAMERA_ONLY) {
                    val allowedGlosses = if (_gloveVocabScope.value == GloveVocabScope.ESSENTIAL) {
                        MultimodalFusionEngine.ESSENTIAL_GLOSSES
                    } else null

                    val recognition = MultimodalFusionEngine.processMultimodal(
                        mode = _multimodalMode.value,
                        telemetry = smoothedFrame,
                        visualDescriptor = lastCameraDescriptor,
                        handOpenness = lastHandOpenness,
                        handTilt = lastHandTilt,
                        cameraFlex = lastCameraFlex,
                        handPitch = lastHandPitch,
                        customSignatures = _customSignatures.value,
                        allowedGlosses = allowedGlosses,
                        isWavingGlove = isWaving
                    )
                    _recognitionResult.value = recognition

                    debouncer.processReading(recognition.gloss)
                    _debouncerProgress.value = debouncer.stabilityProgress
                    _candidateGloss.value = debouncer.candidateGloss
                    handleAutoSpeakState(recognition)
                }
            }
        }

        // Logs del sistema
        viewModelScope.launch {
            bleManager.rawLogFlow.collect { logLine ->
                val current = _systemLogs.value.toMutableList()
                if (current.size > 200) current.removeAt(0)
                current.add(logLine)
                _systemLogs.value = current
            }
        }
    }

    /**
     * Inyecta / actualiza manualmente la postura visual (pruebas rápidas o sin landmarks).
     */
    fun updateCameraVision(descriptor: String, handOpenness: Float = 0.5f, handTilt: Float = 0f) {
        processCameraInput(
            descriptor = descriptor,
            handOpenness = handOpenness,
            handTilt = handTilt,
            cameraFlex = null,
            handPitch = 0f,
            isLiveFrame = false
        )
    }

    /**
     * Actualiza el resultado del seguimiento de landmarks de visión artificial en tiempo real (MediaPipe 3D).
     */
    fun updateHandLandmarks(result: HandLandmarksResult) {
        _handLandmarksResult.value = result
        if (result.isHandDetected) {
            processCameraInput(
                descriptor = result.gesture,
                handOpenness = result.openness,
                handTilt = result.tilt,
                cameraFlex = result.fingerFlex,
                handPitch = result.handPitch,
                isLiveFrame = true
            )
        } else {
            processCameraInput(
                descriptor = "NEUTRAL",
                handOpenness = 0.5f,
                handTilt = 0f,
                cameraFlex = null,
                handPitch = 0f,
                isLiveFrame = true
            )
        }
    }

    private fun processCameraInput(
        descriptor: String,
        handOpenness: Float,
        handTilt: Float,
        cameraFlex: List<Int>?,
        handPitch: Float,
        isLiveFrame: Boolean
    ) {
        lastCameraDescriptor = descriptor
        lastHandOpenness = handOpenness
        lastHandTilt = handTilt
        lastCameraFlex = cameraFlex
        lastHandPitch = handPitch

        if (_multimodalMode.value != MultimodalMode.GLOVE_ONLY) {
            val allowedGlosses = if (_gloveVocabScope.value == GloveVocabScope.ESSENTIAL) {
                MultimodalFusionEngine.ESSENTIAL_GLOSSES
            } else null

            val recognition = MultimodalFusionEngine.processMultimodal(
                mode = _multimodalMode.value,
                telemetry = _telemetry.value,
                visualDescriptor = descriptor,
                handOpenness = handOpenness,
                handTilt = handTilt,
                cameraFlex = cameraFlex,
                handPitch = handPitch,
                customSignatures = _customSignatures.value,
                allowedGlosses = allowedGlosses,
                isWavingGlove = lastIsWavingGlove
            )
            _recognitionResult.value = recognition

            val now = System.currentTimeMillis()
            if (!isLiveFrame || (now - lastCameraDebounceMs) >= 95L) {
                lastCameraDebounceMs = now
                debouncer.processReading(recognition.gloss)
                _debouncerProgress.value = debouncer.stabilityProgress
                _candidateGloss.value = debouncer.candidateGloss
                handleAutoSpeakState(recognition)
            }
        }
    }

    private fun handleAutoSpeakState(recognition: RecognitionResult) {
        if (recognition.gloss != "NINGUNA" || debouncer.candidateGloss != "NINGUNA") {
            autoSpeakJob?.cancel()
            autoSpeakJob = null
        } else if (_autoSpeakOnPause.value && pendingAutoSpeakAfterAdd &&
            !_translation.value.isEmpty && !ttsManager.isSpeaking.value
        ) {
            if (autoSpeakJob?.isActive != true) {
                autoSpeakJob = viewModelScope.launch {
                    delay(1_300L)
                    if (_autoSpeakOnPause.value &&
                        pendingAutoSpeakAfterAdd &&
                        debouncer.candidateGloss == "NINGUNA" &&
                        _recognitionResult.value.gloss == "NINGUNA" &&
                        !_translation.value.isEmpty &&
                        !ttsManager.isSpeaking.value
                    ) {
                        pendingAutoSpeakAfterAdd = false
                        speakCurrentSentence()
                    }
                }
            }
        }
    }

    /**
     * Limpia el estado efímero de cámara al salir del visor sin perder conexión BLE ni secuencia.
     */
    fun resetCameraState() {
        lastCameraDescriptor = "NEUTRAL"
        lastHandOpenness = 0.5f
        lastHandTilt = 0f
        lastCameraFlex = null
        lastHandPitch = 0f
        lastCameraDebounceMs = 0L
        _handLandmarksResult.value = HandLandmarksResult.default()
        if (_multimodalMode.value == MultimodalMode.CAMERA_ONLY) {
            debouncer.reset()
            _debouncerProgress.value = 0f
            _candidateGloss.value = "NINGUNA"
            _recognitionResult.value = RecognitionResult("NINGUNA", 0f, 0f, 0f, "Cámara en pausa")
        }
    }

    /**
     * Agrega una glosa validada a la secuencia sintáctica.
     */
    fun addGloss(gloss: String) {
        val clean = gloss.trim().uppercase()
        if (clean.isBlank() || clean == "NINGUNA") return

        val currentList = _glossSequence.value
        if (currentList.isEmpty() || currentList.last() != clean) {
            val updated = currentList + clean
            _glossSequence.value = updated
            _translation.value = MorphologyEngine.translateSequence(updated)
            pendingAutoSpeakAfterAdd = true
            logSystem("[RECONOCIMIENTO MULTIMODAL] Seña: $clean | Secuencia: ${updated.joinToString(" + ")}")
        }
    }

    /**
     * Elimina la última glosa ingresada (Deshacer paso a paso).
     */
    fun removeLastGloss() {
        val current = _glossSequence.value
        if (current.isNotEmpty()) {
            autoSpeakJob?.cancel()
            pendingAutoSpeakAfterAdd = false
            val updated = current.dropLast(1)
            _glossSequence.value = updated
            _translation.value = if (updated.isEmpty()) TranslationResult() else MorphologyEngine.translateSequence(updated)
            logSystem("[CONTROL] Seña removida: ${current.last()} | Restantes: ${updated.size}")
        }
    }

    /**
     * Elimina una glosa específica por su índice en la secuencia.
     */
    fun removeGlossAt(index: Int) {
        val current = _glossSequence.value
        if (index in current.indices) {
            autoSpeakJob?.cancel()
            pendingAutoSpeakAfterAdd = false
            val updated = current.toMutableList().apply { removeAt(index) }
            _glossSequence.value = updated
            _translation.value = if (updated.isEmpty()) TranslationResult() else MorphologyEngine.translateSequence(updated)
            logSystem("[CONTROL] Seña en índice $index eliminada.")
        }
    }

    /**
     * Limpia la secuencia de glosas acumulada.
     */
    fun clearSequence() {
        autoSpeakJob?.cancel()
        pendingAutoSpeakAfterAdd = false
        debouncer.reset()
        _debouncerProgress.value = 0f
        _candidateGloss.value = "NINGUNA"
        _glossSequence.value = emptyList()
        _translation.value = TranslationResult()
        logSystem("[CONTROL] Secuencia de glosas reiniciada.")
    }

    /**
     * Pronuncia la traducción actual en el modo configurado (Quechua, Español o Bilingüe Simultáneo).
     */
    fun speakCurrentSentence() {
        val result = _translation.value
        if (result.isEmpty) return
        autoSpeakJob?.cancel()
        pendingAutoSpeakAfterAdd = false

        when (_speechMode.value) {
            SpeechMode.QUECHUA -> {
                ttsManager.speak(result.quechuaSentence, isQuechua = true)
                logSystem("[AUDIO TTS] Reproduciendo en Quechua: ${result.quechuaSentence}")
            }
            SpeechMode.SPANISH -> {
                ttsManager.speak(result.spanishSentence, isQuechua = false)
                logSystem("[AUDIO TTS] Reproduciendo en Español: ${result.spanishSentence}")
            }
            SpeechMode.BILINGUAL -> {
                ttsManager.speakBilingual(result.spanishSentence, result.quechuaSentence)
                logSystem("[AUDIO TTS] Reproducción Bilingüe Simultánea (ES + QU)")
            }
        }
    }

    /**
     * Pronuncia una glosa individual del léxico para escuchar su fonética quechua o española.
     */
    fun speakGloss(gloss: String) {
        val item = MorphologyEngine.lexicon[gloss.trim().uppercase()] ?: return
        when (_speechMode.value) {
            SpeechMode.QUECHUA -> ttsManager.speak(item.quechuaStem, isQuechua = true)
            SpeechMode.SPANISH -> ttsManager.speak(item.spanishDefault, isQuechua = false)
            SpeechMode.BILINGUAL -> ttsManager.speakBilingual(item.spanishDefault, item.quechuaStem)
        }
    }

    /**
     * Pronuncia un texto arbitrario en Quechua o Español.
     */
    fun speakText(text: String, isQuechua: Boolean = true) {
        ttsManager.speak(text, isQuechua = isQuechua)
    }

    // Compatibilidad para vistas que usan toggle de Quechua booleano
    val speakInQuechua: StateFlow<Boolean> = MutableStateFlow(true)
    fun setSpeakInQuechua(inQuechua: Boolean) {
        setSpeechMode(if (inQuechua) SpeechMode.QUECHUA else SpeechMode.SPANISH)
    }

    // Controles BLE
    fun connectGlove() {
        bleManager.startScanAndConnect(viewModelScope)
    }

    fun disconnectGlove() {
        bleManager.disconnect()
    }

    fun startSimulationMode() {
        bleManager.startSimulationMode(viewModelScope)
    }

    fun toggleSimulationMode() {
        if (connectionState.value is ConnectionState.SimulationMode) {
            bleManager.disconnect()
        } else {
            bleManager.startSimulationMode(viewModelScope)
        }
    }

    fun simulateGloss(gloss: String) {
        bleManager.simulateSignGesture(viewModelScope, gloss)
    }

    fun logSystem(message: String) {
        val current = _systemLogs.value.toMutableList()
        if (current.size > 200) current.removeAt(0)
        current.add(message)
        _systemLogs.value = current
    }

    // -------------------------------------------------------------
    // GESTIÓN DE CALIBRACIÓN PASO CERO Y DISPOSITIVOS BLUETOOTH
    // -------------------------------------------------------------
    fun openCalibrationDialog() {
        _calibrationStep.value = CalibrationStep.IDLE
        _calibrationCountdown.value = 2.0f
        _isCalibrationDialogOpen.value = true
        logSystem("[PASO CERO] Apertura de asistente de calibración.")
    }

    fun closeCalibrationDialog() {
        calibrationJob?.cancel()
        _isCalibrationDialogOpen.value = false
        _calibrationStep.value = CalibrationStep.IDLE
    }

    /**
     * Inicia el proceso de calibración guiada en 2 fases:
     * 1. Paso 0A: Mano abierta y relajada (2 seg).
     * 2. Paso 0B: Puño cerrado firme (2 seg).
     */
    fun startFullCalibration() {
        val state = connectionState.value
        if (state !is ConnectionState.Connected && state !is ConnectionState.SimulationMode) {
            logSystem("[PASO CERO] Conecta el Guante_ESP32 o activa el Simulador 10 Hz antes de calibrar.")
            return
        }

        calibrationJob?.cancel()
        calibrationJob = viewModelScope.launch {
            // FASE 1: Mano Abierta (Paso 0A) - 2.0 segundos
            _calibrationStep.value = CalibrationStep.STEP_REST
            val restSamples = mutableListOf<List<Int>>()
            var pitchSum = 0.0
            var rollSum = 0.0
            var sampleCount = 0

            val startTime = System.currentTimeMillis()
            while (isActive && (System.currentTimeMillis() - startTime) < 2000) {
                val elapsed = (System.currentTimeMillis() - startTime) / 1000f
                _calibrationCountdown.value = (2.0f - elapsed).coerceAtLeast(0f)
                val curRaw = _telemetry.value.rawFlex
                restSamples.add(curRaw)
                pitchSum += _telemetry.value.imuPitch
                rollSum += _telemetry.value.imuRoll
                sampleCount++
                delay(50)
            }

            val isPercentScale = restSamples.isNotEmpty() && restSamples.all { s -> s.size >= 5 && s.all { it in 0..100 } }

            // Promediar lectura de reposo
            val avgRest = (0 until 5).map { fingerIdx ->
                if (restSamples.isNotEmpty()) {
                    val fallback = if (isPercentScale) 5 else if (fingerIdx == 4) 1950 else 128
                    restSamples.map { it.getOrElse(fingerIdx) { fallback } }.average().roundToInt()
                } else {
                    if (fingerIdx == 4) 1950 else 128
                }
            }
            val prevProfile = _calibrationProfile.value
            val deltaPitch = if (sampleCount > 0) pitchSum / sampleCount else 0.0
            val deltaRoll = if (sampleCount > 0) rollSum / sampleCount else 0.0
            val zeroPitch = if (state is ConnectionState.Connected) prevProfile.zeroPitch + deltaPitch else deltaPitch
            val zeroRoll = if (state is ConnectionState.Connected) prevProfile.zeroRoll + deltaRoll else deltaRoll

            logSystem("[PASO CERO] Reposo capturado: $avgRest, Zero IMU: P=${zeroPitch.toInt()} R=${zeroRoll.toInt()}")

            // Breve pausa para acomodar la mano
            delay(500)

            // FASE 2: Cierra el Puño (Paso 0B) - 2.0 segundos
            _calibrationStep.value = CalibrationStep.STEP_FLEX
            val flexSamples = mutableListOf<List<Int>>()
            val startFlexTime = System.currentTimeMillis()
            while (isActive && (System.currentTimeMillis() - startFlexTime) < 2000) {
                val elapsed = (System.currentTimeMillis() - startFlexTime) / 1000f
                _calibrationCountdown.value = (2.0f - elapsed).coerceAtLeast(0f)
                val curRaw = _telemetry.value.rawFlex
                flexSamples.add(curRaw)
                delay(50)
            }

            val allFlexPercent = isPercentScale && flexSamples.isNotEmpty() && flexSamples.all { s -> s.size >= 5 && s.all { it in 0..100 } }

            // Promediar lectura de flexión
            val avgFlex = (0 until 5).map { fingerIdx ->
                if (flexSamples.isNotEmpty()) {
                    val fallback = if (allFlexPercent) 100 else if (fingerIdx == 4) 2850 else 220
                    flexSamples.map { it.getOrElse(fingerIdx) { fallback } }.average().roundToInt()
                } else {
                    if (fingerIdx == 4) 2850 else 220
                }
            }

            // Construir y guardar perfil (respetando escala 0..100% del firmware final ESP32)
            val fingers = (0 until 5).map { i ->
                val r = avgRest[i]
                val f = avgFlex[i]
                if (allFlexPercent && kotlin.math.abs(f - r) < 12) {
                    FingerCalibration(restRaw = r.coerceIn(0, 45), flexRaw = 100)
                } else {
                    FingerCalibration(restRaw = r, flexRaw = f)
                }
            }
            val newProfile = GloveCalibrationProfile(
                fingers = fingers,
                zeroPitch = zeroPitch,
                zeroRoll = zeroRoll,
                isCalibrated = true,
                timestamp = System.currentTimeMillis()
            )

            _calibrationProfile.value = newProfile
            GloveCalibrationProfile.save(getApplication(), newProfile)
            bleManager.setCalibrationProfile(newProfile)

            _calibrationStep.value = CalibrationStep.COMPLETED
            logSystem("[PASO CERO OK] Calibración completa guardada con éxito.")
        }
    }

    /**
     * Calibración rápida en 2 segundos: toma la postura actual como reposo.
     * Adapta automáticamente el rango según si el ESP32 envía porcentajes 0..100% o valores ADC RAW.
     */
    fun quickAutoCalibrate() {
        val state = connectionState.value
        if (state !is ConnectionState.Connected && state !is ConnectionState.SimulationMode) {
            logSystem("[PASO CERO] Conecta el Guante_ESP32 o activa el Simulador 10 Hz antes de calibrar.")
            return
        }

        calibrationJob?.cancel()
        calibrationJob = viewModelScope.launch {
            _calibrationStep.value = CalibrationStep.STEP_REST
            val startTime = System.currentTimeMillis()
            val samples = mutableListOf<List<Int>>()
            var pSum = 0.0
            var rSum = 0.0
            var count = 0

            while (isActive && (System.currentTimeMillis() - startTime) < 2000) {
                val elapsed = (System.currentTimeMillis() - startTime) / 1000f
                _calibrationCountdown.value = (2.0f - elapsed).coerceAtLeast(0f)
                samples.add(_telemetry.value.rawFlex)
                pSum += _telemetry.value.imuPitch
                rSum += _telemetry.value.imuRoll
                count++
                delay(50)
            }

            val isPercentScale = samples.isNotEmpty() && samples.all { s -> s.size >= 5 && s.all { it in 0..100 } }
            val avgRest = (0 until 5).map { fingerIdx ->
                if (samples.isNotEmpty()) {
                    val fallback = if (isPercentScale) 5 else if (fingerIdx == 4) 1950 else 128
                    samples.map { it.getOrElse(fingerIdx) { fallback } }.average().roundToInt()
                } else {
                    if (fingerIdx == 4) 1950 else 128
                }
            }
            val prevProfile = _calibrationProfile.value
            val dPitch = if (count > 0) pSum / count else 0.0
            val dRoll = if (count > 0) rSum / count else 0.0
            val zPitch = if (state is ConnectionState.Connected) prevProfile.zeroPitch + dPitch else dPitch
            val zRoll = if (state is ConnectionState.Connected) prevProfile.zeroRoll + dRoll else dRoll

            val fingers = (0 until 5).map { i ->
                val rest = avgRest[i]
                if (isPercentScale) {
                    FingerCalibration(restRaw = rest.coerceIn(0, 45), flexRaw = 100)
                } else {
                    val flex = if (i == 4) (rest + 850) else (rest + 92)
                    FingerCalibration(rest, flex)
                }
            }

            val newProfile = GloveCalibrationProfile(
                fingers = fingers,
                zeroPitch = zPitch,
                zeroRoll = zRoll,
                isCalibrated = true,
                timestamp = System.currentTimeMillis()
            )

            _calibrationProfile.value = newProfile
            GloveCalibrationProfile.save(getApplication(), newProfile)
            bleManager.setCalibrationProfile(newProfile)

            _calibrationStep.value = CalibrationStep.COMPLETED
            logSystem("[PASO CERO RÁPIDO OK] Reposo ajustado en 2 segundos.")
        }
    }

    fun resetCalibration() {
        GloveCalibrationProfile.clear(getApplication())
        val def = GloveCalibrationProfile.defaultProfile()
        _calibrationProfile.value = def
        bleManager.setCalibrationProfile(def)
        _calibrationStep.value = CalibrationStep.IDLE
        logSystem("[PASO CERO] Calibración restablecida a fábrica.")
    }

    /**
     * Graba durante 1 segundo (10 muestras a 10 Hz) la postura real actual del guante para una glosa dada,
     * guardándola de forma persistente en [CustomGloveProfileStore] para máxima precisión con la mano del usuario.
     */
    fun recordCustomSignPose(gloss: String) {
        val clean = gloss.trim().uppercase()
        if (!MorphologyEngine.lexicon.containsKey(clean)) return

        recordPoseJob?.cancel()
        recordPoseJob = viewModelScope.launch {
            _recordingCustomGloss.value = clean
            logSystem("[ENTRENAMIENTO GUANTE] Grabando postura personal para '$clean' (1.0 s)...")
            try {
                val flexSums = IntArray(5)
                var pitchSum = 0.0
                var rollSum = 0.0
                val sampleCount = 10

                for (s in 0 until sampleCount) {
                    val frame = _telemetry.value
                    for (i in 0 until 5) {
                        flexSums[i] += frame.flex.getOrElse(i) { 0 }
                    }
                    pitchSum += frame.imuPitch
                    rollSum += frame.imuRoll
                    delay(100)
                }

                val avgFlex = (0 until 5).map { (flexSums[it].toDouble() / sampleCount).roundToInt().coerceIn(0, 100) }
                val avgPitch = pitchSum / sampleCount
                val avgRoll = rollSum / sampleCount
                val signature = CustomGloveSignature(
                    gloss = clean,
                    flex = avgFlex,
                    imuPitch = avgPitch,
                    imuRoll = avgRoll,
                    timestamp = System.currentTimeMillis()
                )
                CustomGloveProfileStore.save(getApplication(), signature)
                _customSignatures.value = _customSignatures.value + (clean to signature)
                logSystem("[ENTRENAMIENTO OK] Seña '$clean' guardada: dedos=$avgFlex P=${avgPitch.toInt()}° R=${avgRoll.toInt()}°")
            } finally {
                _recordingCustomGloss.value = null
            }
        }
    }

    fun clearCustomSignPose(gloss: String) {
        val clean = gloss.trim().uppercase()
        CustomGloveProfileStore.remove(getApplication(), clean)
        _customSignatures.value = _customSignatures.value - clean
        logSystem("[ENTRENAMIENTO] Firma personalizada removida para '$clean'.")
    }

    fun startBleDiscoveryScan() {
        bleManager.startDiscoveryScan()
    }

    fun stopBleDiscoveryScan() {
        bleManager.stopDiscoveryScan()
    }

    fun getBondedDevices(): List<BluetoothDevice> = bleManager.getBondedDevices()

    fun connectToDevice(device: BluetoothDevice) {
        bleManager.connectExplicitDevice(device, viewModelScope)
        logSystem("[BLUETOOTH] Conexión manual a ${device.name ?: "Dispositivo"} solicitada.")
    }

    override fun onCleared() {
        recordPoseJob?.cancel()
        autoSpeakJob?.cancel()
        bleManager.disconnect()
        ttsManager.shutdown()
        super.onCleared()
    }
}
