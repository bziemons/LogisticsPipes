package logisticspipes.proxy;

import static logisticspipes.LPConstants.factorizationModID;

import logisticspipes.proxy.specialinventoryhandler.AEInterfaceInventoryHandler;
import logisticspipes.proxy.specialinventoryhandler.BarrelInventoryHandler;
import logisticspipes.proxy.specialinventoryhandler.CrateInventoryHandler;
import network.rs485.logisticspipes.proxy.StorageDrawersProxy;

public class SpecialInventoryHandlerManager {

	public static void load() {
		if (ModList.get().isLoaded(factorizationModID)) {
			SimpleServiceLocator.inventoryUtilFactory.registerHandler(new BarrelInventoryHandler());
		}

		if (ModList.get().isLoaded(betterStorageModID)) {
			SimpleServiceLocator.inventoryUtilFactory.registerHandler(new CrateInventoryHandler());
		}

		if (ModList.get().isLoaded(appliedenergisticsModID)) {
			SimpleServiceLocator.inventoryUtilFactory.registerHandler(new AEInterfaceInventoryHandler());
		}

		SimpleServiceLocator.buildCraftProxy.registerInventoryHandler();

		StorageDrawersProxy.INSTANCE.registerInventoryHandler();
	}

}
