package com.eat868299

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableNativeMap
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.modules.core.PermissionListener
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max

class MealVoiceRecorderModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    companion object {
        private const val SAMPLE_RATE = 16_000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val RECORD_AUDIO_CODE = 9010
    }

    private var audioRecord: AudioRecord? = null
    private var recordingFile: File? = null
    private var pcmBuffer: ByteArrayOutputStream? = null
    private var recordingThread: Thread? = null
    @Volatile private var isRecording = false
    private var startedAt = 0L

    override fun getName(): String = "MealVoiceRecorder"

    @ReactMethod
    fun requestPermission(promise: Promise) {
        val ctx = reactApplicationContext
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            promise.resolve(true)
            return
        }
        val activity = reactApplicationContext.currentActivity
        if (activity !is PermissionAwareActivity) {
            promise.resolve(false)
            return
        }
        activity.requestPermissions(
            arrayOf(Manifest.permission.RECORD_AUDIO),
            RECORD_AUDIO_CODE,
            PermissionListener { code, _, results ->
                if (code == RECORD_AUDIO_CODE) {
                    promise.resolve(results.firstOrNull() == PackageManager.PERMISSION_GRANTED)
                    true
                } else {
                    false
                }
            }
        )
    }

    @ReactMethod
    fun startRecording(promise: Promise) {
        if (isRecording) {
            promise.reject("already_recording", "A recording session is already active")
            return
        }

        val ctx = reactApplicationContext
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            promise.reject("permission_denied", "RECORD_AUDIO permission not granted")
            return
        }

        val bufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufSize * 4
        )

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            promise.reject("init_failed", "AudioRecord failed to initialize")
            return
        }

        val pcm = ByteArrayOutputStream()
        val wavFile = File(ctx.cacheDir, "what-to-eat-${UUID.randomUUID()}.wav")

        audioRecord = record
        recordingFile = wavFile
        pcmBuffer = pcm
        isRecording = true
        startedAt = System.currentTimeMillis()

        record.startRecording()

        recordingThread = Thread {
            val buf = ByteArray(bufSize)
            while (isRecording) {
                val read = record.read(buf, 0, buf.size)
                if (read > 0) pcm.write(buf, 0, read)
            }
        }.also { it.start() }

        promise.resolve(WritableNativeMap().apply {
            putString("uri", "file://${wavFile.absolutePath}")
            putString("path", wavFile.absolutePath)
        })
    }

    @ReactMethod
    fun stopRecording(promise: Promise) {
        val record = audioRecord
        val wavFile = recordingFile
        val pcm = pcmBuffer

        if (record == null || wavFile == null || pcm == null) {
            promise.reject("not_recording", "No active recording session")
            return
        }

        isRecording = false
        record.stop()
        record.release()
        audioRecord = null

        recordingThread?.join(3_000)
        recordingThread = null

        val durationMs = max(0, (System.currentTimeMillis() - startedAt).toInt())
        startedAt = 0
        recordingFile = null
        pcmBuffer = null

        try {
            writePcmToWav(wavFile, pcm.toByteArray(), SAMPLE_RATE, 1, 16)
        } catch (e: Exception) {
            promise.reject("write_failed", "Failed to write WAV: ${e.message}", e)
            return
        }

        promise.resolve(WritableNativeMap().apply {
            putString("uri", "file://${wavFile.absolutePath}")
            putString("path", wavFile.absolutePath)
            putInt("durationMs", durationMs)
        })
    }

    private fun writePcmToWav(output: File, pcmData: ByteArray, sampleRate: Int, channels: Int, bitsPerSample: Int) {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val dataSize = pcmData.size

        FileOutputStream(output).use { fos ->
            DataOutputStream(fos).use { dos ->
                // RIFF header
                dos.writeBytes("RIFF")
                dos.writeIntLE(36 + dataSize)
                dos.writeBytes("WAVE")
                // fmt chunk
                dos.writeBytes("fmt ")
                dos.writeIntLE(16)
                dos.writeShortLE(1)          // PCM
                dos.writeShortLE(channels)
                dos.writeIntLE(sampleRate)
                dos.writeIntLE(byteRate)
                dos.writeShortLE(blockAlign)
                dos.writeShortLE(bitsPerSample)
                // data chunk
                dos.writeBytes("data")
                dos.writeIntLE(dataSize)
                dos.write(pcmData)
            }
        }
    }

    private fun DataOutputStream.writeIntLE(value: Int) {
        write(value and 0xff)
        write((value shr 8) and 0xff)
        write((value shr 16) and 0xff)
        write((value shr 24) and 0xff)
    }

    private fun DataOutputStream.writeShortLE(value: Int) {
        write(value and 0xff)
        write((value shr 8) and 0xff)
    }
}
