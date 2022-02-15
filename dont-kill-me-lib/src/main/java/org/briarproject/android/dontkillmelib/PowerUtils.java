package org.briarproject.android.dontkillmelib;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.PowerManager;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import static android.content.Context.POWER_SERVICE;
import static android.content.pm.PackageManager.MATCH_DEFAULT_ONLY;
import static android.os.Build.BRAND;
import static android.os.Build.VERSION.SDK_INT;
import static android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS;
import static java.lang.Runtime.getRuntime;

public class PowerUtils {

	private final static String PACKAGE_NAME_HUAWEI =
			"com.huawei.systemmanager";
	private final static String CLASS_NAME_POWER_MANAGER =
			PACKAGE_NAME_HUAWEI + ".power.ui.HwPowerManagerActivity";
	private final static String CLASS_NAME_PROTECTED_APPS =
			PACKAGE_NAME_HUAWEI + ".optimize.process.ProtectActivity";

	public static boolean needsDozeWhitelisting(Context ctx) {
		if (SDK_INT < 23) return false;
		PowerManager pm = (PowerManager) ctx.getSystemService(POWER_SERVICE);
		String packageName = ctx.getPackageName();
		if (pm == null) throw new AssertionError();
		return !pm.isIgnoringBatteryOptimizations(packageName);
	}

	@TargetApi(23)
	@SuppressLint("BatteryLife")
	public static Intent getDozeWhitelistingIntent(Context ctx) {
		Intent i = new Intent();
		i.setAction(ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
		i.setData(Uri.parse("package:" + ctx.getPackageName()));
		return i;
	}

	public static void showOnboardingDialog(Context ctx, String text) {
		new AlertDialog.Builder(ctx, R.style.OnboardingDialogTheme)
				.setMessage(text)
				.setNeutralButton(R.string.got_it,
						(dialog, which) -> dialog.cancel())
				.show();
	}

	/**
	 * Determine whether a Huawei "Protected apps" feature is available on the
	 * device.
	 */
	public static boolean huaweiAppLaunchNeedsToBeShown(Context context) {
		// "App launch" was introduced in EMUI 8 (Android 8.0)
		if (SDK_INT < 26) return false;
		PackageManager pm = context.getPackageManager();
		List<ResolveInfo> resolveInfos =
				pm.queryIntentActivities(getHuaweiProtectedAppsIntent(),
						MATCH_DEFAULT_ONLY);
		return !resolveInfos.isEmpty();
	}

	/**
	 * Determine whether a Huawei "Protected apps" feature is available on the
	 * device.
	 */
	public static boolean huaweiProtectedAppsNeedsToBeShown(Context context) {
		// "Protected apps" no longer exists on Huawei EMUI 5.0 (Android 7.0)
		if (SDK_INT >= 24) return false;
		PackageManager pm = context.getPackageManager();
		List<ResolveInfo> resolveInfos = pm.queryIntentActivities(
				getHuaweiPowerManagerIntent(),
				MATCH_DEFAULT_ONLY);
		return !resolveInfos.isEmpty();
	}

	public static Intent getHuaweiPowerManagerIntent() {
		Intent intent = new Intent();
		intent.setClassName(PACKAGE_NAME_HUAWEI, CLASS_NAME_POWER_MANAGER);
		return intent;
	}

	public static Intent getHuaweiProtectedAppsIntent() {
		Intent intent = new Intent();
		intent.setClassName(PACKAGE_NAME_HUAWEI, CLASS_NAME_PROTECTED_APPS);
		return intent;
	}

	public static boolean isXiaomiOrRedmiDevice() {
		return "Xiaomi".equalsIgnoreCase(BRAND) ||
				"Redmi".equalsIgnoreCase(BRAND);
	}

	public static boolean isMiuiTenOrLater() {
		String version = getSystemProperty("ro.miui.ui.version.name");
		if (version == null || version.equals("")) return false;
		version = version.replaceAll("[^\\d]", "");
		try {
			return Integer.parseInt(version) >= 10;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	@Nullable
	private static String getSystemProperty(String propName) {
		try {
			Process p = getRuntime().exec("getprop " + propName);
			Scanner s = new Scanner(p.getInputStream());
			String line = s.nextLine();
			s.close();
			return line;
		} catch (SecurityException | IOException e) {
			return null;
		}
	}
}
