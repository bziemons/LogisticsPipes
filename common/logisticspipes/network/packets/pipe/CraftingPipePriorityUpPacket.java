package logisticspipes.network.packets.pipe;

import logisticspipes.modules.ModuleCrafter;

import logisticspipes.network.abstractpackets.ModuleCoordinatesPacket;

import net.minecraft.entity.player.EntityPlayer;

import logisticspipes.utils.StaticResolve;

@StaticResolve
public class CraftingPipePriorityUpPacket extends ModuleCoordinatesPacket {

	public CraftingPipePriorityUpPacket(int id) {
		super(id);
	}

	@Override
	public AbstractPacket template() {
		return new CraftingPipePriorityUpPacket(getId());
	}

	@Override
	public void processPacket(EntityPlayer player) {
		ModuleCrafter module = this.getLogisticsModule(player, ModuleCrafter.class);
		if (module == null) {
			return;
		}
		module.priorityUp(player);
	}
}
