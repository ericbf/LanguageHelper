package life.ferreira.languagehelper

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.PrintWriter
import java.io.StringWriter
import java.net.Socket

class DemoActivity : AppCompatActivity() {
    private var recording = false
    private lateinit var recorder: AudioRecord
    private lateinit var socket: Socket

    private lateinit var hostField: EditText
    private lateinit var portField: EditText
    private lateinit var startButton: Button
    private lateinit var resultsLabel: TextView

    private lateinit var actionSpinner: Spinner
    private lateinit var trainingCharSpace: Space
    private lateinit var trainingCharLabel: TextView
    private lateinit var trainingCharSpinner: Spinner

    private lateinit var trainValue: String
    private lateinit var classifyValue: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo)

        this.checkPermissions()

        hostField = findViewById(R.id.host)
        portField = findViewById(R.id.port)
        startButton = findViewById(R.id.start)
        resultsLabel = findViewById(R.id.results)

        actionSpinner = findViewById(R.id.actionSpinner)

        trainingCharSpace = findViewById(R.id.trainingCharSpace)
        trainingCharLabel = findViewById(R.id.trainingCharLabel)
        trainingCharSpinner = findViewById(R.id.trainingCharSpinner)

        val actions = resources.getStringArray(R.array.actions)

        trainValue = actions[0]
        classifyValue = actions[1]

        actionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (actionSpinner.selectedItem.toString()) {
                    trainValue -> {
                        trainingCharSpace.visibility = View.VISIBLE
                        trainingCharLabel.visibility = View.VISIBLE
                        trainingCharSpinner.visibility = View.VISIBLE
                    }
                    classifyValue -> {
                        trainingCharSpace.visibility = View.GONE
                        trainingCharLabel.visibility = View.GONE
                        trainingCharSpinner.visibility = View.GONE
                    }
                }
            }
        }
    }

    fun startClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        if (this.recording) {
            this.stop()
        } else {
            this.start()
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO),0)
        }
    }

    private fun connect() {
        this.socket = Socket(hostField.text.toString(), portField.text.toString().toInt())
    }

    private fun disconnect() {
        if (this.socket.isConnected) {
            this.socket.close()
        }
    }

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    private fun start() {
        this.startButton.text = "Stop"
        this.hostField.isEnabled = false
        this.portField.isEnabled = false
        this.recording = true

        AsyncTask.execute {
            try {
                this.connect()

                val output = socket.getOutputStream()

                var action = actionSpinner.selectedItem.toString()

                if (action.equals(trainValue)) {
                    action += " ${trainingCharSpinner.selectedItem}"
                }

                output.write("${action}\u0004".toByteArray(Charsets.UTF_8))
                output.flush()

                val input = socket.getInputStream()

                input.read()

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

                        output.write(buffer)
                    }
                } catch (err: Exception) {
                    runOnUiThread {
                        this.stop()
                        resultsLabel.text = "Failed to record:\n" +
                                "${getStack(err)}\n\n" +
                                "${resultsLabel.text}"
                    }
                }

                try {
                    output.close()
                } catch (err: Exception) {
                    runOnUiThread {
                        this.stop()
                        resultsLabel.text = "Failed to close writer:\n" +
                                "${getStack(err)}\n\n" +
                                "${resultsLabel.text}"
                    }
                }

                try {
                    this.disconnect()
                } catch (err: Exception) {
                    runOnUiThread {
                        this.stop()
                        resultsLabel.text = "Failed to disconnect socket:\n" +
                                "${getStack(err)}\n\n" +
                                "${resultsLabel.text}"
                    }
                }

                try {
                    this.recorder.release()
                } catch (err: Exception) {
                    runOnUiThread {
                        this.stop()
                        resultsLabel.text = "Failed to stop recorder:\n" +
                                "${getStack(err)}\n\n" +
                                "${resultsLabel.text}"
                    }
                }
            } catch (err: Exception) {
                runOnUiThread {
                    this.stop()
                    resultsLabel.text = "Failed to connect socket:\n" +
                            "${getStack(err)}\n\n" +
                            "${resultsLabel.text}"
                }
            }
        }
    }

    private fun stop() {
        this.recording = false
        this.startButton.text = "Start"
        this.hostField.isEnabled = true
        this.portField.isEnabled = true
    }

    private fun getStack(err: Exception): String {
        val writer = StringWriter()

        err.printStackTrace(PrintWriter(writer))

        return writer.toString()
    }
}
