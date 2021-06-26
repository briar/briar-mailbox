package org.briarproject.mailbox

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

    lateinit var applicationComponent: ApplicationComponent

    @Inject
    lateinit var mailboxViewModel: MailboxViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        (applicationContext as MailboxApplication).appComponent.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val textView = findViewById<TextView>(R.id.text)
        textView.setText(mailboxViewModel.text)
    }
}