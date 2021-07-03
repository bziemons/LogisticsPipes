package logisticspipes.network.packets.module;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.interfaces.IStringBasedModule;
import logisticspipes.network.abstractpackets.ModernPacket;
import network.rs485.logisticspipes.network.packets.ModuleCoordinatesPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class ModuleBasedItemSinkList extends ModuleCoordinatesPacket {

	@Getter
	@Setter
	private CompoundNBT nbt;

	public ModuleBasedItemSinkList(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new ModuleBasedItemSinkList(getId());
	}

	@Override
	public void processPacket(PlayerEntity player) {
		IStringBasedModule module = this.getLogisticsModule(player, IStringBasedModule.class);
		if (module == null) {
			return;
		}
		module.readFromNBT(nbt);
		if (MainProxy.isServer(player.getEntityWorld()) && getType().isInWorld()) {
			module.listChanged();
		}
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
