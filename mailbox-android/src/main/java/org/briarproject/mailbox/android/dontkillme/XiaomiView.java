package org.briarproject.mailbox.android.dontkillme;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.UiThread;

import static android.os.Build.BRAND;
import static org.briarproject.android.dontkillmelib.PowerUtils.getSystemProperty;
import static org.briarproject.android.dontkillmelib.PowerUtils.showOnboardingDialog;

@UiThread
public class XiaomiView extends PowerView {

	public XiaomiView(Context context) {
		this(context, null);
	}

	public XiaomiView(Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public XiaomiView(Context context, @Nullable AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		setText(org.briarproject.android.dontkillmelib.R.string.setup_xiaomi_text);
		setButtonText(
				org.briarproject.android.dontkillmelib.R.string.setup_xiaomi_button);
	}

	@Override
	public boolean needsToBeShown() {
		return isXiaomiOrRedmiDevice();
	}

	public static boolean isXiaomiOrRedmiDevice() {
		return "Xiaomi".equalsIgnoreCase(BRAND) ||
				"Redmi".equalsIgnoreCase(BRAND);
	}

	@Override
	@StringRes
	protected int getHelpText() {
		return org.briarproject.android.dontkillmelib.R.string.setup_xiaomi_help;
	}

	@Override
	protected void onButtonClick() {
		int bodyRes = isMiuiTenOrLater()
				?
				org.briarproject.android.dontkillmelib.R.string.setup_xiaomi_dialog_body_new
				:
				org.briarproject.android.dontkillmelib.R.string.setup_xiaomi_dialog_body_old;
		showOnboardingDialog(getContext(), getContext().getString(bodyRes));
		setChecked(true);
	}

	private boolean isMiuiTenOrLater() {
		String version = getSystemProperty("ro.miui.ui.version.name");
		if (version == null || version.equals("")) return false;
		version = version.replaceAll("[^\\d]", "");
		try {
			return Integer.parseInt(version) >= 10;
		} catch (NumberFormatException e) {
			return false;
		}
	}
}
