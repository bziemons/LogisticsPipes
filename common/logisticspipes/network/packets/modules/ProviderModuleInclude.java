package logisticspipes.network.packets.modules;

import logisticspipes.modules.ModuleProvider;
import logisticspipes.network.abstractpackets.BooleanModuleCoordinatesPacket;


import net.minecraft.entity.player.EntityPlayer;

import logisticspipes.utils.StaticResolve;

@StaticResolve
public class ProviderModuleInclude extends BooleanModuleCoordinatesPacket {

	public ProviderModuleInclude(int id) {
		super(id);
	}

	@Override
	public AbstractPacket template() {
		return new ProviderModuleInclude(getId());
	}

	@Override
	public void processPacket(EntityPlayer player) {
		final ModuleProvider module = this.getLogisticsModule(player, ModuleProvider.class);
		if (module == null) {
			return;
		}
		module.setFilterExcluded(isFlag());
	}
}
