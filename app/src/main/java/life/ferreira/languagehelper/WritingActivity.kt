package life.ferreira.languagehelper

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class WritingActivity : AppCompatActivity() {
    private lateinit var sample: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_writing)

        sample = findViewById(R.id.sample)
    }

    var samples = mutableListOf(
        "1 2 3 4 5 6 7 8 9 0",
        "Pack my box with five dozen liquor jugs",
        "0 1 2 0 1 2",
        "A quart jar of oil mixed with zinc oxide makes a very bright paint.",
        "2 3 4 2 3 4",
        "A quick movement of the enemy will jeopardize six gunboats.",
        "4 5 6 4 5 6",
        "A wizard’s job is to vex chumps quickly in fog.",
        "6 7 8 6 7 8",
        "Amazingly few discotheques provide jukeboxes.",
        "8 9 0 8 9 0",
        "Few black taxis drive up major roads on quiet hazy nights.",
        "12  31  24  53",
        "The public was amazed to view the quickness and dexterity of the juggler.",
        "76  78  98  10",
        "The quick brown fox jumps over the lazy dog"
    )

    fun nextSample(@Suppress("UNUSED_PARAMETER") view: View) {
        samples.add(samples.removeAt(0))
        sample.text = samples.last()
    }
}
