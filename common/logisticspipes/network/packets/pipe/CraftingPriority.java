package logisticspipes.network.packets.pipe;

import net.minecraft.entity.player.PlayerEntity;

import logisticspipes.modules.ModuleCrafter;
import logisticspipes.network.abstractpackets.IntegerModuleCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class CraftingPriority extends IntegerModuleCoordinatesPacket {

	public CraftingPriority(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new CraftingPriority(getId());
	}

	@Override
	public void processPacket(PlayerEntity player) {
		ModuleCrafter module = this.getLogisticsModule(player, ModuleCrafter.class);
		if (module == null) {
			return;
		}
		module.priority.setValue(getInteger());
	}
}
