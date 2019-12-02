package life.ferreira.languagehelper

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun goToVoice(@Suppress("UNUSED_PARAMETER") view: View) {
        startActivity(Intent(this, VoiceActivity::class.java))
    }

    fun goToWriting(@Suppress("UNUSED_PARAMETER") view: View) {
        startActivity(Intent(this, WritingActivity::class.java))
    }

    fun goToDemo(@Suppress("UNUSED_PARAMETER") view: View) {
        startActivity(Intent(this, DemoActivity::class.java))
    }
}
