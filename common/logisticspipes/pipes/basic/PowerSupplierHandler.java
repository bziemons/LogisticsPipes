package logisticspipes.pipes.basic;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.FloatNBT;

public class PowerSupplierHandler {

	private double internalBufferRF = 0F;

	public PowerSupplierHandler() {}

	public void writeToNBT(CompoundNBT tag) {
		if (internalBufferRF > 0) {
			tag.putDouble("bufferRF", internalBufferRF);
		}
	}

	public void readFromNBT(CompoundNBT tag) {
		if (tag.get("bufferRF") instanceof FloatNBT) { // support for old float
			internalBufferRF = tag.getFloat("bufferRF");
		} else {
			internalBufferRF = tag.getDouble("bufferRF");
		}
	}

}
