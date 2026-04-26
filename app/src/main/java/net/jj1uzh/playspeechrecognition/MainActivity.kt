package net.jj1uzh.playspeechrecognition

import android.Manifest
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import net.jj1uzh.playspeechrecognition.ui.TopScreen
import net.jj1uzh.playspeechrecognition.ui.theme.PlaySpeechRecognitionTheme

class MainActivity : ComponentActivity() {

    private val permissionRequestLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PERMISSION_GRANTED
        ) {
            permissionRequestLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        enableEdgeToEdge()
        setContent {
            PlaySpeechRecognitionTheme {
                TopScreen()
            }
        }
    }
}
