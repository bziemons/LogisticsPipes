package logisticspipes.network.packets.block;

import logisticspipes.LPConstants;
import logisticspipes.blocks.powertile.LogisticsPowerJunctionTileEntity;
import logisticspipes.network.abstractpackets.CoordinatesPacket;


import net.minecraft.entity.player.EntityPlayer;

import logisticspipes.utils.StaticResolve;

@StaticResolve
public class PowerJunctionCheatPacket extends CoordinatesPacket {

	public PowerJunctionCheatPacket(int id) {
		super(id);
	}

	@Override
	public AbstractPacket template() {
		return new PowerJunctionCheatPacket(getId());
	}

	@Override
	public void processPacket(EntityPlayer player) {
		if (!LPConstants.DEBUG) {
			return;
		}
		final LogisticsPowerJunctionTileEntity tile = this.getTile(player.world, LogisticsPowerJunctionTileEntity.class);
		if (tile != null) {
			tile.addEnergy(100000);
		}
	}
}
