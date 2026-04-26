package net.jj1uzh.playspeechrecognition.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.audio.AudioSource
import com.google.mlkit.genai.speechrecognition.SpeechRecognition
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerOptions.Mode.Companion.MODE_ADVANCED
import com.google.mlkit.genai.speechrecognition.speechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.speechRecognizerRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.jj1uzh.playspeechrecognition.ui.TopScreenState.Companion.ModelStatus.Downloading
import net.jj1uzh.playspeechrecognition.ui.TopScreenState.Companion.RecognizerStatus.ModelPreparing
import java.util.Locale

data class TopScreenState(
    val recognizedFinal: String = "",
    val recognizedPartial: String = "",
    val status: RecognizerStatus = Unknown
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
    }
}

class TopScreenViewModel : ViewModel() {

    private var _uiState = MutableStateFlow(TopScreenState())
    val uiState = _uiState.asStateFlow()

    private var _modelTotalBytes: MutableStateFlow<Long?> = MutableStateFlow(null)
    val modelTotalBytes = _modelTotalBytes.asStateFlow()

    private var _error: MutableStateFlow<String?> = MutableStateFlow(null)
    val error = _error.asStateFlow()

    val speechRecognizer = SpeechRecognition.getClient(
        speechRecognizerOptions {
            locale = Locale.JAPAN
            preferredMode = MODE_ADVANCED
        }
    )

    init {
        viewModelScope.launch {
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
    }

    suspend fun startRecognition() {
        _uiState.update { it.copy(status = Recognizing) }
        speechRecognizer
            .startRecognition(
                speechRecognizerRequest {
                    audioSource = AudioSource.fromMic()
                }
            )
            .collect {
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
        speechRecognizer.stopRecognition()
    }

    fun clear() {
        _uiState.update { it.copy(recognizedFinal = "", recognizedPartial = "") }
    }

    override fun onCleared() {
        try {
            viewModelScope.launch {
                speechRecognizer.stopRecognition()
                speechRecognizer.close()
            }
        } catch (err: Exception) {
            Log.e("TopScreenViewModel.onCleared", "failed to clean speechRecognizer", err)
        }
        super.onCleared()
    }
}