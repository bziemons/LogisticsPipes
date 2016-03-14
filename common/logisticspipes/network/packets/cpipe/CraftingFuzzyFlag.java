package logisticspipes.network.packets.cpipe;

import logisticspipes.modules.ModuleCrafter;
import logisticspipes.network.abstractpackets.Integer2ModuleCoordinatesPacket;


import net.minecraft.entity.player.EntityPlayer;

public class CraftingFuzzyFlag extends Integer2ModuleCoordinatesPacket {

	public CraftingFuzzyFlag(int id) {
		super(id);
	}

	@Override
	public void processPacket(EntityPlayer player) {
		ModuleCrafter module = this.getLogisticsModule(player, ModuleCrafter.class);
		if (module == null) {
			return;
		}
		module.setFuzzyCraftingFlag(getInteger(), getInteger2(), player);
	}

	@Override
	public AbstractPacket template() {
		return new CraftingFuzzyFlag(getId());
	}

}
