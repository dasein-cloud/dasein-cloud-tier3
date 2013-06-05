package org.dasein.cloud.tier3;

public class NoContextException extends ConfigurationException {
    /**
     * Constructs an exception representing the lack of context.
     */
    public NoContextException() { super("No context was set for this request"); }
}

