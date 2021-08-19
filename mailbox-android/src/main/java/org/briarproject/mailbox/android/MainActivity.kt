package org.briarproject.mailbox.android

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.briarproject.mailbox.R
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MailboxViewModel by viewModels()
    private lateinit var statusTextView: TextView
    private lateinit var startStopButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val textView = findViewById<TextView>(R.id.text)
        val button = findViewById<Button>(R.id.button)
        statusTextView = findViewById(R.id.statusTextView)
        startStopButton = findViewById(R.id.startStopButton)

        button.setOnClickListener {
            viewModel.updateText("Tested")
        }

        // Start a coroutine in the lifecycle scope
        lifecycleScope.launch {
            // repeatOnLifecycle launches the block in a new coroutine every time the
            // lifecycle is in the STARTED state (or above) and cancels it when it's STOPPED.
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Trigger the flow and start listening for values.
                // Note that this happens when lifecycle is STARTED and stops
                // collecting when the lifecycle is STOPPED
                viewModel.lifecycleState.collect { onLifecycleStateChanged(it) }
            }
        }

        viewModel.text.observe(this, { text ->
            textView.text = text
        })
    }

    private fun onLifecycleStateChanged(state: LifecycleState) = when (state) {
        LifecycleState.STOPPED -> {
            statusTextView.text = state.name
            startStopButton.setText(R.string.start)
            startStopButton.setOnClickListener { viewModel.startLifecycle() }
            startStopButton.isEnabled = true
        }
        LifecycleState.RUNNING -> {
            statusTextView.text = state.name
            startStopButton.setText(R.string.stop)
            startStopButton.setOnClickListener { viewModel.stopLifecycle() }
            startStopButton.isEnabled = true
        }
        else -> {
            statusTextView.text = state.name
            startStopButton.isEnabled = false
        }
    }
}
