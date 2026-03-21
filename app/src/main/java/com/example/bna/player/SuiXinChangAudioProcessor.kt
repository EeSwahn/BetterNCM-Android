package com.example.bna.player

import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer

class SuiXinChangAudioProcessor : BaseAudioProcessor() {
    var vocalVolume: Float = 1.0f

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.channelCount == 4 && inputAudioFormat.encoding == androidx.media3.common.C.ENCODING_PCM_16BIT) {
            return AudioFormat(inputAudioFormat.sampleRate, 2, inputAudioFormat.encoding)
        }
        return AudioFormat.NOT_SET
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        // 4 channels, 16-bit (2 bytes per sample) = 8 bytes per frame
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

            outputBuffer.putShort(outL.toShort())
            outputBuffer.putShort(outR.toShort())
        }
        
        outputBuffer.flip()
    }
}
