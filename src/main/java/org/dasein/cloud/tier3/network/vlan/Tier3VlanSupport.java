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

package org.dasein.cloud.tier3.network.vlan;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.network.*;
import org.dasein.cloud.tier3.APIHandler;
import org.dasein.cloud.tier3.APIResponse;
import org.dasein.cloud.tier3.Tier3;
import org.dasein.cloud.util.APITrace;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class Tier3VlanSupport extends AbstractVLANSupport {
    static private final Logger logger = Tier3.getLogger(Tier3VlanSupport.class);
    private Tier3                 provider;
    private transient volatile Tier3VlanCapabilities capabilities;

    public Tier3VlanSupport( Tier3 provider ) {
        super(provider);
        this.provider = provider;
    }

    @Override
    public @Nullable VLAN getVlan(@Nonnull String vlanId) throws CloudException, InternalException {
        APITrace.begin(provider, "getVlan");
        try {
            APIHandler method = new APIHandler(provider);
            JSONObject post = new JSONObject();
            post.put("Name", vlanId);
            APIResponse response = method.post("Network/GetNetworkDetails/JSON", post.toString());
            try {
                response.validate();
            } catch (CloudException e) {
                if (response.getJSON().getInt("StatusCode") == 5) {
                    return null;
                } else {
                    throw e;
                }
            }

            if (response.getJSON().has("NetworkDetails")) {
                return toVlan(response.getJSON().getJSONObject("NetworkDetails"));
            }

            return null;
        } catch (JSONException e) {
            throw new CloudException(e);
        } finally {
            APITrace.end();
        }
    }

    @Override
    public boolean isConnectedViaInternetGateway(String vlanId) throws CloudException, InternalException {
        return true;
    }

    @Override
    public @Nullable String getAttachedInternetGatewayId( @Nonnull String vlanId ) throws CloudException, InternalException {
        return null;
    }

    @Override
    public @Nullable InternetGateway getInternetGatewayById( @Nonnull String gatewayId ) throws CloudException, InternalException {
        return null;
    }

    @Override
    public boolean isSubscribed() throws CloudException, InternalException {
        return true;
    }

    @Override
    public @Nonnull Collection<InternetGateway> listInternetGateways( @Nullable String vlanId ) throws CloudException, InternalException {
        logger.warn("Internet gateways operations are not supported");
        return Collections.emptyList();
    }

    @Override
    public @Nonnull Iterable<NetworkInterface> listNetworkInterfacesInVLAN(@Nonnull String vlanId) throws CloudException,
            InternalException {
        APITrace.begin(provider, "listNetworkInterfacesInVLAN");
        try {
            APIHandler method = new APIHandler(provider);
            JSONObject post = new JSONObject();
            post.put("Name", vlanId);
            APIResponse response = method.post("Network/GetNetworkDetails/JSON", post.toString());
            response.validate();

            List<NetworkInterface> networks = new ArrayList<NetworkInterface>();
            if (response.getJSON().has("NetworkDetails")) {
                networks.add(toNetwork(response.getJSON().getJSONObject("NetworkDetails")));
            }

            return networks;
        } catch (JSONException e) {
            throw new CloudException(e);
        } finally {
            APITrace.end();
        }
    }

    @Override
    public Iterable<ResourceStatus> listVlanStatus() throws CloudException, InternalException {
        APITrace.begin(provider, "listVlanStatus");
        try {
            APIHandler method = new APIHandler(provider);
            APIResponse response = method.post("Network/GetNetworks/JSON", "");
            response.validate();

            List<ResourceStatus> vlans = new ArrayList<ResourceStatus>();

            JSONObject json = response.getJSON();
            if (json.has("Networks")) {
                for (int i = 0; i < json.getJSONArray("Networks").length(); i++) {
                    ResourceStatus vlan = provider.getNetworkTranslations().toVlanStatus(
                            json.getJSONArray("Networks").getJSONObject(i));
                    if (vlan != null) {
                        vlans.add(vlan);
                    }
                }
            }

            return vlans;
        } catch (JSONException e) {
            throw new CloudException(e);
        } finally {
            APITrace.end();
        }
    }

    @Override
    public Iterable<VLAN> listVlans() throws CloudException, InternalException {
        APITrace.begin(provider, "listVlans");
        try {
            APIHandler method = new APIHandler(provider);
            APIResponse response = method.post("Network/GetNetworks/JSON", "");
            response.validate();

            List<VLAN> vlans = new ArrayList<VLAN>();

            JSONObject json = response.getJSON();
            if (json.has("Networks")) {
                for (int i = 0; i < json.getJSONArray("Networks").length(); i++) {

                    JSONObject post = new JSONObject();
                    post.put("Name", json.getJSONArray("Networks").getJSONObject(i).getString("Name"));
                    APIResponse detailResponse = method.post("Network/GetNetworkDetails/JSON", post.toString());
                    detailResponse.validate();

                    JSONObject detailJson = detailResponse.getJSON();
                    if (!detailJson.getBoolean("Success")) {
                        throw new CloudException(detailJson.getString("Message"));
                    }
                    if (detailJson.has("NetworkDetails")) {
                        vlans.add(toVlan(detailJson.getJSONObject("NetworkDetails")));
                    }
                }
            }

            return vlans;
        } catch (JSONException e) {
            throw new CloudException(e);
        } finally {
            APITrace.end();
        }
    }

    @Override
    public void removeInternetGatewayById( @Nonnull String id ) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Internet gateways operations are not supported");
    }

    @Override
    public void removeVlan(String vlanId) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Creating/removing VLANs is not supported");
        // unimplemented
    }


    private VLAN toVlan(JSONObject ob) throws CloudException, InternalException {
        if (ob == null) {
            return null;
        }
        try {
            VLAN vlan = new VLAN();

            vlan.setProviderVlanId(ob.getString("Name"));
            vlan.setProviderOwnerId(provider.getContext().getAccountNumber());
            vlan.setProviderRegionId(provider.getDataCenterServices().getDataCenter(ob.getString("Location"))
                    .getRegionId());
            vlan.setProviderDataCenterId(ob.getString("Location"));

            vlan.setCurrentState(VLANState.AVAILABLE);
            vlan.setName(ob.getString("Name"));
            vlan.setDescription(ob.getString("Description"));
            vlan.setCidr(ob.getString("NetworkMask"), ob.getString("Gateway"));

            vlan.setSupportedTraffic(IPVersion.IPV4);
            vlan.setDnsServers(new String[0]);
            vlan.setNtpServers(new String[0]);
            vlan.setTags(Collections.<String, String> emptyMap());

            return vlan;
        } catch (JSONException e) {
            throw new CloudException(e);
        }
    }

    private NetworkInterface toNetwork(JSONObject ob) throws CloudException, InternalException {
        if (ob == null) {
            return null;
        }
        try {
            NetworkInterface network = new NetworkInterface();

            network.setProviderVlanId(ob.getString("Name"));
            network.setName(ob.getString("Name"));
            network.setDescription(ob.getString("Description"));
            network.setProviderDataCenterId(ob.getString("Location"));

            List<RawAddress> ips = new ArrayList<RawAddress>();
            if (ob.has("IPAddresses")) {
                for (int i = 0; i < ob.getJSONArray("IPAddresses").length(); i++) {
                    JSONObject json = ob.getJSONArray("IPAddresses").getJSONObject(i);
                    ips.add(new RawAddress(json.getString("Address")));
                }
            }
            if (ips.size() > 0) {
                network.setIpAddresses(ips.toArray(new RawAddress[ips.size()]));
            }

            return network;
        } catch (JSONException e) {
            throw new CloudException(e);
        }
    }

    @Override
    public VLAN createVlan(VlanCreateOptions options) throws InternalException, CloudException {
        throw new OperationNotSupportedException("Vlans can only be created via the CenturyLink Cloud Control Portal.");
    }

    @Override
    public VLANCapabilities getCapabilities() throws CloudException, InternalException {
        if( capabilities == null ) {
            capabilities = new Tier3VlanCapabilities(provider);
        }
        return capabilities;
    }

    @Override
    public @Nonnull String getProviderTermForNetworkInterface( @Nonnull Locale locale ) {
        try {
            return getCapabilities().getProviderTermForNetworkInterface(locale);
        } catch( CloudException e ) {
        } catch( InternalException e ) {
        }
        return "interface";
    }

    @Override
    public @Nonnull String getProviderTermForSubnet( @Nonnull Locale locale ) {
        try {
            return getCapabilities().getProviderTermForSubnet(locale);
        } catch( CloudException e ) {
        } catch( InternalException e ) {
        }
        return "subnet";
    }

    @Override
    public @Nonnull String getProviderTermForVlan( @Nonnull Locale locale ) {
        try {
            return getCapabilities().getProviderTermForVlan(locale);
        } catch( CloudException e ) {
        } catch( InternalException e ) {
        }
        return "network";
    }

}
