package org.briarproject.mailbox.android

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import org.briarproject.mailbox.R

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MailboxViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val textView = findViewById<TextView>(R.id.text)
        val button = findViewById<Button>(R.id.button)

        button.setOnClickListener {
            viewModel.updateText("Tested")
        }

        viewModel.text.observe(this, { text ->
            textView.text = text
        })
    }
}
