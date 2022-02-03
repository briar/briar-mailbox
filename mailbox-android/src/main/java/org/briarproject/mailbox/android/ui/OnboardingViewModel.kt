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

package org.briarproject.mailbox.android.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.distinctUntilChanged
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    app: Application,
    handle: SavedStateHandle,
) : AndroidViewModel(app) {

    private val _currentPage = handle.getLiveData("currentPage", 0)
    val currentPage: LiveData<Int> = _currentPage.distinctUntilChanged() // prevent infinite loop

    fun selectPage(position: Int) {
        _currentPage.value = position
    }

}
