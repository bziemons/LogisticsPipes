package logisticspipes.network.packets.block;

import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.network.abstractpackets.IntegerCoordinatesPacket;


import net.minecraft.entity.player.EntityPlayer;

import logisticspipes.utils.StaticResolve;

@StaticResolve
public class SecurityAddCCIdPacket extends IntegerCoordinatesPacket {

	public SecurityAddCCIdPacket(int id) {
		super(id);
	}

	@Override
	public AbstractPacket template() {
		return new SecurityAddCCIdPacket(getId());
	}

	@Override
	public void processPacket(EntityPlayer player) {
		LogisticsSecurityTileEntity tile = this.getTile(player.world, LogisticsSecurityTileEntity.class);
		if (tile != null) {
			tile.addCCToList(getInteger());
			tile.requestList(player);
		}
	}
}
