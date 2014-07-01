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

package org.dasein.cloud.tier3.compute;

import javax.annotation.Nonnull;

import org.dasein.cloud.compute.AbstractComputeServices;
import org.dasein.cloud.compute.MachineImageSupport;
import org.dasein.cloud.compute.SnapshotSupport;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.dasein.cloud.tier3.Tier3;
import org.dasein.cloud.tier3.compute.image.Tier3Image;
import org.dasein.cloud.tier3.compute.snapshot.Tier3Snapshot;
import org.dasein.cloud.tier3.compute.vm.Tier3VM;

public class Tier3ComputeServices extends AbstractComputeServices {
    private Tier3 provider;

    public Tier3ComputeServices(@Nonnull Tier3 provider) {
        this.provider = provider;
    }

    @Override
    public boolean hasVirtualMachineSupport() {
        return true;
    }

    @Override
    public VirtualMachineSupport getVirtualMachineSupport() {
        return new Tier3VM(provider);
    }

    @Override
    public boolean hasImageSupport() {
        return true;
    }

    @Override
    public MachineImageSupport getImageSupport() {
        return new Tier3Image(provider);
    }

    @Override
    public boolean hasVolumeSupport() {
        return false;
    }

    // @Override
    // public VolumeSupport getVolumeSupport() {
    // return new Tier3Storage(provider);
    // }

    @Override
    public boolean hasAutoScalingSupport() {
        return false;
    }

    @Override
    public boolean hasSnapshotSupport() {
        return false;
    }

    // @Override
    // public SnapshotSupport getSnapshotSupport() {
    // return new Tier3Snapshot(provider);
    // }
}
