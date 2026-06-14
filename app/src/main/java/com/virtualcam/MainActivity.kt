package com.virtualcam

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.google.accompanist.permissions.*
import com.virtualcam.ui.MainScreen
import com.virtualcam.ui.MainViewModel
import com.virtualcam.ui.theme.BgDeep
import com.virtualcam.ui.theme.VirtualCamTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            VirtualCamTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding(),
                    color = BgDeep
                ) {
                    PermissionGate {
                        MainScreen(viewModel)
                    }
                }
            }
        }
    }
}

// ── Permission gate ───────────────────────────────────────────────────────────

/**
 * Requests the permissions the service needs before showing the main UI.
 *
 * Required:
 *  - POST_NOTIFICATIONS  (Android 13+, needed for foreground service notification)
 *  - CAMERA              (needed if we later add a live camera source)
 *
 * READ_MEDIA_VIDEO and READ_MEDIA_IMAGES are requested at the point of use
 * (when the user taps "Pick image / video") via the system file picker, so we
 * don't need to explicitly request them here.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionGate(content: @Composable () -> Unit) {
    val permissions = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.CAMERA
        )
    )

    LaunchedEffect(Unit) {
        if (!permissions.allPermissionsGranted) {
            permissions.launchMultiplePermissionRequest()
        }
    }

    // Always show the content – permissions gate is non-blocking,
    // the service itself will report errors if a permission is missing.
    content()
}
