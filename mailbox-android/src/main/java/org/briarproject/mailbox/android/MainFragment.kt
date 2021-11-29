package org.briarproject.mailbox.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.briarproject.mailbox.R
import org.briarproject.mailbox.core.lifecycle.LifecycleManager

@AndroidEntryPoint
class MainFragment : Fragment() {

    private val viewModel: MailboxViewModel by activityViewModels()
    private lateinit var statusTextView: TextView
    private lateinit var startStopButton: Button
    private lateinit var wipeButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        val textView = v.findViewById<TextView>(R.id.text)
        val button = v.findViewById<Button>(R.id.button)
        statusTextView = v.findViewById(R.id.statusTextView)
        startStopButton = v.findViewById(R.id.startStopButton)
        wipeButton = v.findViewById(R.id.wipeButton)

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

        viewModel.text.observe(viewLifecycleOwner, { text ->
            textView.text = text
        })
    }

    private fun onLifecycleStateChanged(state: LifecycleManager.LifecycleState) = when (state) {
        LifecycleManager.LifecycleState.NOT_STARTED -> {
            statusTextView.text = state.name
            startStopButton.setText(R.string.start)
            startStopButton.setOnClickListener { viewModel.startLifecycle() }
            startStopButton.isEnabled = true
        }
        LifecycleManager.LifecycleState.RUNNING -> {
            statusTextView.text = state.name
            startStopButton.setText(R.string.stop)
            startStopButton.setOnClickListener { viewModel.stopLifecycle() }
            wipeButton.setOnClickListener { viewModel.wipe() }
            startStopButton.isEnabled = true
            wipeButton.isEnabled = true
        }
        else -> {
            statusTextView.text = state.name
            startStopButton.isEnabled = false
            wipeButton.isEnabled = false
        }
    }

}
