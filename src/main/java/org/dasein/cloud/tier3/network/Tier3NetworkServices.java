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

import javax.annotation.Nonnull;

import org.dasein.cloud.network.DNSSupport;
import org.dasein.cloud.network.FirewallSupport;
import org.dasein.cloud.network.IpAddressSupport;
import org.dasein.cloud.network.LoadBalancerSupport;
import org.dasein.cloud.network.NetworkFirewallSupport;
import org.dasein.cloud.network.NetworkServices;
import org.dasein.cloud.network.VLANSupport;
import org.dasein.cloud.network.VPNSupport;
import org.dasein.cloud.tier3.Tier3;
import org.dasein.cloud.tier3.network.vlan.Tier3VlanSupport;

public class Tier3NetworkServices implements NetworkServices {
    private Tier3 provider;

    public Tier3NetworkServices(@Nonnull Tier3 provider) {
        this.provider = provider;
    }

    @Override
    public DNSSupport getDnsSupport() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FirewallSupport getFirewallSupport() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IpAddressSupport getIpAddressSupport() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public LoadBalancerSupport getLoadBalancerSupport() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NetworkFirewallSupport getNetworkFirewallSupport() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VLANSupport getVlanSupport() {
        return new Tier3VlanSupport(provider);
    }

    @Override
    public VPNSupport getVpnSupport() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasDnsSupport() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasFirewallSupport() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasIpAddressSupport() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasLoadBalancerSupport() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasNetworkFirewallSupport() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasVlanSupport() {
        return true;
    }

    @Override
    public boolean hasVpnSupport() {
        // TODO Auto-generated method stub
        return false;
    }

}
