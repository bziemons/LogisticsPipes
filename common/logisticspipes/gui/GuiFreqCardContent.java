package logisticspipes.gui;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;

import logisticspipes.LPItems;
import logisticspipes.items.LogisticsItemCard;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.GuiGraphics;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;

public class GuiFreqCardContent extends LogisticsBaseGuiScreen {

	public GuiFreqCardContent(PlayerEntity player, IInventory card) {
		super(inv, titleIn, 180, 130, 0, 0);
		DummyContainer dummy = new DummyContainer(player.inventory, card);
		dummy.addRestrictedSlot(0, card, 82, 15, itemStack ->
				!itemStack.isEmpty() && itemStack.getItem() == LPItems.itemCard && itemStack.getDamage() == LogisticsItemCard.FREQ_CARD);
		dummy.addNormalSlotsForPlayerInventory(10, 45);
		inventorySlots = dummy;
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float var1, int var2, int var3) {
		GuiGraphics.drawGuiBackGround(mc, guiLeft, guiTop, right, bottom, blitOffset, true);
		GuiGraphics.drawPlayerInventoryBackground(mc, guiLeft + 10, guiTop + 45);
		GuiGraphics.drawSlotBackground(mc, guiLeft + 81, guiTop + 14);
	}

}
