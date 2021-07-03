package logisticspipes.network.packets.block;

import net.minecraft.entity.player.PlayerEntity;

import net.minecraftforge.fml.client.FMLClientHandler;

import logisticspipes.gui.GuiStatistics;
import logisticspipes.network.abstractpackets.InventoryModuleCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class AmountTaskSubGui extends InventoryModuleCoordinatesPacket {

	public AmountTaskSubGui(int id) {
		super(id);
	}

	@Override
	public void processPacket(PlayerEntity player) {
		if (FMLClientHandler.instance().getClient().currentScreen instanceof GuiStatistics) {
			((GuiStatistics) FMLClientHandler.instance().getClient().currentScreen).handlePacket1(getIdentList());
		}
	}

	@Override
	public ModernPacket template() {
		return new AmountTaskSubGui(getId());
	}
}
