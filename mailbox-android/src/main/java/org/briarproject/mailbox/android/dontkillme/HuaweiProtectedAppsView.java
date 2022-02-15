package org.briarproject.mailbox.android.dontkillme;

import android.content.Context;
import android.util.AttributeSet;

import org.briarproject.android.dontkillmelib.PowerUtils;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.UiThread;

import static org.briarproject.android.dontkillmelib.PowerUtils.getHuaweiProtectedAppsIntent;

@UiThread
public class HuaweiProtectedAppsView extends PowerView {

	public HuaweiProtectedAppsView(Context context) {
		this(context, null);
	}

	public HuaweiProtectedAppsView(Context context,
			@Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public HuaweiProtectedAppsView(Context context,
			@Nullable AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		setText(org.briarproject.android.dontkillmelib.R.string.setup_huawei_text);
		setButtonText(
				org.briarproject.android.dontkillmelib.R.string.setup_huawei_button);
	}

	@Override
	public boolean needsToBeShown() {
		return PowerUtils.huaweiProtectedAppsNeedsToBeShown(getContext());
	}

	@Override
	@StringRes
	protected int getHelpText() {
		return org.briarproject.android.dontkillmelib.R.string.setup_huawei_help;
	}

	@Override
	protected void onButtonClick() {
		getContext().startActivity(getHuaweiProtectedAppsIntent());
		setChecked(true);
	}

}
