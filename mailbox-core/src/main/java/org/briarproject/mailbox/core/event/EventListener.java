package org.briarproject.mailbox.core.event;

/**
 * An interface for receiving notifications when events occur.
 */
public interface EventListener {

	/**
	 * Called when an event is broadcast. Implementations of this method must
	 * not block.
	 */
	@EventExecutor
	void eventOccurred(Event e);
}
