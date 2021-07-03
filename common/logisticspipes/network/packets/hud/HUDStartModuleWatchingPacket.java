package logisticspipes.network.packets.hud;

import net.minecraft.entity.player.PlayerEntity;

import logisticspipes.interfaces.IModuleWatchReciver;
import logisticspipes.network.abstractpackets.ModernPacket;
import network.rs485.logisticspipes.network.packets.ModuleCoordinatesPacket;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class HUDStartModuleWatchingPacket extends ModuleCoordinatesPacket {

	public HUDStartModuleWatchingPacket(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new HUDStartModuleWatchingPacket(getId());
	}

	@Override
	public void processPacket(PlayerEntity player) {
		IModuleWatchReciver handler = this.getLogisticsModule(player, IModuleWatchReciver.class);
		if (handler == null) {
			return;
		}
		handler.startWatching(player);
	}
}
