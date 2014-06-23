package org.dasein.cloud.tier3;

import org.dasein.cloud.CloudException;

import javax.annotation.Nonnull;

public class ConfigurationException extends CloudException {
    private static final long serialVersionUID = 3374617600574494527L;

    public ConfigurationException(@Nonnull String message) {
        super(message);
    }

    public ConfigurationException(@Nonnull Throwable cause) {
        super(cause);
    }
}
