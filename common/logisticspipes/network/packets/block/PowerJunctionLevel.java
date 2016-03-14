package logisticspipes.network.packets.block;

import logisticspipes.blocks.powertile.LogisticsPowerJunctionTileEntity;
import logisticspipes.network.abstractpackets.IntegerCoordinatesPacket;


import net.minecraft.entity.player.EntityPlayer;

import logisticspipes.utils.StaticResolve;

@StaticResolve
public class PowerJunctionLevel extends IntegerCoordinatesPacket {

	public PowerJunctionLevel(int id) {
		super(id);
	}

	@Override
	public AbstractPacket template() {
		return new PowerJunctionLevel(getId());
	}

	@Override
	public void processPacket(EntityPlayer player) {
		LogisticsPowerJunctionTileEntity tile = this.getTile(player.world, LogisticsPowerJunctionTileEntity.class);
		if (tile != null) {
			tile.handlePowerPacket(getInteger());
		}
	}
}
