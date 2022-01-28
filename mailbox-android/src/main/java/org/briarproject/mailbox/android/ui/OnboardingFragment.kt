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
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat.getColorStateList
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import org.briarproject.mailbox.R
import org.briarproject.mailbox.databinding.FragmentOnboardingBinding

class Onboarding1Fragment : OnboardingFragment(
    number = 1,
    icon = R.mipmap.ic_launcher_round,
    title = R.string.onboarding_1_title,
    description = R.string.onboarding_1_description,
    topButtonAction = { nav ->
        nav.navigate(R.id.action_onboarding_1_to_2)
    },
    bottomButtonText = R.string.button_skip_intro,
    bottomButtonAction = {
        requireActivity().supportFinishAfterTransition()
    },
)

class Onboarding2Fragment : OnboardingFragment(
    number = 2,
    icon = R.mipmap.ic_launcher_round,
    title = R.string.onboarding_2_title,
    description = R.string.onboarding_2_description,
    topButtonAction = { nav ->
        nav.navigate(R.id.action_onboarding_2_to_3)
    },
    bottomButtonAction = { nav ->
        nav.popBackStack()
    },
)

class Onboarding3Fragment : OnboardingFragment(
    number = 3,
    icon = R.mipmap.ic_launcher_round,
    title = R.string.onboarding_3_title,
    description = R.string.onboarding_3_description,
    topButtonAction = { nav ->
        nav.navigate(R.id.action_onboarding_3_to_4)
    },
    bottomButtonAction = { nav ->
        nav.popBackStack()
    },
)

class Onboarding4Fragment : OnboardingFragment(
    number = 4,
    icon = R.mipmap.ic_launcher_round,
    title = R.string.onboarding_4_title,
    description = R.string.onboarding_4_description,
    topButtonAction = {
        requireActivity().supportFinishAfterTransition()
    },
    bottomButtonAction = { nav ->
        nav.popBackStack()
    },
)

abstract class OnboardingFragment(
    private val number: Int,
    @DrawableRes
    private val icon: Int,
    @StringRes
    private val title: Int,
    @StringRes
    private val description: Int,
    @StringRes
    private val bottomButtonText: Int = R.string.button_back,
    private val topButtonAction: Fragment.(NavController) -> Unit,
    private val bottomButtonAction: Fragment.(NavController) -> Unit,
) : Fragment() {

    private var _ui: FragmentOnboardingBinding? = null

    /**
     * This property is only valid between [onCreateView] and [onDestroyView].
     */
    private val ui get() = _ui!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _ui = FragmentOnboardingBinding.inflate(inflater, container, false)
        return ui.root
    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        ui.icon.setImageResource(icon)
        ui.title.setText(title)
        ui.description.setText(description)
        listOf(ui.bullet1, ui.bullet2, ui.bullet3, ui.bullet4).forEachIndexed { i, imageView ->
            val color = if (i + 1 <= number) R.color.briar_green else R.color.briar_night
            val tintList = getColorStateList(requireContext(), color)
            ImageViewCompat.setImageTintList(imageView, tintList)
        }
        val nav = findNavController()
        ui.topButton.setOnClickListener {
            topButtonAction(nav)
        }
        ui.bottomButton.setText(bottomButtonText)
        ui.bottomButton.setOnClickListener {
            bottomButtonAction(nav)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _ui = null
    }

}
