package com.virtualcam.source

import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.view.Surface

/**
 * Captures the device screen and mirrors it to a [Surface] using [MediaProjection].
 *
 * PERMISSION FLOW:
 * The user must grant screen-capture permission before you call [start].
 * Typical usage from an Activity:
 *
 *   val pm = getSystemService(MediaProjectionManager::class.java)
 *   startActivityForResult(pm.createScreenCaptureIntent(), REQUEST_CODE)
 *
 *   // In onActivityResult:
 *   screenSource.setProjectionPermission(resultCode, data)
 *   screenSource.start(surface, width, height)
 */
class ScreenCaptureSource(private val context: Context) : VideoSource {

    override val displayName = "Screen Share"
    override val type        = SourceType.SCREEN

    private var projectionResultCode: Int    = -1
    private var projectionData: Intent?      = null
    private var mediaProjection: MediaProjection?  = null
    private var virtualDisplay: VirtualDisplay?    = null

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Store the result from the system screen-capture permission dialog.
     * Must be called before [start].
     */
    fun setProjectionPermission(resultCode: Int, data: Intent) {
        projectionResultCode = resultCode
        projectionData       = data
    }

    fun hasPermission(): Boolean = projectionData != null

    // ── VideoSource impl ────────────────────────────────────────────────────

    override fun start(surface: Surface, width: Int, height: Int) {
        val data = projectionData ?: return

        val manager = context.getSystemService(MediaProjectionManager::class.java)
        mediaProjection = manager.getMediaProjection(projectionResultCode, data)

        // Register a stop callback so we know if the system revokes projection
        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                virtualDisplay?.release()
                virtualDisplay = null
            }
        }, null)

        val density = context.resources.displayMetrics.densityDpi

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "VirtualCam-Screen",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            surface,
            null, null
        )
    }

    override fun stop() {
        virtualDisplay?.release()
        virtualDisplay = null
        mediaProjection?.stop()
        mediaProjection = null
    }

    override fun release() = stop()
}
