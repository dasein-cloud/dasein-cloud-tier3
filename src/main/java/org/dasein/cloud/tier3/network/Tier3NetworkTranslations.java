package org.dasein.cloud.tier3.network;

import javax.annotation.Nullable;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.network.VLANState;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Helper class to contain re-useable standalone methods that deal with translations.
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
