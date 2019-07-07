package logisticspipes.network.packets.block;

import logisticspipes.gui.GuiStatistics;
import logisticspipes.network.abstractpackets.InventoryModuleCoordinatesPacket;


import net.minecraft.entity.player.EntityPlayer;

import net.minecraftforge.fml.client.FMLClientHandler;

import logisticspipes.utils.StaticResolve;

@StaticResolve
public class AmountTaskSubGui extends InventoryModuleCoordinatesPacket {

	public AmountTaskSubGui(int id) {
		super(id);
	}

	@Override
	public void processPacket(EntityPlayer player) {
		if (FMLClientHandler.instance().getClient().currentScreen instanceof GuiStatistics) {
			((GuiStatistics) FMLClientHandler.instance().getClient().currentScreen).handlePacket1(getIdentList());
		}
	}

	@Override
	public AbstractPacket template() {
		return new AmountTaskSubGui(getId());
	}
}
