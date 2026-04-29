package net.jj1uzh.playspeechrecognition.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeechRecognizerOptionsScreen(
    options: TopScreenState.Companion.RecognizerOptions,
    onUpdateOptions: (TopScreenState.Companion.RecognizerOptions) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Unspecified,
) {
    var localeDialogOpened by rememberSaveable { mutableStateOf(false) }
    var modeDialogOpened by rememberSaveable { mutableStateOf(false) }

    SpeechRecognizerOptionsScreenContent(
        options = options,
        modifier = modifier,
        onClickLocaleOption = { localeDialogOpened = true },
        onClickModeOption = { modeDialogOpened = true },
        containerColor = containerColor,
    )

    if (localeDialogOpened) {
        BasicAlertDialog(
            onDismissRequest = { localeDialogOpened = false }
        ) {
            Surface(
                modifier = Modifier
                    .wrapContentWidth()
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.large,
                tonalElevation = AlertDialogDefaults.TonalElevation,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    TopScreenState.Companion.RecognizerOptions.Companion.RecognizerLocale.entries.forEach { l ->
                        ListItem(
                            leadingContent = {
                                RadioButton(
                                    selected = l == options.locale,
                                    onClick = {},
                                )
                            },
                            headlineContent = { Text(l.displayName()) },
                            modifier = Modifier.clickable(
                                onClick = {
                                    onUpdateOptions(options.copy(locale = l))
                                    localeDialogOpened = false
                                },
                            ),
                        )
                    }
                }
            }
        }
    }

    if (modeDialogOpened) {
        BasicAlertDialog(
            onDismissRequest = { modeDialogOpened = false },
        ) {
            Surface(
                modifier = Modifier
                    .wrapContentWidth()
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.large,
                tonalElevation = AlertDialogDefaults.TonalElevation,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    TopScreenState.Companion.RecognizerOptions.Companion.RecognizerMode.entries.forEach { m ->
                        ListItem(
                            leadingContent = {
                                RadioButton(
                                    selected = m == options.mode,
                                    onClick = {},
                                )
                            },
                            headlineContent = { Text(m.displayName()) },
                            modifier = Modifier.clickable(
                                onClick = {
                                    onUpdateOptions(options.copy(mode = m))
                                    modeDialogOpened = false
                                },
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeechRecognizerOptionsScreenContent(
    options: TopScreenState.Companion.RecognizerOptions,
    onClickLocaleOption: () -> Unit,
    onClickModeOption: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Unspecified,
) {
    Scaffold(
        topBar = {
            Text(
                text = "Recognizer Options",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(8.dp),
            )
        },
        modifier = modifier
            .padding(16.dp)
            .background(color = containerColor),
        containerColor = containerColor,
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding),
        ) {
            ListItem(
                headlineContent = {
                    Text("Locale")
                },
                supportingContent = {
                    Text(options.locale.displayName())
                },
                modifier = Modifier.clickable(
                    onClick = onClickLocaleOption,
                )
            )
            ListItem(
                headlineContent = {
                    Text("Mode")
                },
                supportingContent = {
                    Text(options.mode.displayName())
                },
                modifier = Modifier.clickable(
                    onClick = onClickModeOption,
                )
            )
        }
    }

}

@Composable
@Preview
private fun SpeechRecognizerOptionsScreenContentPreview() {
    SpeechRecognizerOptionsScreenContent(
        options = TopScreenState.Companion.RecognizerOptions(locale = JaJP, mode = Advanced),
        onClickLocaleOption = {},
        onClickModeOption = {},
    )
}