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
package org.dasein.cloud.tier3.network.vlan;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.network.IPVersion;
import org.dasein.cloud.network.VLANCapabilities;

import java.util.Arrays;
import java.util.Locale;

/**
* Description
* <p>Created by stas: 06/08/2014 13:11</p>
*
* @author Stas Maksimov
* @version 2014.08 initial version
* @since 2014.08
*/
class Tier3VlanCapabilities implements VLANCapabilities {

    @Override
    public String getRegionId() {
        return provider.getContext().getRegionId();
    }

    @Override
    public String getAccountNumber() {
        return provider.getContext().getAccountNumber();
    }

    @Override
    public boolean supportsRawAddressRouting() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean supportsInternetGatewayCreation() throws CloudException, InternalException {
        return false;
    }

    @Override
    public Iterable<IPVersion> listSupportedIPVersions() throws CloudException, InternalException {
        return Arrays.asList(IPVersion.IPV4);
    }

    @Override
    public boolean isVlanDataCenterConstrained() throws CloudException, InternalException {
        return true;
    }

    @Override
    public boolean isSubnetDataCenterConstrained() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean isNetworkInterfaceSupportEnabled() throws CloudException, InternalException {
        return false;
    }

    @Override
    public Requirement identifySubnetDCRequirement() throws CloudException, InternalException {
        return Requirement.NONE;
    }

    @Override
    public Requirement getSubnetSupport() throws CloudException, InternalException {
        return Requirement.NONE;
    }

    @Override
    public Requirement getRoutingTableSupport() throws CloudException, InternalException {
        return Requirement.NONE;
    }

    @Override
    public String getProviderTermForVlan(Locale locale) {
        return "network";
    }

    @Override
    public String getProviderTermForSubnet(Locale locale) {
        return "subnet";
    }

    @Override
    public String getProviderTermForNetworkInterface(Locale locale) {
        return "interface";
    }

    @Override
    public int getMaxVlanCount() throws CloudException, InternalException {
        return LIMIT_UNLIMITED;
    }

    @Override
    public int getMaxNetworkInterfaceCount() throws CloudException, InternalException {
        throw new OperationNotSupportedException();
    }

    @Override
    public boolean allowsNewVlanCreation() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean allowsNewSubnetCreation() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean allowsNewRoutingTableCreation() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean allowsNewNetworkInterfaceCreation() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean allowsMultipleTrafficTypesOverVlan() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean allowsMultipleTrafficTypesOverSubnet() throws CloudException, InternalException {
        return false;
    }
}
