package logisticspipes.interfaces.routing;

import logisticspipes.utils.item.ItemIdentifier;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public interface IFilter {

	boolean isBlocked();

	boolean isFilteredItem(ItemIdentifier item);

	boolean blockProvider();

	boolean blockCrafting();

	boolean blockRouting();

	boolean blockPower();

	DoubleCoordinates getLPPosition();
}
