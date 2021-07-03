package logisticspipes.utils;

import javax.annotation.Nullable;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;

import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.utils.transactor.ITransactor;
import logisticspipes.utils.transactor.TransactorSimple;
import network.rs485.logisticspipes.inventory.ProviderMode;

public class InventoryHelper {

	/**
	 * BC getTransactorFor using our getInventory
	 */
	@Nullable
	public static ITransactor getTransactorFor(Object object, Direction dir) {
		if (object instanceof TileEntity) {
			ITransactor t = SimpleServiceLocator.inventoryUtilFactory.getSpecialHandlerFor((TileEntity) object, dir, ProviderMode.DEFAULT);
			if (t != null) {
				return t;
			}
		}

		return ((ICapabilityProvider) object).getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, dir)
				.map(TransactorSimple::new)
				.orElse(null);
	}

}
