package logisticspipes.network.packets.module;

import network.rs485.logisticspipes.network.LPChannel;

import logisticspipes.modules.ModuleProvider;

import logisticspipes.network.abstractpackets.ModuleCoordinatesPacket;
import logisticspipes.network.packets.modules.ProviderModuleInclude;

import net.minecraft.entity.player.EntityPlayer;

import logisticspipes.utils.StaticResolve;

@StaticResolve
public class ProviderModuleIncludePacket extends ModuleCoordinatesPacket {

	public ProviderModuleIncludePacket(int id) {
		super(id);
	}

	@Override
	public AbstractPacket template() {
		return new ProviderModuleIncludePacket(getId());
	}

	@Override
	public void processPacket(EntityPlayer player) {
		final ModuleProvider module = this.getLogisticsModule(player, ModuleProvider.class);
		if (module == null) {
			return;
		}
		module.setFilterExcluded(!module.isExcludeFilter());
		LPChannel.sendPacketToPlayer(PacketHandler.getPacket(ProviderModuleInclude.class).setFlag(module.isExcludeFilter()).setModulePos(module), player);
	}
}
