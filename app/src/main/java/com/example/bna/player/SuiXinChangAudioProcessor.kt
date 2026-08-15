package com.example.bna.player

import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import kotlin.math.abs

class SuiXinChangAudioProcessor : BaseAudioProcessor() {
    var vocalVolume: Float = 1.0f
    
    @Volatile
    var currentAmplitude: Float = 0f
        private set

    private var is4Channel = false
    private var is2Channel = false
    
    // IIR Low-pass filter states for bass detection
    private var lpfLeft = 0f
    private var lpfRight = 0f

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding == androidx.media3.common.C.ENCODING_PCM_16BIT) {
            if (inputAudioFormat.channelCount == 4) {
                is4Channel = true
                is2Channel = false
                return AudioFormat(inputAudioFormat.sampleRate, 2, inputAudioFormat.encoding)
            } else if (inputAudioFormat.channelCount == 2) {
                is4Channel = false
                is2Channel = true
                return AudioFormat(inputAudioFormat.sampleRate, 2, inputAudioFormat.encoding)
            }
        }
        is4Channel = false
        is2Channel = false
        return AudioFormat.NOT_SET
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        var maxAmp = 0

        if (is4Channel) {
            val frameCount = remaining / 8
            val outputBuffer = replaceOutputBuffer(frameCount * 4)

            for (i in 0 until frameCount) {
                val accompanyL = inputBuffer.short
                val accompanyR = inputBuffer.short
                val vocalL = inputBuffer.short
                val vocalR = inputBuffer.short

                var outL = (accompanyL + vocalL * vocalVolume).toInt()
                var outR = (accompanyR + vocalR * vocalVolume).toInt()

                outL = outL.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                outR = outR.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())

                if (i % 8 == 0) {
                    lpfLeft = 0.1f * outL + 0.9f * lpfLeft
                    lpfRight = 0.1f * outR + 0.9f * lpfRight
                    val amp = (abs(lpfLeft) + abs(lpfRight)).toInt()
                    if (amp > maxAmp) maxAmp = amp
                }

                outputBuffer.putShort(outL.toShort())
                outputBuffer.putShort(outR.toShort())
            }
            outputBuffer.flip()
        } else if (is2Channel) {
            val frameCount = remaining / 4
            val startPos = inputBuffer.position()
            // Sample amplitude directly from the input buffer without advancing position
            for (i in 0 until frameCount step 8) {
                val l = inputBuffer.getShort(startPos + i * 4).toFloat()
                val r = inputBuffer.getShort(startPos + i * 4 + 2).toFloat()
                lpfLeft = 0.1f * l + 0.9f * lpfLeft
                lpfRight = 0.1f * r + 0.9f * lpfRight
                val amp = (abs(lpfLeft) + abs(lpfRight)).toInt()
                if (amp > maxAmp) maxAmp = amp
            }
            
            // Fast bulk copy pass-through
            val outputBuffer = replaceOutputBuffer(remaining)
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
        } else {
            inputBuffer.position(inputBuffer.limit())
            return
        }

        // Normalize sum of L+R (max possible is 32767*2 = 65534)
        // We use a lower denominator to make the beats more prominent
        val normalized = (maxAmp / 40000f).coerceIn(0f, 1f)
        
        // Fast attack, slower decay for beat effect
        if (normalized > currentAmplitude) {
            currentAmplitude = normalized
        } else {
            currentAmplitude = currentAmplitude * 0.85f + normalized * 0.15f
        }
    }
}
