package org.briarproject.mailbox.android.dontkillme;

import android.content.Context;

import org.briarproject.mailbox.R;

import androidx.appcompat.app.AlertDialog;

public class DoNotKillMeUtils {

	static void showOnboardingDialog(Context ctx, String text) {
		new AlertDialog.Builder(ctx,
				R.style.OnboardingDialogTheme)
				.setMessage(text)
				.setNeutralButton(
						org.briarproject.android.dontkillmelib.R.string.got_it,
						(dialog, which) -> dialog.cancel())
				.show();
	}
}
