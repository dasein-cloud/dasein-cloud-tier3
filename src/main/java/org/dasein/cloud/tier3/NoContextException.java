package org.dasein.cloud.tier3;

public class NoContextException extends ConfigurationException {
	private static final long serialVersionUID = 4728360671690748932L;

	public NoContextException() { super("No context was set for this request"); }
}

