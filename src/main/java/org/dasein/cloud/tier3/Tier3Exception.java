package org.dasein.cloud.tier3;

import org.dasein.cloud.CloudErrorType;
import org.dasein.cloud.CloudException;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

public class Tier3Exception extends CloudException {
    public Tier3Exception(@Nonnull Throwable cause) {
        super(cause);
    }

    public Tier3Exception(@Nonnull CloudErrorType type, @Nonnegative int httpCode, @Nonnull String providerCode, @Nonnull String message) {
        super(type, httpCode, providerCode, message);
    }
}
