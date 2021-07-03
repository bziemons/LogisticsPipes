package logisticspipes.network.packets.gui;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tileentity.TileEntity;

import network.rs485.logisticspipes.network.packets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class GuiClosePacket extends CoordinatesPacket {

	public GuiClosePacket(int id) {
		super(id);
	}

	@Override
	public void processPacket(PlayerEntity player) {
		// always mark the GUI origin's chunk dirty - something may have changed in the GUI
		getTileAs(player.world, TileEntity.class).markDirty();
	}

	@Override
	public ModernPacket template() {
		return new GuiClosePacket(getId());
	}
}
