/**
 * Copyright (C) 2009-2013 Dell, Inc.
 * See annotations for authorship information
 *
 * ====================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ====================================================================
 */
package org.dasein.cloud.tier3.compute.snapshot;

import org.dasein.cloud.*;
import org.dasein.cloud.compute.SnapshotCapabilities;
import org.dasein.cloud.tier3.Tier3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;

/**
* Describes CLC VM snapshot capabilities
* <p>Created by stas: 06/08/2014 13:00</p>
*
* @author Stas Maksimov
* @version 2014.08 initial version
* @since 2014.08
*/
class Tier3SnapshotCapabilities extends AbstractCapabilities<Tier3> implements SnapshotCapabilities {

    public Tier3SnapshotCapabilities( @Nonnull Tier3 provider ) {
        super(provider);
    }

    @Override
    public boolean supportsSnapshotSharingWithPublic() throws InternalException, CloudException {
        return false;
    }

    @Override
    public boolean supportsSnapshotSharing() throws InternalException, CloudException {
        return false;
    }

    @Override
    public boolean supportsSnapshotCreation() throws CloudException, InternalException {
        return true;
    }

    @Override
    public boolean supportsSnapshotCopying() throws CloudException, InternalException {
        return false;
    }

    @Override
    public Requirement identifyAttachmentRequirement() throws InternalException, CloudException {
        return Requirement.REQUIRED;
    }

    @Override
    public String getProviderTermForSnapshot(Locale locale) {
        return "snapshot";
    }

    @Override
    public @Nullable VisibleScope getSnapshotVisibleScope() {
        return null;
    }
}
