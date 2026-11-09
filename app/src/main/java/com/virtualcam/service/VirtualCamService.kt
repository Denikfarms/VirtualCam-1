package com.virtualcam.service

import android.app.Service
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.virtualcam.manager.VideoSourceManager
import com.virtualcam.source.ImageSource
import com.virtualcam.source.ScreenCaptureSource
import com.virtualcam.source.SourceType
import com.virtualcam.source.VideoFileSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * VirtualCamService
 * ─────────────────
 * Foreground service that manages video sources and feeds them to a virtual
 * camera session so other apps (Zoom, WhatsApp, etc.) can use your custom feed.
 *
 * ┌──────────────────────────────────────────────────────────────────────────┐
 * │  ANDROID 14 VirtualCamera API – integration point                       │
 * │                                                                          │
 * │  Extend this service from android.hardware.camera2.VirtualCameraService │
 * │  (or the equivalent class in your SDK build).                            │
 * │                                                                          │
 * │  The system will call onVirtualCameraOpened(cameraId) when another app  │
 * │  selects your virtual camera. Inside that callback:                      │
 * │                                                                          │
 * │    val session = VirtualCamera.open(cameraId, this, mainExecutor) {     │
 * │        // 'it' is VirtualCamera.Session                                 │
 * │        val cfg = it.currentStreamConfig               // chosen res      │
 * │        sourceManager.attachSurface(it.surface,                          │
 * │            cfg.width, cfg.height)                                        │
 * │        sourceManager.startStreaming()                                    │
 * │        updateNotification()                                              │
 * │    }                                                                     │
 * │                                                                          │
 * │  In onVirtualCameraClosed(cameraId) call sourceManager.stopStreaming().  │
 * │                                                                          │
 * │  Reference:                                                              │
 * │  https://developer.android.com/reference/android/hardware/camera2/      │
 * │              (search VirtualCamera* in your SDK 34 sources)              │
 * └──────────────────────────────────────────────────────────────────────────┘
 *
 * Until the VirtualCamera API is wired in, the service runs the full source
 * pipeline and notification — just without an active camera consumer.
 */
class VirtualCamService : Service() {

    // ── Static state (observable by UI without binding) ──────────────────────

    companion object {
        private const val TAG = "VirtualCamService"

        const val ACTION_STOP          = "com.virtualcam.ACTION_STOP"
        const val ACTION_SWITCH_SOURCE = "com.virtualcam.ACTION_SWITCH_SOURCE"
        const val EXTRA_SOURCE_TYPE    = "extra_source_type"

        /** Whether the service is currently running. */
        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        /** Whether a virtual camera consumer is actively receiving frames. */
        private val _isStreaming = MutableStateFlow(false)
        val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

        /** The VideoSourceManager – exposed so MainActivity can update source config. */
        var sourceManager: VideoSourceManager? = null
            private set

        // Convenience start / stop helpers

        fun start(context: Context) {
            context.startForegroundService(Intent(context, VirtualCamService::class.java))
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, VirtualCamService::class.java).apply {
                    action = ACTION_STOP
                }
            )
        }
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        NotificationHelper.createChannel(this)
        sourceManager = VideoSourceManager(
            imageSource  = ImageSource(applicationContext),
            videoSource  = VideoFileSource(applicationContext),
            screenSource = ScreenCaptureSource(applicationContext)
        )
        _isRunning.value = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {

            ACTION_STOP -> {
                Log.d(TAG, "Received stop action")
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_SWITCH_SOURCE -> {
                val typeName = intent.getStringExtra(EXTRA_SOURCE_TYPE)
                val type = typeName?.let { runCatching { SourceType.valueOf(it) }.getOrNull() }
                type?.let {
                    Log.d(TAG, "Switching source to $it")
                    sourceManager?.switchSource(it)
                    updateNotification()
                }
            }

            else -> {
                // First start – promote to foreground
                startForeground(
                    NotificationHelper.NOTIFICATION_ID,
                    NotificationHelper.build(this, sourceManager?.active?.displayName ?: "Image")
                )

                // ─────────────────────────────────────────────────────────────
                // TODO: Call initVirtualCamera() here once the VirtualCamera
                //       API is integrated.  The method stub is below.
                // ─────────────────────────────────────────────────────────────
                Log.d(TAG, "Service started – waiting for virtual camera consumer")
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        teardownVirtualCamera()
        sourceManager?.release()
        sourceManager = null
        _isRunning.value   = false
        _isStreaming.value = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── VirtualCamera API integration stubs ──────────────────────────────────

    /**
     * TODO: Replace this stub with real VirtualCamera API calls.
     *
     * Minimal skeleton (SDK 34):
     *
     *   override fun onVirtualCameraOpened(cameraId: String) {
     *       val session = VirtualCamera.open(cameraId, this, mainExecutor) { sess ->
     *           val cfg = sess.currentStreamConfig
     *           sourceManager?.attachSurface(sess.surface, cfg.width, cfg.height)
     *           sourceManager?.startStreaming()
     *           _isStreaming.value = true
     *           updateNotification()
     *       }
     *   }
     *
     *   override fun onVirtualCameraClosed(cameraId: String) {
     *       sourceManager?.stopStreaming()
     *       _isStreaming.value = false
     *       updateNotification()
     *   }
     */
    private fun initVirtualCamera() {
        Log.d(TAG, "initVirtualCamera – stub; wire in VirtualCamera API here")
    }

    private fun teardownVirtualCamera() {
        Log.d(TAG, "teardownVirtualCamera")
        sourceManager?.stopStreaming()
        _isStreaming.value = false
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun updateNotification() {
        val sm       = sourceManager ?: return
        val notif    = NotificationHelper.build(this, sm.active.displayName)
        val manager  = getSystemService(NotificationManager::class.java)
        manager.notify(NotificationHelper.NOTIFICATION_ID, notif)
    }
}
