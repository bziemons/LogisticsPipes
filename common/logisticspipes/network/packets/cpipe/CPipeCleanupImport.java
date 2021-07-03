package logisticspipes.network.packets.cpipe;

import net.minecraft.entity.player.PlayerEntity;

import logisticspipes.modules.ModuleCrafter;
import logisticspipes.network.abstractpackets.ModernPacket;
import network.rs485.logisticspipes.network.packets.ModuleCoordinatesPacket;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class CPipeCleanupImport extends ModuleCoordinatesPacket {

	public CPipeCleanupImport(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new CPipeCleanupImport(getId());
	}

	@Override
	public void processPacket(PlayerEntity player) {
		final ModuleCrafter module = this.getLogisticsModule(player, ModuleCrafter.class);
		if (module == null) {
			return;
		}
		module.importCleanup();
	}
}
