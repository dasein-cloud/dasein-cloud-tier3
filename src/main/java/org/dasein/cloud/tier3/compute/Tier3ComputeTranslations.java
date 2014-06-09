package org.dasein.cloud.tier3.compute;

import java.util.ArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.compute.Architecture;
import org.dasein.cloud.compute.Platform;
import org.dasein.cloud.compute.VmState;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Helper class to contain re-useable standalone methods that deal with
 * translations.
 * 
 * @author David.Young
 */
public class Tier3ComputeTranslations {

	/**
	 * Given an operating system, parse out the appropriate architecture object.
	 * 
	 * @param os
	 * @return
	 */
	public Architecture toArchitecture(@Nonnull String os) {
		if (os == null) {
			return null;
		}
		if (os.contains("64")) {
			return Architecture.I64;
		} else if (os.contains("32")) {
			return Architecture.I32;
		} else {
			return Architecture.I64;
		}
	}

	/**
	 * Given an operating system, add keywords where needed to fit the Dasein
	 * standard and then return a valid platform object.
	 * 
	 * @param os
	 * @return
	 */
	public Platform toPlatform(@Nonnull String os) {
		if (os == null) {
			return null;
		}
		if (os.toLowerCase().contains("win")) {
			os += "-windows";
		}
		if (os.toLowerCase().contains("pxe")) {
			// this will return a UNIX platform
			os += "-linix";
		}
		if (os.toLowerCase().contains("bosh")) {
			os += "-ubuntu";
		}
		return Platform.guess(os);
	}

	/**
	 * Custom translation method between CLC power states and the Dasein VmState
	 * object.
	 * 
	 * @param jsonObject
	 *            string state of a CLC server
	 * @return the standard state for a vm
	 * @throws JSONException
	 */
	public @Nonnull
	VmState toVmState(@Nonnull JSONObject jsonObject) throws JSONException {
		if (jsonObject == null || !jsonObject.has("PowerState")) {
			return VmState.PENDING;
		}
		if ("Started".equals(jsonObject.getString("PowerState"))) {
			return VmState.RUNNING;
		} else if ("Stopped".equals(jsonObject.getString("PowerState"))) {
			return VmState.STOPPED;
		} else if ("Paused".equals(jsonObject.getString("PowerState"))) {
			return VmState.PAUSED;
		} else {
			return VmState.PENDING;
		}
	}

	/**
	 * Custom translation method to determine an appropriate resource status
	 * object (uses toVmState).
	 * 
	 * @param ob
	 * @return
	 * @throws CloudException
	 * @throws InternalException
	 */
	public @Nullable
	ResourceStatus toVmStatus(@Nullable JSONObject ob) throws CloudException, InternalException {
		if (ob == null) {
			return null;
		}
		try {
			VmState state = VmState.PENDING;
			String vmId = null;
			if (ob.has("ID")) {
				vmId = ob.getString("ID");
			}
			state = toVmState(ob);
			if (vmId == null) {
				return null;
			}
			return new ResourceStatus(vmId, state);
		} catch (JSONException e) {
			throw new CloudException(e);
		}
	}

	/**
	 * Preferrably, this list would come from an API but none exists yet. Build
	 * a list of supported operating systems along with their max settings for
	 * cpu and memory.
	 * 
	 * @param architecture
	 * @return
	 */
	public ArrayList<Tier3OS> getOperatingSystems(Architecture architecture) {
		ArrayList<Tier3OS> osList = new ArrayList<Tier3OS>();
		if (architecture == null || architecture == Architecture.I32) {
			osList.add(new Tier3OS(32, "CentOS 5 | 32-Bit", 8, 128));
			osList.add(new Tier3OS(34, "CentOS 6 | 32-Bit", 16, 128));
			osList.add(new Tier3OS(29, "Ubuntu 10 | 32-Bit", 16, 128));
			osList.add(new Tier3OS(2, "Windows 2003 32-bit", 4, 32));
			osList.add(new Tier3OS(15, "Windows 2003 R2 Enterprise | 32-bit", 8, 128));
			osList.add(new Tier3OS(15, "Windows 2003 R2 Standard | 32-bit", 4, 128));
		}
		if (architecture == null || architecture == Architecture.I64) {
			osList.add(new Tier3OS(33, "CentOS 5 | 64-Bit", 16, 128));
			osList.add(new Tier3OS(35, "CentOS 6 | 64-Bit", 16, 128));
			osList.add(new Tier3OS(36, "Debian 6 | 64-Bit", 16, 128));
			osList.add(new Tier3OS(37, "Debian 7 | 64-Bit", 16, 128));
			osList.add(new Tier3OS(25, "RedHat Enterprise Linux 5 | 64-bit", 16, 28));
			osList.add(new Tier3OS(30, "Ubuntu 10 | 64-Bit", 16, 128));
			osList.add(new Tier3OS(31, "Ubuntu 12 | 64-Bit", 16, 128));
			osList.add(new Tier3OS(15, "Windows 2003 R2 Enterprise | 64-bit", 8, 128));
			osList.add(new Tier3OS(15, "Windows 2003 R2 Standard | 64-bit", 4, 32));
			osList.add(new Tier3OS(26, "Windows 2008 Datacenter 64-bit", 4, 4));
			osList.add(new Tier3OS(18, "Windows 2008 Enterprise | 64-bit", 8, 128));
			osList.add(new Tier3OS(18, "Windows 2008 Standard | 64-bit", 4, 32));
			osList.add(new Tier3OS(27, "Windows 2012 Datacenter Edition | 64-bit", 16, 128));
			osList.add(new Tier3OS(28, "Windows 2012 R2 Datacenter Edition | 64-Bit", 16, 128));
		}

		return osList;
	}

	/**
	 * Given an operating system id, return the name.
	 * 
	 * @param os
	 * @return
	 */
	public String translateOS(Object os) {
		Integer osId = Integer.parseInt(os.toString());
		if (osId != null) {
			for (Tier3OS o : getOperatingSystems(null)) {
				if (osId == o.id) {
					return o.name;
				}
			}
		}
		return null;
	}
}
