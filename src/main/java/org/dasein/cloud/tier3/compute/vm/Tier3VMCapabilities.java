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
package org.dasein.cloud.tier3.compute.vm;

import org.dasein.cloud.*;
import org.dasein.cloud.compute.*;
import org.dasein.cloud.tier3.Tier3;
import org.dasein.cloud.util.NamingConstraints;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Locale;

/**
* Description
* <p>Created by stas: 06/08/2014 12:57</p>
*
* @author Stas Maksimov
* @version 2014.08 initial version
* @since 2014.08
*/
class Tier3VMCapabilities extends AbstractCapabilities<Tier3> implements VirtualMachineCapabilities {

    public Tier3VMCapabilities( Tier3 provider ) {
        super(provider);
    }

    @Override
    public Iterable<Architecture> listSupportedArchitectures() throws InternalException, CloudException {
        return Arrays.asList(Architecture.I32, Architecture.I64);
    }

    @Override public boolean supportsSpotVirtualMachines() throws InternalException, CloudException {
        return false;
    }

    @Override public boolean supportsAlterVM() {
        return false;
    }

    @Override public boolean supportsClone() {
        return false;
    }

    @Override public boolean supportsPause() {
        return true;
    }

    @Override public boolean supportsReboot() {
        return true;
    }

    @Override public boolean supportsResume() {
        return true;
    }

    @Override public boolean supportsStart() {
        return true;
    }

    @Override public boolean supportsStop() {
        return true;
    }

    @Override public boolean supportsSuspend() {
        return true;
    }

    @Override public boolean supportsTerminate() {
        return true;
    }

    @Override public boolean supportsUnPause() {
        return true;
    }

    @Override
    public boolean isUserDataSupported() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean isExtendedAnalyticsSupported() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean isBasicAnalyticsSupported() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean isAPITerminationPreventable() throws CloudException, InternalException {
        return false;
    }

    @Override
    public Requirement identifyVlanRequirement() throws CloudException, InternalException {
        return Requirement.REQUIRED;
    }

    @Override
    public Requirement identifySubnetRequirement() throws CloudException, InternalException {
        return Requirement.NONE;
    }

    @Override
    public Requirement identifyStaticIPRequirement() throws CloudException, InternalException {
        return Requirement.NONE;
    }

    @Override
    public Requirement identifyShellKeyRequirement(Platform platform) throws CloudException, InternalException {
        return Requirement.NONE;
    }

    @Override
    public Requirement identifyRootVolumeRequirement() throws CloudException, InternalException {
        return Requirement.NONE;
    }

    @Override
    public Requirement identifyPasswordRequirement(Platform platform) throws CloudException, InternalException {
        return Requirement.REQUIRED;
    }

    @Override
    public Requirement identifyImageRequirement(ImageClass cls) throws CloudException, InternalException {
        return (cls.equals(ImageClass.MACHINE) ? Requirement.REQUIRED : Requirement.NONE);
    }

    @Override
    public Requirement identifyDataCenterLaunchRequirement() throws CloudException, InternalException {
        return Requirement.OPTIONAL;
    }

    @Override
    public NamingConstraints getVirtualMachineNamingConstraints() throws CloudException, InternalException {
        return NamingConstraints.getAlphaOnly(1, 6);
    }

    @Nullable @Override public VisibleScope getVirtualMachineVisibleScope() {
        return null;
    }

    @Nullable @Override public VisibleScope getVirtualMachineProductVisibleScope() {
        return null;
    }

    @Override
    public VMScalingCapabilities getVerticalScalingCapabilities() throws CloudException, InternalException {
        return VMScalingCapabilities.getInstance(false, true, Requirement.NONE, Requirement.NONE);
    }

    @Override
    public String getProviderTermForVirtualMachine(Locale locale) throws CloudException, InternalException {
        return "server";
    }

    @Override
    public int getMaximumVirtualMachineCount() throws CloudException, InternalException {
        return LIMIT_UNLIMITED;
    }

    @Override
    public int getCostFactor(VmState state) throws CloudException, InternalException {
        switch (state) {
        case TERMINATED:
            return 0;
        case PAUSED:
            return 50;
        case SUSPENDED:
            return 10;
        default:
            return 100;
        }
    }

    @Override
    public boolean canUnpause(VmState fromState) throws CloudException, InternalException {
        if (fromState == VmState.PAUSED) {
            return true;
        }
        return false;
    }

    @Override
    public boolean canTerminate(VmState fromState) throws CloudException, InternalException {
        return true;
    }

    @Override
    public boolean canSuspend(VmState fromState) throws CloudException, InternalException {
        if (fromState == VmState.RUNNING) {
            return true;
        }
        return false;
    }

    @Override
    public boolean canStop(VmState fromState) throws CloudException, InternalException {
        if (fromState == VmState.RUNNING) {
            return true;
        }
        return false;
    }

    @Override
    public boolean canStart(VmState fromState) throws CloudException, InternalException {
        if (fromState == VmState.STOPPED) {
            return true;
        }
        return false;
    }

    @Override
    public boolean canResume(VmState fromState) throws CloudException, InternalException {
        if (fromState == VmState.SUSPENDED) {
            return true;
        }
        return false;
    }

    @Override
    public boolean canReboot(VmState fromState) throws CloudException, InternalException {
        if (fromState == VmState.RUNNING) {
            return true;
        }
        return false;
    }

    @Override
    public boolean canPause(VmState fromState) throws CloudException, InternalException {
        if (fromState == VmState.RUNNING) {
            return true;
        }
        return false;
    }

    @Override
    public boolean canClone(VmState fromState) throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean canAlter(VmState fromState) throws CloudException, InternalException {
        if (fromState == VmState.RUNNING) {
            return true;
        }
        return false;
    }
}
