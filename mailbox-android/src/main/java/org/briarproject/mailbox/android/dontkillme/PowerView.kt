/*
 *     Briar Mailbox
 *     Copyright (C) 2021-2022  The Briar Project
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package org.briarproject.mailbox.android.dontkillme

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat.getDrawable
import org.briarproject.mailbox.R
import org.briarproject.mailbox.android.dontkillme.DoNotKillMeUtils.showOnboardingDialog

@UiThread
abstract class PowerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    interface OnCheckedChangedListener {
        fun onCheckedChanged()
    }

    private val textView: TextView
    private val icon: ImageView
    private val checkImage: ImageView
    private val button: Button
    private var checked = false
    private var onCheckedChangedListener: OnCheckedChangedListener? = null

    init {
        val inflater = context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val v = inflater.inflate(R.layout.power_view, this, true)
        textView = v.findViewById(R.id.textView)
        icon = v.findViewById(R.id.icon)
        checkImage = v.findViewById(R.id.checkImage)
        button = v.findViewById(R.id.button)
        button.setOnClickListener { onButtonClick() }
        val helpButton = v.findViewById<ImageButton>(R.id.helpButton)
        helpButton.setOnClickListener { onHelpButtonClick() }

        // we need to manage the checkImage state ourselves, because automatic
        // state saving is done based on the view's ID and there can be
        // multiple ImageViews with the same ID in the view hierarchy
        isSaveFromParentEnabled = true
        if (!isInEditMode && !needsToBeShown()) {
            visibility = GONE
        }
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        return SavedState(superState)
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        val ss = state as SavedState
        super.onRestoreInstanceState(ss.superState)
        setChecked(ss.value[0]) // also calls listener
    }

    abstract fun needsToBeShown(): Boolean

    fun setChecked(checked: Boolean) {
        this.checked = checked
        if (checked) {
            checkImage.visibility = VISIBLE
        } else {
            checkImage.visibility = INVISIBLE
        }
        if (onCheckedChangedListener != null) {
            onCheckedChangedListener!!.onCheckedChanged()
        }
    }

    fun isChecked(): Boolean {
        return visibility == GONE || checked
    }

    fun setOnCheckedChangedListener(onCheckedChangedListener: OnCheckedChangedListener) {
        this.onCheckedChangedListener = onCheckedChangedListener
    }

    @get:StringRes
    protected abstract val helpText: Int

    protected fun setText(@StringRes res: Int) {
        textView.setText(res)
    }

    protected fun setIcon(@DrawableRes drawable: Int) {
        icon.setImageDrawable(getDrawable(context, drawable))
    }

    protected fun setButtonText(@StringRes res: Int) {
        button.setText(res)
    }

    protected abstract fun onButtonClick()

    private fun onHelpButtonClick() {
        showOnboardingDialog(context, context.getString(helpText))
    }

    private class SavedState : BaseSavedState {
        val value = booleanArrayOf(false)

        constructor(superState: Parcelable?) : super(superState)

        private constructor(inValue: Parcel) : super(inValue) {
            inValue.readBooleanArray(value)
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeBooleanArray(value)
        }

        companion object {
            @Suppress("unused")
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(`in`: Parcel): SavedState {
                    return SavedState(`in`)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }
}
