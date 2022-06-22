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
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import org.briarproject.android.dontkillmelib.XiaomiUtils.xiaomiLockAppsIntent
import org.briarproject.android.dontkillmelib.XiaomiUtils.xiaomiLockAppsNeedsToBeShown
import org.briarproject.mailbox.R
import org.briarproject.mailbox.core.util.LogUtils.logException
import org.slf4j.LoggerFactory.getLogger

@UiThread
class XiaomiLockAppsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : PowerView(context, attrs, defStyleAttr) {

    companion object {
        private val LOG = getLogger(XiaomiLockAppsView::class.java)
    }

    init {
        setText(R.string.dnkm_xiaomi_lock_apps_text)
        setButtonText(R.string.dnkm_xiaomi_lock_apps_button)
    }

    override fun needsToBeShown(): Boolean {
        return xiaomiLockAppsNeedsToBeShown(context)
    }

    @get:StringRes
    override val helpText: Int
        get() = R.string.dnkm_xiaomi_lock_apps_help

    override fun onButtonClick() {
        try {
            context.startActivity(xiaomiLockAppsIntent)
            setChecked(true)
            return
        } catch (e: SecurityException) {
            logException(LOG, e, "Error starting XiaomiLockAppsIntent")
        }
        Toast.makeText(context, R.string.dnkm_xiaomi_lock_apps_error_toast, LENGTH_LONG).show()
        // Let the user continue with setup
        setChecked(true)
    }
}
