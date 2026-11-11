package bo.edu.lsb.quechua.audio

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import bo.edu.lsb.quechua.engine.MorphologyEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Gestor del Motor Text-To-Speech (TTS) nativo de Android.
 *
 * Prioriza voces Latinoamericanas / Andinas (`es-BO`, `es-PE`, `es-EC`, `es-419`, `es-CO`, `es-CL`, `es-AR`, `es-MX`, `es-US`)
 * sobre castellano peninsular (`es-ES`), y usa [MorphologyEngine.toTtsPhoneticSpeech] para pronunciación fluida en Quechua
 * cuando el teléfono no dispone de sintetizador nativo `qu-BO`.
 */
class TextToSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _activeVoiceLabel = MutableStateFlow("es-BO")
    val activeVoiceLabel: StateFlow<String> = _activeVoiceLabel.asStateFlow()

    private var selectedQuechuaLocale: Locale = Locale.forLanguageTag("qu-BO")
    private var selectedQuechuaVoice: Voice? = null
    private var selectedSpanishLocale: Locale = Locale.forLanguageTag("es-BO")
    private var selectedSpanishVoice: Voice? = null
    private var hasNativeQuechuaVoice = false

    private val activeUtterances = ConcurrentHashMap.newKeySet<String>()

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            try {
                tts?.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
            } catch (_: Exception) {}

            selectOptimalVoices()

            tts?.setSpeechRate(0.88f)
            tts?.setPitch(1.0f)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    if (utteranceId != null) {
                        activeUtterances.add(utteranceId)
                    }
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    if (utteranceId != null) {
                        activeUtterances.remove(utteranceId)
                    }
                    _isSpeaking.value = activeUtterances.isNotEmpty()
                }

                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    if (utteranceId != null) {
                        activeUtterances.remove(utteranceId)
                    }
                    _isSpeaking.value = activeUtterances.isNotEmpty()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (utteranceId != null) {
                        activeUtterances.remove(utteranceId)
                    }
                    _isSpeaking.value = activeUtterances.isNotEmpty()
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    if (utteranceId != null) {
                        activeUtterances.remove(utteranceId)
                    }
                    _isSpeaking.value = activeUtterances.isNotEmpty()
                }
            })

            _isReady.value = true
        } else {
            _isReady.value = false
        }
    }

    private fun selectOptimalVoices() {
        val engine = tts ?: return
        val voices = try {
            engine.voices?.toList().orEmpty()
        } catch (_: Exception) {
            emptyList()
        }.filter { voice ->
            val missing = try {
                voice.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) == true
            } catch (_: Exception) {
                false
            }
            !missing
        }

        // 1. Buscar voz nativa de Quechua (qu-BO > qu-PE > qu)
        val bestQuechuaVoice = voices
            .mapNotNull { v ->
                val score = scoreQuechuaLocale(v.locale.toLanguageTag())
                if (score <= 0) null else {
                    val offlineBonus = if (!v.isNetworkConnectionRequired) 20 else 0
                    val qualityBonus = if (v.quality >= Voice.QUALITY_HIGH) 5 else 0
                    v to (score * 100 + offlineBonus + qualityBonus)
                }
            }
            .maxByOrNull { it.second }
            ?.first

        if (bestQuechuaVoice != null) {
            hasNativeQuechuaVoice = true
            selectedQuechuaVoice = bestQuechuaVoice
            selectedQuechuaLocale = bestQuechuaVoice.locale
        } else {
            val qFallback = listOf("qu-BO", "quz-BO", "qu-PE", "qu").firstOrNull { tag ->
                try {
                    val res = engine.isLanguageAvailable(Locale.forLanguageTag(tag))
                    res >= TextToSpeech.LANG_AVAILABLE
                } catch (_: Exception) {
                    false
                }
            }
            if (qFallback != null) {
                hasNativeQuechuaVoice = true
                selectedQuechuaLocale = Locale.forLanguageTag(qFallback)
            } else {
                hasNativeQuechuaVoice = false
            }
        }

        // 2. Seleccionar mejor voz en Español Latinoamericano / Andino evitando es-ES salvo último recurso
        val bestSpanishVoice = voices
            .mapNotNull { v ->
                val score = scoreSpanishLocale(v.locale.toLanguageTag())
                if (score <= 0) null else {
                    val offlineBonus = if (!v.isNetworkConnectionRequired) 20 else 0
                    val qualityBonus = if (v.quality >= Voice.QUALITY_HIGH) 5 else 0
                    v to (score * 100 + offlineBonus + qualityBonus)
                }
            }
            .maxByOrNull { it.second }
            ?.first

        if (bestSpanishVoice != null) {
            selectedSpanishVoice = bestSpanishVoice
            selectedSpanishLocale = bestSpanishVoice.locale
            engine.setLanguage(bestSpanishVoice.locale)
            try {
                engine.voice = bestSpanishVoice
            } catch (_: Exception) {}
        } else {
            val fallbackTags = listOf("es-BO", "es-PE", "es-EC", "es-419", "es-CO", "es-CL", "es-AR", "es-MX", "es-US", "es-ES")
            val chosenTag = fallbackTags.firstOrNull { tag ->
                try {
                    engine.isLanguageAvailable(Locale.forLanguageTag(tag)) >= TextToSpeech.LANG_AVAILABLE
                } catch (_: Exception) {
                    false
                }
            } ?: "es-ES"
            selectedSpanishLocale = Locale.forLanguageTag(chosenTag)
            engine.setLanguage(selectedSpanishLocale)
        }

        val spanishLabel = selectedSpanishLocale.toLanguageTag()
        _activeVoiceLabel.value = if (hasNativeQuechuaVoice) {
            "${selectedQuechuaLocale.toLanguageTag()} / $spanishLabel"
        } else {
            spanishLabel
        }
    }

    private fun scoreQuechuaLocale(rawTag: String): Int {
        val tag = rawTag.lowercase(Locale.ROOT).replace('_', '-')
        return when {
            tag.startsWith("qu-bo") || tag.startsWith("quz-bo") -> 100
            tag.startsWith("qu-pe") || tag.startsWith("quz-pe") -> 90
            tag == "qu" || tag == "quz" || tag.startsWith("qu-") || tag.startsWith("quz-") -> 70
            else -> -1
        }
    }

    private fun scoreSpanishLocale(rawTag: String): Int {
        val tag = rawTag.lowercase(Locale.ROOT).replace('_', '-')
        return when {
            tag.startsWith("es-bo") -> 100
            tag.startsWith("es-pe") -> 95
            tag.startsWith("es-ec") -> 90
            tag.startsWith("es-419") -> 85
            tag.startsWith("es-co") -> 80
            tag.startsWith("es-cl") -> 75
            tag.startsWith("es-ar") -> 70
            tag.startsWith("es-mx") -> 65
            tag.startsWith("es-us") -> 60
            tag.startsWith("es-es") -> 10
            tag == "es" || tag.startsWith("es-") -> 50
            else -> -1
        }
    }

    private fun applySpanishVoice() {
        val engine = tts ?: return
        engine.setLanguage(selectedSpanishLocale)
        selectedSpanishVoice?.let { voice ->
            try {
                engine.voice = voice
            } catch (_: Exception) {}
        }
    }

    private fun applyQuechuaVoice() {
        val engine = tts ?: return
        engine.setLanguage(selectedQuechuaLocale)
        selectedQuechuaVoice?.let { voice ->
            try {
                engine.voice = voice
            } catch (_: Exception) {}
        }
    }

    private fun buildParams(utteranceId: String): Bundle =
        Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

    /**
     * Pronuncia la oración en el idioma seleccionado.
     * Para Quechua sin voz nativa utiliza [MorphologyEngine.toTtsPhoneticSpeech] con ritmo andino (0.86x).
     */
    fun speak(text: String, isQuechua: Boolean) {
        val engine = tts ?: return
        if (!_isReady.value || text.isBlank()) return

        val utteranceId = "LSB_TTS_${System.currentTimeMillis()}"
        activeUtterances.clear()
        activeUtterances.add(utteranceId)
        _isSpeaking.value = true

        val status = if (isQuechua) {
            if (hasNativeQuechuaVoice) {
                applyQuechuaVoice()
                engine.setSpeechRate(0.86f)
                engine.setPitch(1.0f)
                engine.speak(text, TextToSpeech.QUEUE_FLUSH, buildParams(utteranceId), utteranceId)
            } else {
                applySpanishVoice()
                engine.setSpeechRate(0.86f)
                engine.setPitch(1.0f)
                val utteranceText = MorphologyEngine.toTtsPhoneticSpeech(text)
                engine.speak(utteranceText, TextToSpeech.QUEUE_FLUSH, buildParams(utteranceId), utteranceId)
            }
        } else {
            applySpanishVoice()
            engine.setSpeechRate(0.90f)
            engine.setPitch(1.0f)
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, buildParams(utteranceId), utteranceId)
        }

        if (status == TextToSpeech.ERROR) {
            activeUtterances.remove(utteranceId)
            _isSpeaking.value = activeUtterances.isNotEmpty()
        }
    }

    /**
     * Pronuncia secuencialmente en Español (SVO) y luego en Quechua Boliviano (SOV)
     * con una pausa natural de 420 ms, manteniendo [_isSpeaking] activo durante toda la locución.
     */
    fun speakBilingual(spanishText: String, quechuaText: String) {
        val engine = tts ?: return
        if (!_isReady.value) return
        if (spanishText.isBlank()) {
            if (quechuaText.isNotBlank()) speak(quechuaText, isQuechua = true)
            return
        }

        val ts = System.currentTimeMillis()
        val idEs = "LSB_TTS_ES_$ts"
        val idPause = "LSB_TTS_PAUSE_$ts"
        val idQu = "LSB_TTS_QU_$ts"

        activeUtterances.clear()
        activeUtterances.add(idEs)
        if (quechuaText.isNotBlank()) {
            activeUtterances.add(idPause)
            activeUtterances.add(idQu)
        }
        _isSpeaking.value = true

        applySpanishVoice()
        engine.setSpeechRate(0.90f)
        engine.setPitch(1.0f)
        val esStatus = engine.speak(spanishText, TextToSpeech.QUEUE_FLUSH, buildParams(idEs), idEs)
        if (esStatus == TextToSpeech.ERROR) {
            activeUtterances.remove(idEs)
        }

        if (quechuaText.isNotBlank()) {
            val pStatus = engine.playSilentUtterance(420L, TextToSpeech.QUEUE_ADD, idPause)
            if (pStatus == TextToSpeech.ERROR) {
                activeUtterances.remove(idPause)
            }

            val utteranceText = if (hasNativeQuechuaVoice) {
                applyQuechuaVoice()
                quechuaText
            } else {
                applySpanishVoice()
                MorphologyEngine.toTtsPhoneticSpeech(quechuaText)
            }
            engine.setSpeechRate(0.86f)
            engine.setPitch(1.0f)
            val quStatus = engine.speak(utteranceText, TextToSpeech.QUEUE_ADD, buildParams(idQu), idQu)
            if (quStatus == TextToSpeech.ERROR) {
                activeUtterances.remove(idQu)
            }
        }

        _isSpeaking.value = activeUtterances.isNotEmpty()
    }

    fun stop() {
        activeUtterances.clear()
        tts?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        activeUtterances.clear()
        tts?.stop()
        tts?.shutdown()
        tts = null
        _isReady.value = false
        _isSpeaking.value = false
    }
}
