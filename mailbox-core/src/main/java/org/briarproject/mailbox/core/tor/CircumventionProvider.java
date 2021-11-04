package org.briarproject.mailbox.core.tor;

import java.util.List;

public interface CircumventionProvider {

	/**
	 * Countries where Tor is blocked, i.e. vanilla Tor connection won't work.
	 * <p>
	 * See https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2
	 * and https://trac.torproject.org/projects/tor/wiki/doc/OONI/censorshipwiki
	 */
	String[] BLOCKED = {"CN", "IR", "EG", "BY", "TR", "SY", "VE"};

	/**
	 * Countries where obfs4 or meek bridge connections are likely to work.
	 * Should be a subset of {@link #BLOCKED}.
	 */
	String[] BRIDGES = {"CN", "IR", "EG", "BY", "TR", "SY", "VE"};

	/**
	 * Countries where obfs4 bridges won't work and meek is needed.
	 * Should be a subset of {@link #BRIDGES}.
	 */
	String[] NEEDS_MEEK = {"CN", "IR"};

	boolean isTorProbablyBlocked(String countryCode);

	boolean doBridgesWork(String countryCode);

	boolean needsMeek(String countryCode);

	List<String> getBridges(boolean meek);

}
