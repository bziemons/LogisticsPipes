package logisticspipes.network.guis;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Container;

import logisticspipes.gui.GuiLogisticsSettings;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.DummyContainer;

@StaticResolve
public class LogisticsPlayerSettingsGuiProvider extends GuiProvider {

	public LogisticsPlayerSettingsGuiProvider(int id) {
		super(id);
	}

	@Override
	public Object getClientGui(PlayerEntity player) {
		return new GuiLogisticsSettings(player);
	}

	@Override
	public Container getContainer(PlayerEntity player) {
		DummyContainer dummy = new DummyContainer(player.inventory, null);
		dummy.addNormalSlotsForPlayerInventory(0, 0); // server does not care where the slots are
		return dummy;
	}

	@Override
	public GuiProvider template() {
		return new LogisticsPlayerSettingsGuiProvider(getId());
	}
}
