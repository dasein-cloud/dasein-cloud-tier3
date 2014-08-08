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

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.compute.SnapshotCapabilities;

import java.util.Locale;

/**
* Description
* <p>Created by stas: 06/08/2014 13:00</p>
*
* @author Stas Maksimov
* @version 2014.08 initial version
* @since 2014.08
*/
class Tier3SnapshotCapabilities implements SnapshotCapabilities {

    @Override
    public String getRegionId() {
        return provider.getContext().getRegionId();
    }

    @Override
    public String getAccountNumber() {
        return provider.getContext().getAccountNumber();
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
    public String getProviderTermForSnapshot(Locale arg0) {
        return "snapshot";
    }
}
