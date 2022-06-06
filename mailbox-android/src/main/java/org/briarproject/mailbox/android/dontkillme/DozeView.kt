package org.briarproject.mailbox.android.dontkillme

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.UiThread
import org.briarproject.android.dontkillmelib.DozeUtils.needsDozeWhitelisting
import org.briarproject.mailbox.R

@UiThread
internal class DozeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : PowerView(context, attrs, defStyleAttr) {

    companion object {
        fun needsToBeShown(context: Context?): Boolean {
            return needsDozeWhitelisting(context!!)
        }
    }

    private var onButtonClickListener: Runnable? = null

    init {
        setText(R.string.dnkm_doze_intro)
        setIcon(R.drawable.ic_battery_alert_white)
        setButtonText(R.string.dnkm_doze_button)
    }

    override fun needsToBeShown(): Boolean {
        return needsToBeShown(context)
    }

    override val helpText: Int = R.string.dnkm_doze_explanation

    override fun onButtonClick() {
        checkNotNull(onButtonClickListener)
        onButtonClickListener!!.run()
    }

    fun setOnButtonClickListener(runnable: Runnable) {
        onButtonClickListener = runnable
    }
}
