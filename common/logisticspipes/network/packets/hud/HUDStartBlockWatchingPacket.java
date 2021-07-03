package logisticspipes.network.packets.hud;

import net.minecraft.entity.player.PlayerEntity;

import logisticspipes.interfaces.IBlockWatchingHandler;
import network.rs485.logisticspipes.network.packets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class HUDStartBlockWatchingPacket extends CoordinatesPacket {

	public HUDStartBlockWatchingPacket(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new HUDStartBlockWatchingPacket(getId());
	}

	@Override
	public void processPacket(PlayerEntity player) {
		IBlockWatchingHandler tile = this.getTileAs(player.world, IBlockWatchingHandler.class);
		if (tile != null) {
			tile.playerStartWatching(player);
		}
	}
}
