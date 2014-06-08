package org.dasein.cloud.tier3.compute.snapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

import javax.annotation.Nullable;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.Tag;
import org.dasein.cloud.compute.Snapshot;
import org.dasein.cloud.compute.SnapshotCreateOptions;
import org.dasein.cloud.compute.SnapshotFilterOptions;
import org.dasein.cloud.compute.SnapshotState;
import org.dasein.cloud.compute.SnapshotSupport;
import org.dasein.cloud.identity.ServiceAction;
import org.dasein.cloud.tier3.APIHandler;
import org.dasein.cloud.tier3.APIResponse;
import org.dasein.cloud.tier3.Tier3;
import org.dasein.cloud.tier3.compute.image.Tier3Image;
import org.dasein.cloud.util.APITrace;
import org.dasein.util.CalendarWrapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Tier3Snapshot implements SnapshotSupport {
	static private final Logger logger = Tier3.getLogger(Tier3Image.class);
	private Tier3 provider;
	static final String SNAPSHOT_ID_DELIMITER = ".";

	public Tier3Snapshot(Tier3 provider) {
		this.provider = provider;
	}

	@Override
	public String[] mapServiceAction(ServiceAction action) {
		return new String[0];
	}

	@Override
	public void addSnapshotShare(String providerSnapshotId, String accountNumber) throws CloudException,
			InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void addPublicShare(String providerSnapshotId) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public String createSnapshot(SnapshotCreateOptions options) throws CloudException, InternalException {
		APITrace.begin(provider, "createSnapshot");
		try {
			if (options == null || options.getVolumeId() == null) {
				throw new CloudException("VolumeId is required");
			}

			APIHandler method = new APIHandler(provider);
			JSONObject post = new JSONObject();
			post.put("Name", options.getVolumeId());
			APIResponse response = method.post("Server/SnapshotServer/JSON", post.toString());

			JSONObject json = response.getJSON();
			if ((json.has("Success") && !json.getBoolean("Success")) || !json.has("Server")) {
				logger.warn(json.getString("Message"));
				return null;
			}

			// TODO watch the deployment status response to see what we can
			// return, looking for the snapshot name
			int requestId = response.getJSON().getInt("RequestID");
			String name = null;
			long timeout = System.currentTimeMillis() + CalendarWrapper.MINUTE;
			while (timeout > System.currentTimeMillis()) {

				JSONObject deployStatus = provider.getDeploymentStatus(requestId);

				JSONArray servers = deployStatus.getJSONArray("Servers");
				if (deployStatus.has("Servers") && !deployStatus.isNull("Servers") && servers.length() > 0) {
					name = servers.get(0).toString();
					break;
				} else {
					try {
						Thread.sleep(10000L);
					} catch (InterruptedException ignore) {
					}
				}
			}

			// since the name of a snapshot doesn't contain the server name,
			// might need to concatenate the return
			return name;

		} catch (JSONException e) {
			throw new CloudException(e);
		} finally {
			APITrace.end();
		}
	}

	@Override
	public String create(String ofVolume, String description) throws InternalException, CloudException {
		SnapshotCreateOptions options = SnapshotCreateOptions.getInstanceForCreate(ofVolume, null, null);
		return createSnapshot(options);
	}

	@Override
	public String getProviderTermForSnapshot(Locale locale) {
		return "snapshot";
	}

	@Override
	public Snapshot getSnapshot(String snapshotId) throws InternalException, CloudException {
		APITrace.begin(provider, "getSnapshot");
		try {
			if (snapshotId == null) {
				throw new CloudException("SnapshotId is required");
			}
			String serverName = extractServerNameFromSnapshotId(snapshotId);
			snapshotId = removeServerNameFromSnapshotId(snapshotId, serverName);

			APIHandler method = new APIHandler(provider);
			JSONObject post = new JSONObject();
			post.put("Name", serverName);
			APIResponse response = method.post("Server/GetSnapshots/JSON", "");
			response.validate();

			JSONObject json = response.getJSON();
			if (json.has("Snapshots")) {
				for (int i = 0; i < json.getJSONArray("Snapshots").length(); i++) {
					JSONObject snapshot = json.getJSONArray("Snapshots").getJSONObject(i);
					if (snapshot.getString("Name").equals(snapshotId)) {
						return toSnapshot(serverName, snapshot);
					}
				}
			}

			return null;

		} catch (JSONException e) {
			throw new CloudException(e);
		} finally {
			APITrace.end();
		}
	}

	private String removeServerNameFromSnapshotId(String snapshotId, String serverName) {
		snapshotId = snapshotId.replace(serverName + SNAPSHOT_ID_DELIMITER, "");
		return snapshotId;
	}

	private String extractServerNameFromSnapshotId(String snapshotId) {
		String serverName = snapshotId.substring(0, snapshotId.indexOf(SNAPSHOT_ID_DELIMITER));
		return serverName;
	}

	private @Nullable
	Snapshot toSnapshot(String serverName, @Nullable JSONObject json) throws CloudException, InternalException {
		if (json == null) {
			return null;
		}

		try {
			String snapshotId = buildSnapshotId(serverName, json.getString("Name"));
			String regionId = provider.getContext().getRegionId();
			String snapshotName = (json.has("Name") ? json.getString("Name") : null);
			if (snapshotName == null) {
				snapshotName = snapshotId;
			}

			String description = (json.has("Description") ? json.getString("Description") : null);
			if (description == null) {
				description = snapshotName;
			}

			long created = (json.has("DateCreated") ? provider.parseTimestamp(json.getString("DateCreated")) : -1L);

			Snapshot snapshot = new Snapshot();
			snapshot.setCurrentState(SnapshotState.AVAILABLE);
			snapshot.setDescription(description);
			snapshot.setName(snapshotName);
			snapshot.setOwner(provider.getContext().getAccountNumber());
			snapshot.setProviderSnapshotId(snapshotId);
			snapshot.setRegionId(regionId);
			snapshot.setSizeInGb(-1);
			snapshot.setSnapshotTimestamp(created);
			snapshot.setVolumeId(serverName);
			return snapshot;
		} catch (JSONException e) {
			throw new CloudException(e);
		}
	}

	private String buildSnapshotId(String serverName, String snapshotId) throws JSONException {
		return serverName + SNAPSHOT_ID_DELIMITER + snapshotId;
	}

	@Override
	public Requirement identifyAttachmentRequirement() throws InternalException, CloudException {
		return Requirement.REQUIRED;
	}

	@Override
	public boolean isPublic(String snapshotId) throws InternalException, CloudException {
		return false;
	}

	@Override
	public boolean isSubscribed() throws InternalException, CloudException {
		return true;
	}

	@Override
	public Iterable<String> listShares(String snapshotId) throws InternalException, CloudException {
		return Collections.emptyList();
	}

	@Override
	public Iterable<ResourceStatus> listSnapshotStatus() throws InternalException, CloudException {
		// can't do a lookup of snapshots across the account - must be tied to a
		// server
		throw new OperationNotSupportedException();
	}

	@Override
	public Iterable<Snapshot> listSnapshots() throws InternalException, CloudException {
		throw new OperationNotSupportedException();
	}

	@Override
	public Iterable<Snapshot> listSnapshots(SnapshotFilterOptions options) throws InternalException, CloudException {
		APITrace.begin(provider, "listSnapshots");
		try {
			if (options == null || options.getTags() == null || !options.getTags().containsKey("Server")) {
				throw new CloudException("Tag with name of 'Server' and value of server name is required");
			}
			APIHandler method = new APIHandler(provider);
			JSONObject post = new JSONObject();
			if (options.getAccountNumber() != null) {
				post.put("AccountAlias", options.getAccountNumber());
			}
			post.put("Name", options.getTags().get("Server"));
			APIResponse response = method.post("Server/GetSnapshots/JSON", post.toString());
			response.validate();

			ArrayList<Snapshot> snapshots = new ArrayList<Snapshot>();

			JSONObject json = response.getJSON();
			if (json.has("Snapshots")) {
				for (int i = 0; i < json.getJSONArray("Snapshots").length(); i++) {
					snapshots.add(toSnapshot(options.getTags().get("Server"), json.getJSONArray("Snapshots").getJSONObject(i)));
				}
			}

			return snapshots;

		} catch (JSONException e) {
			throw new CloudException(e);
		} finally {
			APITrace.end();
		}

	}

	@Override
	public void remove(String snapshotId) throws InternalException, CloudException {
		APITrace.begin(provider, "getSnapshot");
		try {
			if (snapshotId == null) {
				throw new CloudException("SnapshotId is required");
			}

			String serverName = extractServerNameFromSnapshotId(snapshotId);
			snapshotId = removeServerNameFromSnapshotId(snapshotId, serverName);

			APIHandler method = new APIHandler(provider);
			JSONObject post = new JSONObject();
			post.put("Name", serverName);
			post.put("SnapshotName", snapshotId);
			APIResponse response = method.post("Server/DeleteSnapshot/JSON", "");
			response.validate();

		} catch (JSONException e) {
			throw new CloudException(e);
		} finally {
			APITrace.end();
		}
	}

	@Override
	public void removeAllSnapshotShares(String providerSnapshotId) throws CloudException, InternalException {
		// unimplemented

	}

	@Override
	public void removeSnapshotShare(String providerSnapshotId, String accountNumber) throws CloudException,
			InternalException {
		// unimplemented

	}

	@Override
	public void removePublicShare(String providerSnapshotId) throws CloudException, InternalException {
		// unimplemented

	}

	@Override
	public void removeTags(String snapshotId, Tag... tags) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void removeTags(String[] snapshotIds, Tag... tags) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public Iterable<Snapshot> searchSnapshots(SnapshotFilterOptions options) throws InternalException, CloudException {
		return listSnapshots(options);
	}

	@Override
	public Iterable<Snapshot> searchSnapshots(String ownerId, String keyword) throws InternalException, CloudException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void shareSnapshot(String snapshotId, String withAccountId, boolean affirmative) throws InternalException,
			CloudException {
		throw new OperationNotSupportedException();
	}

	@Override
	public Snapshot snapshot(String volumeId, String name, String description, Tag... tags) throws InternalException,
			CloudException {
		throw new OperationNotSupportedException("Use the createSnapshot method instead");
	}

	@Override
	public boolean supportsSnapshotCopying() throws CloudException, InternalException {
		return false;
	}

	@Override
	public boolean supportsSnapshotCreation() throws CloudException, InternalException {
		return true;
	}

	@Override
	public boolean supportsSnapshotSharing() throws InternalException, CloudException {
		return false;
	}

	@Override
	public boolean supportsSnapshotSharingWithPublic() throws InternalException, CloudException {
		return false;
	}

	@Override
	public void updateTags(String snapshotId, Tag... tags) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void updateTags(String[] snapshotIds, Tag... tags) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}
}
