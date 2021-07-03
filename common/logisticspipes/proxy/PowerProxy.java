package logisticspipes.proxy;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;

import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;

import logisticspipes.proxy.cofh.subproxies.ICoFHEnergyReceiver;
import logisticspipes.proxy.cofh.subproxies.ICoFHEnergyStorage;
import logisticspipes.proxy.interfaces.IPowerProxy;

public class PowerProxy implements IPowerProxy {

	private static class MEnergyStorage extends EnergyStorage {

		public MEnergyStorage(int capacity) {
			super(capacity);
		}

		public void readFromNBT(CompoundNBT tag) {
			this.energy = tag.getInt("Energy");

			if (energy > capacity) {
				energy = capacity;
			}
		}

		public CompoundNBT writeToNBT(CompoundNBT tag) {
			if (energy < 0) {
				energy = 0;
			}
			tag.putInt("Energy", energy);
			return tag;
		}
	}

	@Override
	public boolean isEnergyReceiver(TileEntity tile, Direction face) {
		return tile.getCapability(CapabilityEnergy.ENERGY, face)
				.map(IEnergyStorage::canReceive)
				.orElse(tile instanceof IEnergyStorage);
	}

	@Override
	public boolean isAvailable() {
		return true;
	}
}
