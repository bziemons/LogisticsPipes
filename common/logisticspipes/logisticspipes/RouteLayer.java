/**
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.logisticspipes;

import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

import net.minecraft.util.EnumFacing;

import logisticspipes.logisticspipes.IRoutedItem.TransportMode;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.IRouter;

/**
 * @author Krapht This class is responsible for resolving where incoming items
 * should go.
 */
public class RouteLayer {

	protected final @Nonnull IRouter _router;
	private final TransportLayer _transport;
	private final CoreRoutedPipe _pipe;

	public RouteLayer(@Nonnull IRouter router, TransportLayer transportLayer, CoreRoutedPipe pipe) {
		_router = router;
		_transport = transportLayer;
		_pipe = pipe;
	}

	public CompletableFuture<EnumFacing> getOrientationForItem(IRoutedItem routedItem, EnumFacing blocked) {
		routedItem.checkIDFromUUID();
		//If a item has no destination, find one
		if (routedItem.getDestination() < 0) {
			routedItem = SimpleServiceLocator.logisticsManager.assignDestinationFor(routedItem, _router.getSimpleID(), false);
			_pipe.debug.log("No Destination, assigned new destination: (" + routedItem.getInfo().toString() + ")");
		}

		final CompletableFuture<EnumFacing> returnedFuture = new CompletableFuture<>();

		final CompletableFuture<Boolean> hasRouteFuture = _router
				.hasRoute(routedItem.getDestination(), routedItem.getTransportMode() == TransportMode.Active, routedItem.getItemIdentifierStack().getItem());
		final IRoutedItem finalItem = routedItem;

		hasRouteFuture.thenAccept(hasRoute -> {
			IRoutedItem item = finalItem;
			//If the destination is unknown / unroutable or it already arrived at its destination and somehow looped back
			if (item.getDestination() >= 0 && (!hasRoute || item.getArrived())) {
				item = SimpleServiceLocator.logisticsManager.assignDestinationFor(item, _router.getSimpleID(), false);
				_pipe.debug.log("Unreachable Destination, assigned new destination: (" + item.getInfo().toString() + ")");
			}

			item.checkIDFromUUID();
			//If we still have no destination or client side unroutable, drop it
			if (item.getDestination() < 0) {
				returnedFuture.complete(null);
				return;
			}

			//Is the destination ourself? Deliver it
			if (item.getDestinationUUID().equals(_router.getId())) {

				_transport.handleItem(item);

				if (item.getDistanceTracker() != null) {
					item.getDistanceTracker().setCurrentDistanceToTarget(0);
					item.getDistanceTracker().setDestinationReached();
				}

				if (item.getTransportMode() != TransportMode.Active && !_transport.stillWantItem(item)) {
					getOrientationForItem(SimpleServiceLocator.logisticsManager.assignDestinationFor(item, _router.getSimpleID(), true), null)
							.whenComplete((forgeDirection, throwable) -> {
								if (throwable == null) {
									returnedFuture.complete(forgeDirection);
								} else {
									returnedFuture.completeExceptionally(throwable);
								}
							});
					return;
				}

				item.setDoNotBuffer(true);
				item.setArrived(true);
				returnedFuture.complete(_transport.itemArrived(item, blocked));
				return;
			}

			//Do we now know the destination?
			if (!hasRoute) {
				returnedFuture.complete(null);
				return;
			}

			final IRoutedItem finalfinalItem = item;
			final CompletableFuture<ExitRoute> exitRouteFuture = _router
					.getExitFor(item.getDestination(), item.getTransportMode() == TransportMode.Active, item.getItemIdentifierStack().getItem());

			exitRouteFuture.thenAccept(exitRoute -> {
				//Which direction should we send it
				if (exitRoute == null) {
					returnedFuture.complete(null);
					return;
				}

				if (finalfinalItem.getDistanceTracker() != null) {
					finalfinalItem.getDistanceTracker().setCurrentDistanceToTarget(exitRoute.blockDistance);
				}

				returnedFuture.complete(exitRoute.exitOrientation);
			}).exceptionally(throwable -> {
				returnedFuture.completeExceptionally(throwable);
				return null;
			});
		}).exceptionally(throwable -> {
			returnedFuture.completeExceptionally(throwable);
			return null;
		});

		return returnedFuture;
	}
}
