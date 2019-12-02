package life.ferreira.languagehelper

import android.Manifest
import android.content.pm.PackageManager
import android.media.*
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream


class VoiceActivity : AppCompatActivity() {
    private val sampleRate = 44100
    private val inChannelConfig = AudioFormat.CHANNEL_IN_MONO
    private val outChannelConfig = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val inBufSize = AudioRecord.getMinBufferSize(sampleRate, inChannelConfig, audioFormat)
    private val outBufSize = AudioTrack.getMinBufferSize(sampleRate, outChannelConfig, audioFormat);

    private var foxStream = ByteArrayOutputStream()
    private var stopRecordingFox: (((() -> Unit)?) -> Unit)? = null
    private var stopPlayingFox: (() -> Unit)? = null
    private lateinit var foxRecorder: AudioRecord
    private lateinit var foxSample: MediaPlayer

    private var boxStream = ByteArrayOutputStream()
    private var stopRecordingBox: (((() -> Unit)?) -> Unit)? = null
    private var stopPlayingBox: (() -> Unit)? = null
    private lateinit var boxRecorder: AudioRecord
    private lateinit var boxSample: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice)

        foxSample = MediaPlayer.create(applicationContext, R.raw.fox)
        foxRecorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            inChannelConfig,
            audioFormat,
            inBufSize * 10
        )

        boxSample = MediaPlayer.create(applicationContext, R.raw.box)
        boxRecorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            inChannelConfig,
            audioFormat,
            inBufSize * 10
        )

        this.checkPermissions()
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO),0)
        }
    }

    private fun pauseAll() {
        arrayOf(stopPlayingFox, stopPlayingBox).forEachIndexed { index, it ->
            if (it != null) {
                it()

                when (index) {
                    0 -> stopPlayingFox = null
                    1 -> stopPlayingBox = null
                }
            }
        }

        arrayOf(foxSample, boxSample).forEach {
            if (it != null && it.isPlaying) {
                it.pause()
                it.seekTo(0)
            }
        }

        arrayOf(stopRecordingFox, stopRecordingBox).forEachIndexed { index, it ->
            if (it != null) {
                it(null)

                when (index) {
                    0 -> stopRecordingFox = null
                    1 -> stopRecordingBox = null
                }
            }
        }
    }

    private fun toggle(player: MediaPlayer?) {
        if (player == null) {
            return
        }

        val wasPlaying = player.isPlaying

        pauseAll()

        if (!wasPlaying) {
            player.start()
        }
    }

    fun sampleFox(@Suppress("UNUSED_PARAMETER") view: View) {
        toggle(foxSample)
    }

    fun sampleBox(@Suppress("UNUSED_PARAMETER") view: View) {
        toggle(boxSample)
    }

    fun toggleRecord(view: View, wasRecording: Boolean, buttonId: Int, recorder: AudioRecord, stream: ByteArrayOutputStream, saveStop: (((() -> Unit)?) -> Unit) -> Unit) {
        pauseAll()

        if (!wasRecording) {
            val button = findViewById<Button>(buttonId)

            button.isEnabled = false

            saveStop(record(view as Button, recorder, stream) {
                button.isEnabled = true
            })
        }
    }

    fun recordFox(view: View) {
        toggleRecord(view, stopRecordingFox != null, R.id.playFox, foxRecorder, foxStream) {
            stopRecordingFox = it
        }
    }

    fun recordBox(view: View) {
        toggleRecord(view, stopRecordingBox != null, R.id.playBox, boxRecorder, boxStream) {
            stopRecordingBox = it
        }
    }

    fun doPlay(view: View, stream: ByteArrayOutputStream): () -> Unit {
        val at = AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, outChannelConfig, audioFormat, outBufSize, AudioTrack.MODE_STREAM);

        at.play();

        var playing = true
        val datas = stream.toByteArray().toList().chunked(1024).map { it.toByteArray() }

        (view as Button).text = "Stop"

        AsyncTask.execute {
            for (data in datas) {
                if (!playing) {
                    break
                }

                at.write(data, 0, data.size);
            }

            runOnUiThread {
                (view as Button).text = "Play"
            }

            at.stop();
            at.release();
        }

        return {
            playing = false
        }
    }

    fun playFox(view: View) {
        val wasPlaying = stopPlayingFox != null

        pauseAll()

        if (!wasPlaying) {
            stopPlayingFox = doPlay(view, foxStream)
        }
    }

    fun playBox(@Suppress("UNUSED_PARAMETER") view: View) {
        val wasPlaying = stopPlayingBox != null

        pauseAll()

        if (!wasPlaying) {
            stopPlayingBox = doPlay(view, boxStream)
        }
    }

    private fun record(button: Button, recorder: AudioRecord, stream: ByteArrayOutputStream, stopCallback: () -> Unit): ((() -> Unit)?) -> Unit {
        button.text = "Stop"

        stream.reset()

        var recording = true
        var stopCallbacks = mutableListOf(stopCallback)

        AsyncTask.execute {
            try {
                recorder.startRecording()

                val buffer = ByteArray(inBufSize)

                while (recording) { //reading data from MIC into buffer
                    recorder.read(buffer, 0, buffer.size)

                    stream.write(buffer)
                }

                recorder.stop()

                runOnUiThread {
                    button.text = "Record"
                    stopCallbacks.forEach { it() }
                }
            } catch (err: Exception) {
                runOnUiThread {
                    button.text = "Record"
                    stopCallbacks.forEach { it() }
                }
            }
        }

        return { callback ->
            if (callback != null) {
                stopCallbacks.add(callback)
            }

            recording = false
        }
    }
}
