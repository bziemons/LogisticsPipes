package logisticspipes.network.packets.block;

import net.minecraft.entity.player.PlayerEntity;

import logisticspipes.LogisticsPipes;
import logisticspipes.blocks.LogisticsPowerJunctionTileEntity;
import network.rs485.logisticspipes.network.packets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class PowerJunctionCheatPacket extends CoordinatesPacket {

	public PowerJunctionCheatPacket(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new PowerJunctionCheatPacket(getId());
	}

	@Override
	public void processPacket(PlayerEntity player) {
		if (!LogisticsPipes.isDEBUG()) {
			return;
		}
		final LogisticsPowerJunctionTileEntity tile = this.getTileAs(player.world, LogisticsPowerJunctionTileEntity.class);
		if (tile != null) {
			tile.addEnergy(100000);
		}
	}
}
