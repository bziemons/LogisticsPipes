package logisticspipes.network.packets.block;

import net.minecraft.entity.player.PlayerEntity;

import logisticspipes.blocks.LogisticsSecurityTileEntity;
import network.rs485.logisticspipes.network.packets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class SecurityRequestCCIdsPacket extends CoordinatesPacket {

	public SecurityRequestCCIdsPacket(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new SecurityRequestCCIdsPacket(getId());
	}

	@Override
	public void processPacket(PlayerEntity player) {
		LogisticsSecurityTileEntity tile = this.getTileAs(player.world, LogisticsSecurityTileEntity.class);
		if (tile != null) {
			tile.requestList(player);
		}
	}
}
