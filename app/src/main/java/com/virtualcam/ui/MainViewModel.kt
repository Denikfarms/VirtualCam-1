package com.virtualcam.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.virtualcam.service.VirtualCamService
import com.virtualcam.source.SourceType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Bridges the UI layer with [VirtualCamService] and the [VideoSourceManager].
 *
 * The ViewModel deliberately avoids binding to the service; instead it reads
 * the companion-object StateFlows and sends commands via Intents, which keeps
 * the relationship simple and avoids lifecycle leaks.
 */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    // ── Service state (forwarded from VirtualCamService companion) ───────────

    val isServiceRunning: StateFlow<Boolean> = VirtualCamService.isRunning
    val isStreaming: StateFlow<Boolean>       = VirtualCamService.isStreaming

    // ── Source selection ─────────────────────────────────────────────────────

    private val _activeSource = MutableStateFlow(SourceType.IMAGE)
    val activeSource: StateFlow<SourceType> = _activeSource.asStateFlow()

    // ── Image source config ──────────────────────────────────────────────────

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    // ── Video source config ──────────────────────────────────────────────────

    private val _selectedVideoUri = MutableStateFlow<Uri?>(null)
    val selectedVideoUri: StateFlow<Uri?> = _selectedVideoUri.asStateFlow()

    private val _videoLoopEnabled = MutableStateFlow(true)
    val videoLoopEnabled: StateFlow<Boolean> = _videoLoopEnabled.asStateFlow()

    // ── Screen capture state ─────────────────────────────────────────────────

    private val _hasScreenPermission = MutableStateFlow(false)
    val hasScreenPermission: StateFlow<Boolean> = _hasScreenPermission.asStateFlow()

    // ── Service control ──────────────────────────────────────────────────────

    fun toggleService() {
        if (isServiceRunning.value) {
            VirtualCamService.stop(getApplication())
        } else {
            VirtualCamService.start(getApplication())
        }
    }

    // ── Source management ────────────────────────────────────────────────────

    fun selectSource(type: SourceType) {
        _activeSource.value = type

        // Tell a running service to switch its active source
        if (isServiceRunning.value) {
            val ctx = getApplication<Application>()
            ctx.startService(
                Intent(ctx, VirtualCamService::class.java).apply {
                    action = VirtualCamService.ACTION_SWITCH_SOURCE
                    putExtra(VirtualCamService.EXTRA_SOURCE_TYPE, type.name)
                }
            )
        }

        // Mirror the selection into the source manager (if service is running)
        VirtualCamService.sourceManager?.switchSource(type)
    }

    // ── Image helpers ────────────────────────────────────────────────────────

    fun setImageUri(uri: Uri) {
        _selectedImageUri.value = uri
        VirtualCamService.sourceManager?.imageSource?.setImageUri(uri)
    }

    // ── Video helpers ────────────────────────────────────────────────────────

    fun setVideoUri(uri: Uri) {
        _selectedVideoUri.value = uri
        VirtualCamService.sourceManager?.videoSource?.setVideoUri(uri)
    }

    fun setVideoLoop(enabled: Boolean) {
        _videoLoopEnabled.value = enabled
        VirtualCamService.sourceManager?.videoSource?.loopEnabled = enabled
    }

    // ── Screen helpers ───────────────────────────────────────────────────────

    fun onScreenPermissionGranted(resultCode: Int, data: Intent) {
        _hasScreenPermission.value = true
        VirtualCamService.sourceManager?.screenSource?.setProjectionPermission(resultCode, data)
    }
}
