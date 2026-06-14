package com.virtualcam.manager

import android.view.Surface
import com.virtualcam.source.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Owns all [VideoSource] instances and routes the active one to the output [Surface].
 *
 * The manager is created inside [VirtualCamService] and survives for as long as
 * the service is alive. Sources may be swapped mid-stream without stopping the
 * virtual camera session.
 *
 * The [VideoSourceManager] is also exposed as a companion-object singleton so that
 * the UI layer can read source state without a full service binding.
 */
class VideoSourceManager(
    val imageSource: ImageSource,
    val videoSource: VideoFileSource,
    val screenSource: ScreenCaptureSource
) {
    // ── State ────────────────────────────────────────────────────────────────

    private val _activeType = MutableStateFlow(SourceType.IMAGE)
    val activeType: StateFlow<SourceType> = _activeType.asStateFlow()

    private var outputSurface: Surface? = null
    private var outputWidth   = DEFAULT_WIDTH
    private var outputHeight  = DEFAULT_HEIGHT

    @Volatile var isStreaming = false
        private set

    val allSources: List<VideoSource> get() = listOf(imageSource, videoSource, screenSource)

    val active: VideoSource
        get() = when (_activeType.value) {
            SourceType.IMAGE  -> imageSource
            SourceType.VIDEO  -> videoSource
            SourceType.SCREEN -> screenSource
        }

    // ── Surface wiring ───────────────────────────────────────────────────────

    /** Called by VirtualCamService once the VirtualCamera session provides a Surface. */
    fun attachSurface(surface: Surface, width: Int, height: Int) {
        outputSurface = surface
        outputWidth   = width
        outputHeight  = height
    }

    // ── Streaming control ────────────────────────────────────────────────────

    fun startStreaming() {
        val surface = outputSurface ?: return
        if (isStreaming) return
        isStreaming = true
        active.start(surface, outputWidth, outputHeight)
    }

    fun stopStreaming() {
        if (!isStreaming) return
        isStreaming = false
        active.stop()
    }

    /**
     * Hot-swap the active source. If streaming, the old source is stopped and
     * the new one is started immediately on the same surface.
     */
    fun switchSource(type: SourceType) {
        if (type == _activeType.value) return

        if (isStreaming) active.stop()
        _activeType.value = type
        if (isStreaming) {
            outputSurface?.let { active.start(it, outputWidth, outputHeight) }
        }
    }

    // ── Cleanup ──────────────────────────────────────────────────────────────

    fun release() {
        stopStreaming()
        allSources.forEach { it.release() }
        outputSurface = null
    }

    companion object {
        const val DEFAULT_WIDTH  = 1280
        const val DEFAULT_HEIGHT = 720
    }
}
