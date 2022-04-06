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

package org.briarproject.mailbox.android

import android.content.Context
import android.text.format.DateUtils
import android.text.format.DateUtils.DAY_IN_MILLIS
import android.text.format.DateUtils.FORMAT_ABBREV_MONTH
import android.text.format.DateUtils.FORMAT_ABBREV_RELATIVE
import android.text.format.DateUtils.FORMAT_ABBREV_TIME
import android.text.format.DateUtils.FORMAT_SHOW_DATE
import android.text.format.DateUtils.WEEK_IN_MILLIS
import android.text.format.DateUtils.getRelativeDateTimeString
import android.text.format.DateUtils.getRelativeTimeSpanString
import org.briarproject.mailbox.R

object UiUtils {

    private const val MIN_DATE_RESOLUTION = DateUtils.MINUTE_IN_MILLIS

    fun formatDate(ctx: Context, time: Long): String {
        val flags = FORMAT_ABBREV_RELATIVE or FORMAT_SHOW_DATE or FORMAT_ABBREV_TIME or
            FORMAT_ABBREV_MONTH
        val now = System.currentTimeMillis()
        val diff = now - time
        return when {
            time == 0L -> ctx.getString(R.string.never)
            diff < MIN_DATE_RESOLUTION -> ctx.getString(R.string.now)
            // also show time when older than a day, but newer than a week
            diff in DAY_IN_MILLIS until WEEK_IN_MILLIS -> getRelativeDateTimeString(
                ctx, time, MIN_DATE_RESOLUTION, WEEK_IN_MILLIS, flags
            ).toString()
            // otherwise just show "...ago" or date string
            else -> getRelativeTimeSpanString(time, now, MIN_DATE_RESOLUTION, flags).toString()
        }
    }
}
