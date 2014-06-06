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
import org.dasein.cloud.tier3.APIHandler;
import org.dasein.cloud.tier3.APIResponse;
import org.dasein.cloud.tier3.Tier3;
import org.dasein.cloud.util.APITrace;
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

			if (response == null) {
				throw new CloudException("No server was altered");
			}

			JSONObject json = new JSONObject();
			json.put("RequestId", response.getJSON().getInt("RequestID"));
			response = method.post("Blueprint/GetDeploymentStatus/JSON", new JSONObject(post).toString());

			if (response == null) {
				throw new CloudException("Could not retrieve server configure request");
			}

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
		throw new OperationNotSupportedException();
	}

	@Override
	public void enableAnalytics(String vmId) throws InternalException, CloudException {
		throw new OperationNotSupportedException();
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

			json = response.getJSON();
			if (json.has("Success") && !json.getBoolean("Success")) {
				logger.warn(json.getString("Message"));
				return null;
			}
			return toVirtualMachine(new JSONObject(json));
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
				vm.setProviderVirtualMachineId(ob.getString("ID"));
			}
			if (ob.has("Name")) {
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
			if (ob.has("PowerState")) {
				vm.setCurrentState(provider.getComputeTranslations().toVmState(ob.getString("PowerState")));

				if (VmState.RUNNING.equals(vm.getCurrentState())) {
					vm.setPausable(true);
					vm.setRebootable(true);
				}
			}
			if (vm.getName() == null) {
				vm.setName(vm.getProviderVirtualMachineId());
			}
			if (vm.getDescription() == null) {
				vm.setDescription(vm.getName());
			}

			String os = translateOS(ob.get("OperatingSystem"));
			vm.setArchitecture(provider.getComputeTranslations().toArchitecture(os));
			vm.setPlatform(Platform.guess(os));

			if (ob.has("CustomFields") && !ob.isNull("CustomFields")) {
				JSONArray fields = ob.getJSONArray("CustomFields");
				for (int i = 0; i < fields.length(); i++) {
					JSONObject f = fields.getJSONObject(i);
					vm.addTag(new Tag(f.getString("Name"), f.getString("Value")));
				}
			}

			return vm;
		} catch (JSONException e) {
			throw new CloudException(e);
		}
	}

	private ArrayList<OperatingSystem> getOperatingSystems(Architecture architecture) {
		ArrayList<OperatingSystem> osList = new ArrayList<OperatingSystem>();
		if (architecture == null || architecture == Architecture.I32) {
			osList.add(new OperatingSystem(32, "CentOS 5 | 32-Bit", 8, 128));
			osList.add(new OperatingSystem(34, "CentOS 6 | 32-Bit", 16, 128));
			osList.add(new OperatingSystem(29, "Ubuntu 10 | 32-Bit", 16, 128));
			osList.add(new OperatingSystem(2, "Windows 2003 32-bit", 4, 32));
			osList.add(new OperatingSystem(15, "Windows 2003 R2 Enterprise | 32-bit", 8, 128));
			osList.add(new OperatingSystem(15, "Windows 2003 R2 Standard | 32-bit", 4, 128));
		}
		if (architecture == null || architecture == Architecture.I64) {
			osList.add(new OperatingSystem(33, "CentOS 5 | 64-Bit", 16, 128));
			osList.add(new OperatingSystem(35, "CentOS 6 | 64-Bit", 16, 128));
			osList.add(new OperatingSystem(36, "Debian 6 | 64-Bit", 16, 128));
			osList.add(new OperatingSystem(37, "Debian 7 | 64-Bit", 16, 128));
			osList.add(new OperatingSystem(25, "RedHat Enterprise Linux 5 | 64-bit", 16, 28));
			osList.add(new OperatingSystem(30, "Ubuntu 10 | 64-Bit", 16, 128));
			osList.add(new OperatingSystem(31, "Ubuntu 12 | 64-Bit", 16, 128));
			osList.add(new OperatingSystem(15, "Windows 2003 R2 Enterprise | 64-bit", 8, 128));
			osList.add(new OperatingSystem(15, "Windows 2003 R2 Standard | 64-bit", 4, 32));
			osList.add(new OperatingSystem(26, "Windows 2008 Datacenter 64-bit", 4, 4));
			osList.add(new OperatingSystem(18, "Windows 2008 Enterprise | 64-bit", 8, 128));
			osList.add(new OperatingSystem(18, "Windows 2008 Standard | 64-bit", 4, 32));
			osList.add(new OperatingSystem(27, "Windows 2012 Datacenter Edition | 64-bit", 16, 128));
			osList.add(new OperatingSystem(28, "Windows 2012 R2 Datacenter Edition | 64-Bit", 16, 128));
		}
		// osList.add(new OperatingSystem(6, "CentOS 32-bit", ?, ?));
		// osList.add(new OperatingSystem(7, "CentOS 64-bit", ?, ?));
		// osList.add(new OperatingSystem(21, "Debian 64-bit", ?, ?));
		// osList.add(new OperatingSystem(13, "FreeBSD 32-bit", ?, ?));
		// osList.add(new OperatingSystem(14, "FreeBSD 64-bit", ?, ?));
		// osList.add(new OperatingSystem(?, "RedHat Enterprise Linux 6 64-bit",
		// ?, ?));
		// osList.add(new OperatingSystem(22, "RedHat Enterprise Linux 64-bit",
		// ?, ?));
		// osList.add(new OperatingSystem(38, "RedHat 6 64-Bit", ?, ?));
		// osList.add(new OperatingSystem(?, "Small CentOS", ?, ?));
		// osList.add(new OperatingSystem(?, "Stemcell | BOSH", ?, ?));
		// osList.add(new OperatingSystem(?, "Stemcell | Micro-BOSH", ?, ?));
		// osList.add(new OperatingSystem(19, "Ubuntu 32-bit", ?, ?));
		// osList.add(new OperatingSystem(20, "Ubuntu 64-bit", ?, ?));
		// osList.add(new OperatingSystem(?, "Ubuntu 10 LAMP | 64-Bit", ?, ?));
		// osList.add(new OperatingSystem(?, "Web Fabric Ubuntu x64 Template",
		// 16, 128));
		// osList.add(new OperatingSystem(?,
		// "Web Fabric Ubuntu x64 Template V2", 16, 128));
		// osList.add(new OperatingSystem(8, "Windows XP 32-bit", ?, ?));
		// osList.add(new OperatingSystem(9, "Windows Vista 32-bit", ?, ?));
		// osList.add(new OperatingSystem(10, "Windows Vista 64-bit", ?, ?));
		// osList.add(new OperatingSystem(11, "Windows 7 32-bit", ?, ?));
		// osList.add(new OperatingSystem(12, "Windows 7 64-bit", ?, ?));
		// osList.add(new OperatingSystem(23, "Windows 8 64-bit", ?, ?));
		// osList.add(new OperatingSystem(3, "Windows 2003 64-bit", ?, ?));
		// osList.add(new OperatingSystem(4, "Windows 2008 32-bit", ?, ?));
		// osList.add(new OperatingSystem(5, "Windows 2008 64-bit", ?, ?));
		// osList.add(new OperatingSystem(15, "Windows 2003 Enterprise 32-bit",
		// ?, ?));
		// osList.add(new OperatingSystem(16, "Windows 2003 Enterprise 64-bit",
		// ?, ?));
		// osList.add(new OperatingSystem(17, "Windows 2008 Enterprise 32-bit",
		// ?, ?));
		// osList.add(new OperatingSystem(24, "Windows 2012 64-bit", ?, ?));

		return osList;
	}

	private String translateOS(Object os) {
		Integer osId = Integer.parseInt(os.toString());
		if (osId != null) {
			for (OperatingSystem o : getOperatingSystems(null)) {
				if (osId == o.id) {
					return o.name;
				}
			}
		}
		return null;
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
			APIHandler method = new APIHandler(provider);
			JSONObject post = new JSONObject();

			post.put("AccountAlias", provider.getContext().getAccountNumber());
			post.put("LocationAlias", withLaunchOptions.getDataCenterId());

			MachineImage template = provider.getComputeServices().getImageSupport()
					.getImage(withLaunchOptions.getMachineImageId());
			System.out.println("CTS launch getImage template tags: " + template.getTags());
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
			System.out.println("launch options vlanId: " + withLaunchOptions.getVlanId());
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
			System.out.println("CTS launch create server request: " + post.toString());
			APIResponse response = method.post("Server/CreateServer/JSON", post.toString());

			if (response == null) {
				throw new CloudException("No server was created");
			}
			System.out.println("CTS launch create server response : " + response.getJSON());

			JSONObject json = new JSONObject();
			if (json.has("Success") && !json.getBoolean("Success")) {
				throw new CloudException(json.getString("Message"));
			}
			json.put("RequestId", response.getJSON().getInt("RequestID"));
			response = method.post("Blueprint/GetDeploymentStatus/JSON", post.toString());

			if (response == null) {
				throw new CloudException("Could not retrieve server build request");
			}

			json = response.getJSON();
			System.out.println("CTS launch get deployment status response : " + json);
			if (json.has("Success") && !json.getBoolean("Success")) {
				throw new CloudException(json.getString("Message"));
			}

			VirtualMachine vm = new VirtualMachine();
			vm.addTag(new Tag("RequestID", json.getString("RequestID")));
			if (json.has("Servers") && json.get("Servers") != null) {
				@SuppressWarnings("unchecked")
				List<String> servers = (List<String>) json.get("Servers");
				vm.setName(servers.get(0));
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

		for (OperatingSystem os : getOperatingSystems(architecture)) {
			for (int cpu = 1; cpu <= os.maxCpu; cpu++) {

				// for lower numbers, increment by 1
				for (int mem = 1; mem <= 16; mem++) {
					VirtualMachineProduct product = new VirtualMachineProduct();
					product.setName(os.name + ", " + cpu + " CPU, " + mem + " GB Memory");
					product.setRamSize(new Storage<Gigabyte>(mem, Storage.GIGABYTE));
					product.setCpuCount(cpu);
					product.setDescription(product.getName());
					product.setProviderProductId(product.getName());
					product.setRootVolumeSize(new Storage<Gigabyte>(0, Storage.GIGABYTE));
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
					product.setRootVolumeSize(new Storage<Gigabyte>(0, Storage.GIGABYTE));
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
		APITrace.begin(provider, "listVirtualMachineStatus");
		try {
			APIHandler method = new APIHandler(provider);
			APIResponse response = method.post("Server/GetAllServers/JSON", "");

			ArrayList<ResourceStatus> vms = new ArrayList<ResourceStatus>();

			JSONObject json = response.getJSON();
			if (json.has("Servers")) {
				for (int i = 0; i < json.getJSONArray("Servers").length(); i++) {
					ResourceStatus vm = provider.getComputeTranslations().toVmStatus(
							json.getJSONArray("Servers").getJSONObject(i));
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
	public Iterable<VirtualMachine> listVirtualMachines() throws InternalException, CloudException {
		APITrace.begin(provider, "listVirtualMachines");
		try {
			APIHandler method = new APIHandler(provider);
			APIResponse response = method.post("Server/GetAllServers/JSON", "");

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
			method.post("Server/PauseServer/JSON", json.toString());
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
			method.post("Server/RebootServer/JSON", json.toString());
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
			method.post("Server/PowerOnServer/JSON", json.toString());
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
			if (force) {
				method.post("Server/ResetServer/JSON", json.toString());
			} else {
				method.post("Server/ShutdownServer/JSON", json.toString());
			}
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
			method.post("Server/DeleteServer/JSON", json.toString());
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

	private class OperatingSystem {
		int id;
		String name;
		int maxCpu;
		int maxMemory;

		public OperatingSystem(int id, String name, int maxCpu, int maxMemory) {
			this.id = id;
			this.name = name;
			this.maxCpu = maxCpu;
			this.maxMemory = maxMemory;
		}
	}

	public int getDefaultHardwareGroupId(String dataCenterId) throws CloudException, InternalException {
		APITrace.begin(provider, "getDefaultHardwareGroupId");
		APIHandler method = new APIHandler(provider);

		try {
			JSONObject json = new JSONObject();
			json.put("Location", dataCenterId);
			APIResponse response = method.post("Group/GetGroups/JSON", json.toString());

			json = response.getJSON();
			if (json == null) {
				throw new CloudException("Unable to find any hardware groups");
			}

			if (json.has("HardwareGroups") && json.get("HardwareGroups") != null) {
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