package logisticspipes.gui;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Direction;

import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.signs.ItemAmountPipeSign;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.GuiGraphics;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;

public class ItemAmountSignCreationGui extends LogisticsBaseGuiScreen {

	public ItemAmountSignCreationGui(PlayerEntity player, CoreRoutedPipe pipe, Direction dir) {
		super(inv, titleIn, 180, 125, 0, 0);
		ItemAmountPipeSign sign = ((ItemAmountPipeSign) pipe.getPipeSign(dir));
		DummyContainer dummy = new DummyContainer(player.inventory, sign.itemTypeInv);
		dummy.addDummySlot(0, 10, 13);
		dummy.addNormalSlotsForPlayerInventory(10, 40);
		inventorySlots = dummy;
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float f, int i, int j) {
		GuiGraphics.drawGuiBackGround(mc, guiLeft, guiTop, right, bottom, blitOffset, true);
		GuiGraphics.drawPlayerInventoryBackground(mc, guiLeft + 10, guiTop + 40);
		GuiGraphics.drawSlotBackground(mc, guiLeft + 9, guiTop + 12);
	}
}
