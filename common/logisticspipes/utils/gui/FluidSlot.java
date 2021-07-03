package logisticspipes.utils.gui;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Slot;

public class FluidSlot extends Slot {

	public FluidSlot(IInventory par1iInventory, int par2, int par3, int par4) {
		super(par1iInventory, par2, par3, par4);
	}

	public FluidSlot(Slot slot) {
		super(slot.inventory, slot.getSlotIndex(), slot.xPos, slot.yPos);
	}

	@Override
	public boolean canTakeStack(PlayerEntity par1PlayerEntity) {
		return false;
	}
}
