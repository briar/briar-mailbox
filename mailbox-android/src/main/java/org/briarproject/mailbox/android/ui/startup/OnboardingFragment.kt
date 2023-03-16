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

package org.briarproject.mailbox.android.ui.startup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat.getColorStateList
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import org.briarproject.mailbox.R
import org.briarproject.mailbox.android.ui.MailboxViewModel
import org.briarproject.mailbox.databinding.FragmentOnboardingBinding

class Onboarding0Fragment : OnboardingFragment(
    number = 0,
    icon = R.drawable.il_onboarding_1,
    title = R.string.onboarding_0_title,
    description = R.string.onboarding_0_description,
    bottomButtonText = R.string.button_skip_intro,
    bottomButtonAction = { viewModel ->
        viewModel.onOnboardingComplete()
    },
    backButtonAction = {
        requireActivity().finishAffinity()
    },
)

class Onboarding1Fragment : OnboardingFragment(
    number = 1,
    icon = R.drawable.il_onboarding_2,
    title = R.string.onboarding_1_title,
    description = R.string.onboarding_1_description,
)

class Onboarding2Fragment : OnboardingFragment(
    number = 2,
    icon = R.drawable.il_onboarding_3,
    title = R.string.onboarding_2_title,
    description = R.string.onboarding_2_description,
)

class Onboarding3Fragment : OnboardingFragment(
    number = 3,
    icon = R.drawable.il_onboarding_4,
    title = R.string.onboarding_3_title,
    description = R.string.onboarding_3_description,
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
    private val topButtonAction: OnboardingFragment.(MailboxViewModel) -> Unit = { viewModel ->
        viewModel.selectOnboardingPage(number + 1)
    },
    private val bottomButtonAction: OnboardingFragment.(MailboxViewModel) -> Unit =
        { viewModel ->
            viewModel.selectOnboardingPage(number - 1)
        },
    private val backButtonAction: OnboardingFragment.(MailboxViewModel) -> Unit =
        bottomButtonAction,
) : Fragment() {

    private val viewModel: MailboxViewModel by activityViewModels()
    private var _ui: FragmentOnboardingBinding? = null

    /**
     * This property is only valid between [onCreateView] and [onDestroyView].
     */
    private val ui get() = _ui!!

    // This callback will only be called when this Fragment is at least Resumed, not Started.
    private val callback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            backButtonAction(viewModel)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _ui = FragmentOnboardingBinding.inflate(inflater, container, false)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
        return ui.root
    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        ui.icon.setImageResource(icon)
        ui.title.setText(title)
        ui.description.setText(description)
        listOf(ui.bullet1, ui.bullet2, ui.bullet3, ui.bullet4).forEachIndexed { i, imageView ->
            val color = if (i == number) R.color.briar_green else R.color.briar_night
            val tintList = getColorStateList(requireContext(), color)
            ImageViewCompat.setImageTintList(imageView, tintList)
        }
        ui.topButton.setOnClickListener {
            topButtonAction(viewModel)
        }
        ui.bottomButton.setText(bottomButtonText)
        ui.bottomButton.setOnClickListener {
            bottomButtonAction(viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        callback.isEnabled = true
    }

    override fun onPause() {
        super.onPause()
        callback.isEnabled = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _ui = null
    }
}

class FinishFragment : Fragment() {

    private val viewModel: MailboxViewModel by activityViewModels()

    override fun onResume() {
        super.onResume()
        viewModel.onOnboardingComplete()
    }
}
