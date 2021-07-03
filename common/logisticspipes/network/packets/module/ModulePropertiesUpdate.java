package logisticspipes.network.packets.module;

import java.util.Objects;
import javax.annotation.Nonnull;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.nbt.CompoundNBT;

import logisticspipes.logisticspipes.ItemModuleInformationManager;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.ModuleCoordinatesPacket;
import logisticspipes.proxy.MainProxy;
import network.rs485.logisticspipes.property.PropertyHolder;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class ModulePropertiesUpdate extends ModuleCoordinatesPacket {

	@Nonnull
	public CompoundNBT tag = new CompoundNBT();

	public ModulePropertiesUpdate(int id) {
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
		tag = Objects.requireNonNull(input.readCompoundNBT(), "read null NBT in ModulePropertiesUpdate");
	}

	@Override
	public ModernPacket template() {
		return new ModulePropertiesUpdate(getId());
	}

	@Override
	public void processPacket(PlayerEntity player) {
		final LogisticsModule module = this.getLogisticsModule(player, LogisticsModule.class);
		if (module == null) {
			return;
		}

		// sync updated properties
		module.readFromNBT(tag);

		if (!getType().isInWorld() && player.openContainer instanceof PlayerContainer) {
			// FIXME: saveInformation & markDirty on module property change? should be called only once
			// sync slot in player inventory and mark player inventory dirty
			ItemModuleInformationManager.saveInformation(player.inventory.mainInventory.get(getPositionInt()), module);
			player.inventory.markDirty();
		}

		MainProxy.runOnServer(player.world, () -> () -> {
			// resync client; always
			MainProxy.sendPacketToPlayer(fromPropertyHolder(module).setModulePos(module), player);
		});
	}

	@Nonnull
	public static ModuleCoordinatesPacket fromPropertyHolder(PropertyHolder holder) {
		final ModulePropertiesUpdate packet = PacketHandler.getPacket(ModulePropertiesUpdate.class);
		holder.writeToNBT(packet.tag);
		return packet;
	}

}
