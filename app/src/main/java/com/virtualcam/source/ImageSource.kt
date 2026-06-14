package com.virtualcam.source

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.view.Surface
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Streams a static image (JPEG, PNG, WebP, etc.) to a [Surface] at ~30 fps.
 *
 * Usage:
 *   val src = ImageSource(context)
 *   src.setImageUri(someUri)
 *   src.start(surface, 1280, 720)
 */
class ImageSource(private val context: Context) : VideoSource {

    override val displayName = "Image"
    override val type        = SourceType.IMAGE

    private var bitmap: Bitmap? = null
    private var executor: ScheduledExecutorService? = null

    @Volatile private var running = false

    // ── Public API ──────────────────────────────────────────────────────────

    fun setImageUri(uri: Uri) {
        releaseBitmap()
        try {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            bitmap = ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator        = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hasImage(): Boolean = bitmap != null

    // ── VideoSource impl ────────────────────────────────────────────────────

    override fun start(surface: Surface, width: Int, height: Int) {
        running  = true
        executor = Executors.newSingleThreadScheduledExecutor()

        executor?.scheduleAtFixedRate(
            { renderFrame(surface, width, height) },
            0L, 33L, TimeUnit.MILLISECONDS     // 33 ms ≈ 30 fps
        )
    }

    override fun stop() {
        running = false
        executor?.shutdownNow()
        executor = null
    }

    override fun release() {
        stop()
        releaseBitmap()
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private fun renderFrame(surface: Surface, width: Int, height: Int) {
        if (!running) return
        var canvas: Canvas? = null
        try {
            canvas = surface.lockCanvas(null)
            canvas.drawColor(Color.BLACK)

            val bmp = bitmap
            if (bmp != null) {
                drawFitCenter(canvas, bmp, width, height)
            } else {
                drawPlaceholder(canvas, width, height)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { canvas?.let { surface.unlockCanvasAndPost(it) } } catch (_: Exception) {}
        }
    }

    /** Scale the bitmap to fill the frame while preserving aspect ratio. */
    private fun drawFitCenter(canvas: Canvas, bmp: Bitmap, w: Int, h: Int) {
        val scale   = minOf(w.toFloat() / bmp.width, h.toFloat() / bmp.height)
        val scaledW = bmp.width  * scale
        val scaledH = bmp.height * scale
        val left    = (w - scaledW) / 2f
        val top     = (h - scaledH) / 2f
        val dst     = RectF(left, top, left + scaledW, top + scaledH)
        canvas.drawBitmap(bmp, null, dst, null)
    }

    /** Draw a "tap to pick an image" placeholder when no URI is set. */
    private fun drawPlaceholder(canvas: Canvas, w: Int, h: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color    = Color.parseColor("#1A2340")
            style    = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

        paint.apply {
            color    = Color.parseColor("#00D9FF")
            textSize = w / 20f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("No image selected", w / 2f, h / 2f, paint)

        paint.apply {
            color    = Color.parseColor("#8899BB")
            textSize = w / 35f
        }
        canvas.drawText("Pick an image in the app", w / 2f, h / 2f + paint.textSize * 1.8f, paint)
    }

    private fun releaseBitmap() {
        bitmap?.recycle()
        bitmap = null
    }
}
