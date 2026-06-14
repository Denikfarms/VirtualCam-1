package com.virtualcam.source

import android.view.Surface

/**
 * Common interface for all virtual camera video sources.
 *
 * A VideoSource accepts an Android [Surface] and drives it with frames at
 * whatever rate is appropriate for that source type (typically 30 fps).
 *
 * Lifecycle:
 *   1. Create the source with its Context and any config.
 *   2. Optionally call [configure] to set source-specific parameters.
 *   3. Call [start] once a Surface is available (from VirtualCamera session).
 *   4. Call [stop] when the camera session ends.
 *   5. Call [release] when the service is destroyed.
 */
interface VideoSource {
    val displayName: String
    val type: SourceType

    /** Begin pushing frames to [surface]. Called from a worker thread. */
    fun start(surface: Surface, width: Int, height: Int)

    /** Stop pushing frames. May be called more than once safely. */
    fun stop()

    /** Free all resources. After release, start() must not be called again. */
    fun release()
}

enum class SourceType(val displayName: String, val description: String) {
    IMAGE(
        displayName  = "Image",
        description  = "Stream a still photo as your camera"
    ),
    VIDEO(
        displayName  = "Video File",
        description  = "Loop a video file as your camera feed"
    ),
    SCREEN(
        displayName  = "Screen Share",
        description  = "Broadcast your screen as your camera"
    )
}
