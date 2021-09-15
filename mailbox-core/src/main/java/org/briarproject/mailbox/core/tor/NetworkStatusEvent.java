package org.briarproject.mailbox.core.tor;

import org.briarproject.mailbox.core.event.Event;

import javax.annotation.concurrent.Immutable;

@Immutable
public class NetworkStatusEvent extends Event {

	private final NetworkStatus status;

	public NetworkStatusEvent(NetworkStatus status) {
		this.status = status;
	}

	public NetworkStatus getStatus() {
		return status;
	}
}
