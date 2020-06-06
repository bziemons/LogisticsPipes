package logisticspipes.routing;

import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import javax.annotation.Nullable;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import logisticspipes.LogisticsPipes;
import logisticspipes.api.ILogisticsPowerProvider;
import logisticspipes.interfaces.ISubSystemPowerProvider;
import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.modules.abstractmodules.LogisticsModule;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.tuples.Pair;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public class ClientRouter implements IRouter {

	private final int _xCoord;
	private final int _yCoord;
	private final int _zCoord;

	public ClientRouter(UUID id, int dimension, int xCoord, int yCoord, int zCoord) {
		_xCoord = xCoord;
		_yCoord = yCoord;
		_zCoord = zCoord;
	}

	@Override
	public void destroy() {}

	@Override
	public int getSimpleID() {
		return -420;
	}

	@Override
	public void update(boolean doFullRefresh, CoreRoutedPipe pipe) {

	}

	@Override
	public boolean isRoutedExit(EnumFacing connection) {
		if (LogisticsPipes.isDEBUG()) {
			throw new UnsupportedOperationException("noClientRouting");
		}
		return false;
	}

	@Override
	public CompletableFuture<Boolean> hasRoute(int id, boolean flag, ItemIdentifier item) {
		if (LogisticsPipes.isDEBUG()) {
			throw new UnsupportedOperationException("noClientRouting");
		}
		return CompletableFuture.completedFuture(false);
	}

	@Override
	public CompletableFuture<ExitRoute> getExitFor(int id, boolean flag, ItemIdentifier item) {
		if (LogisticsPipes.isDEBUG()) {
			throw new UnsupportedOperationException("noClientRouting");
		}
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<List<List<ExitRoute>>> getRouteTable() {
		if (LogisticsPipes.isDEBUG()) {
			throw new UnsupportedOperationException("noClientRouting");
		}
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	@Override
	public CompletableFuture<List<ExitRoute>> getIRoutersByCost() {
		if (LogisticsPipes.isDEBUG()) {
			throw new UnsupportedOperationException("noClientRouting");
		}
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	@Override
	public CoreRoutedPipe getPipe() {
		World world = MainProxy.proxy.getWorld();
		if (world == null) {
			return null;
		}
		TileEntity tile = world.getTileEntity(new BlockPos(_xCoord, _yCoord, _zCoord));

		if (!(tile instanceof LogisticsTileGenericPipe)) {
			return null;
		}
		LogisticsTileGenericPipe pipe = (LogisticsTileGenericPipe) tile;
		if (!(pipe.pipe instanceof CoreRoutedPipe)) {
			return null;
		}
		return (CoreRoutedPipe) pipe.pipe;
	}

	@Override
	public CoreRoutedPipe getCachedPipe() {
		return getPipe();
	}

	@Override
	public boolean isInDim(int dimension) {
		return true;
	}

	@Override
	public boolean isAt(int dimension, int xCoord, int yCoord, int zCoord) {
		return _xCoord == xCoord && _yCoord == yCoord && _zCoord == zCoord;
	}

	@Override
	public DoubleCoordinates getLPPosition() {
		return new DoubleCoordinates(_xCoord, _yCoord, _zCoord);
	}

	@Override
	public UUID getId() {
		return UUID.randomUUID();
	}

	@Override
	public LogisticsModule getLogisticsModule() {
		CoreRoutedPipe pipe = getPipe();
		if (pipe == null) {
			return null;
		}
		return pipe.getLogisticsModule();
	}

	@Override
	public void clearPipeCache() {
		//Not On Client Side
	}

	@Override
	public List<Pair<ILogisticsPowerProvider, List<IFilter>>> getPowerProvider() {
		return null;
	}

	@Override
	public void act(BitSet hasBeenProcessed, IRAction actor) {

	}

	@Override
	public void flagForRoutingUpdate() {

	}

	@Override
	public boolean checkAdjacentUpdate() {
		return false;
	}

	@Override
	public boolean isSideDisconnected(EnumFacing dir) {
		return false;
	}

	@Override
	public void updateInterests() {}

	@Override
	public CompletableFuture<List<ExitRoute>> getDistanceTo(IRouter r) {
		return null;
	}

	@Override
	public void clearInterests() {}

	@Override
	public boolean isValidCache() {
		return true;
	}

	@Override
	public void forceLsaUpdate() {}

	@Override
	public boolean isSubPoweredExit(EnumFacing connection) {
		return false;
	}

	@Override
	public List<Pair<ISubSystemPowerProvider, List<IFilter>>> getSubSystemPowerProvider() {
		return null;
	}

	@Override
	public String toString() {
		return String.format("ServerRouter: {UUID: %s, AT: (%d, %d, %d)}", getId(), _xCoord, _yCoord, _zCoord);
	}

	@Override
	public @Nullable Stream<ExitRoute> getRoutersOnSide(EnumFacing exitOrientation) {
		return null;
	}

	@Override
	public int getDimension() {
		return 0;
	}

	@Override
	public int getDistanceToNextPowerPipe(EnumFacing dir) {
		return 0;
	}

	@Override
	public void queueTask(int i, IRouterQueuedTask callable) {}
}
