package net.jj1uzh.playspeechrecognition.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.audio.AudioSource
import com.google.mlkit.genai.speechrecognition.SpeechRecognition
import com.google.mlkit.genai.speechrecognition.SpeechRecognizer
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerOptions.Mode.Companion.MODE_ADVANCED
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerOptions.Mode.Companion.MODE_BASIC
import com.google.mlkit.genai.speechrecognition.speechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.speechRecognizerRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.runningReduce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.jj1uzh.playspeechrecognition.ui.TopScreenState.Companion.ModelStatus.Downloading
import net.jj1uzh.playspeechrecognition.ui.TopScreenState.Companion.RecognizerStatus.ModelPreparing
import java.util.Locale

data class TopScreenState(
    val recognizedFinal: String = "",
    val recognizedPartial: String = "",
    val status: RecognizerStatus = Unknown,
    val options: RecognizerOptions = RecognizerOptions(JaJP, Advanced),
) {
    companion object {
        sealed interface ModelStatus {
            data object DownloadFailed : ModelStatus
            data class Downloading(val downloaded: Long) : ModelStatus
            data object Unavailable : ModelStatus
        }

        sealed interface RecognizerStatus {
            data object Unknown : RecognizerStatus
            data class ModelPreparing(val status: ModelStatus) : RecognizerStatus
            data object Ready : RecognizerStatus
            data object Recognizing : RecognizerStatus
        }

        data class RecognizerOptions(
            val locale: RecognizerLocale,
            val mode: RecognizerMode
        ) {
            companion object {
                enum class RecognizerLocale {
                    JaJP, EnUS;

                    fun toLocale() = when (this) {
                        JaJP -> Locale.JAPAN
                        EnUS -> Locale.ENGLISH
                    }

                    fun displayName() = toLocale().displayName
                }

                enum class RecognizerMode {
                    Basic, Advanced;

                    fun toMode() = when (this) {
                        Basic -> MODE_BASIC
                        Advanced -> MODE_ADVANCED
                    }

                    fun displayName() = when (this) {
                        Basic -> "Basic"
                        Advanced -> "Advanced"
                    }
                }
            }
        }
    }
}

class TopScreenViewModel : ViewModel() {

    private var _uiState = MutableStateFlow(TopScreenState())
    val uiState = _uiState.asStateFlow()

    private var _modelTotalBytes: MutableStateFlow<Long?> = MutableStateFlow(null)
    val modelTotalBytes = _modelTotalBytes.asStateFlow()

    private var _error: MutableStateFlow<String?> = MutableStateFlow(null)
    val error = _error.asStateFlow()

    private var _speechRecognizer = MutableStateFlow<SpeechRecognizer?>(null)

    private fun mkSpeechRecognizerClient(options: TopScreenState.Companion.RecognizerOptions) =
        SpeechRecognition.getClient(
            speechRecognizerOptions {
                locale = options.locale.toLocale()
                preferredMode = options.mode.toMode()
            }
        )

    private suspend fun checkSpeechRecognizerClient(speechRecognizer: SpeechRecognizer) {
        when (speechRecognizer.checkStatus()) {
            FeatureStatus.DOWNLOADABLE -> {
                speechRecognizer.download().collect {
                    when (it) {
                        DownloadCompleted -> {
                            _uiState.update { it.copy(status = Ready) }
                        }

                        is DownloadProgress -> {
                            val downloaded = it.totalBytesDownloaded
                            _uiState.update {
                                it.copy(
                                    status = ModelPreparing(
                                        Downloading(
                                            downloaded
                                        )
                                    )
                                )
                            }
                        }

                        is DownloadFailed -> {
                            val msg = it.e.message
                            _error.update { msg }
                            Log.e("TopScreenViewModel.init", "DownloadFailed", it.e)
                            _uiState.update { it.copy(status = ModelPreparing(DownloadFailed)) }
                        }

                        is DownloadStarted -> {
                            val totalBytes = it.bytesToDownload
                            _modelTotalBytes.update { totalBytes }
                            _uiState.update { it.copy(status = ModelPreparing(Downloading(0L))) }
                        }
                    }
                }
            }

            FeatureStatus.AVAILABLE -> {
                _uiState.update { it.copy(status = Ready) }
            }

            FeatureStatus.DOWNLOADING -> {}
            FeatureStatus.UNAVAILABLE -> {
                _uiState.update { it.copy(status = ModelPreparing(Unavailable)) }
            }
        }
    }

    init {
        viewModelScope.launch {
            _uiState
                .distinctUntilChanged { old, new -> old.options == new.options }
                .collectLatest {
                    val options = it.options
                    val client = mkSpeechRecognizerClient(options)
                    _speechRecognizer.update { client }
                    checkSpeechRecognizerClient(client)
                }
        }
        viewModelScope.launch {
            _speechRecognizer
                .runningReduce { old, new ->
                    old?.stopRecognition()
                    old?.close()
                    new
                }
                .collect()
        }
    }

    suspend fun startRecognition() {
        _uiState.update { it.copy(status = Recognizing) }
        _speechRecognizer.value
            ?.startRecognition(
                speechRecognizerRequest {
                    audioSource = AudioSource.fromMic()
                }
            )
            ?.collect {
                when (it) {
                    is ErrorResponse -> {
                        val msg = it.e.message
                        _error.update { msg }
                        Log.e("TopScreenViewModel.startRecognition", "ErrorResponse", it.e)
                        _uiState.update { it.copy(status = Ready) }
                    }

                    is CompletedResponse -> {
                        _uiState.update { it.copy(status = Ready) }
                    }

                    is PartialTextResponse -> {
                        val t = it.text
                        _uiState.update { it.copy(recognizedPartial = t) }
                    }

                    is FinalTextResponse -> {
                        val t = it.text
                        _uiState.update {
                            it.copy(
                                recognizedFinal = it.recognizedFinal + t,
                                recognizedPartial = ""
                            )
                        }
                    }
                }
            }
    }

    suspend fun stopRecognition() {
        _speechRecognizer.value?.stopRecognition()
    }

    fun clear() {
        _uiState.update { it.copy(recognizedFinal = "", recognizedPartial = "") }
    }

    fun updateOptions(options: TopScreenState.Companion.RecognizerOptions) {
        _uiState.update { it.copy(options = options) }
    }

    override fun onCleared() {
        try {
            viewModelScope.launch {
                _speechRecognizer.value?.stopRecognition()
                _speechRecognizer.value?.close()
            }
        } catch (err: Exception) {
            Log.e("TopScreenViewModel.onCleared", "failed to clean speechRecognizer", err)
        }
        super.onCleared()
    }
}