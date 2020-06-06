package logisticspipes.proxy.td;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

import cofh.core.util.helpers.BlockHelper;
import cofh.thermaldynamics.duct.item.DuctUnitItem;
import cofh.thermaldynamics.duct.item.GridItem;
import cofh.thermaldynamics.duct.item.TravelingItem;
import cofh.thermaldynamics.duct.tiles.DuctToken;
import cofh.thermaldynamics.duct.tiles.IDuctHolder;
import cofh.thermaldynamics.duct.tiles.TileDuctItem;
import cofh.thermaldynamics.multiblock.Route;
import cofh.thermaldynamics.multiblock.RouteCache;

import logisticspipes.asm.td.ILPTravelingItemInfo;
import logisticspipes.asm.te.ILPTEInformation;
import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.logisticspipes.IRoutedItem;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.routing.IRouter;
import logisticspipes.routing.pathfinder.IPipeInformationProvider;
import logisticspipes.routing.pathfinder.IRouteProvider;
import logisticspipes.transport.LPTravelingItem;
import logisticspipes.utils.CacheHolder;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.tuples.Pair;
import logisticspipes.utils.tuples.Triplet;
import network.rs485.grow.GROW;
import network.rs485.logisticspipes.world.CoordinateUtils;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public class TDDuctInformationProvider implements IPipeInformationProvider, IRouteProvider {

	private final TileDuctItem duct;

	public TDDuctInformationProvider(TileDuctItem duct) {
		this.duct = duct;
	}

	@Override
	public boolean isCorrect(ConnectionPipeType type) {
		return duct != null && !duct.isInvalid() && SimpleServiceLocator.thermalDynamicsProxy.isActive();
	}

	@Override
	public int getX() {
		return duct.getPos().getX();
	}

	@Override
	public int getY() {
		return duct.getPos().getY();
	}

	@Override
	public int getZ() {
		return duct.getPos().getZ();
	}

	@Override
	public World getWorld() {
		return duct.getWorld();
	}

	@Override
	public boolean isRouterInitialized() {
		return !duct.isInvalid();
	}

	@Override
	public boolean isRoutingPipe() {
		return false;
	}

	@Override
	public CoreRoutedPipe getRoutingPipe() {
		throw new UnsupportedOperationException();
	}

	@Override
	public TileEntity getNextConnectedTile(EnumFacing direction) {
		return BlockHelper.getAdjacentTileEntity(duct, direction);
	}

	@Override
	public boolean isFirewallPipe() {
		return false;
	}

	@Override
	public IFilter getFirewallFilter() {
		throw new UnsupportedOperationException();
	}

	@Override
	public TileEntity getTile() {
		return duct;
	}

	@Override
	public boolean divideNetwork() {
		return false;
	}

	@Override
	public boolean powerOnly() {
		return false;
	}

	@Override
	public boolean isOnewayPipe() {
		return false;
	}

	@Override
	public boolean isOutputOpen(EnumFacing direction) {
		return duct.getDuct(DuctToken.ITEMS).isSideConnected((byte) direction.ordinal());
	}

	@Override
	public boolean canConnect(TileEntity to, EnumFacing direction, boolean ignoreSystemDisconnect) {
		TileEntity connection = CoordinateUtils.add(new DoubleCoordinates(duct.getPos()), direction).getTileEntity(duct.getWorld());
		if (duct.isSideBlocked((byte) direction.ordinal())) {
			return false;
		}
		if (!(connection instanceof IDuctHolder)) {
			return false;
		}
		DuctUnitItem connectedDuct = ((IDuctHolder) connection).getDuct(DuctToken.ITEMS);
		if (connectedDuct instanceof LPDuctUnitItem) {
			return !((LPDuctUnitItem) connectedDuct).isLPBlockedSide(direction.getOpposite().ordinal(), ignoreSystemDisconnect);
		} else {
			return !connectedDuct.parent.isSideBlocked(direction.getOpposite().ordinal());
		}
	}

	@Override
	public double getDistance() {
		return Math.max(duct.getDuct(DuctToken.ITEMS).getDuctLength(), 0);
	}

	@Override
	public double getDistanceWeight() {
		return Math.max(duct.getDuct(DuctToken.ITEMS).getWeight(), 0);
	}

	@Override
	public boolean isItemPipe() {
		return true;
	}

	@Override
	public boolean isFluidPipe() {
		return false;
	}

	@Override
	public boolean isPowerPipe() {
		return false;
	}

	@Override
	public CompletableFuture<Double> getDistanceToTE(int destinationint, EnumFacing ignore, ItemIdentifier ident, boolean isActive, double traveled,
													 double max,
			List<DoubleCoordinates> visited) {
		if (traveled >= max) {
			return CompletableFuture.completedFuture(Double.POSITIVE_INFINITY);
		}
		IRouter destination = SimpleServiceLocator.routerManager.getRouter(destinationint);
		if (destination == null) {
			return CompletableFuture.completedFuture(Double.POSITIVE_INFINITY);
		}

		double closesedConnection = Double.POSITIVE_INFINITY;
		final Stream<CompletableFuture<Double>> distanceFutureStream = duct.getCache(true).outputRoutes.stream()
				.filter(route -> route.endPoint instanceof LPItemDuct)
				.map(route -> {
					LPItemDuct lpDuct = (LPItemDuct) route.endPoint;

					if (traveled + route.pathWeight > max) {
						return null;
					}

					DoubleCoordinates pos = new DoubleCoordinates((TileEntity) lpDuct.pipe);
					if (visited.contains(pos)) {
						return null;
					}
					visited.add(pos);

					final EnumFacing opposite = EnumFacing.getFront(route.pathDirections.get(route.pathDirections.size() - 1)).getOpposite();
					final CompletableFuture<Double> distanceFuture = lpDuct.pipe
						.getDistanceToTE(destinationint, opposite, ident, isActive, traveled + route.pathWeight, Math.min(max, closesedConnection), visited);

					return distanceFuture.thenApply(distance -> {
						visited.remove(pos);

						if (distance.isInfinite()) {
							return distance;
						} else {
							return distance + route.pathWeight;
						}
					});
				});

		final CompletableFuture[] distanceFutures = distanceFutureStream.filter(Objects::nonNull).toArray(CompletableFuture[]::new);
		CompletableFuture<Void> distancesCheckedFuture = CompletableFuture.allOf(distanceFutures);

		return distancesCheckedFuture.thenApply(aVoid -> {
			final Optional<Double> reduce = Arrays.stream(distanceFutures)
					.map(completableFuture -> {
						try {
							//noinspection unchecked
							return ((CompletableFuture<Double>) completableFuture).get();
						} catch (ClassCastException | InterruptedException | ExecutionException e) {
							e.printStackTrace();
						}
						return null;
					}).filter(Objects::nonNull).reduce(Double::min);
			return reduce.orElse(Double.POSITIVE_INFINITY);
		});
	}

	@Override
	public boolean acceptItem(LPTravelingItem item, TileEntity from) {
		if (item instanceof LPTravelingItem.LPTravelingItemServer) {
			LPTravelingItem.LPTravelingItemServer serverItem = (LPTravelingItem.LPTravelingItemServer) item;
			int id = serverItem.getInfo().destinationint;
			if (id == -1) {
				id = SimpleServiceLocator.routerManager.getIDforUUID(serverItem.getInfo().destinationUUID);
			}
			IRouter destination = SimpleServiceLocator.routerManager.getRouter(id);
			if (destination == null) {
				return false;
			}
			RouteCache routes = duct.getDuct(DuctToken.ITEMS).getCache(true);
			Iterable<Route> paramIterable = routes.outputRoutes;
			Route route = null;
			Object cache = null;
			Triplet<Integer, ItemIdentifier, Boolean> key = new Triplet<>(id, item.getItemIdentifierStack()
					.getItem(), serverItem.getInfo()._transportMode == IRoutedItem.TransportMode.Active);
			if (duct instanceof ILPTEInformation && ((ILPTEInformation) duct).getObject() != null) {
				cache = ((ILPTEInformation) duct).getObject().getCacheHolder().getCacheFor(CacheHolder.CacheTypes.Routing, key);
			}
			if (cache instanceof Route) {
				route = (Route) cache;
				if (!routes.outputRoutes.contains(route)) {
					route = null;
				}
			}
			if (route == null) {
				Pair<Double, Route> closesedConnection = null;
				List<DoubleCoordinates> visited = new ArrayList<>();
				visited.add(new DoubleCoordinates(from));
				for (Route localRoute1 : paramIterable) {
					if (localRoute1.endPoint instanceof LPDuctUnitItem) {
						LPDuctUnitItem lpDuct = (LPDuctUnitItem) localRoute1.endPoint;

						double max = Integer.MAX_VALUE;
						if (closesedConnection != null) {
							max = closesedConnection.getValue1();
						}

						DoubleCoordinates pos = new DoubleCoordinates((TileEntity) lpDuct.pipe);
						if (visited.contains(pos)) {
							continue;
						}
						visited.add(pos);

						CompletableFuture<Double> distanceToTEFuture = lpDuct.pipe.getDistanceToTE(id,
								EnumFacing.getFront(localRoute1.pathDirections.get(localRoute1.pathDirections.size() - 1)).getOpposite(),
								item.getItemIdentifierStack().getItem(), serverItem.getInfo()._transportMode == IRoutedItem.TransportMode.Active,
								localRoute1.pathWeight,
								max,
								visited);
						double distance = GROW.asyncWorkAround(distanceToTEFuture);

						visited.remove(pos);

						if (distance != Integer.MAX_VALUE && (closesedConnection == null || distance + localRoute1.pathDirections.size() < closesedConnection
								.getValue1())) {
							closesedConnection = new Pair<>(distance + localRoute1.pathWeight, localRoute1);
						}
					}
				}
				if (closesedConnection != null) {
					route = closesedConnection.getValue2();
				}
			}
			if (route != null) {
				if (duct instanceof ILPTEInformation && ((ILPTEInformation) duct).getObject() != null) {
					((ILPTEInformation) duct).getObject().getCacheHolder().setCache(CacheHolder.CacheTypes.Routing, key, route);
				}
				TravelingItem travelItem = new TravelingItem(item.getItemIdentifierStack().makeNormalStack(), duct.getDuct(DuctToken.ITEMS), route.copy(),
						(byte) serverItem.output.ordinal(), (byte) 1 /* Speed */);
				((ILPTravelingItemInfo) travelItem).setLPRoutingInfoAddition(serverItem.getInfo());
				duct.getDuct(DuctToken.ITEMS).insertNewItem(travelItem);
				return true;
			}
		} else {
			return true;
		}
		return false;
	}

	@Override
	public void refreshTileCacheOnSide(EnumFacing side) {
		if (duct.getDuct(DuctToken.ITEMS).getGrid() == null) return;
		duct.getDuct(DuctToken.ITEMS).getGrid().destroyAndRecreate();
	}

	@Override
	public boolean isMultiBlock() {
		return false;
	}

	@Override
	public Stream<TileEntity> getPartsOfPipe() {
		return Stream.empty();
	}

	@Override
	public List<RouteInfo> getConnectedPipes(EnumFacing from) {
		List<RouteInfo> list = new ArrayList<>();
		if (duct.getDuct(DuctToken.ITEMS).getGrid() == null) {
			return null;
		}
		LinkedList<Route<DuctUnitItem, GridItem>> paramIterable = duct.getDuct(DuctToken.ITEMS).getCache(true).outputRoutes;
		for (Route<DuctUnitItem, GridItem> localRoute1 : paramIterable) {
			if (localRoute1.endPoint instanceof LPDuctUnitItem) {
				LPDuctUnitItem lpDuct = (LPDuctUnitItem) localRoute1.endPoint;
				list.add(new RouteInfo(lpDuct.pipe, localRoute1.pathWeight,
						EnumFacing.getFront(localRoute1.pathDirections.get(localRoute1.pathDirections.size() - 1))));
			}
		}
		return list;
	}
}