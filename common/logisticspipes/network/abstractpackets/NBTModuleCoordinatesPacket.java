package logisticspipes.network.abstractpackets;

import net.minecraft.nbt.CompoundNBT;

import lombok.Getter;
import lombok.Setter;

import network.rs485.logisticspipes.network.packets.ModuleCoordinatesPacket;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

public abstract class NBTModuleCoordinatesPacket extends ModuleCoordinatesPacket {

	@Getter
	@Setter
	private CompoundNBT tag;

	public NBTModuleCoordinatesPacket(int id) {
		super(id);
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeCompoundNBT(tag);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		tag = input.readCompoundNBT();
	}
}
