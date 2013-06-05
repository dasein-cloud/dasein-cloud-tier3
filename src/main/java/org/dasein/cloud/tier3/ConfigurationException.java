package org.dasein.cloud.tier3;

import org.dasein.cloud.CloudException;

import javax.annotation.Nonnull;

/**
 * An error in configuring Dell ASM's context in some manner.
 * <p>Created by George Reese: 05/17/2013 9:44 AM</p>
 * @author George Reese
 * @version 2013.4 initial version
 * @since 2013.4
 */
public class ConfigurationException extends CloudException {
    /**
     * Constructs a configuration error with the specified message.
     * @param message the message describing the nature of the configuration problem
     */
    public ConfigurationException(@Nonnull String message) {
        super(message);
    }

    /**
     * Constructs a configuration error based on a prior exception.
     * @param cause the prior exception resulting in the configuration problem
     */
    public ConfigurationException(@Nonnull Throwable cause) {
        super(cause);
    }
}
