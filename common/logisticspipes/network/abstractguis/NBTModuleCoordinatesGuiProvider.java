package logisticspipes.network.abstractguis;

import net.minecraft.nbt.CompoundNBT;

import lombok.Getter;
import lombok.Setter;

import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

public abstract class NBTModuleCoordinatesGuiProvider extends ModuleCoordinatesGuiProvider {

	@Getter
	@Setter
	private CompoundNBT nbt;

	public NBTModuleCoordinatesGuiProvider(int id) {
		super(id);
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeCompoundNBT(nbt);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		nbt = input.readCompoundNBT();
	}
}
