/**
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.routing;

import java.util.BitSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import net.minecraft.util.EnumFacing;

import logisticspipes.api.ILogisticsPowerProvider;
import logisticspipes.interfaces.ISubSystemPowerProvider;
import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.modules.abstractmodules.LogisticsModule;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.tuples.Pair;
import network.rs485.logisticspipes.util.LPDataOutput;
import network.rs485.logisticspipes.util.LPFinalSerializable;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public interface IRouter extends LPFinalSerializable {

	void destroy();

	void update(boolean doFullRefresh, CoreRoutedPipe pipe);

	void updateInterests(); // calls getInterests on the attached pipe, and updates the global cache.

	boolean isRoutedExit(EnumFacing connection);

	boolean isSubPoweredExit(EnumFacing connection);

	int getDistanceToNextPowerPipe(EnumFacing dir);

	CompletableFuture<Boolean> hasRoute(int id, boolean active, ItemIdentifier type);

	CompletableFuture<ExitRoute> getExitFor(int id, boolean active, ItemIdentifier type);

	CompletableFuture<List<List<ExitRoute>>> getRouteTable();

	CompletableFuture<List<ExitRoute>> getIRoutersByCost();

	CoreRoutedPipe getPipe();

	CoreRoutedPipe getCachedPipe();

	boolean isInDim(int dimension);

	boolean isAt(int dimension, int xCoord, int yCoord, int zCoord);

	UUID getId();

	LogisticsModule getLogisticsModule();

	void clearPipeCache();

	int getSimpleID();

	DoubleCoordinates getLPPosition();

	/**
	 * @param hasBeenProcessed
	 *            a bitset flagging which nodes have already been acted on (the
	 *            router should set the bit for it's own id, then return true.
	 * @param actor
	 *            the visitor
	 * @return true if the bitset was cleared at some stage during the process,
	 *         resulting in a potentially incomplete bitset.
	 */
	void act(BitSet hasBeenProcessed, IRAction actor);

	void flagForRoutingUpdate();

	boolean checkAdjacentUpdate();

	/* Automated Disconnection */
	boolean isSideDisconnected(EnumFacing dir);

	CompletableFuture<List<ExitRoute>> getDistanceTo(IRouter r);

	void clearInterests();

	List<Pair<ILogisticsPowerProvider, List<IFilter>>> getPowerProvider();

	List<Pair<ISubSystemPowerProvider, List<IFilter>>> getSubSystemPowerProvider();

	boolean isValidCache();

	//force-update LSA version in the network
	void forceLsaUpdate();

	Stream<ExitRoute> getRoutersOnSide(EnumFacing direction);

	int getDimension();

	void queueTask(int i, IRouterQueuedTask callable);

	@Override
	default void write(LPDataOutput output) {
		output.writeSerializable(getLPPosition());
	}

	interface IRAction {

		boolean isInteresting(IRouter that);

		void doTo(IRouter that);
	}
}
