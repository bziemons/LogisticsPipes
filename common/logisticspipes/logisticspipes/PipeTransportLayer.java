package logisticspipes.logisticspipes;

import java.util.LinkedList;
import javax.annotation.Nonnull;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;

import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.routing.IRouter;
import network.rs485.logisticspipes.connection.NeighborTileEntity;
import network.rs485.logisticspipes.world.WorldCoordinatesWrapper;

/**
 * This class is responsible for handling incoming items for standard pipes
 *
 * @author Krapht
 */
public class PipeTransportLayer extends TransportLayer {

	private final CoreRoutedPipe routedPipe;
	private final ITrackStatistics _trackStatistics;
	private final @Nonnull IRouter _router;

	public PipeTransportLayer(CoreRoutedPipe routedPipe, ITrackStatistics trackStatistics, @Nonnull IRouter router) {
		this.routedPipe = routedPipe;
		_trackStatistics = trackStatistics;
		_router = router;
	}

	@Override
	public Direction itemArrived(IRoutedItem item, Direction denied) {
		if (item.getItemIdentifierStack() != null) {
			_trackStatistics.recievedItem(item.getItemIdentifierStack().getStackSize());
		}

		// 1st prio, deliver to adjacent inventories
		LinkedList<Direction> possibleDirection = new LinkedList<>();
		for (NeighborTileEntity<TileEntity> adjacent : routedPipe.getAvailableAdjacent().inventories()) {
			if (_router.isRoutedExit(adjacent.getDirection())) {
				continue;
			}
			if (denied != null && denied.equals(adjacent.getDirection())) {
				continue;
			}

			CoreRoutedPipe pipe = _router.getPipe();
			if (pipe != null) {
				if (pipe.isLockedExit(adjacent.getDirection())) {
					continue;
				}
			}

			possibleDirection.add(adjacent.getDirection());
		}
		if (possibleDirection.size() != 0) {
			return possibleDirection.get(routedPipe.getWorld().rand.nextInt(possibleDirection.size()));
		}

		// 2nd prio, deliver to non-routed exit
		new WorldCoordinatesWrapper(routedPipe.container).connectedTileEntities().stream()
				.filter(neighbor -> {
					if (_router.isRoutedExit(neighbor.getDirection())) return false;
					final CoreRoutedPipe routerPipe = _router.getPipe();
					return routerPipe == null || !routerPipe.isLockedExit(neighbor.getDirection());
				})
				.forEach(neighbor -> possibleDirection.add(neighbor.getDirection()));

		if (possibleDirection.size() == 0) {
			// last resort, drop item
			return null;
		} else {
			return possibleDirection.get(routedPipe.getWorld().rand.nextInt(possibleDirection.size()));
		}
	}

	@Override
	public boolean stillWantItem(IRoutedItem item) {
		// pipes are dumb and always want the item
		return true;
	}

}
