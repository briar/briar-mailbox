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
import android.util.AttributeSet
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.annotation.UiThread
import org.briarproject.android.dontkillmelib.HuaweiUtils.appLaunchNeedsToBeShown
import org.briarproject.android.dontkillmelib.HuaweiUtils.huaweiAppLaunchIntents
import org.briarproject.mailbox.R
import org.slf4j.LoggerFactory.getLogger

@UiThread
internal class HuaweiAppLaunchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : PowerView(context, attrs, defStyleAttr) {

    companion object {
        private val LOG = getLogger(HuaweiAppLaunchView::class.java)
    }

    init {
        setText(R.string.dnkm_huawei_app_launch_text)
        setIcon(R.drawable.ic_restore_mirrored_white)
        setButtonText(R.string.dnkm_huawei_app_launch_button)
    }

    override fun needsToBeShown(): Boolean {
        return appLaunchNeedsToBeShown(context)
    }

    override val helpText: Int = R.string.dnkm_huawei_app_launch_help

    override fun onButtonClick() {
        for (i in huaweiAppLaunchIntents) {
            try {
                context.startActivity(i)
                setChecked(true)
                return
            } catch (e: Exception) {
                LOG.warn("Error launching intent", e)
            }
        }
        Toast.makeText(context, R.string.dnkm_huawei_app_launch_error_toast, LENGTH_LONG).show()
        // Let the user continue with setup
        setChecked(true)
    }
}
