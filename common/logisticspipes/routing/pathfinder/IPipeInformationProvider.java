package logisticspipes.routing.pathfinder;

import java.util.List;
import java.util.stream.Stream;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.world.World;

import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.transport.LPTravelingItem;
import logisticspipes.utils.item.ItemIdentifier;
import network.rs485.logisticspipes.connection.ConnectionType;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public interface IPipeInformationProvider {

	boolean isCorrect(ConnectionType type);

	int getX();

	int getY();

	int getZ();

	World getWorld();

	boolean isRouterInitialized();

	boolean isRoutingPipe();

	CoreRoutedPipe getRoutingPipe();

	TileEntity getNextConnectedTile(Direction direction);

	boolean isFirewallPipe();

	IFilter getFirewallFilter();

	TileEntity getTile();

	boolean divideNetwork();

	boolean powerOnly();

	boolean isOnewayPipe();

	boolean isOutputClosed(Direction direction);

	boolean canConnect(TileEntity to, Direction direction, boolean flag);

	double getDistance();

	double getDistanceWeight();

	boolean isItemPipe();

	boolean isFluidPipe();

	boolean isPowerPipe();

	double getDistanceTo(int destinationint, Direction ignore, ItemIdentifier ident, boolean isActive, double travled, double max, List<DoubleCoordinates> visited);

	boolean acceptItem(LPTravelingItem item, TileEntity from);

	void refreshTileCacheOnSide(Direction side);

	boolean isMultiBlock();

	Stream<TileEntity> getPartsOfPipe();
}
