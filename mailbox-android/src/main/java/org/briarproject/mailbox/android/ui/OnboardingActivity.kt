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

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import org.briarproject.mailbox.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {

    private val viewModel: OnboardingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ui = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(ui.root)

        ui.pager.adapter = OnboardingAdapter(this)
        ui.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                viewModel.selectPage(position)
            }
        })
        viewModel.currentPage.observe(this) { position ->
            ui.pager.setCurrentItem(position, true)
        }
    }

    class OnboardingAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 5
        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> Onboarding0Fragment()
            1 -> Onboarding1Fragment()
            2 -> Onboarding2Fragment()
            3 -> Onboarding3Fragment()
            4 -> FinishFragment()
            else -> error("Unexpected OnboardingFragment: $position")
        }
    }

}
