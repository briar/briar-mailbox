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

package org.briarproject.mailbox.core.tor;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import org.briarproject.mailbox.core.system.LocationUtils;
import org.slf4j.Logger;

import java.util.Locale;

import javax.inject.Inject;

import static android.content.Context.TELEPHONY_SERVICE;
import static org.slf4j.LoggerFactory.getLogger;

class AndroidLocationUtils implements LocationUtils {

	private static final Logger LOG = getLogger(AndroidLocationUtils.class);

	private final Context appContext;

	@Inject
	AndroidLocationUtils(Application app) {
		appContext = app.getApplicationContext();
	}

	/**
	 * This guesses the current country from the first of these sources that
	 * succeeds (also in order of likelihood of being correct):
	 *
	 * <ul>
	 * <li>Phone network. This works even when no SIM card is inserted, or a
	 *     foreign SIM card is inserted.</li>
	 * <li>SIM card. This is only an heuristic and assumes the user is not
	 *     roaming.</li>
	 * <li>User locale. This is an even worse heuristic.</li>
	 * </ul>
	 * <p>
	 * Note: this is very similar to <a href="https://android.googlesource.com/platform/frameworks/base/+/cd92588%5E/location/java/android/location/CountryDetector.java">
	 * this API</a> except it seems that Google doesn't want us to use it for
	 * some reason - both that class and {@code Context.COUNTRY_CODE} are
	 * annotated {@code @hide}.
	 */
	@Override
	@SuppressLint("DefaultLocale")
	public String getCurrentCountry() {
		String countryCode = getCountryFromPhoneNetwork();
		if (!TextUtils.isEmpty(countryCode)) return countryCode.toUpperCase();
		LOG.info("Falling back to SIM card country");
		countryCode = getCountryFromSimCard();
		if (!TextUtils.isEmpty(countryCode)) return countryCode.toUpperCase();
		LOG.info("Falling back to user-defined locale");
		return Locale.getDefault().getCountry();
	}

	private String getCountryFromPhoneNetwork() {
		Object o = appContext.getSystemService(TELEPHONY_SERVICE);
		TelephonyManager tm = (TelephonyManager) o;
		return tm == null ? "" : tm.getNetworkCountryIso();
	}

	private String getCountryFromSimCard() {
		Object o = appContext.getSystemService(TELEPHONY_SERVICE);
		TelephonyManager tm = (TelephonyManager) o;
		return tm == null ? "" : tm.getSimCountryIso();
	}
}
