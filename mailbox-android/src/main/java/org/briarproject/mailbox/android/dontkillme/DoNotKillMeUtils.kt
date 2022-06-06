package org.briarproject.mailbox.android.dontkillme;

import android.content.Context;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.briarproject.mailbox.R;

public class DoNotKillMeUtils {

	static void showOnboardingDialog(Context ctx, String text) {
		new MaterialAlertDialogBuilder(ctx)
				.setMessage(text)
				.setNeutralButton(R.string.dnkm_got_it,
						(dialog, which) -> dialog.cancel())
				.show();
	}
}
