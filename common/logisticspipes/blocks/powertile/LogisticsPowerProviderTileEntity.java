package logisticspipes.blocks.powertile;

import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Stream;

import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.util.EnumFacing;

import logisticspipes.LogisticsPipes;
import logisticspipes.blocks.LogisticsSolidTileEntity;
import logisticspipes.gui.hud.HUDPowerLevel;
import logisticspipes.interfaces.IBlockWatchingHandler;
import logisticspipes.interfaces.IGuiOpenControler;
import logisticspipes.interfaces.IGuiTileEntity;
import logisticspipes.interfaces.IHeadUpDisplayBlockRendererProvider;
import logisticspipes.interfaces.IHeadUpDisplayRenderer;
import logisticspipes.interfaces.IPowerLevelDisplay;
import logisticspipes.interfaces.ISubSystemPowerProvider;
import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.guis.block.PowerProviderGui;
import logisticspipes.network.packets.block.PowerProviderLevel;
import logisticspipes.network.packets.hud.HUDStartBlockWatchingPacket;
import logisticspipes.network.packets.hud.HUDStopBlockWatchingPacket;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.computers.interfaces.CCCommand;
import logisticspipes.proxy.computers.interfaces.CCType;
import logisticspipes.renderer.LogisticsHUDRenderer;
import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.IRouter;
import logisticspipes.routing.PipeRoutingConnectionType;
import logisticspipes.routing.ServerRouter;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.tuples.Pair;
import logisticspipes.utils.tuples.Triplet;
import network.rs485.grow.GROW;
import network.rs485.logisticspipes.connection.NeighborTileEntity;
import network.rs485.logisticspipes.world.WorldCoordinatesWrapper;

@CCType(name = "LogisticsPowerProvider")
public abstract class LogisticsPowerProviderTileEntity extends LogisticsSolidTileEntity
		implements IGuiTileEntity, ISubSystemPowerProvider, IPowerLevelDisplay, IGuiOpenControler, IHeadUpDisplayBlockRendererProvider, IBlockWatchingHandler {

	public static final int BC_COLOR = 0x00ffff;
	public static final int RF_COLOR = 0xff0000;
	public static final int IC2_COLOR = 0xffff00;

	// true if it needs more power, turns off at full, turns on at 50%.
	public boolean needMorePowerTriggerCheck = true;

	protected Map<Integer, Double> orders = new HashMap<>();
	protected BitSet reOrdered = new BitSet(ServerRouter.getBiggestSimpleID());
	protected boolean pauseRequesting = false;

	protected double internalStorage = 0;
	protected int maxMode = 1;
	private double lastUpdateStorage = 0;
	private PlayerCollectionList guiListener = new PlayerCollectionList();
	private PlayerCollectionList watcherList = new PlayerCollectionList();
	private IHeadUpDisplayRenderer HUD;
	private boolean init = false;

	protected LogisticsPowerProviderTileEntity() {
		HUD = new HUDPowerLevel(this);
	}

	private void createLaserRoute(IRouter sourceRouter, IRouter destinationRouter, NeighborTileEntity<LogisticsTileGenericPipe> adjacent, ExitRoute route, double toSend) {
		CoreRoutedPipe pipe = sourceRouter.getPipe();
		if (pipe != null && pipe.isInitialized()) {
			pipe.container.addLaser(adjacent.getOurDirection(), 1, getLaserColor(), true, true);
		}
		sendPowerLaserPackets(destinationRouter, new Triplet<>(sourceRouter, route.exitOrientation, (route.exitOrientation != adjacent.getDirection())));
		internalStorage -= toSend;
		handlePower(destinationRouter.getPipe(), toSend);
	}

	private CompletableFuture<Void> searchLaserRoute(IRouter destinationRouter, double toSend) {
		final WorldCoordinatesWrapper worldCoordinates = new WorldCoordinatesWrapper(this);
		final Stream<NeighborTileEntity<LogisticsTileGenericPipe>> adjacentTileEntityStream = worldCoordinates.allNeighborTileEntities()
				.flatMap(neighbor -> neighbor.getJavaInstanceOf(LogisticsTileGenericPipe.class).map(Stream::of).orElseGet(Stream::empty))
				.filter(adjacent -> adjacent.getTileEntity().pipe instanceof CoreRoutedPipe)
				.filter(adjacent -> !((CoreRoutedPipe) (adjacent.getTileEntity()).pipe).stillNeedReplace());

		final Stream<CompletableFuture<Runnable>> futureStream = adjacentTileEntityStream.map(adjacent -> {
			IRouter sourceRouter = ((CoreRoutedPipe) adjacent.getTileEntity().pipe).getRouter();
			final CompletableFuture<List<ExitRoute>> distanceFuture = sourceRouter.getDistanceTo(destinationRouter);
			return distanceFuture.thenApply(exitRoutes -> {
				final Optional<ExitRoute> correctRoute = exitRoutes.stream()
						.filter(exit -> exit.containsFlag(PipeRoutingConnectionType.canPowerSubSystemFrom))
						.filter(exit -> exit.filters.stream().noneMatch(IFilter::blockPower))
						.findFirst();

				return correctRoute.<Runnable>map(exitRoute -> () -> createLaserRoute(sourceRouter, destinationRouter, adjacent, exitRoute, toSend)).orElse(null);
			});
		});

		final CompletableFuture[] completableFutures = futureStream.toArray(CompletableFuture[]::new);
		final CompletableFuture<Void> allRoutesFound = CompletableFuture.allOf(completableFutures);
		return allRoutesFound.thenAccept(aVoid -> {
			for (CompletableFuture future : completableFutures) {
				if (!future.isDone()) {
					throw new RuntimeException("Future should be done");
				}
				Object obj;
				try {
					obj = future.get();

					if (obj != null) {
						((Runnable) obj).run();
						break;
					}
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	public void update() {
		super.update();
		pauseRequesting = false;
		if (!init) {
			if (MainProxy.isClient(getWorld())) {
				LogisticsHUDRenderer.instance().add(this);
			}
			init = true;
		}
		double globalRequest = orders.values().stream().reduce(Double::sum).orElse(0.0);
		if (globalRequest > 0) {
			final double fullfillRatio = Math.min(1, Math.min(internalStorage, getMaxProvidePerTick()) / globalRequest);
			if (fullfillRatio > 0) {
				final Function<NeighborTileEntity<LogisticsTileGenericPipe>, CoreRoutedPipe> getPipe =
						(NeighborTileEntity<LogisticsTileGenericPipe> neighbor) -> (CoreRoutedPipe) neighbor.getTileEntity().pipe;
				orders.entrySet().stream()
						.map(routerIdToOrderCount -> new Pair<>(SimpleServiceLocator.routerManager.getRouter(routerIdToOrderCount.getKey()),
								Math.min(internalStorage, routerIdToOrderCount.getValue() * fullfillRatio)))
						.filter(destinationToPower -> destinationToPower.getValue1() != null && destinationToPower.getValue1().getPipe() != null)
						.forEach(destinationToPower -> new WorldCoordinatesWrapper(this)
								.allNeighborTileEntities()
								.flatMap(neighbor -> neighbor.getJavaInstanceOf(LogisticsTileGenericPipe.class).map(Stream::of).orElseGet(Stream::empty))
								.filter(neighbor -> neighbor.getTileEntity().pipe instanceof CoreRoutedPipe &&
										!getPipe.apply(neighbor).stillNeedReplace())
								.map(neighbor -> new Pair<>(neighbor, GROW.asyncWorkAround(getPipe.apply(neighbor).getRouter().getDistanceTo(destinationToPower.getValue1()))))
								.flatMap(neighborToDistances -> neighborToDistances.getValue2().stream().map(exitRoute -> new Pair<>(neighborToDistances.getValue1(), exitRoute)))
								.filter(neighborToExit -> neighborToExit.getValue2().containsFlag(PipeRoutingConnectionType.canPowerSubSystemFrom) &&
										neighborToExit.getValue2().filters.stream().noneMatch(IFilter::blockPower))
								.findFirst()
								.ifPresent(neighborToSource -> {
									CoreRoutedPipe sourcePipe = getPipe.apply(neighborToSource.getValue1());
									if (sourcePipe.isInitialized()) {
										sourcePipe.container.addLaser(neighborToSource.getValue1().getOurDirection(), 1, getLaserColor(), true, true);
									}
									sendPowerLaserPackets(sourcePipe.getRouter(), destinationToPower.getValue1(), neighborToSource.getValue2().exitOrientation,
											neighborToSource.getValue2().exitOrientation != neighborToSource.getValue1().getDirection());
									internalStorage -= destinationToPower.getValue2();
									if (internalStorage <= 0) internalStorage = 0; // because calculations with floats
									handlePower(destinationToPower.getValue1().getPipe(), destinationToPower.getValue2());
								}));
			}
		}
		orders.clear();
		if (MainProxy.isServer(world)) {
			if (internalStorage != lastUpdateStorage) {
				updateClients();
				lastUpdateStorage = internalStorage;
			}
		}

		// Async Alternative:
//		List<CompletableFuture<Void>> completedList = new ArrayList<>();
//		if (globalRequest > 0) {
//			double fullfillRatio = Math.min(1, Math.min(internalStorage, getMaxProvidePerTick()) / globalRequest);
//			if (fullfillRatio > 0) {
//				for (Entry<Integer, Double> order : orders.entrySet()) {
//					double toSend = order.getValue() * fullfillRatio;
//					if (toSend > internalStorage) {
//						toSend = internalStorage;
//					}
//					IRouter destinationRouter = SimpleServiceLocator.routerManager.getRouter(order.getKey());
//					if (destinationRouter != null && destinationRouter.getPipe() != null) {
//						final CompletableFuture<Void> laserRouteFuture = searchLaserRoute(destinationRouter, toSend);
//						completedList.add(laserRouteFuture);
//					}
//				}
//			}
//		}
//
//		CompletableFuture.allOf(completedList.toArray(new CompletableFuture[completedList.size()]))
//				.thenAccept(aVoid -> {
//					orders.clear();
//					if (MainProxy.isServer(worldObj)) {
//						if (internalStorage != lastUpdateStorage) {
//							updateClients();
//							lastUpdateStorage = internalStorage;
//						}
//					}
//				}).whenComplete((result, error) -> GROW.asyncComplete(result, error, "updateEntity", this));
	}

	protected abstract void handlePower(CoreRoutedPipe pipe, double toSend);

	private void sendPowerLaserPackets(IRouter destinationRouter, Triplet<IRouter, EnumFacing, Boolean> tripletData) {
		if (tripletData.getValue1() == destinationRouter) {
			return;
		}

		tripletData.getValue1().getRoutersOnSide(tripletData.getValue2())
				.filter(exitRoute -> exitRoute.containsFlag(PipeRoutingConnectionType.canPowerSubSystemFrom))
				.forEach(exitRoute -> {
					int distance = tripletData.getValue1().getDistanceToNextPowerPipe(exitRoute.exitOrientation);

					CoreRoutedPipe pipe = tripletData.getValue1().getPipe();
					if (pipe != null && pipe.isInitialized()) {
						pipe.container.addLaser(exitRoute.exitOrientation, distance, getLaserColor(), false, tripletData.getValue3());
					}

					IRouter nextRouter = exitRoute.destination; // Use new sourceRouter
					if (nextRouter == destinationRouter) {
						return;
					}

					final CompletableFuture<List<ExitRoute>> distanceFuture = nextRouter.getDistanceTo(destinationRouter);
					distanceFuture.thenAccept(exitRoutes -> exitRoutes.stream()
							.filter(newRoute -> newRoute.containsFlag(PipeRoutingConnectionType.canPowerSubSystemFrom))
							.filter(newRoute -> !newRoute.filters.stream().anyMatch(IFilter::blockPower))
							.forEach(newRoute ->
									sendPowerLaserPackets(destinationRouter, new Triplet<>(nextRouter, newRoute.exitOrientation, newRoute.exitOrientation != exitRoute.exitOrientation)))
					).whenComplete((result, error) -> GROW.asyncComplete(result, error, "updateEntity", this));
				});
	}

	protected abstract double getMaxProvidePerTick();

	@CCCommand(description = "Returns the color for the power provided by this power provider")
	protected abstract int getLaserColor();

	@Override
	@CCCommand(description = "Returns the max. amount of storable power")
	public abstract int getMaxStorage();

	@Override
	@CCCommand(description = "Returns the power type stored in this power provider")
	public abstract String getBrand();

	@Override
	public void invalidate() {
		super.invalidate();
		if (MainProxy.isClient(getWorld())) {
			LogisticsHUDRenderer.instance().remove(this);
		}
	}

	@Override
	public void validate() {
		super.validate();
		if (MainProxy.isClient(getWorld())) {
			init = false;
		}
	}

	@Override
	public void onChunkUnload() {
		super.onChunkUnload();
		if (MainProxy.isClient(getWorld())) {
			LogisticsHUDRenderer.instance().remove(this);
		}
	}

	@Override
	public void requestPower(int destination, double amount) {
		if (pauseRequesting) {
			return;
		}
		if (getBrand().equals("EU")) {
			System.out.print("");
		}
		if (orders.containsKey(destination)) {
			if (reOrdered.get(destination)) {
				pauseRequesting = true;
				reOrdered.clear();
			} else {
				reOrdered.set(destination);
			}
		} else {
			reOrdered.clear();
		}
		orders.put(destination, amount);
	}

	@Override
	@CCCommand(description = "Returns the current power level for this power provider")
	public double getPowerLevel() {
		return lastUpdateStorage;
	}

	@Override
	public boolean usePaused() {
		return pauseRequesting;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		if (nbt.getTag("internalStorage") instanceof NBTTagFloat) { // support for old float
			internalStorage = nbt.getFloat("internalStorage");
		} else {
			internalStorage = nbt.getDouble("internalStorage");
		}
		maxMode = nbt.getInteger("maxMode");

	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt = super.writeToNBT(nbt);
		nbt.setDouble("internalStorageDouble", internalStorage);
		nbt.setInteger("maxMode", maxMode);
		return nbt;
	}

	@Override
	public IHeadUpDisplayRenderer getRenderer() {
		return HUD;
	}

	@Override
	public int getX() {
		return pos.getX();
	}

	@Override
	public int getY() {
		return pos.getY();
	}

	@Override
	public int getZ() {
		return pos.getZ();
	}

	@Override
	public void startWatching() {
		MainProxy.sendPacketToServer(PacketHandler.getPacket(HUDStartBlockWatchingPacket.class).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
	}

	@Override
	public void stopWatching() {
		MainProxy.sendPacketToServer(PacketHandler.getPacket(HUDStopBlockWatchingPacket.class).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
	}

	@Override
	public void playerStartWatching(EntityPlayer player) {
		watcherList.add(player);
		updateClients();
	}

	@Override
	public void playerStopWatching(EntityPlayer player) {
		watcherList.remove(player);
	}

	@Override
	public boolean isHUDExistent() {
		return getWorld().getTileEntity(pos) == this;
	}

	@Override
	public void guiOpenedByPlayer(EntityPlayer player) {
		guiListener.add(player);
		updateClients();
	}

	@Override
	public void guiClosedByPlayer(EntityPlayer player) {
		guiListener.remove(player);
	}

	public void updateClients() {
		MainProxy.sendToPlayerList(PacketHandler.getPacket(PowerProviderLevel.class).setDouble(internalStorage).setTilePos(this), guiListener);
		MainProxy.sendToPlayerList(PacketHandler.getPacket(PowerProviderLevel.class).setDouble(internalStorage).setTilePos(this), watcherList);
	}

	@Override
	public void addInfoToCrashReport(CrashReportCategory par1CrashReportCategory) {
		super.addInfoToCrashReport(par1CrashReportCategory);
		par1CrashReportCategory.addCrashSection("LP-Version", LogisticsPipes.getVersionString());
	}

	public void handlePowerPacket(double d) {
		if (MainProxy.isClient(getWorld())) {
			internalStorage = d;
		}
	}

	@Override
	public int getChargeState() {
		return (int) Math.min(100F, internalStorage * 100 / getMaxStorage());
	}

	@Override
	public int getDisplayPowerLevel() {
		return internalStorage > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) internalStorage;
	}

	@Override
	public boolean isHUDInvalid() {
		return isInvalid();
	}

	@Override
	public CoordinatesGuiProvider getGuiProvider() {
		return NewGuiHandler.getGui(PowerProviderGui.class);
	}
}
