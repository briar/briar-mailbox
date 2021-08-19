package org.briarproject.mailbox.core.tor;

public interface TorConstants {

	// Transport properties
	String PROP_ONION_V3 = "onion3";

	int SOCKS_PORT = 59050;
	int CONTROL_PORT = 59051;

	int CONNECT_TO_PROXY_TIMEOUT = 5000; // Milliseconds
	int EXTRA_SOCKET_TIMEOUT = 30000; // Milliseconds

	// Local settings (not shared with contacts)
	String HS_PRIVATE_KEY_V3 = "onionPrivKey3";
	String HS_V3_CREATED = "onionPrivKey3Created";

	// Values for PREF_TOR_NETWORK
	int PREF_TOR_NETWORK_AUTOMATIC = 0;
	int PREF_TOR_NETWORK_WITHOUT_BRIDGES = 1;
	int PREF_TOR_NETWORK_WITH_BRIDGES = 2;

	// Default values for local settings
	boolean DEFAULT_PREF_PLUGIN_ENABLE = true;
	int DEFAULT_PREF_TOR_NETWORK = PREF_TOR_NETWORK_AUTOMATIC;
	boolean DEFAULT_PREF_TOR_MOBILE = true;
	boolean DEFAULT_PREF_TOR_ONLY_WHEN_CHARGING = false;

}
