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
import androidx.annotation.UiThread
import org.briarproject.android.dontkillmelib.HuaweiUtils.huaweiProtectedAppsIntent
import org.briarproject.android.dontkillmelib.HuaweiUtils.protectedAppsNeedsToBeShown
import org.briarproject.mailbox.R

@UiThread
internal class HuaweiProtectedAppsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : PowerView(context, attrs, defStyleAttr) {

    init {
        setText(R.string.dnkm_huawei_protected_text)
        setIcon(R.drawable.ic_security_white)
        setButtonText(R.string.dnkm_huawei_protected_button)
    }

    override fun needsToBeShown(): Boolean {
        return protectedAppsNeedsToBeShown(context)
    }

    override val helpText: Int = R.string.dnkm_huawei_protected_help

    override fun onButtonClick() {
        context.startActivity(huaweiProtectedAppsIntent)
        setChecked(true)
    }
}
