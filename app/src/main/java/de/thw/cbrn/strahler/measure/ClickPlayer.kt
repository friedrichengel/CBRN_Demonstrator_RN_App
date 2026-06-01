package de.thw.cbrn.strahler.measure

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.exp
import kotlin.math.sin

/**
 * Plays a short Geiger-counter "tick". The click is a synthesised, exponentially decaying
 * tone burst (~6 ms) held in a static AudioTrack buffer and re-triggered per click.
 */
class ClickPlayer {

    private val sampleRate = 44_100
    private val clickSamples: ShortArray = buildClick()
    private var track: AudioTrack? = null

    private fun buildClick(): ShortArray {
        val durationMs = 6
        val n = sampleRate * durationMs / 1000
        val freq = 1800.0      // tone of the tick
        val decay = 480.0      // envelope decay rate (1/s) → sharp, click-like
        return ShortArray(n) { i ->
            val t = i.toDouble() / sampleRate
            val env = exp(-decay * t)
            val s = sin(2.0 * Math.PI * freq * t) * env
            (s * Short.MAX_VALUE * 0.9).toInt().toShort()
        }
    }

    private fun ensureTrack(): AudioTrack {
        track?.let { return it }
        val bytes = clickSamples.size * 2
        val t = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            bytes,
            AudioTrack.MODE_STATIC,
            AudioManager.AUDIO_SESSION_ID_GENERATE,
        )
        t.write(clickSamples, 0, clickSamples.size)
        track = t
        return t
    }

    /** Triggers one click. Safe to call from a background thread. */
    fun click() {
        val t = runCatching { ensureTrack() }.getOrNull() ?: return
        runCatching {
            t.stop()
            t.reloadStaticData()
            t.play()
        }
    }

    fun release() {
        runCatching { track?.release() }
        track = null
    }
}
