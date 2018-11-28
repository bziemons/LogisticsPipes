package logisticspipes.logisticspipes;

import logisticspipes.interfaces.IInventoryUtil;
import logisticspipes.interfaces.ISlotUpgradeManager;
import logisticspipes.modules.abstractmodules.LogisticsModule.ModulePositionType;
import logisticspipes.utils.item.ItemIdentifier;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

public interface IInventoryProvider {

	public IInventoryUtil getPointedInventory();

	public IInventoryUtil getPointedInventory(ExtractionMode mode, boolean forExtraction);

	public IInventoryUtil getSneakyInventory(boolean forExtraction, ModulePositionType slot, int positionInt);

	public IInventoryUtil getSneakyInventory(EnumFacing _sneakyOrientation);

	public IInventoryUtil getUnsidedInventory();

	public TileEntity getRealInventory();

	public EnumFacing inventoryOrientation();

	public void queueRoutedItem(IRoutedItem routedItem, EnumFacing from);

	public ISlotUpgradeManager getUpgradeManager(ModulePositionType slot, int positionInt);

	public int countOnRoute(ItemIdentifier item);
}
