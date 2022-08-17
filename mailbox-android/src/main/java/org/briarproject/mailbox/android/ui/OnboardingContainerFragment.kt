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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import org.briarproject.mailbox.databinding.FragmentOnboardingContainerBinding

class OnboardingContainerFragment : Fragment() {

    private val viewModel: MailboxViewModel by activityViewModels()
    private var _ui: FragmentOnboardingContainerBinding? = null

    /**
     * This property is only valid between [onCreateView] and [onDestroyView].
     */
    private val ui get() = _ui!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _ui = FragmentOnboardingContainerBinding.inflate(inflater, container, false)
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ui.pager.adapter = OnboardingAdapter(this)
        ui.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                viewModel.selectOnboardingPage(position)
            }
        })
        viewModel.currentOnboardingPage.observe(viewLifecycleOwner) { position ->
            ui.pager.setCurrentItem(position, true)
        }
    }

    class OnboardingAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _ui = null
    }
}
