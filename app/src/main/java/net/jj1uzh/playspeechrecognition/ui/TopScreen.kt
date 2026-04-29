package net.jj1uzh.playspeechrecognition.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import net.jj1uzh.playspeechrecognition.R
import net.jj1uzh.playspeechrecognition.ui.theme.PlaySpeechRecognitionTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopScreen(
    modifier: Modifier = Modifier,
    viewModel: TopScreenViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val error by viewModel.error.collectAsState()
    val modelTotalBytes by viewModel.modelTotalBytes.collectAsState()

    var optionsSheetOpened by rememberSaveable { mutableStateOf(false) }

    val onClickRecognizeButton: () -> Unit = {
        when (uiState.status) {
            is ModelPreparing, Unknown -> {}
            Ready -> {
                coroutineScope.launch { viewModel.startRecognition() }
            }

            Recognizing -> {
                coroutineScope.launch { viewModel.stopRecognition() }
            }
        }
    }
    val recognizeButtonText = when (val recognizerStatus = uiState.status) {
        Ready -> "Recognize"
        Recognizing -> "Stop"
        Unknown -> "..."
        is ModelPreparing -> when (val modelStatus = recognizerStatus.status) {
            DownloadFailed -> "!"
            is Downloading -> "${modelStatus.downloaded / 1000} / ${modelTotalBytes?.div(1000) ?: "?"}"
            Unavailable -> "Unavailable"
        }
    }

    LaunchedEffect(error) {
        error?.also {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    TopScreenContent(
        recognizedFinalText = uiState.recognizedFinal,
        recognizedPartialText = uiState.recognizedPartial,
        recognizeButtonText = recognizeButtonText,
        onClickRecognizeButton = onClickRecognizeButton,
        onClickClearButton = viewModel::clear,
        onClickOptionsButton = { optionsSheetOpened = true },
        modifier = modifier,
    )

    if (optionsSheetOpened) {
        ModalBottomSheet(
            onDismissRequest = { optionsSheetOpened = false },
        ) {
            SpeechRecognizerOptionsScreen(
                options = uiState.options,
                onUpdateOptions = viewModel::updateOptions,
                modifier = Modifier.background(color = BottomSheetDefaults.ContainerColor),
                containerColor = BottomSheetDefaults.ContainerColor,
            )
        }
    }
}

@Composable
private fun TopScreenContent(
    recognizedFinalText: String,
    recognizedPartialText: String,
    recognizeButtonText: String,
    onClickRecognizeButton: () -> Unit,
    onClickClearButton: () -> Unit,
    onClickOptionsButton: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = buildAnnotatedString {
                            append(recognizedFinalText)
                            withStyle(SpanStyle(color = Color.Gray)) {
                                append(recognizedPartialText)
                            }
                        },
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = onClickRecognizeButton) {
                    Text(recognizeButtonText)
                }
                Button(onClick = onClickClearButton) {
                    Text("Clear")
                }
                IconButton(onClick = onClickOptionsButton) {
                    Image(
                        painter = painterResource(R.drawable.icon_settings),
                        contentDescription = "recognizer options",
                    )
                }
            }
        }
    }
}

@Composable
@Preview
private fun TopScreenContentPreview() {
    PlaySpeechRecognitionTheme {
        TopScreenContent(
            recognizedFinalText = "Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello Hello",
            recognizedPartialText = "this is partial text.",
            recognizeButtonText = "Stop",
            onClickRecognizeButton = {},
            onClickClearButton = {},
            onClickOptionsButton = {},
        )
    }
}
