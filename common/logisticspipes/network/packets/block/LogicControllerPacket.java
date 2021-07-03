package logisticspipes.network.packets.block;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tileentity.TileEntity;

import logisticspipes.logic.interfaces.ILogicControllerTile;
import logisticspipes.network.NewGuiHandler;
import network.rs485.logisticspipes.network.packets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.guis.logic.LogicControllerGuiProvider;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class LogicControllerPacket extends CoordinatesPacket {

	public LogicControllerPacket(int id) {
		super(id);
	}

	@Override
	public void processPacket(PlayerEntity player) {
		ILogicControllerTile tile = this.getTileAs(player.getEntityWorld(), ILogicControllerTile.class);
		if (tile == null) {
			return;
		}
		NewGuiHandler.getGui(LogicControllerGuiProvider.class).setTilePos((TileEntity) tile).open(player);
	}

	@Override
	public ModernPacket template() {
		return new LogicControllerPacket(getId());
	}
}
