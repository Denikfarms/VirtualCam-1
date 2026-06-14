package com.virtualcam.source

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.view.Surface
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Decodes a video file and renders it to a [Surface] using MediaCodec.
 *
 * The video loops by default. Set [loopEnabled] = false to stop after one play.
 *
 * Usage:
 *   val src = VideoFileSource(context)
 *   src.setVideoUri(someUri)
 *   src.loopEnabled = true
 *   src.start(surface, 1280, 720)
 */
class VideoFileSource(private val context: Context) : VideoSource {

    override val displayName = "Video File"
    override val type        = SourceType.VIDEO

    var loopEnabled = true
    var videoUri: Uri? = null
        private set

    private var executor: ExecutorService? = null

    @Volatile private var running = false

    // ── Public API ──────────────────────────────────────────────────────────

    fun setVideoUri(uri: Uri) {
        videoUri = uri
    }

    fun hasVideo(): Boolean = videoUri != null

    // ── VideoSource impl ────────────────────────────────────────────────────

    override fun start(surface: Surface, width: Int, height: Int) {
        val uri = videoUri ?: return
        running  = true
        executor = Executors.newSingleThreadExecutor()
        executor?.execute { decodeLoop(uri, surface) }
    }

    override fun stop() {
        running = false
        executor?.shutdownNow()
        executor = null
    }

    override fun release() = stop()

    // ── Decode loop ─────────────────────────────────────────────────────────

    /**
     * Runs a MediaExtractor + MediaCodec pipeline until stopped.
     * Loops the video when [loopEnabled] is true.
     */
    private fun decodeLoop(uri: Uri, surface: Surface) {
        do {
            val extractor = MediaExtractor()
            var decoder: MediaCodec? = null

            try {
                extractor.setDataSource(context, uri, null)

                // ── locate the video track ──────────────────────────────────
                var trackIndex = -1
                var format: MediaFormat? = null
                for (i in 0 until extractor.trackCount) {
                    val fmt  = extractor.getTrackFormat(i)
                    val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                    if (mime.startsWith("video/")) {
                        trackIndex = i
                        format     = fmt
                        break
                    }
                }
                if (trackIndex < 0 || format == null) { running = false; break }
                extractor.selectTrack(trackIndex)

                // ── set up decoder ──────────────────────────────────────────
                val mime = format.getString(MediaFormat.KEY_MIME)!!
                decoder = MediaCodec.createDecoderByType(mime)
                decoder.configure(format, surface, null, 0)
                decoder.start()

                val bufInfo  = MediaCodec.BufferInfo()
                var inputEOS = false

                while (running) {
                    // feed compressed data in
                    if (!inputEOS) {
                        val idx = decoder.dequeueInputBuffer(10_000L)
                        if (idx >= 0) {
                            val buf        = decoder.getInputBuffer(idx)!!
                            val sampleSize = extractor.readSampleData(buf, 0)
                            if (sampleSize < 0) {
                                decoder.queueInputBuffer(
                                    idx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                inputEOS = true
                            } else {
                                decoder.queueInputBuffer(
                                    idx, 0, sampleSize, extractor.sampleTime, 0
                                )
                                extractor.advance()
                            }
                        }
                    }

                    // pull decoded frames out
                    val outIdx = decoder.dequeueOutputBuffer(bufInfo, 10_000L)
                    if (outIdx >= 0) {
                        val render = bufInfo.size > 0
                        decoder.releaseOutputBuffer(outIdx, render)

                        // honour presentation timestamps for correct playback speed
                        if (render && bufInfo.presentationTimeUs > 0) {
                            Thread.sleep(maxOf(0, bufInfo.presentationTimeUs / 1_000 - 16))
                        }

                        if (bufInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                    }
                }

            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            } catch (e: Exception) {
                e.printStackTrace()
                running = false
            } finally {
                try { decoder?.stop(); decoder?.release() } catch (_: Exception) {}
                extractor.release()
            }

        } while (running && loopEnabled)

        running = false
    }
}
