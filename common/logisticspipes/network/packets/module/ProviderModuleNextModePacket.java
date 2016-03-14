package logisticspipes.network.packets.module;

import network.rs485.logisticspipes.network.LPChannel;

import logisticspipes.modules.ModuleProvider;

import logisticspipes.network.abstractpackets.ModuleCoordinatesPacket;
import logisticspipes.network.packets.modules.ProviderModuleMode;

import net.minecraft.entity.player.EntityPlayer;

import logisticspipes.utils.StaticResolve;

@StaticResolve
public class ProviderModuleNextModePacket extends ModuleCoordinatesPacket {

	public ProviderModuleNextModePacket(int id) {
		super(id);
	}

	@Override
	public AbstractPacket template() {
		return new ProviderModuleNextModePacket(getId());
	}

	@Override
	public void processPacket(EntityPlayer player) {
		final ModuleProvider module = this.getLogisticsModule(player, ModuleProvider.class);
		if (module == null) {
			return;
		}
		module.nextExtractionMode();
		LPChannel.sendPacketToPlayer(PacketHandler.getPacket(ProviderModuleMode.class).setMode(module.getExtractionMode().ordinal()).setModulePos(module),
				player);
	}
}
