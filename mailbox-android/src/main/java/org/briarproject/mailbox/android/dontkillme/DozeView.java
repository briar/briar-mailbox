package org.briarproject.mailbox.android.dontkillme;

import android.content.Context;
import android.util.AttributeSet;

import org.briarproject.mailbox.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import static org.briarproject.android.dontkillmelib.PowerUtils.needsDozeWhitelisting;

@UiThread
public class DozeView extends PowerView {

	@Nullable
	private Runnable onButtonClickListener;

	public DozeView(Context context) {
		this(context, null);
	}

	public DozeView(Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public DozeView(Context context, @Nullable AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		setText(org.briarproject.android.dontkillmelib.R.string.setup_doze_intro);
		setIcon(R.drawable.ic_battery_alert_white);
		setButtonText(
				org.briarproject.android.dontkillmelib.R.string.setup_doze_button);
	}

	@Override
	public boolean needsToBeShown() {
		return needsToBeShown(getContext());
	}

	public static boolean needsToBeShown(Context context) {
		return needsDozeWhitelisting(context);
	}

	@Override
	protected int getHelpText() {
		return org.briarproject.android.dontkillmelib.R.string.setup_doze_explanation;
	}

	@Override
	protected void onButtonClick() {
		if (onButtonClickListener == null) throw new IllegalStateException();
		onButtonClickListener.run();
	}

	public void setOnButtonClickListener(@NonNull Runnable runnable) {
		onButtonClickListener = runnable;
	}

}
