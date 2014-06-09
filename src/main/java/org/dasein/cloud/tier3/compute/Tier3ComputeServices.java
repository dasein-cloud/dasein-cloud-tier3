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

//	@Override
//	public VolumeSupport getVolumeSupport() {
//		return new Tier3Storage(provider);
//	}

	@Override
	public boolean hasAutoScalingSupport() {
		return false;
	}

	@Override
	public boolean hasSnapshotSupport() {
		return true;
	}

	@Override
	public SnapshotSupport getSnapshotSupport() {
		return new Tier3Snapshot(provider);
	}
}
