package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed class VoiceState {
    object Idle : VoiceState()
    object Initializing : VoiceState()
    data class Listening(val rmsDb: Float = 0f) : VoiceState()
    object Processing : VoiceState()
    data class Success(val recognizedText: String) : VoiceState()
    data class Error(val message: String) : VoiceState()
}

class VoiceManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening(language: String = "fa-IR") {
        stopListening()

        if (!isAvailable()) {
            _voiceState.value = VoiceState.Error("سرویس تشخیص گفتار روی این دستگاه فعال نیست. می‌توانید از کیبورد صوتی یا ورودی متنی استفاده کنید.")
            return
        }

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d("VoiceManager", "Ready for speech")
                        _voiceState.value = VoiceState.Listening(0f)
                    }

                    override fun onBeginningOfSpeech() {
                        Log.d("VoiceManager", "Beginning of speech")
                        _voiceState.value = VoiceState.Listening(5f)
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        if (_voiceState.value is VoiceState.Listening) {
                            _voiceState.value = VoiceState.Listening(rmsdB)
                        }
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        Log.d("VoiceManager", "End of speech")
                        _voiceState.value = VoiceState.Processing
                    }

                    override fun onError(error: Int) {
                        Log.e("VoiceManager", "Speech error code: $error")
                        val errorMessage = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "خطای ضبط صدا"
                            SpeechRecognizer.ERROR_CLIENT -> "خطای داخلی کلاینت"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "مجوز ضبط صدا داده نشده است"
                            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "خطای شبکه در تشخیص صدا"
                            SpeechRecognizer.ERROR_NO_MATCH -> "صدایی متوجه نشدم، دوباره امتحان کنید"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "سرویس تشخیص صدا مشغول است"
                            SpeechRecognizer.ERROR_SERVER -> "خطای سرور تشخیص صدا"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "صدایی شنیده نشد"
                            else -> "خطا در دریافت صدا (کد $error)"
                        }
                        _voiceState.value = VoiceState.Error(errorMessage)
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim()
                        if (!text.isNullOrBlank()) {
                            Log.d("VoiceManager", "Speech recognized: $text")
                            _voiceState.value = VoiceState.Success(text)
                        } else {
                            _voiceState.value = VoiceState.Error("صدایی شنیده نشد. دوباره امتحان کنید.")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, language)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "در حال گوش دادن به برنامه کاری شما...")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            _voiceState.value = VoiceState.Initializing
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("VoiceManager", "Failed to start listening", e)
            _voiceState.value = VoiceState.Error("امکان شروع ضبط صدا وجود ندارد: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
    }

    fun resetState() {
        _voiceState.value = VoiceState.Idle
    }
}
