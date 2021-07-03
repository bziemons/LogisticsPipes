package logisticspipes.network.packets.modules;

import net.minecraft.entity.player.PlayerEntity;

import logisticspipes.modules.ModuleItemSink;
import logisticspipes.network.abstractpackets.BooleanModuleCoordinatesPacket;

public class ItemSinkDefault extends BooleanModuleCoordinatesPacket {

	@Override
	public void processPacket(PlayerEntity player) {
		ModuleItemSink module = this.getLogisticsModule(player, ModuleItemSink.class);
		if (module == null) {
			return;
		}
		module.setDefaultRoute(isFlag());
	}
}
