package org.briarproject.mailbox.core.system;

import java.io.InputStream;

public interface ResourceProvider {

	InputStream getResourceInputStream(String name, String extension);
}
