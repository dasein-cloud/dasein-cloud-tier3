package org.dasein.cloud.tier3.compute.vm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.Tag;
import org.dasein.cloud.compute.Architecture;
import org.dasein.cloud.compute.ImageClass;
import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.compute.Platform;
import org.dasein.cloud.compute.VMFilterOptions;
import org.dasein.cloud.compute.VMLaunchOptions;
import org.dasein.cloud.compute.VMLaunchOptions.VolumeAttachment;
import org.dasein.cloud.compute.VMScalingCapabilities;
import org.dasein.cloud.compute.VMScalingOptions;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineProduct;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.compute.VmStatistics;
import org.dasein.cloud.identity.ServiceAction;
import org.dasein.cloud.network.RawAddress;
import org.dasein.cloud.network.VLAN;
import org.dasein.cloud.tier3.APIHandler;
import org.dasein.cloud.tier3.APIResponse;
import org.dasein.cloud.tier3.Tier3;
import org.dasein.cloud.tier3.compute.Tier3OS;
import org.dasein.cloud.util.APITrace;
import org.dasein.util.CalendarWrapper;
import org.dasein.util.uom.storage.Gigabyte;
import org.dasein.util.uom.storage.Storage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Tier3VM implements VirtualMachineSupport {
	static private final Logger logger = Tier3.getLogger(Tier3VM.class);
	private Tier3 provider;

	public Tier3VM(Tier3 provider) {
		this.provider = provider;
	}

	@Override
	public String[] mapServiceAction(ServiceAction action) {
		return new String[0];
	}

	@Override
	public VirtualMachine alterVirtualMachine(String vmId, VMScalingOptions options) throws InternalException,
			CloudException {
		APITrace.begin(provider, "alterVirtualMachine");
		try {
			APIHandler method = new APIHandler(provider);
			JSONObject post = new JSONObject();

			VirtualMachine vm = getVirtualMachine(vmId);

			VirtualMachineProduct product = getProduct(options.getProviderProductId());

			post.put("Name", vmId);
			post.put("HardwareGroupID", vm.getTag("HardwareGroupID"));
			post.put("Cpu", product.getCpuCount());
			post.put("MemoryGB", product.getRamSize());

			APIResponse response = method.post("Server/ConfigureServer/JSON", new JSONObject(post).toString());
			response.validate();

			return vm;

		} catch (JSONException e) {
			throw new CloudException(e);
		} finally {
			APITrace.end();
		}
	}

	@Override
	public VirtualMachine clone(String vmId, String intoDcId, String name, String description, boolean powerOn,
			String... firewallIds) throws InternalException, CloudException {
		throw new OperationNotSupportedException();
	}

	@Override
	/**
	 * Use {@link Tier3Volume} to manage volumes and for vertical scaling.
	 */
	public VMScalingCapabilities describeVerticalScalingCapabilities() throws CloudException, InternalException {
		return VMScalingCapabilities.getInstance(false, true, Requirement.NONE, Requirement.NONE);
	}

	@Override
	public void disableAnalytics(String vmId) throws InternalException, CloudException {
		// unimplemented
	}

	@Override
	public void enableAnalytics(String vmId) throws InternalException, CloudException {
		// unimplemented
	}

	@Override
	public String getConsoleOutput(String vmId) throws InternalException, CloudException {
		throw new OperationNotSupportedException();
	}

	@Override
	public int getCostFactor(VmState state) throws InternalException, CloudException {
		return 100;
	}

	@Override
	public int getMaximumVirtualMachineCount() throws CloudException, InternalException {
		return -1;
	}

	@Override
	public VirtualMachineProduct getProduct(String productId) throws InternalException, CloudException {
		for (VirtualMachineProduct product : listProducts(null)) {
			if (productId != null && productId.equals(product.getProviderProductId())) {
				return product;
			}
		}
		return null;
	}

	public VirtualMachineProduct getProduct(String os, Integer cpu, Integer memory) throws InternalException,
			CloudException {
		if (os == null || cpu == null || memory == null) {
			return null;
		}
		for (VirtualMachineProduct product : listProducts(null)) {
			if (product.getCpuCount() == cpu && product.getRamSize().convertTo(Storage.GIGABYTE).intValue() == memory
					&& product.getName().startsWith(os)) {
				return product;
			}
		}
		return null;
	}

	@Override
	public String getProviderTermForServer(Locale locale) {
		return "server";
	}

	@Override
	public VirtualMachine getVirtualMachine(String vmId) throws InternalException, CloudException {
		APITrace.begin(provider, "getVirtualMachine");
		APIHandler method = new APIHandler(provider);

		try {
			JSONObject json = new JSONObject();
			json.put("Name", vmId);
			APIResponse response = method.post("Server/GetServer/JSON", json.toString());
			response.validate();

			return toVirtualMachine(response.getJSON().getJSONObject("Server"));
		} catch (JSONException e) {
			throw new CloudException(e);
		} finally {
			APITrace.end();
		}
	}

	private VirtualMachine toVirtualMachine(JSONObject ob) throws CloudException, InternalException {
		if (ob == null) {
			return null;
		}
		try {
			VirtualMachine vm = new VirtualMachine();

			vm.setClonable(false);
			vm.setImagable(false);
			vm.setLastPauseTimestamp(-1L);
			vm.setPersistent(true);
			if (ob.has("Location")) {
				vm.setProviderDataCenterId(ob.getString("Location"));
			}
			vm.setProviderOwnerId(provider.getContext().getAccountNumber());
			vm.setProviderRegionId(provider.getContext().getRegionId());
			vm.setTerminationTimestamp(-1L);
			if (ob.has("HardwareGroupID")) {
				vm.addTag(new Tag("HardwareGroupId", ob.getString("HardwareGroupID")));
			}

			if (ob.has("ID")) {
				vm.addTag(new Tag("ServerID", ob.getString("ID")));
			}
			if (ob.has("Name")) {
				vm.setProviderVirtualMachineId(ob.getString("Name"));
				vm.setName(ob.getString("Name"));
			}
			if (ob.has("IPAddresses") && ob.get("IPAddresses") != null) {
				JSONArray ips = ob.getJSONArray("IPAddresses");
				ArrayList<RawAddress> pubIp = new ArrayList<RawAddress>();
				ArrayList<RawAddress> privIp = new ArrayList<RawAddress>();

				for (int i = 0; i < ips.length(); i++) {
					JSONObject ip = ips.getJSONObject(i);

					if ("MIP".equals(ip.getString("AddressType"))) {
						pubIp.add(new RawAddress(ip.getString("Address")));
					} else {
						privIp.add(new RawAddress(ip.getString("Address")));
					}
				}
				if (!pubIp.isEmpty()) {
					vm.setPublicAddresses(pubIp.toArray(new RawAddress[pubIp.size()]));
				}
				if (!privIp.isEmpty()) {
					vm.setPrivateAddresses(privIp.toArray(new RawAddress[privIp.size()]));
				}
			}
			vm.setPausable(false);
			vm.setRebootable(false);
			vm.setCurrentState(provider.getComputeTranslations().toVmState(ob));
			if (VmState.RUNNING.equals(vm.getCurrentState())) {
				vm.setPausable(true);
				vm.setRebootable(true);
			}
			if (vm.getName() == null) {
				vm.setName(vm.getProviderVirtualMachineId());
			}
			if (vm.getDescription() == null) {
				vm.setDescription(vm.getName());
			}

			if (ob.has("OperatingSystem")) {
				String os = provider.getComputeTranslations().translateOS(ob.get("OperatingSystem"));
				vm.setArchitecture(provider.getComputeTranslations().toArchitecture(os));
				vm.setPlatform(Platform.guess(os));
				if (ob.has("Cpu") && ob.getInt("Cpu") > 0 && ob.has("MemoryGB") && ob.getInt("MemoryGB") > 0) {
					vm.setProductId(getProduct(os, ob.getInt("Cpu"), ob.getInt("MemoryGB")).getProviderProductId());
				} else {
					vm.setProductId("-1");
				}
			}

			if (ob.has("CustomFields") && !ob.isNull("CustomFields") && ob.getJSONArray("CustomFields").length() > 0) {
				JSONArray fields = ob.getJSONArray("CustomFields");
				for (int i = 0; i < fields.length(); i++) {
					JSONObject f = fields.getJSONObject(i);
					vm.addTag(new Tag(f.getString("Name"), f.getString("Value")));
				}
			}
			
			// since vlan isn't handed back in get server, need to look it up and match on ip
			Iterable<VLAN> vlans = provider.getNetworkServices().getVlanSupport().listVlans();
			JSONArray ips = ob.getJSONArray("IPAddresses");
			for (VLAN vlan : vlans) {
				for (int i = 0; i < ips.length(); i++) {
					JSONObject ip = ips.getJSONObject(i);
					String address = ip.getString("Address").substring(0, ip.getString("Address").lastIndexOf("."));
					if (vlan.getName().contains(address)) {
						vm.setProviderVlanId(vlan.getProviderVlanId());
						break;
					}
				}
				if (vm.getProviderVlanId() != null) {
					break;
				}
			}

			return vm;
		} catch (JSONException e) {
			throw new CloudException(e);
		}
	}

	@Override
	public VmStatistics getVMStatistics(String vmId, long from, long to) throws InternalException, CloudException {
		throw new OperationNotSupportedException();
	}

	@Override
	public Iterable<VmStatistics> getVMStatisticsForPeriod(String vmId, long from, long to) throws InternalException,
			CloudException {
		throw new OperationNotSupportedException();
	}

	@Override
	public Requirement identifyImageRequirement(ImageClass cls) throws CloudException, InternalException {
		return (cls.equals(ImageClass.MACHINE) ? Requirement.REQUIRED : Requirement.NONE);
	}

	@Override
	public Requirement identifyPasswordRequirement() throws CloudException, InternalException {
		return Requirement.REQUIRED;
	}

	@Override
	public Requirement identifyPasswordRequirement(Platform platform) throws CloudException, InternalException {
		return Requirement.REQUIRED;
	}

	@Override
	public Requirement identifyRootVolumeRequirement() throws CloudException, InternalException {
		return Requirement.NONE;
	}

	@Override
	public Requirement identifyShellKeyRequirement() throws CloudException, InternalException {
		return Requirement.NONE;
	}

	@Override
	public Requirement identifyShellKeyRequirement(Platform platform) throws CloudException, InternalException {
		return Requirement.NONE;
	}

	@Override
	public Requirement identifyStaticIPRequirement() throws CloudException, InternalException {
		return Requirement.NONE;
	}

	@Override
	public Requirement identifyVlanRequirement() throws CloudException, InternalException {
		return Requirement.REQUIRED;
	}

	@Override
	public boolean isAPITerminationPreventable() throws CloudException, InternalException {
		return false;
	}

	@Override
	public boolean isBasicAnalyticsSupported() throws CloudException, InternalException {
		return false;
	}

	@Override
	public boolean isExtendedAnalyticsSupported() throws CloudException, InternalException {
		return false;
	}

	@Override
	public boolean isSubscribed() throws CloudException, InternalException {
		return true;
	}

	@Override
	public boolean isUserDataSupported() throws CloudException, InternalException {
		return false;
	}

	@Override
	public VirtualMachine launch(VMLaunchOptions withLaunchOptions) throws CloudException, InternalException {
		APITrace.begin(provider, "launch");
		try {
			ProviderContext ctx = provider.getContext();
			if (ctx == null) {
				throw new CloudException("No context was established for this request");
			}
			MachineImage template = provider.getComputeServices().getImageSupport()
					.getImage(withLaunchOptions.getMachineImageId());

			if (template == null) {
				throw new InternalException("No such image: " + withLaunchOptions.getMachineImageId());
			}
			APIHandler method = new APIHandler(provider);
			JSONObject post = new JSONObject();

			post.put("AccountAlias", provider.getContext().getAccountNumber());
			post.put("LocationAlias", withLaunchOptions.getDataCenterId());
			post.put("Template", template.getName());
			int cpu = 1;
			if (template.getTags().containsKey("Cpu") && !template.getTag("Cpu").equals("0")) {
				cpu = Integer.parseInt(template.getTag("Cpu").toString());
			}
			post.put("Cpu", cpu);
			int memoryGb = 1;
			if (template.getTags().containsKey("MemoryGB") && !template.getTag("MemoryGB").equals("0")) {
				memoryGb = Integer.parseInt(template.getTag("MemoryGB").toString());
			}
			post.put("MemoryGB", memoryGb);

			post.put("HardwareGroupID", getDefaultHardwareGroupId(withLaunchOptions.getDataCenterId()));

			post.put("Alias", validateName(withLaunchOptions.getHostName()));
			post.put("Description", withLaunchOptions.getDescription());
			if (withLaunchOptions.getMetaData().containsKey("ServerType")) {
				post.put("ServerType", withLaunchOptions.getMetaData().get("ServerType"));
			} else {
				post.put("ServerType", 1);
			}
			if (withLaunchOptions.getMetaData().containsKey("ServiceLevel")) {
				post.put("ServiceLevel", withLaunchOptions.getMetaData().get("ServiceLevel"));
			} else {
				post.put("ServiceLevel", 1);
			}
			if (withLaunchOptions.getVolumes() != null && withLaunchOptions.getVolumes().length > 0) {
				if (withLaunchOptions.getVolumes().length > 1) {
					throw new CloudException("Only one volume allowed at server creation.");
				}
				VolumeAttachment vol = withLaunchOptions.getVolumes()[0];
				post.put("ExtraDriveGB", vol.volumeToCreate.getVolumeSize().intValue());
			} else {
				post.put("ExtraDriveGB", 0);
			}
			post.put("Network", withLaunchOptions.getVlanId());

			// TODO Dasein tests contain insufficiently strong passwords
			// if (withLaunchOptions.getBootstrapPassword() != null) {
			// post.put("Password", withLaunchOptions.getBootstrapPassword());
			// }

			Map<String, Object> meta = withLaunchOptions.getMetaData();

			if (meta.size() > 0) {
				JSONArray customFields = new JSONArray();
				for (Map.Entry<String, Object> entry : meta.entrySet()) {
					JSONObject cf = new JSONObject();
					cf.put("CustomFieldID", entry.getKey());
					cf.put("Value", entry.getValue().toString());
					customFields.put(cf);
				}
				post.put("CustomFields", customFields);
			}
			APIResponse response = method.post("Server/CreateServer/JSON", post.toString());
			response.validate();

			String vmId = null;
			long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE*2);
			int requestId = response.getJSON().getInt("RequestID");
			while (timeout > System.currentTimeMillis()) {

				// wait for CLC blueprints to at least hand back the server name
				JSONObject deployStatus = provider.getDeploymentStatus(requestId);
				JSONArray servers = deployStatus.getJSONArray("Servers");
				if (deployStatus.has("Servers") && !deployStatus.isNull("Servers") && servers.length() > 0) {
					vmId = servers.get(0).toString();
					break;
				} else {
					try {
						Thread.sleep(10000L);
					} catch (InterruptedException ignore) {
					}
				}
			}
			
			// now wait for CLC to recognize the server exists
			VirtualMachine vm = getVirtualMachine(vmId);
			if (vm == null || (vm.getName() == null)) {
				timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE * 2);

				while (timeout > System.currentTimeMillis()) {
					try {
						Thread.sleep(10000L);
					} catch (InterruptedException ignore) {
					}
					try {
						vm = getVirtualMachine(vmId);
					} catch (Throwable ignore) {
					}
					if (vm != null && vm.getName() != null) {
						break;
					}
				}
			}

			return vm;

		} catch (JSONException e) {
			throw new CloudException(e);
		} finally {
			APITrace.end();
		}
	}

	private String validateName(String originalName) {
		StringBuilder name = new StringBuilder();

		for (int i = 0; i < originalName.length(); i++) {
			char c = originalName.charAt(i);

			if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
				name.append(c);
			} else if (((c >= '0' && c <= '9') || c == '-' || c == '_' || c == ' ') && name.length() > 0) {
				if (c == ' ') {
					c = '-';
				}
				name.append(c);
			}
		}
		if (name.length() < 1) {
			return "SVR";
		}
		if (name.charAt(name.length() - 1) == '-' || name.charAt(name.length() - 1) == '_') {
			// check for trailing - or _
			name.deleteCharAt(name.length() - 1);
		}
		if (name.length() > 6) {
			return name.substring(0, 6);
		}
		return name.toString();
	}

	@Override
	public VirtualMachine launch(String fromMachineImageId, VirtualMachineProduct product, String dataCenterId,
			String name, String description, String withKeypairId, String inVlanId, boolean withAnalytics,
			boolean asSandbox, String... firewallIds) throws InternalException, CloudException {
		throw new OperationNotSupportedException();
	}

	@Override
	public VirtualMachine launch(String fromMachineImageId, VirtualMachineProduct product, String dataCenterId,
			String name, String description, String withKeypairId, String inVlanId, boolean withAnalytics,
			boolean asSandbox, String[] firewallIds, Tag... tags) throws InternalException, CloudException {
		throw new OperationNotSupportedException();
	}

	@Override
	public Iterable<String> listFirewalls(String vmId) throws InternalException, CloudException {
		throw new OperationNotSupportedException();
	}

	/**
	 * Since all the options of CenturyLink Cloud are configurable, there isn't
	 * necessarily a product catalog. Since the Dasein launch methods require a
	 * product id to determine CPU and Memory, dynamically build a list of
	 * products using all the potential combinations.
	 */
	@Override
	public Iterable<VirtualMachineProduct> listProducts(Architecture architecture) throws InternalException,
			CloudException {

		ArrayList<VirtualMachineProduct> products = new ArrayList<VirtualMachineProduct>();

		for (Tier3OS os : provider.getComputeTranslations().getOperatingSystems(architecture)) {
			for (int cpu = 1; cpu <= os.maxCpu; cpu++) {

				// for lower numbers, increment by 1
				for (int mem = 1; mem <= 16; mem++) {
					VirtualMachineProduct product = new VirtualMachineProduct();
					product.setName(os.name + ", " + cpu + " CPU, " + mem + " GB Memory");
					product.setRamSize(new Storage<Gigabyte>(mem, Storage.GIGABYTE));
					product.setCpuCount(cpu);
					product.setDescription(product.getName());
					product.setProviderProductId(product.getName());
					product.setRootVolumeSize(new Storage<Gigabyte>(1, Storage.GIGABYTE));
					product.setStandardHourlyRate(0.0f);
					products.add(product);
				}

				// for higher ranges, increment by 4
				for (int mem = 20; mem <= os.maxMemory; mem += 4) {
					VirtualMachineProduct product = new VirtualMachineProduct();
					product.setName(os.name + ", " + cpu + " CPU, " + mem + " GB Memory");
					product.setRamSize(new Storage<Gigabyte>(mem, Storage.GIGABYTE));
					product.setCpuCount(cpu);
					product.setDescription(product.getName());
					product.setProviderProductId(product.getName());
					product.setRootVolumeSize(new Storage<Gigabyte>(1, Storage.GIGABYTE));
					product.setStandardHourlyRate(0.0f);
					products.add(product);
				}
			}
		}

		return products;
	}

	@Override
	public Iterable<Architecture> listSupportedArchitectures() throws InternalException, CloudException {
		return Arrays.asList(Architecture.I32, Architecture.I64);
	}

	@Override
	public Iterable<ResourceStatus> listVirtualMachineStatus() throws InternalException, CloudException {
		ArrayList<ResourceStatus> vms = new ArrayList<ResourceStatus>();
		for (VirtualMachine vm : listVirtualMachines()) {
			vms.add(new ResourceStatus(vm.getProviderVirtualMachineId(), vm.getCurrentState()));
		}
		return vms;
	}

	@Override
	public Iterable<VirtualMachine> listVirtualMachines() throws InternalException, CloudException {
		APITrace.begin(provider, "listVirtualMachines");
		try {
			APIHandler method = new APIHandler(provider);
			APIResponse response = method.post("Server/GetAllServers/JSON", "");
			response.validate();
			
			ArrayList<VirtualMachine> vms = new ArrayList<VirtualMachine>();

			JSONObject json = response.getJSON();
			if (json.has("Servers")) {
				for (int i = 0; i < json.getJSONArray("Servers").length(); i++) {
					VirtualMachine vm = toVirtualMachine(json.getJSONArray("Servers").getJSONObject(i));
					if (vm != null) {
						vms.add(vm);
					}
				}
			}

			return vms;
		} catch (JSONException e) {
			throw new CloudException(e);
		} finally {
			APITrace.end();
		}
	}

	@Override
	public Iterable<VirtualMachine> listVirtualMachines(VMFilterOptions options) throws InternalException,
			CloudException {
		return listVirtualMachines();
	}

	@Override
	public void pause(String vmId) throws InternalException, CloudException {
		APITrace.begin(provider, "pause");
		try {
			APIHandler method = new APIHandler(provider);
			JSONObject json = new JSONObject();
			json.put("Name", vmId);
			APIResponse response = method.post("Server/PauseServer/JSON", json.toString());
			response.validate();
		} catch (JSONException e) {
			throw new CloudException(e);
		} finally {
			APITrace.end();
		}
	}

	@Override
	public void reboot(String vmId) throws CloudException, InternalException {
		APITrace.begin(provider, "reboot");
		try {
			APIHandler method = new APIHandler(provider);
			JSONObject json = new JSONObject();
			json.put("Name", vmId);
			APIResponse response = method.post("Server/RebootServer/JSON", json.toString());
			response.validate();
		} catch (JSONException e) {
			throw new CloudException(e);
		} finally {
			APITrace.end();
		}
	}

	@Override
	public void resume(String vmId) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void start(String vmId) throws InternalException, CloudException {
		APITrace.begin(provider, "start");
		try {
			APIHandler method = new APIHandler(provider);
			JSONObject json = new JSONObject();
			json.put("Name", vmId);
			APIResponse response = method.post("Server/PowerOnServer/JSON", json.toString());
			response.validate();
		} catch (JSONException e) {
			throw new CloudException(e);
		} finally {
			APITrace.end();
		}
	}

	@Override
	public void stop(String vmId) throws InternalException, CloudException {
		stop(vmId, false);
	}

	@Override
	public void stop(String vmId, boolean force) throws InternalException, CloudException {
		APITrace.begin(provider, "stop");
		try {
			APIHandler method = new APIHandler(provider);
			JSONObject json = new JSONObject();
			json.put("Name", vmId);
			APIResponse apiResponse = null;
			if (force) {
				apiResponse = method.post("Server/PowerOffServer/JSON", json.toString());
			} else {
				apiResponse = method.post("Server/ShutdownServer/JSON", json.toString());
			}
			apiResponse.validate();
		} catch (JSONException e) {
			throw new CloudException(e);
		} finally {
			APITrace.end();
		}
	}

	@Override
	public boolean supportsAnalytics() throws CloudException, InternalException {
		return false;
	}

	@Override
	public boolean supportsPauseUnpause(VirtualMachine vm) {
		return true;
	}

	@Override
	public boolean supportsStartStop(VirtualMachine vm) {
		return true;
	}

	@Override
	public boolean supportsSuspendResume(VirtualMachine vm) {
		return false;
	}

	@Override
	public void suspend(String vmId) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void terminate(String vmId) throws InternalException, CloudException {
		APITrace.begin(provider, "terminate");
		try {
			APIHandler method = new APIHandler(provider);
			JSONObject json = new JSONObject();
			json.put("Name", vmId);
			APIResponse response = method.post("Server/DeleteServer/JSON", json.toString());
			response.validate();
		} catch (JSONException e) {
			throw new CloudException(e);
		} finally {
			APITrace.end();
		}
	}

	@Override
	public void unpause(String vmId) throws CloudException, InternalException {
		start(vmId);
	}

	@Override
	public void updateTags(String vmId, Tag... tags) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void updateTags(String[] vmIds, Tag... tags) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void removeTags(String vmId, Tag... tags) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void removeTags(String[] vmIds, Tag... tags) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	public int getDefaultHardwareGroupId(String dataCenterId) throws CloudException, InternalException {
		APITrace.begin(provider, "getDefaultHardwareGroupId");
		APIHandler method = new APIHandler(provider);

		try {
			JSONObject json = new JSONObject();
			json.put("Location", dataCenterId);
			APIResponse response = method.post("Group/GetGroups/JSON", json.toString());
			response.validate();
			
			json = response.getJSON();
			if (json != null && json.has("HardwareGroups") && json.getJSONArray("HardwareGroups").length() > 0) {
				for (int i = 0; i < json.getJSONArray("HardwareGroups").length(); i++) {
					JSONObject group = json.getJSONArray("HardwareGroups").getJSONObject(i);
					if (group != null && group.getString("Name").equals("Default Group")) {
						return group.getInt("ID");
					}
				}

				// TODO improve with groups being passed in with tags or launch
				// options metadata
			}

			throw new CloudException("Unable to find the system hardware group");

		} catch (JSONException e) {
			throw new CloudException(e);
		} finally {
			APITrace.end();
		}
	}
}