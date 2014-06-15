package org.dasein.cloud.tier3.compute.image;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.dasein.cloud.AsynchronousTask;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.Tag;
import org.dasein.cloud.compute.Architecture;
import org.dasein.cloud.compute.ImageCapabilities;
import org.dasein.cloud.compute.ImageClass;
import org.dasein.cloud.compute.ImageCreateOptions;
import org.dasein.cloud.compute.ImageFilterOptions;
import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.compute.MachineImageFormat;
import org.dasein.cloud.compute.MachineImageState;
import org.dasein.cloud.compute.MachineImageSupport;
import org.dasein.cloud.compute.MachineImageType;
import org.dasein.cloud.compute.Platform;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.identity.ServiceAction;
import org.dasein.cloud.tier3.APIHandler;
import org.dasein.cloud.tier3.APIResponse;
import org.dasein.cloud.tier3.Tier3;
import org.dasein.cloud.util.APITrace;
import org.json.JSONException;
import org.json.JSONObject;

public class Tier3Image implements MachineImageSupport {
	static private final Logger logger = Tier3.getLogger(Tier3Image.class);
	private Tier3 provider;

	public Tier3Image(Tier3 provider) {
		this.provider = provider;
	}

	@Override
	public String[] mapServiceAction(ServiceAction action) {
		return new String[0];
	}

	@Override
	public void addImageShare(String providerImageId, String accountNumber) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void addPublicShare(String providerImageId) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public String bundleVirtualMachine(String virtualMachineId, MachineImageFormat format, String bucket, String name)
			throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void bundleVirtualMachineAsync(String virtualMachineId, MachineImageFormat format, String bucket,
			String name, AsynchronousTask<String> trackingTask) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public MachineImage captureImage(ImageCreateOptions options) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
//		if (!options.getMetaData().containsKey("Password")) {
//			throw new CloudException("MetaData entry for 'Password' required");
//		}
//		APITrace.begin(provider, "captureImage");
//		try {
//			APIHandler method = new APIHandler(provider);
//			JSONObject post = new JSONObject();
//			post.put("Name", options.getVirtualMachineId());
//			post.put("Password", options.getMetaData().get("Password"));
//			post.put("TemplateAlias", options.getName());
//			APIResponse response = method.post("Server/ConvertServerToTemplate/JSON", post.toString());
//			response.validate();
//
//			JSONObject deployStatus = provider.getDeploymentStatus(response.getJSON().getInt("RequestID"));
//			MachineImage image = toMachineImage(deployStatus);
//			return image;
//
//		} catch (JSONException e) {
//			throw new CloudException(e);
//		} finally {
//			APITrace.end();
//		}
	}

	@Override
	public void captureImageAsync(ImageCreateOptions options, AsynchronousTask<MachineImage> taskTracker)
			throws CloudException, InternalException {
		throw new OperationNotSupportedException();
//		captureImage(options);
	}

	@Override
	public MachineImage getImage(String providerImageId) throws CloudException, InternalException {
		if (providerImageId == null) {
			return null;
		}
		for (MachineImage image : listImages((ImageFilterOptions) null)) {
			if (providerImageId.equals(image.getProviderMachineImageId())) {
				return image;
			}
		}
		return null;
	}

	@Override
	public MachineImage getMachineImage(String providerImageId) throws CloudException, InternalException {
		return getImage(providerImageId);
	}

	@Override
	public String getProviderTermForImage(Locale locale) {
		try {
			return getCapabilities().getProviderTermForImage(locale, null);
		} catch (CloudException e) {
		} catch (InternalException e) {
		}
		return "template";
	}

	@Override
	public String getProviderTermForImage(Locale locale, ImageClass cls) {
		switch (cls) {
		case KERNEL:
			return "n/a";
		case RAMDISK:
			return "n/a";
		default:
			break;
		}
		return getProviderTermForImage(locale);
	}

	@Override
	public String getProviderTermForCustomImage(Locale locale, ImageClass cls) {
		try {
			return getCapabilities().getProviderTermForCustomImage(locale, cls);
		} catch (CloudException e) {
		} catch (InternalException e) {
		}
		return "template";
	}

	@Override
	public boolean hasPublicLibrary() {
		return false;
	}

	@Override
	public Requirement identifyLocalBundlingRequirement() throws CloudException, InternalException {
		return getCapabilities().identifyLocalBundlingRequirement();
	}

	@Override
	public AsynchronousTask<String> imageVirtualMachine(String vmId, String name, String description)
			throws CloudException, InternalException {
		throw new CloudException("Utilize the captureImage method to create server templates");
	}

	@Override
	public boolean isImageSharedWithPublic(String providerImageId) throws CloudException, InternalException {
		return false;
	}

	@Override
	public boolean isSubscribed() throws CloudException, InternalException {
		return false;
	}

	@Override
	public Iterable<ResourceStatus> listImageStatus(ImageClass cls) throws CloudException, InternalException {
		if (cls != null && cls != ImageClass.MACHINE) {
			return Collections.emptyList();
		}
		APITrace.begin(provider, "listImageStatus");
		try {
			APIHandler method = new APIHandler(provider);
			APIResponse response = method.post("Server/GetServerTemplates/JSON", "");
			response.validate();
			
			ArrayList<ResourceStatus> resources = new ArrayList<ResourceStatus>();

			JSONObject json = response.getJSON();
			if (json.has("Templates")) {
				for (int i = 0; i < json.getJSONArray("Templates").length(); i++) {
					resources.add(new ResourceStatus(json.getJSONArray("Templates").getJSONObject(i).getString("ID"),
							MachineImageState.ACTIVE));
				}
			}

			return resources;
		} catch (JSONException e) {
			throw new CloudException(e);
		} finally {
			APITrace.end();
		}
	}

	@Override
	public Iterable<MachineImage> listImages(ImageFilterOptions options) throws CloudException, InternalException {
		APITrace.begin(provider, "listImages");
		try {
			APIHandler method = new APIHandler(provider);
			APIResponse response = method.post("Server/GetServerTemplates/JSON", "");
			response.validate();
			
			ArrayList<MachineImage> images = new ArrayList<MachineImage>();

			JSONObject json = response.getJSON();
			if (json.has("Templates")) {
				for (int i = 0; i < json.getJSONArray("Templates").length(); i++) {
					MachineImage image = toMachineImage(json.getJSONArray("Templates").getJSONObject(i));
					if (options == null) {
						images.add(image);
					} else {
						if (image != null && options.matches(image)) {
							images.add(image);
						}
					}
				}
			}

			return images;
		} catch (JSONException e) {
			throw new CloudException(e);
		} finally {
			APITrace.end();
		}
	}

	@Override
	public Iterable<MachineImage> listImages(ImageClass cls) throws CloudException, InternalException {
		if (cls != null && cls != ImageClass.MACHINE) {
			return Collections.emptyList();
		}
		return listImages((ImageFilterOptions) null);
	}

	private MachineImage toMachineImage(JSONObject ob) throws CloudException, InternalException {
		if (ob == null) {
			return null;
		}
		try {
			MachineImage image = MachineImage.getMachineImageInstance(provider.getContext().getAccountNumber(), "*", ob
					.getString("ID"), MachineImageState.ACTIVE, ob.getString("Name"), ob.getString("Description"),
					provider.getComputeTranslations().toArchitecture(ob.getString("Name")), provider
							.getComputeTranslations().toPlatform(ob.getString("Name").toLowerCase()));

			if (ob.has("Cpu")) {
				image.setTag("Cpu", ob.get("Cpu").toString());
			}
			if (ob.has("MemoryGB")) {
				image.setTag("MemoryGB", ob.get("MemoryGB").toString());
			}
			if (ob.has("TotalDiskSpaceGB")) {
				image.setTag("TotalDiskSpaceGB", ob.get("TotalDiskSpaceGB").toString());
			}
			if (ob.has("OperatingSystem")) {
				image.setTag("OperatingSystem", ob.get("OperatingSystem").toString());
			}
			if (ob.has("RequestID")) {
				image.setTag("ResourceID", ob.get("ResourceID").toString());
			}

			return image;
		} catch (JSONException e) {
			throw new CloudException(e);
		}
	}

	@Override
	public Iterable<MachineImage> listImages(ImageClass cls, String ownedBy) throws CloudException, InternalException {
		return listImages(cls);
	}

	@Override
	public Iterable<MachineImageFormat> listSupportedFormats() throws CloudException, InternalException {
		return getCapabilities().listSupportedFormats();
	}

	@Override
	public Iterable<MachineImageFormat> listSupportedFormatsForBundling() throws CloudException, InternalException {
		return getCapabilities().listSupportedFormatsForBundling();
	}

	@Override
	public Iterable<MachineImage> listMachineImages() throws CloudException, InternalException {
		return listImages((ImageFilterOptions) null);
	}

	@Override
	public Iterable<MachineImage> listMachineImagesOwnedBy(String accountId) throws CloudException, InternalException {
		return listImages((ImageFilterOptions) null);
	}

	@Override
	public Iterable<String> listShares(String providerImageId) throws CloudException, InternalException {
		return Collections.emptyList();
	}

	@Override
	public Iterable<ImageClass> listSupportedImageClasses() throws CloudException, InternalException {
		return getCapabilities().listSupportedImageClasses();
	}

	@Override
	public Iterable<MachineImageType> listSupportedImageTypes() throws CloudException, InternalException {
		return getCapabilities().listSupportedImageTypes();
	}

	@Override
	public MachineImage registerImageBundle(ImageCreateOptions options) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void remove(String providerImageId) throws CloudException, InternalException {
		APITrace.begin(provider, "remove");
		try {
			MachineImage image = getImage(providerImageId);

			APIHandler method = new APIHandler(provider);
			JSONObject post = new JSONObject();
			post.put("Name", image.getName());
			APIResponse response = method.post("Server/DeleteTemplate/JSON", post.toString());
			response.validate();
			
		} catch (JSONException e) {
			throw new CloudException(e);
		} finally {
			APITrace.end();
		}
	}

	@Override
	public void remove(String providerImageId, boolean checkState) throws CloudException, InternalException {
		remove(providerImageId);
	}

	@Override
	public void removeAllImageShares(String providerImageId) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void removeImageShare(String providerImageId, String accountNumber) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void removePublicShare(String providerImageId) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public Iterable<MachineImage> searchImages(String accountNumber, String keyword, Platform platform,
			Architecture architecture, ImageClass... imageClasses) throws CloudException, InternalException {
		ArrayList<MachineImage> images = new ArrayList<MachineImage>();
		for (MachineImage image : listImages((ImageFilterOptions) null)) {
			for (int i = 0; i < imageClasses.length; i++) {
				// test keyword
				if (keyword != null) {
					if (image.getName() != null) {
						if (image.getName().contains(keyword)) {
							if (!images.contains(image)) {
								images.add(image);
							}
						}
					}
					if (image.getDescription() != null) {
						if (image.getDescription().contains(keyword)) {
							if (!images.contains(image)) {
								images.add(image);
							}
						}
					}
					if (image.getTags() != null) {
						for (String value : image.getTags().values()) {
							if (value != null && value.contains(keyword)) {
								images.add(image);
							}
						}
					}
				}

				// test platform
				if (platform != null && image.getPlatform().compareTo(platform) == 0) {
					if (!images.contains(image)) {
						images.add(image);
					}
				}

				// test architecture
				if (architecture != null && image.getArchitecture().compareTo(architecture) == 0) {
					if (!images.contains(image)) {
						images.add(image);
					}
				}

				// test image class
				if (imageClasses[i] == image.getImageClass()) {
					if (!images.contains(image)) {
						images.add(image);
					}
				}
			}
		}
		return images;
	}

	@Override
	public Iterable<MachineImage> searchMachineImages(String keyword, Platform platform, Architecture architecture)
			throws CloudException, InternalException {
		return searchImages(null, keyword, platform, architecture, (ImageClass) null);
	}

	@Override
	public Iterable<MachineImage> searchPublicImages(ImageFilterOptions options) throws InternalException,
			CloudException {
		return Collections.emptyList();
	}

	@Override
	public Iterable<MachineImage> searchPublicImages(String keyword, Platform platform, Architecture architecture,
			ImageClass... imageClasses) throws CloudException, InternalException {
		return Collections.emptyList();
	}

	@Override
	public void shareMachineImage(String providerImageId, String withAccountId, boolean allow) throws CloudException,
			InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public boolean supportsCustomImages() throws CloudException, InternalException {
		return getCapabilities().supportsImageCapture(null);
	}

	@Override
	public boolean supportsDirectImageUpload() throws CloudException, InternalException {
		return getCapabilities().supportsDirectImageUpload();
	}

	@Override
	public boolean supportsImageCapture(MachineImageType type) throws CloudException, InternalException {
		return getCapabilities().supportsImageCapture(type);
	}

	@Override
	public boolean supportsImageSharing() throws CloudException, InternalException {
		return getCapabilities().supportsImageSharing();
	}

	@Override
	public boolean supportsImageSharingWithPublic() throws CloudException, InternalException {
		return getCapabilities().supportsImageSharingWithPublic();
	}

	@Override
	public boolean supportsPublicLibrary(ImageClass cls) throws CloudException, InternalException {
		return getCapabilities().supportsPublicLibrary(cls);
	}

	@Override
	public void updateTags(String imageId, Tag... tags) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void updateTags(String[] imageIds, Tag... tags) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void removeTags(String imageId, Tag... tags) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void removeTags(String[] imageIds, Tag... tags) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public ImageCapabilities getCapabilities() throws CloudException, InternalException {
		return new ImageCapabilities() {
			
			@Override
			public String getRegionId() {
				return provider.getContext().getRegionId();
			}
			
			@Override
			public String getAccountNumber() {
				return provider.getContext().getAccountNumber();
			}
			
			@Override
			public boolean supportsPublicLibrary(ImageClass cls) throws CloudException, InternalException {
				return false;
			}
			
			@Override
			public boolean supportsImageSharingWithPublic() throws CloudException, InternalException {
				return false;
			}
			
			@Override
			public boolean supportsImageSharing() throws CloudException, InternalException {
				return false;
			}
			
			@Override
			public boolean supportsImageCapture(MachineImageType type) throws CloudException, InternalException {
				return false;
			}
			
			@Override
			public boolean supportsDirectImageUpload() throws CloudException, InternalException {
				return false;
			}
			
			@Override
			public Iterable<MachineImageType> listSupportedImageTypes() throws CloudException, InternalException {
				return Collections.singletonList(MachineImageType.VOLUME);
			}
			
			@Override
			public Iterable<ImageClass> listSupportedImageClasses() throws CloudException, InternalException {
				return Collections.singletonList(ImageClass.MACHINE);
			}
			
			@Override
			public Iterable<MachineImageFormat> listSupportedFormatsForBundling() throws CloudException, InternalException {
				return Collections.emptyList();
			}
			
			@Override
			public Iterable<MachineImageFormat> listSupportedFormats() throws CloudException, InternalException {
				return Collections.emptyList();
			}
			
			@Override
			public Requirement identifyLocalBundlingRequirement() throws CloudException, InternalException {
				return Requirement.REQUIRED;
			}
			
			@Override
			public String getProviderTermForImage(Locale locale, ImageClass cls) {
				return "template";
			}
			
			@Override
			public String getProviderTermForCustomImage(Locale locale, ImageClass cls) {
				return getProviderTermForImage(locale, cls);
			}
			
			@Override
			public boolean canImage(VmState fromState) throws CloudException, InternalException {
				return false;
			}
			
			@Override
			public boolean canBundle(VmState fromState) throws CloudException, InternalException {
				return false;
			}
		};
	}
}