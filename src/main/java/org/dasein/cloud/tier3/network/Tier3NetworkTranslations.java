/**
 * Copyright (C) 2012-2013 Dell, Inc.
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

package org.dasein.cloud.tier3.network;

import javax.annotation.Nullable;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.network.VLANState;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Helper class to contain re-useable standalone methods that deal with
 * translations.
 * 
 * @author David.Young
 */
public class Tier3NetworkTranslations {

    public @Nullable
    ResourceStatus toVlanStatus(@Nullable JSONObject ob) throws CloudException, InternalException {
        if (ob == null) {
            return null;
        }
        try {
            VLANState state = VLANState.AVAILABLE;
            String vlanId = null;

            if (ob.has("Name")) {
                vlanId = ob.getString("Name");
            }
            if (vlanId == null) {
                return null;
            }
            return new ResourceStatus(vlanId, state);
        } catch (JSONException e) {
            throw new CloudException(e);
        }
    }

}
