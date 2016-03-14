package logisticspipes.network.packets.block;

import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.network.abstractpackets.IntegerCoordinatesPacket;


import net.minecraft.entity.player.EntityPlayer;

import logisticspipes.utils.StaticResolve;

@StaticResolve
public class SecurityAuthorizationPacket extends IntegerCoordinatesPacket {

	public SecurityAuthorizationPacket(int id) {
		super(id);
	}

	@Override
	public AbstractPacket template() {
		return new SecurityAuthorizationPacket(getId());
	}

	@Override
	public void processPacket(EntityPlayer player) {
		LogisticsSecurityTileEntity tile = this.getTile(player.world, LogisticsSecurityTileEntity.class);
		if (tile != null) {
			if (getInteger() == 1) {
				tile.authorizeStation();
			} else {
				tile.deauthorizeStation();
			}
		}
	}
}
