package life.ferreira.languagehelper

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.AsyncTask
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.PrintWriter
import java.io.StringWriter
import java.net.Socket


class MainActivity : AppCompatActivity() {
    private var recording = false
    private lateinit var recorder: AudioRecord
    private lateinit var socket: Socket

    private lateinit var host: EditText
    private lateinit var port: EditText
    private lateinit var button: Button
    private lateinit var results: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        this.checkPermissions()

        host = findViewById(R.id.host)
        port = findViewById(R.id.port)
        button = findViewById(R.id.start)
        results = findViewById(R.id.results)

        button.setOnClickListener {
            if (this.recording) {
                this.stop()
            } else {
                this.start()
            }
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO),0)
        }
    }

    private fun connect() {
        this.socket = Socket(host.text.toString(), port.text.toString().toInt())
    }

    private fun disconnect() {
        if (this.socket.isConnected) {
            this.socket.close()
        }
    }

    val sampleRate = 44100
    val channelConfig = AudioFormat.CHANNEL_IN_MONO
    val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    val minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    private fun start() {
        this.button.text = "Stop"
        this.host.isEnabled = false
        this.port.isEnabled = false
        this.recording = true

        AsyncTask.execute {
            try {
                this.connect()

                val write = socket.getOutputStream()

                try {
                    recorder = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        sampleRate,
                        channelConfig,
                        audioFormat,
                        minBufSize * 10
                    )

                    recorder.startRecording()

                    val buffer = ByteArray(minBufSize)

                    while (this.recording) { //reading data from MIC into buffer
                        recorder.read(buffer, 0, buffer.size)

                        write.write(buffer)
                    }
                } catch (err: Exception) {
                    runOnUiThread {
                        this.stop()
                        results.text = "Failed to record:\n" +
                                "${getStack(err)}\n\n" +
                                "${results.text}"
                    }
                }

                try {
                    write.close()
                } catch (err: Exception) {
                    runOnUiThread {
                        this.stop()
                        results.text = "Failed to close writer:\n" +
                                "${getStack(err)}\n\n" +
                                "${results.text}"
                    }
                }

                try {
                    this.disconnect()
                } catch (err: Exception) {
                    runOnUiThread {
                        this.stop()
                        results.text = "Failed to disconnect socket:\n" +
                                "${getStack(err)}\n\n" +
                                "${results.text}"
                    }
                }

                try {
                    this.recorder.release()
                } catch (err: Exception) {
                    runOnUiThread {
                        this.stop()
                        results.text = "Failed to stop recorder:\n" +
                                "${getStack(err)}\n\n" +
                                "${results.text}"
                    }
                }
            } catch (err: Exception) {
                runOnUiThread {
                    this.stop()
                    results.text = "Failed to connect socket:\n" +
                            "${getStack(err)}\n\n" +
                            "${results.text}"
                }
            }
        }
    }

    private fun stop() {
        this.recording = false
        this.button.text = "Start"
        this.host.isEnabled = true
        this.port.isEnabled = true
    }

    private fun getStack(err: Exception): String {
        val writer = StringWriter()

        err.printStackTrace(PrintWriter(writer))

        return writer.toString()
    }
}
