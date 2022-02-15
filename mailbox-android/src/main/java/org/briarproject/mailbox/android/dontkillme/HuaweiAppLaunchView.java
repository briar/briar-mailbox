package org.briarproject.mailbox.android.dontkillme;

import android.content.Context;
import android.util.AttributeSet;

import org.briarproject.mailbox.R;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.UiThread;

import static org.briarproject.android.dontkillmelib.PowerUtils.getHuaweiPowerManagerIntent;
import static org.briarproject.android.dontkillmelib.PowerUtils.huaweiAppLaunchNeedsToBeShown;

@UiThread
public class HuaweiAppLaunchView extends PowerView {

	public HuaweiAppLaunchView(Context context) {
		this(context, null);
	}

	public HuaweiAppLaunchView(Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public HuaweiAppLaunchView(Context context, @Nullable AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		setText(org.briarproject.android.dontkillmelib.R.string.setup_huawei_app_launch_text);
		setIcon(R.drawable.ic_restore_mirrored_white);
		setButtonText(
				org.briarproject.android.dontkillmelib.R.string.setup_huawei_app_launch_button);
	}

	@Override
	public boolean needsToBeShown() {
		return huaweiAppLaunchNeedsToBeShown(getContext());
	}

	@Override
	@StringRes
	protected int getHelpText() {
		return org.briarproject.android.dontkillmelib.R.string.setup_huawei_app_launch_help;
	}

	@Override
	protected void onButtonClick() {
		getContext().startActivity(getHuaweiPowerManagerIntent());
		setChecked(true);
	}

}
