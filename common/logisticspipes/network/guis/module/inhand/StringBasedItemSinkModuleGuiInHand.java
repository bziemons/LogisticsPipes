package logisticspipes.network.guis.module.inhand;

import net.minecraft.entity.player.EntityPlayer;

import logisticspipes.gui.modules.GuiStringBasedItemSink;
import logisticspipes.interfaces.IStringBasedModule;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.network.abstractguis.ModuleInHandGuiProvider;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.DummyModuleContainer;
import logisticspipes.utils.item.ItemIdentifierInventory;
import network.rs485.logisticspipes.api.LogisticsModule;

@StaticResolve
public class StringBasedItemSinkModuleGuiInHand extends ModuleInHandGuiProvider {

	public StringBasedItemSinkModuleGuiInHand(int id) {
		super(id);
	}

	@Override
	public Object getClientGui(EntityPlayer player) {
		LogisticsModule module = getLogisticsModule(player);
		if (!(module instanceof IStringBasedModule)) {
			return null;
		}
		return new GuiStringBasedItemSink(player.inventory, module);
	}

	@Override
	public DummyContainer getContainer(EntityPlayer player) {
		DummyModuleContainer dummy = new DummyModuleContainer(player, getInvSlot());
		if (!(dummy.getModule() instanceof IStringBasedModule)) {
			return null;
		}
		dummy.setInventory(new ItemIdentifierInventory(1, "TMP", 1));
		dummy.addDummySlot(0, 0, 0);
		dummy.addNormalSlotsForPlayerInventory(0, 0);
		return dummy;
	}

	@Override
	public GuiProvider template() {
		return new StringBasedItemSinkModuleGuiInHand(getId());
	}
}
