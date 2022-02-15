package org.briarproject.android.dontkillmelib;

import android.content.Context;

import static org.briarproject.android.dontkillmelib.PowerUtils.huaweiAppLaunchNeedsToBeShown;
import static org.briarproject.android.dontkillmelib.PowerUtils.huaweiProtectedAppsNeedsToBeShown;
import static org.briarproject.android.dontkillmelib.PowerUtils.isXiaomiOrRedmiDevice;
import static org.briarproject.android.dontkillmelib.PowerUtils.needsDozeWhitelisting;

public class DozeHelperImpl implements DozeHelper {
	@Override
	public boolean needToShowDoNotKillMeFragment(Context context) {
		Context appContext = context.getApplicationContext();
		return needsDozeWhitelisting(appContext) ||
				huaweiProtectedAppsNeedsToBeShown(appContext) ||
				huaweiAppLaunchNeedsToBeShown(appContext) ||
				isXiaomiOrRedmiDevice();
	}
}
