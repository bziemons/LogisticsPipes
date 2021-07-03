/*
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.pipes.basic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.PriorityBlockingQueue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import net.minecraftforge.fml.network.NetworkHooks;

import lombok.Getter;

import logisticspipes.LPItems;
import logisticspipes.LogisticsPipes;
import logisticspipes.api.ILogisticsPowerProvider;
import logisticspipes.asm.te.ILPTEInformation;
import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.config.Configs;
import logisticspipes.gui.orderer.NormalGuiOrderer;
import logisticspipes.interfaces.IClientState;
import logisticspipes.interfaces.ILPPositionProvider;
import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.interfaces.IPipeUpgradeManager;
import logisticspipes.interfaces.IQueueCCEvent;
import logisticspipes.interfaces.ISecurityProvider;
import logisticspipes.interfaces.ISlotUpgradeManager;
import logisticspipes.interfaces.ISubSystemPowerProvider;
import logisticspipes.interfaces.IWatchingHandler;
import logisticspipes.interfaces.IWorldProvider;
import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.interfaces.routing.IRequireReliableFluidTransport;
import logisticspipes.interfaces.routing.IRequireReliableTransport;
import logisticspipes.items.ItemPipeSignCreator;
import logisticspipes.logisticspipes.IRoutedItem;
import logisticspipes.logisticspipes.IRoutedItem.TransportMode;
import logisticspipes.logisticspipes.ITrackStatistics;
import logisticspipes.logisticspipes.PipeTransportLayer;
import logisticspipes.logisticspipes.RouteLayer;
import logisticspipes.logisticspipes.TransportLayer;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.modules.LogisticsModule.ModulePositionType;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.guis.pipe.PipeController;
import logisticspipes.network.packets.pipe.ParticleFX;
import logisticspipes.network.packets.pipe.PipeSignTypes;
import logisticspipes.network.packets.pipe.RequestSignPacket;
import logisticspipes.network.packets.pipe.StatUpdate;
import logisticspipes.pipefxhandlers.Particles;
import logisticspipes.pipefxhandlers.PipeFXRenderHandler;
import logisticspipes.pipes.basic.debug.DebugLogController;
import logisticspipes.pipes.basic.debug.StatusEntry;
import logisticspipes.pipes.signs.IPipeSign;
import logisticspipes.pipes.upgrades.UpgradeManager;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.renderer.LogisticsRenderPipe;
import logisticspipes.renderer.newpipe.IHighlightPlacementRenderer;
import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.IRouter;
import logisticspipes.routing.ItemRoutingInformation;
import logisticspipes.routing.ServerRouter;
import logisticspipes.routing.order.IOrderInfoProvider;
import logisticspipes.routing.order.LogisticsItemOrderManager;
import logisticspipes.routing.order.LogisticsOrderManager;
import logisticspipes.security.PermissionException;
import logisticspipes.security.SecuritySettings;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.transport.LPTravelingItem.LPTravelingItemServer;
import logisticspipes.transport.PipeTransportLogistics;
import logisticspipes.utils.CacheHolder;
import logisticspipes.utils.DirectionUtil;
import logisticspipes.utils.FluidIdentifierStack;
import logisticspipes.utils.OrientationsUtil;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.tuples.Pair;
import logisticspipes.utils.tuples.Triplet;
import network.rs485.logisticspipes.connection.Adjacent;
import network.rs485.logisticspipes.connection.AdjacentFactory;
import network.rs485.logisticspipes.connection.NoAdjacent;
import network.rs485.logisticspipes.module.Gui;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public abstract class CoreRoutedPipe extends CoreUnroutedPipe
		implements IClientState, IRequestItems, ITrackStatistics, IWorldProvider, IWatchingHandler, IPipeServiceProvider, IQueueCCEvent, ILPPositionProvider {

	private static int pipecount = 0;
	public final PlayerCollectionList watchers = new PlayerCollectionList();
	protected final PriorityBlockingQueue<ItemRoutingInformation> _inTransitToMe = new PriorityBlockingQueue<>(10,
			new ItemRoutingInformation.DelayComparator());
	protected final LinkedList<Triplet<IRoutedItem, Direction, ItemSendMode>> _sendQueue = new LinkedList<>();
	protected final Map<ItemIdentifier, Queue<Pair<Integer, ItemRoutingInformation>>> queuedDataForUnroutedItems = Collections.synchronizedMap(new TreeMap<>());
	public boolean _textureBufferPowered;
	public long delayTo = 0;
	public int repeatFor = 0;
	public int stat_session_sent;
	public int stat_session_recieved;
	public int stat_session_relayed;
	public long stat_lifetime_sent;
	public long stat_lifetime_recieved;
	public long stat_lifetime_relayed;
	public int server_routing_table_size = 0;
	protected boolean stillNeedReplace = true;
	protected IRouter router;
	protected String routerId;
	protected final Object routerIdLock = new Object();
	protected int _delayOffset;
	protected boolean _initialInit = true;
	protected RouteLayer _routeLayer;
	protected TransportLayer _transportLayer;

	@Nonnull
	final protected UpgradeManager upgradeManager = new UpgradeManager(this);

	protected LogisticsItemOrderManager _orderItemManager = null;
	protected int throttleTime = 20;
	protected IPipeSign[] signItem = new IPipeSign[6];
	private boolean recheckConnections = false;
	private boolean enabled = true;
	private boolean preventRemove = false;
	private boolean destroyByPlayer = false;
	private final PowerSupplierHandler powerHandler = new PowerSupplierHandler();
	@Getter
	private final List<IOrderInfoProvider> clientSideOrderManager = new ArrayList<>();
	private int throttleTimeLeft = 20 + new Random().nextInt(Configs.LOGISTICS_DETECTION_FREQUENCY);
	private final int[] queuedParticles = new int[Particles.values().length];
	private boolean hasQueuedParticles = false;
	private boolean isOpaqueClientSide = false;

	/** Caches adjacent state, only on Side.SERVER */
	@Nonnull
	private Adjacent adjacent = NoAdjacent.INSTANCE;

	/**
	 * @return the adjacent cache directly.
	 */
	@Nonnull
	protected Adjacent getAdjacent() {
		return adjacent;
	}

	/**
	 * Returns all adjacents on a regular routed pipe.
	 */
	@Nonnull
	@Override
	public Adjacent getAvailableAdjacent() {
		return getAdjacent();
	}

	@Nullable
	@Override
	public Direction getPointedOrientation() {
		// from IPipeServiceProvider, overridden in the PipeLogisticsChassis
		return null;
	}

	/**
	 * Re-creates adjacent cache.
	 */
	protected void updateAdjacentCache() {
		adjacent = AdjacentFactory.INSTANCE.createAdjacentCache(this);
	}

	private CacheHolder cacheHolder;

	public CoreRoutedPipe(Item item) {
		this(new PipeTransportLogistics(true), item);
	}

	public CoreRoutedPipe(PipeTransportLogistics transport, Item item) {
		super(transport, item);

		CoreRoutedPipe.pipecount++;

		//Roughly spread pipe updates throughout the frequency, no need to maintain balance
		_delayOffset = CoreRoutedPipe.pipecount % Configs.LOGISTICS_DETECTION_FREQUENCY;
	}

	@Override
	public void markTileDirty() {
		if (container != null) container.markDirty();
	}

	@Nonnull
	public RouteLayer getRouteLayer() {
		if (_routeLayer == null) {
			_routeLayer = new RouteLayer(getRouter(), getTransportLayer(), this);
		}
		return _routeLayer;
	}

	@Nonnull
	public TransportLayer getTransportLayer() {
		if (_transportLayer == null) {
			_transportLayer = new PipeTransportLayer(this, this, getRouter());
		}
		return _transportLayer;
	}

	@Nonnull
	@Override
	public ISlotUpgradeManager getUpgradeManager(ModulePositionType slot, int positionInt) {
		return upgradeManager;
	}

	@Override
	public IPipeUpgradeManager getUpgradeManager() {
		return upgradeManager;
	}

	public UpgradeManager getOriginalUpgradeManager() {
		return upgradeManager;
	}

	@Override
	public void queueRoutedItem(IRoutedItem routedItem, Direction from) {
		if (from == null) {
			throw new NullPointerException();
		}
		_sendQueue.addLast(new Triplet<>(routedItem, from, ItemSendMode.Normal));
		sendQueueChanged(false);
	}

	public void queueRoutedItem(IRoutedItem routedItem, Direction from, ItemSendMode mode) {
		if (from == null) {
			throw new NullPointerException();
		}
		_sendQueue.addLast(new Triplet<>(routedItem, from, mode));
		sendQueueChanged(false);
	}

	/**
	 * @param force == true never delegates to a thread
	 * @return number of things sent.
	 */
	public int sendQueueChanged(boolean force) {
		return 0;
	}

	private void sendRoutedItem(IRoutedItem routedItem, Direction from) {

		if (from == null) {
			throw new NullPointerException();
		}

		transport.injectItem(routedItem, from.getOpposite());

		IRouter r = SimpleServiceLocator.routerManager.getServerRouter(routedItem.getDestination());
		if (r != null) {
			CoreRoutedPipe pipe = r.getCachedPipe();
			if (pipe != null) {
				pipe.notifyOfSend(routedItem.getInfo());
			} // else TODO: handle sending items to known chunk-unloaded destination?
		} // should not be able to send to a non-existing router
		// router.startTrackingRoutedItem((RoutedItemEntity) routedItem.getTravelingItem());
		spawnParticle(Particles.OrangeParticle, 2);
		stat_lifetime_sent++;
		stat_session_sent++;
		updateStats();
	}

	private void notifyOfSend(ItemRoutingInformation routedItem) {
		_inTransitToMe.add(routedItem);
		//LogisticsPipes.getLOGGER().info("Sending: "+routedItem.getIDStack().getItem().getFriendlyName());
	}

	public void notifyOfReroute(ItemRoutingInformation routedItem) {
		_inTransitToMe.remove(routedItem);
	}

	//When Recreating the Item from the TE version we have the same hashCode but a different instance so we need to refresh this
	public void refreshItem(ItemRoutingInformation routedItem) {
		if (_inTransitToMe.contains(routedItem)) {
			_inTransitToMe.remove(routedItem);
			_inTransitToMe.add(routedItem);
		}
	}

	public abstract ItemSendMode getItemSendMode();

	/**
	 * Designed to help protect against routing loops - if both pipes are on the same block
	 *
	 * @return boolean indicating if other and this are attached to the same inventory.
	 */
	public boolean isOnSameContainer(CoreRoutedPipe other) {
		// FIXME: Same TileEntity? Same Inventory view?
		return adjacent.connectedPos().keySet().stream().anyMatch(
			other.adjacent.connectedPos().keySet()::contains
		);
	}

	/***
	 * first tick just create a router and do nothing.
	 */
	public void firstInitialiseTick() {
		getRouter();
		if (MainProxy.isClient(getWorld())) {
			MainProxy.sendPacketToServer(PacketHandler.getPacket(RequestSignPacket.class).setTilePos(container));
		}
	}

	/***
	 * Only Called Server Side Only Called when the pipe is enabled
	 */
	public void enabledUpdateEntity() {
		powerHandler.update();
		for (int i = 0; i < 6; i++) {
			if (signItem[i] != null) {
				signItem[i].updateServerSide();
			}
		}
	}

	/***
	 * Called Server and Client Side Called every tick
	 */
	public void ignoreDisableUpdateEntity() {}

	@Override
	public final void updateEntity() {
		debug.tick();
		spawnParticleTick();
		if (stillNeedReplace) {
			stillNeedReplace = false;
			//BlockState state = getWorld().getBlockState(getPos());
			//getWorld().notifyNeighborsOfStateChange(getPos(), state == null ? null : state.getBlock(), true);
			/* TravelingItems are just held by a pipe, they don't need to know their world
			 * for(Triplet<IRoutedItem, Direction, ItemSendMode> item : _sendQueue) {
				//assign world to any ItemEntity we created in readfromnbt
				item.getValue1().getTravelingItem().setWorld(getWorld());
			}*/
			//first tick just create a router and do nothing.
			firstInitialiseTick();
			return;
		}
		if (repeatFor > 0) {
			if (delayTo < System.currentTimeMillis()) {
				delayTo = System.currentTimeMillis() + 200;
				repeatFor--;
				getWorld().notifyNeighborsOfStateChange(getPos(), getWorld().getBlockState(getPos()).getBlock());
			}
		}

		// remove old items _inTransit -- these should have arrived, but have probably been lost instead. In either case, it will allow a re-send so that another attempt to re-fill the inventory can be made.
		while (_inTransitToMe.peek() != null && _inTransitToMe.peek().getTickToTimeOut() <= 0) {
			final ItemRoutingInformation polledInfo = _inTransitToMe.poll();
			if (polledInfo != null) {
				if (LogisticsPipes.isDEBUG()) {
					LogisticsPipes.getLOGGER().info("Timed Out: " + polledInfo.getItem().getFriendlyName() + " (" + polledInfo.hashCode() + ")");
				}
				debug.log("Timed Out: " + polledInfo.getItem().getFriendlyName() + " (" + polledInfo.hashCode() + ")");
			}
		}
		//update router before ticking logic/transport
		final boolean doFullRefresh =
				getWorld().getGameTime() % Configs.LOGISTICS_DETECTION_FREQUENCY == _delayOffset
				|| _initialInit || recheckConnections;
		if (doFullRefresh) {
			// update adjacent cache first, so interests can be gathered correctly
			// in getRouter().update(…) below
			updateAdjacentCache();
		}
		getRouter().update(doFullRefresh, this);
		recheckConnections = false;
		getOriginalUpgradeManager().securityTick();
		super.updateEntity();

		if (isNthTick(200)) {
			getCacheHolder().trigger(null);
		}

		// from BaseRoutingLogic
		if (--throttleTimeLeft <= 0) {
			throttledUpdateEntity();
			throttleTimeLeft = throttleTime;
		}

		ignoreDisableUpdateEntity();
		_initialInit = false;
		if (!_sendQueue.isEmpty()) {
			if (getItemSendMode() == ItemSendMode.Normal) {
				Triplet<IRoutedItem, Direction, ItemSendMode> itemToSend = _sendQueue.getFirst();
				sendRoutedItem(itemToSend.getValue1(), itemToSend.getValue2());
				_sendQueue.removeFirst();
				for (int i = 0; i < 16 && !_sendQueue.isEmpty() && _sendQueue.getFirst().getValue3() == ItemSendMode.Fast; i++) {
					if (!_sendQueue.isEmpty()) {
						itemToSend = _sendQueue.getFirst();
						sendRoutedItem(itemToSend.getValue1(), itemToSend.getValue2());
						_sendQueue.removeFirst();
					}
				}
				sendQueueChanged(false);
			} else if (getItemSendMode() == ItemSendMode.Fast) {
				for (int i = 0; i < 16; i++) {
					if (!_sendQueue.isEmpty()) {
						Triplet<IRoutedItem, Direction, ItemSendMode> itemToSend = _sendQueue.getFirst();
						sendRoutedItem(itemToSend.getValue1(), itemToSend.getValue2());
						_sendQueue.removeFirst();
					}
				}
				sendQueueChanged(false);
			} else if (getItemSendMode() == null) {
				throw new UnsupportedOperationException("getItemSendMode() can't return null. " + this.getClass().getName());
			} else {
				throw new UnsupportedOperationException(
						"getItemSendMode() returned unhandled value. " + getItemSendMode().name() + " in " + this.getClass().getName());
			}
		}
		if (MainProxy.isClient(getWorld())) {
			return;
		}
		checkTexturePowered();
		if (!isEnabled()) {
			return;
		}
		enabledUpdateEntity();
		if (getLogisticsModule() == null) {
			return;
		}
		getLogisticsModule().tick();
	}

	protected void onAllowedRemoval() {}

	// From BaseRoutingLogic
	public void throttledUpdateEntity() {}

	protected void delayThrottle() {
		//delay 6(+1) ticks to prevent suppliers from ticking between a item arriving at them and the item hitting their adj. inv
		if (throttleTimeLeft < 7) {
			throttleTimeLeft = 7;
		}
	}

	@Override
	public boolean isNthTick(int n) {
		return ((getWorld().getGameTime() + _delayOffset) % n == 0);
	}

	private void doDebugStuff(PlayerEntity player) {
		//player.world.setWorldTime(4951);
		if (!MainProxy.isServer(player.world)) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		ServerRouter router = (ServerRouter) getRouter();

		sb.append("***\n");
		sb.append("---------Interests---------------\n");
		ServerRouter.forEachGlobalSpecificInterest((itemIdentifier, serverRouters) -> {
			sb.append(itemIdentifier.getFriendlyName()).append(":");
			for (IRouter j : serverRouters) {
				sb.append(j.getSimpleID()).append(",");
			}
			sb.append('\n');
		});

		sb.append("ALL ITEMS:");
		for (IRouter j : ServerRouter.getInterestedInGeneral()) {
			sb.append(j.getSimpleID()).append(",");
		}
		sb.append('\n');

		sb.append(router).append('\n');
		sb.append("---------CONNECTED TO---------------\n");
		for (CoreRoutedPipe adj : router._adjacent.keySet()) {
			sb.append(adj.getRouter().getSimpleID()).append('\n');
		}
		sb.append('\n');
		sb.append("========DISTANCE TABLE==============\n");
		for (ExitRoute n : router.getIRoutersByCost()) {
			sb.append(n.destination.getSimpleID())
					.append(" @ ")
					.append(n.distanceToDestination)
					.append(" -> ")
					.append(n.connectionDetails)
					.append("(")
					.append(n.destination.getId())
					.append(")")
					.append('\n');
		}
		sb.append('\n');
		sb.append("*******EXIT ROUTE TABLE*************\n");
		List<List<ExitRoute>> table = router.getRouteTable();
		for (int i = 0; i < table.size(); i++) {
			if (table.get(i) != null) {
				if (table.get(i).size() > 0) {
					sb.append(i).append(" -> ").append(table.get(i).get(0).destination.getSimpleID()).append('\n');
					for (ExitRoute route : table.get(i)) {
						sb.append("\t\t via ").append(route.exitOrientation).append("(").append(route.distanceToDestination).append(" distance)").append('\n');
					}
				}
			}
		}
		sb.append('\n');
		sb.append("++++++++++CONNECTIONS+++++++++++++++\n");
		sb.append(Arrays.toString(Direction.values())).append('\n');
		sb.append(Arrays.toString(router.sideDisconnected)).append('\n');
		if (container != null) {
			sb.append(Arrays.toString(container.pipeConnectionsBuffer)).append('\n');
		}
		sb.append("+++++++++++++ADJACENT+++++++++++++++\n");
		sb.append(adjacent).append('\n');
		sb.append("pointing: ").append(getPointedOrientation()).append('\n');
		sb.append("~~~~~~~~~~~~~~~POWER~~~~~~~~~~~~~~~~\n");
		sb.append(router.getPowerProvider()).append('\n');
		sb.append("~~~~~~~~~~~SUBSYSTEMPOWER~~~~~~~~~~~\n");
		sb.append(router.getSubSystemPowerProvider()).append('\n');
		if (_orderItemManager != null) {
			sb.append("################ORDERDUMP#################\n");
			_orderItemManager.dump(sb);
		}
		sb.append("################END#################\n");
		refreshConnectionAndRender(true);
		System.out.print(sb);
		router.CreateRouteTable(Integer.MAX_VALUE);
	}

	// end FromBaseRoutingLogic

	@Override
	public final void onBlockRemoval() {
		try {
			onAllowedRemoval();
			super.onBlockRemoval();
			//Just in case
			CoreRoutedPipe.pipecount = Math.max(CoreRoutedPipe.pipecount - 1, 0);

			if (transport != null) {
				transport.dropBuffer();
			}
			getOriginalUpgradeManager().dropUpgrades();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void invalidate() {
		super.invalidate();
		if (router != null) {
			router.destroy();
			router = null;
		}
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		if (router != null) {
			router.clearPipeCache();
			router.clearInterests();
		}
	}

	public void checkTexturePowered() {
		if (Configs.LOGISTICS_POWER_USAGE_DISABLED) {
			return;
		}
		if (!isNthTick(10)) {
			return;
		}
		if (stillNeedReplace || _initialInit || router == null) {
			return;
		}
		boolean flag;
		if ((flag = canUseEnergy(1)) != _textureBufferPowered) {
			_textureBufferPowered = flag;
			refreshRender(false);
			spawnParticle(Particles.RedParticle, 3);
		}
	}

	@Override
	public int getTextureIndex() {
		return getCenterTexture().newTexture;
	}

	public abstract TextureType getCenterTexture();

	public TextureType getTextureType(Direction connection) {
		if (stillNeedReplace || _initialInit) {
			return getCenterTexture();
		}

		if (connection == null) {
			return getCenterTexture();
		} else if ((router != null) && getRouter().isRoutedExit(connection)) {
			return getRoutedTexture(connection);
		} else {
			TextureType texture = getNonRoutedTexture(connection);
			if (this.getUpgradeManager().hasRFPowerSupplierUpgrade() || this.getUpgradeManager().getIC2PowerLevel() > 0) {
				if (texture.fileName.equals(Textures.LOGISTICSPIPE_NOTROUTED_TEXTURE.fileName)) {
					texture = Textures.LOGISTICSPIPE_NOTROUTED_POWERED_TEXTURE;
				} else if (texture.fileName.equals(Textures.LOGISTICSPIPE_LIQUID_TEXTURE.fileName)) {
					texture = Textures.LOGISTICSPIPE_LIQUID_POWERED_TEXTURE;
				} else if (texture.fileName.equals(Textures.LOGISTICSPIPE_POWERED_TEXTURE.fileName)) {
					texture = Textures.LOGISTICSPIPE_POWERED_POWERED_TEXTURE;
				} else if (texture.fileName.equals(Textures.LOGISTICSPIPE_CHASSI_NOTROUTED_TEXTURE.fileName)) {
					texture = Textures.LOGISTICSPIPE_NOTROUTED_POWERED_TEXTURE;
				} else if (texture.fileName.equals(Textures.LOGISTICSPIPE_CHASSI_DIRECTION_TEXTURE.fileName)) {
					texture = Textures.LOGISTICSPIPE_DIRECTION_POWERED_TEXTURE;
				} else {
					System.out.println("Unknown texture to power, :" + texture.fileName);
					System.out.println(this.getClass());
					System.out.println(connection);
				}
			}
			return texture;
		}
	}

	public TextureType getRoutedTexture(Direction connection) {
		if (getRouter().isSubPoweredExit(connection)) {
			return Textures.LOGISTICSPIPE_SUBPOWER_TEXTURE;
		} else {
			return Textures.LOGISTICSPIPE_ROUTED_TEXTURE;
		}
	}

	public TextureType getNonRoutedTexture(Direction connection) {
		if (isPowerProvider(connection)) {
			return Textures.LOGISTICSPIPE_POWERED_TEXTURE;
		}
		return Textures.LOGISTICSPIPE_NOTROUTED_TEXTURE;
	}

	@Override
	public void spawnParticle(Particles particle, int amount) {
		if (!Configs.ENABLE_PARTICLE_FX) {
			return;
		}
		queuedParticles[particle.ordinal()] += amount;
		hasQueuedParticles = true;
	}

	private void spawnParticleTick() {
		if (!hasQueuedParticles) {
			return;
		}
		if (MainProxy.isServer(getWorld())) {
			ArrayList<ParticleCount> tosend = new ArrayList<>(queuedParticles.length);
			for (int i = 0; i < queuedParticles.length; i++) {
				if (queuedParticles[i] > 0) {
					tosend.add(new ParticleCount(Particles.values()[i], queuedParticles[i]));
				}
			}
			MainProxy.sendPacketToAllWatchingChunk(container, PacketHandler.getPacket(ParticleFX.class).setParticles(tosend).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
		} else {
			if (Minecraft.isFancyGraphicsEnabled()) {
				for (int i = 0; i < queuedParticles.length; i++) {
					if (queuedParticles[i] > 0) {
						PipeFXRenderHandler.spawnGenericParticle(Particles.values()[i], getX(), getY(), getZ(), queuedParticles[i]);
					}
				}
			}
		}
		Arrays.fill(queuedParticles, 0);
		hasQueuedParticles = false;
	}

	protected boolean isPowerProvider(Direction ori) {
		TileEntity tilePipe = container.getTile(ori);
		if (tilePipe == null || !container.canPipeConnect(tilePipe, ori)) {
			return false;
		}

		return tilePipe instanceof ILogisticsPowerProvider || tilePipe instanceof ISubSystemPowerProvider;
	}

	@Override
	public void writeToNBT(@Nonnull CompoundNBT tag) {
		super.writeToNBT(tag);

		synchronized (routerIdLock) {
			if (routerId == null || routerId.isEmpty()) {
				if (router != null) {
					routerId = router.getId().toString();
				} else {
					routerId = UUID.randomUUID().toString();
				}
			}
		}
		tag.putString("routerId", routerId);
		tag.putLong("stat_lifetime_sent", stat_lifetime_sent);
		tag.putLong("stat_lifetime_recieved", stat_lifetime_recieved);
		tag.putLong("stat_lifetime_relayed", stat_lifetime_relayed);
		if (getLogisticsModule() != null) {
			getLogisticsModule().writeToNBT(tag);
		}
		CompoundNBT upgradeNBT = new CompoundNBT();
		upgradeManager.writeToNBT(upgradeNBT);
		tag.put("upgradeManager", upgradeNBT);

		CompoundNBT powerNBT = new CompoundNBT();
		powerHandler.writeToNBT(powerNBT);
		if (!powerNBT.isEmpty()) {
			tag.put("powerHandler", powerNBT);
		}

		ListNBT sendqueue = new ListNBT();
		for (Triplet<IRoutedItem, Direction, ItemSendMode> p : _sendQueue) {
			CompoundNBT tagentry = new CompoundNBT();
			CompoundNBT tagItemEntity = new CompoundNBT();
			p.getValue1().writeToNBT(tagItemEntity);
			tagentry.put("ItemEntity", tagItemEntity);
			tagentry.putByte("from", (byte) (p.getValue2().ordinal()));
			tagentry.putByte("mode", (byte) (p.getValue3().ordinal()));
			sendqueue.add(tagentry);
		}
		tag.put("sendqueue", sendqueue);

		for (int i = 0; i < 6; i++) {
			if (signItem[i] != null) {
				tag.putBoolean("PipeSign_" + i, true);
				int signType = -1;
				List<Class<? extends IPipeSign>> typeClasses = ItemPipeSignCreator.signTypes;
				for (int j = 0; j < typeClasses.size(); j++) {
					if (typeClasses.get(j) == signItem[i].getClass()) {
						signType = j;
						break;
					}
				}
				tag.putInt("PipeSign_" + i + "_type", signType);
				CompoundNBT pipeSignTag = new CompoundNBT();
				signItem[i].writeToNBT(pipeSignTag);
				tag.put("PipeSign_" + i + "_tags", pipeSignTag);
			} else {
				tag.putBoolean("PipeSign_" + i, false);
			}
		}
	}

	@Override
	public void readFromNBT(@Nonnull CompoundNBT tag) {
		super.readFromNBT(tag);

		synchronized (routerIdLock) {
			routerId = tag.getString("routerId");
		}

		stat_lifetime_sent = tag.getLong("stat_lifetime_sent");
		stat_lifetime_recieved = tag.getLong("stat_lifetime_recieved");
		stat_lifetime_relayed = tag.getLong("stat_lifetime_relayed");
		if (getLogisticsModule() != null) {
			getLogisticsModule().readFromNBT(tag);
		}
		upgradeManager.readFromNBT(tag.getCompound("upgradeManager"));
		powerHandler.readFromNBT(tag.getCompound("powerHandler"));

		_sendQueue.clear();
		ListNBT sendqueue = tag.getList("sendqueue", tag.getId());
		for (int i = 0; i < sendqueue.size(); i++) {
			CompoundNBT tagentry = sendqueue.getCompound(i);
			CompoundNBT tagItemEntity = tagentry.getCompound("ItemEntity");
			LPTravelingItemServer item = new LPTravelingItemServer(tagItemEntity);
			Direction from = Direction.values()[tagentry.getByte("from")];
			ItemSendMode mode = ItemSendMode.values()[tagentry.getByte("mode")];
			_sendQueue.add(new Triplet<>(item, from, mode));
		}
		for (int i = 0; i < 6; i++) {
			if (tag.getBoolean("PipeSign_" + i)) {
				int type = tag.getInt("PipeSign_" + i + "_type");
				Class<? extends IPipeSign> typeClass = ItemPipeSignCreator.signTypes.get(type);
				try {
					signItem[i] = typeClass.newInstance();
					signItem[i].init(this, DirectionUtil.getOrientation(i));
					signItem[i].readFromNBT(tag.getCompound("PipeSign_" + i + "_tags"));
				} catch (InstantiationException | IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	@Override
	@Nonnull
	public IRouter getRouter() {
		if (stillNeedReplace) {
			System.out.format("Hey, don't get routers for pipes that aren't ready (%d, %d, %d, '%s')", this.getX(), this.getY(), this.getZ(),
					this.getWorld().getWorldInfo().getWorldName());
			new Throwable().printStackTrace();
		}
		if (router == null) {
			synchronized (routerIdLock) {

				UUID routerIntId = null;
				if (routerId != null && !routerId.isEmpty()) {
					routerIntId = UUID.fromString(routerId);
				}
				router = SimpleServiceLocator.routerManager.getOrCreateRouter(routerIntId, getWorld(), getX(), getY(), getZ());
			}
		}
		return router;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public abstract LogisticsModule getLogisticsModule();

	@Override
	public final boolean blockActivated(PlayerEntity player) {
		if (container == null) return super.blockActivated(player);
		SecuritySettings settings = null;
		if (MainProxy.isServer(player.world)) {
			LogisticsSecurityTileEntity station = SimpleServiceLocator.securityStationManager.getStation(getOriginalUpgradeManager().getSecurityID());
			if (station != null) {
				settings = station.getSecuritySettingsForPlayer(player, true);
			}
		}

		if (MainProxy.isPipeControllerEquipped(player)) {
			if (MainProxy.isServer(player.world)) {
				if (settings == null || settings.openNetworkMonitor) {
					NewGuiHandler.getGui(PipeController.class).setTilePos(container).open(player);
				} else {
					player.sendMessage(new TranslationTextComponent("lp.chat.permissiondenied"));
				}
			}
			return true;
		}

		if (handleClick(player, settings)) {
			return true;
		}

		if (player.getItemStackFromSlot(EquipmentSlotType.MAINHAND).isEmpty()) {
			if (!player.isSneaking()) {
				return false;
			}
			/*
			if (MainProxy.isClient(player.world)) {
				if (!LogisticsHUDRenderer.instance().hasLasers()) {
					MainProxy.sendPacketToServer(PacketHandler.getPacket(RequestRoutingLasersPacket.class).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
				} else {
					LogisticsHUDRenderer.instance().resetLasers();
				}
			}
			*/
			if (LogisticsPipes.isDEBUG()) {
				doDebugStuff(player);
			}
			return true;
		}

		if (player.getItemStackFromSlot(EquipmentSlotType.MAINHAND).getItem() == LPItems.remoteOrderer) {
			if (MainProxy.isServer(player.world)) {
				if (settings == null || settings.openRequest) {
					NetworkHooks.openGui((ServerPlayerEntity) player, new INamedContainerProvider() {

						@Override
						public ITextComponent getDisplayName() {
							return new StringTextComponent("Orderer GUI");
						}

						@Override
						public Container createMenu(int windowId, PlayerInventory playerInv, PlayerEntity player) {
							final NormalGuiOrderer orderer = new NormalGuiOrderer(getX(), getY(), getZ(),
									getWorld().dimension, player);
							return orderer.getContainer();
						}
					});
				} else {
					player.sendMessage(new TranslationTextComponent("lp.chat.permissiondenied"));
				}
			}
			return true;
		}

		if (SimpleServiceLocator.configToolHandler.canWrench(player, player.getItemStackFromSlot(EquipmentSlotType.MAINHAND), container)) {
			if (MainProxy.isServer(player.world)) {
				if (settings == null || settings.openGui) {
					final LogisticsModule module = getLogisticsModule();
					if (module instanceof Gui) {
						Gui.getPipeGuiProvider((Gui) module).setTilePos(container).open(player);
					} else {
						onWrenchClicked(player);
					}
				} else {
					player.sendMessage(new TranslationTextComponent("lp.chat.permissiondenied"));
				}
			}
			SimpleServiceLocator.configToolHandler.wrenchUsed(player, player.getItemStackFromSlot(EquipmentSlotType.MAINHAND), container);
			return true;
		}

		if (!(player.isSneaking()) && getOriginalUpgradeManager().tryIserting(getWorld(), player)) {
			return true;
		}

		return super.blockActivated(player);
	}

	protected boolean handleClick(PlayerEntity player, SecuritySettings settings) {
		return false;
	}

	public void refreshRender(boolean spawnPart) {
		container.scheduleRenderUpdate();
		if (spawnPart) {
			spawnParticle(Particles.GreenParticle, 3);
		}
	}

	public void refreshConnectionAndRender(boolean spawnPart) {
		container.scheduleNeighborChange();
		if (spawnPart) {
			spawnParticle(Particles.GreenParticle, 3);
		}
	}

	/* ITrackStatistics */

	@Override
	public void recievedItem(int count) {
		stat_session_recieved += count;
		stat_lifetime_recieved += count;
		updateStats();
	}

	@Override
	public void relayedItem(int count) {
		stat_session_relayed += count;
		stat_lifetime_relayed += count;
		updateStats();
	}

	@Override
	public void playerStartWatching(PlayerEntity player, int mode) {
		if (mode == 0) {
			watchers.add(player);
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(StatUpdate.class).setPipe(this), player);
		}
	}

	@Override
	public void playerStopWatching(PlayerEntity player, int mode) {
		if (mode == 0) {
			watchers.remove(player);
		}
	}

	public void updateStats() {
		if (watchers.size() > 0) {
			MainProxy.sendToPlayerList(PacketHandler.getPacket(StatUpdate.class).setPipe(this), watchers);
		}
	}

	@Override
	public void itemCouldNotBeSend(ItemIdentifierStack item, IAdditionalTargetInformation info) {
		if (this instanceof IRequireReliableTransport) {
			((IRequireReliableTransport) this).itemLost(item, info);
		}
	}

	public boolean isLockedExit(Direction orientation) {
		return false;
	}

	public boolean logisitcsIsPipeConnected(TileEntity tile, Direction dir) {
		return false;
	}

	@Override
	public final boolean canPipeConnect(TileEntity tile, Direction dir) {
		return canPipeConnect(tile, dir, false);
	}

	@Override
	public final boolean canPipeConnect(TileEntity tile, Direction dir, boolean ignoreSystemDisconnection) {
		Direction side = OrientationsUtil.getOrientationOfTilewithTile(container, tile);
		if (isSideBlocked(side, ignoreSystemDisconnection)) {
			return false;
		}
		return (super.canPipeConnect(tile, dir) || logisitcsIsPipeConnected(tile, dir));
	}

	@Override
	public final boolean isSideBlocked(Direction side, boolean ignoreSystemDisconnection) {
		if (getUpgradeManager().isSideDisconnected(side)) {
			return true;
		}
		return !stillNeedReplace && getRouter().isSideDisconnected(side) && !ignoreSystemDisconnection;
	}

	public void connectionUpdate() {
		if (container != null && !stillNeedReplace) {
			if (MainProxy.isClient(getWorld())) throw new IllegalStateException("Wont do connectionUpdate on client-side");
			container.scheduleNeighborChange();
			BlockState state = getWorld().getBlockState(getPos());
			getWorld().notifyNeighborsOfStateChange(getPos(), state.getBlock(), true);
		}
	}

	public UUID getSecurityID() {
		return getOriginalUpgradeManager().getSecurityID();
	}

	public void insetSecurityID(UUID id) {
		getOriginalUpgradeManager().insetSecurityID(id);
	}

	public List<Pair<ILogisticsPowerProvider, List<IFilter>>> getRoutedPowerProviders() {
		if (MainProxy.isClient(getWorld())) {
			return null;
		}
		if (stillNeedReplace) {
			return null;
		}
		return getRouter().getPowerProvider();
	}

	/* Power System */

	@Override
	public boolean useEnergy(int amount) {
		return useEnergy(amount, null, true);
	}

	public boolean useEnergy(int amount, boolean sparkles) {
		return useEnergy(amount, null, sparkles);
	}

	@Override
	public boolean canUseEnergy(int amount) {
		return canUseEnergy(amount, null);
	}

	@Override
	public boolean canUseEnergy(int amount, List<Object> providersToIgnore) {
		if (MainProxy.isClient(getWorld())) {
			return false;
		}
		if (Configs.LOGISTICS_POWER_USAGE_DISABLED) {
			return true;
		}
		if (amount == 0) {
			return true;
		}
		if (providersToIgnore != null && providersToIgnore.contains(this)) {
			return false;
		}
		List<Pair<ILogisticsPowerProvider, List<IFilter>>> list = getRoutedPowerProviders();
		if (list == null) {
			return false;
		}
		outer:
		for (Pair<ILogisticsPowerProvider, List<IFilter>> provider : list) {
			for (IFilter filter : provider.getValue2()) {
				if (filter.blockPower()) {
					continue outer;
				}
			}
			if (provider.getValue1().canUseEnergy(amount, providersToIgnore)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean useEnergy(int amount, List<Object> providersToIgnore) {
		return useEnergy(amount, providersToIgnore, false);
	}

	private boolean useEnergy(int amount, List<Object> providersToIgnore, boolean sparkles) {
		if (MainProxy.isClient(getWorld())) {
			return false;
		}
		if (Configs.LOGISTICS_POWER_USAGE_DISABLED) {
			return true;
		}
		if (amount == 0) {
			return true;
		}
		if (providersToIgnore == null) {
			providersToIgnore = new ArrayList<>();
		}
		if (providersToIgnore.contains(this)) {
			return false;
		}
		providersToIgnore.add(this);
		List<Pair<ILogisticsPowerProvider, List<IFilter>>> list = getRoutedPowerProviders();
		if (list == null) {
			return false;
		}
		outer:
		for (Pair<ILogisticsPowerProvider, List<IFilter>> provider : list) {
			for (IFilter filter : provider.getValue2()) {
				if (filter.blockPower()) {
					continue outer;
				}
			}
			if (provider.getValue1().canUseEnergy(amount, providersToIgnore)) {
				if (provider.getValue1().useEnergy(amount, providersToIgnore)) {
					if (sparkles) {
						int particlecount = amount;
						if (particlecount > 10) {
							particlecount = 10;
						}
						spawnParticle(Particles.GoldParticle, particlecount);
					}
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void queueEvent(String event, Object[] arguments) {
		if (container != null) {
			container.queueEvent(event, arguments);
		}
	}

	public boolean stillNeedReplace() {
		return stillNeedReplace;
	}

	public boolean initialInit() {
		return _initialInit;
	}

	@Override
	public int compareTo(@Nonnull IRequestItems other) {
		return Integer.compare(getID(), other.getID());
	}

	@Override
	public int getID() {
		return getRouter().getSimpleID();
	}

	public void collectSpecificInterests(@Nonnull Collection<ItemIdentifier> itemidCollection) {}

	public boolean hasGenericInterests() {
		return false;
	}

	@Nullable
	public ISecurityProvider getSecurityProvider() {
		return SimpleServiceLocator.securityStationManager.getStation(getOriginalUpgradeManager().getSecurityID());
	}

	public boolean canBeDestroyedByPlayer(PlayerEntity player) {
		LogisticsSecurityTileEntity station = SimpleServiceLocator.securityStationManager.getStation(getOriginalUpgradeManager().getSecurityID());
		return station == null || station.getSecuritySettingsForPlayer(player, true).removePipes;
	}

	@Override
	public boolean canBeDestroyed() {
		ISecurityProvider sec = getSecurityProvider();
		return sec == null || sec.canAutomatedDestroy();
	}

	public void setDestroyByPlayer() {
		destroyByPlayer = true;
	}

	@Override
	public boolean destroyByPlayer() {
		return destroyByPlayer;
	}

	@Override
	public boolean preventRemove() {
		return preventRemove;
	}

	@CCSecurtiyCheck
	public void checkCCAccess() throws PermissionException {
		ISecurityProvider sec = getSecurityProvider();
		if (sec != null) {
			int id = -1;
			if (container != null) {
				id = container.getLastCCID();
			}
			if (!sec.getAllowCC(id)) {
				throw new PermissionException();
			}
		}
	}

	public void queueUnroutedItemInformation(ItemIdentifierStack item, ItemRoutingInformation information) {
		if (item != null) {
			synchronized (queuedDataForUnroutedItems) {
				Queue<Pair<Integer, ItemRoutingInformation>> queue = queuedDataForUnroutedItems.computeIfAbsent(item.getItem(), k -> new LinkedList<>());
				queue.add(new Pair<>(item.getStackSize(), information));
			}
		}
	}

	public ItemRoutingInformation getQueuedForItemStack(ItemIdentifierStack item) {
		synchronized (queuedDataForUnroutedItems) {
			Queue<Pair<Integer, ItemRoutingInformation>> queue = queuedDataForUnroutedItems.get(item.getItem());
			if (queue == null || queue.isEmpty()) {
				return null;
			}

			Pair<Integer, ItemRoutingInformation> pair = queue.peek();
			int wantItem = pair.getValue1();

			if (wantItem <= item.getStackSize()) {
				if (queue.remove() != pair) {
					LogisticsPipes.getLOGGER().fatal("Item queue mismatch");
					return null;
				}
				if (queue.isEmpty()) {
					queuedDataForUnroutedItems.remove(item.getItem());
				}
				item.setStackSize(wantItem);
				return pair.getValue2();
			}
		}
		return null;
	}

	/**
	 * used as a distance offset when deciding which pipe to use NOTE: called
	 * very regularly, returning a pre-calculated int is probably appropriate.
	 */
	public double getLoadFactor() {
		return 0.0;
	}

	public void notifyOfItemArival(ItemRoutingInformation information) {
		_inTransitToMe.remove(information);
		if (this instanceof IRequireReliableTransport) {
			((IRequireReliableTransport) this).itemArrived(information.getItem(), information.targetInfo);
		}
		if (this instanceof IRequireReliableFluidTransport) {
			ItemIdentifierStack stack = information.getItem();
			if (stack.getItem().isFluidContainer()) {
				FluidIdentifierStack liquid = SimpleServiceLocator.logisticsFluidManager.getFluidFromContainer(stack);
				if (liquid != null) {
					((IRequireReliableFluidTransport) this).liquidArrived(liquid.getFluid(), liquid.getAmount());
				}
			}
		}
	}

	@Override
	public int countOnRoute(ItemIdentifier it) {
		int count = 0;
		for (ItemRoutingInformation next : _inTransitToMe) {
			if (next.getItem().getItem().equals(it)) {
				count += next.getItem().getStackSize();
			}
		}
		return count;
	}

	@Override
	public void markDirty() {
		container.markDirty();
	}

	@Override
	public final int getIconIndex(Direction connection) {
		TextureType texture = getTextureType(connection);
		if (_textureBufferPowered) {
			return texture.powered;
		} else if (Configs.LOGISTICS_POWER_USAGE_DISABLED) {
			return texture.normal;
		} else {
			return texture.unpowered;
		}
	}

	public void addCrashReport(CrashReportCategory crashReportCategory) {
		addRouterCrashReport(crashReportCategory);
		crashReportCategory.func_71507_a("stillNeedReplace", stillNeedReplace);
	}

	protected void addRouterCrashReport(CrashReportCategory crashReportCategory) {
		crashReportCategory.func_71507_a("Router", getRouter().toString());
	}

	public void onWrenchClicked(PlayerEntity player) {
		//do nothing, every pipe with a GUI should either have a LogisticsGuiModule or override this method
	}

	public void handleRFPowerArival(double toSend) {
		powerHandler.addRFPower(toSend);
	}

	public void handleIC2PowerArival(double toSend) {
		powerHandler.addIC2Power(toSend);
	}

	/* ISendRoutedItem */

	@Override
	public IRoutedItem sendStack(@Nonnull ItemStack stack, Pair<Integer, SinkReply> reply, ItemSendMode mode, Direction direction) {
		IRoutedItem itemToSend = SimpleServiceLocator.routedItemHelper.createNewTravelItem(stack);
		itemToSend.setDestination(reply.getValue1());
		if (reply.getValue2().isPassive) {
			if (reply.getValue2().isDefault) {
				itemToSend.setTransportMode(TransportMode.Default);
			} else {
				itemToSend.setTransportMode(TransportMode.Passive);
			}
		}
		itemToSend.setAdditionalTargetInformation(reply.getValue2().addInfo);
		queueRoutedItem(itemToSend, direction, mode);
		return itemToSend;
	}

	@Override
	public IRoutedItem sendStack(@Nonnull ItemStack stack, int destination, ItemSendMode mode, IAdditionalTargetInformation info, Direction direction) {
		IRoutedItem itemToSend = SimpleServiceLocator.routedItemHelper.createNewTravelItem(stack);
		itemToSend.setDestination(destination);
		itemToSend.setTransportMode(TransportMode.Active);
		itemToSend.setAdditionalTargetInformation(info);
		queueRoutedItem(itemToSend, direction, mode);
		return itemToSend;
	}

	@Override
	public LogisticsItemOrderManager getItemOrderManager() {
		_orderItemManager = _orderItemManager != null ? _orderItemManager : new LogisticsItemOrderManager(this);
		return _orderItemManager;
	}

	public LogisticsOrderManager<?, ?> getOrderManager() {
		return getItemOrderManager();
	}

	public void addPipeSign(Direction dir, IPipeSign type, PlayerEntity player) {
		if (dir.ordinal() < 6) {
			if (signItem[dir.ordinal()] == null) {
				signItem[dir.ordinal()] = type;
				signItem[dir.ordinal()].init(this, dir);
			}
			if (container != null) {
				sendSignData(player, true);
				refreshRender(false);
			}
		}
	}

	public void sendSignData(PlayerEntity player, boolean sendToAll) {
		List<Integer> types = new ArrayList<>();
		for (int i = 0; i < 6; i++) {
			if (signItem[i] == null) {
				types.add(-1);
			} else {
				List<Class<? extends IPipeSign>> typeClasses = ItemPipeSignCreator.signTypes;
				for (int j = 0; j < typeClasses.size(); j++) {
					if (typeClasses.get(j) == signItem[i].getClass()) {
						types.add(j);
						break;
					}
				}
			}
		}
		ModernPacket packet = PacketHandler.getPacket(PipeSignTypes.class).setTypes(types).setTilePos(container);
		if (sendToAll) {
			MainProxy.sendPacketToAllWatchingChunk(container, packet);
		}
		MainProxy.sendPacketToPlayer(packet, player);
		for (int i = 0; i < 6; i++) {
			if (signItem[i] != null) {
				packet = signItem[i].getPacket();
				if (packet != null) {
					MainProxy.sendPacketToAllWatchingChunk(container, packet);
					MainProxy.sendPacketToPlayer(packet, player);
				}
			}
		}
	}

	public void removePipeSign(Direction dir, PlayerEntity player) {
		if (dir.ordinal() < 6) {
			signItem[dir.ordinal()] = null;
		}
		sendSignData(player, true);
		refreshRender(false);
	}

	public boolean hasPipeSign(Direction dir) {
		if (dir.ordinal() < 6) {
			return signItem[dir.ordinal()] != null;
		}
		return false;
	}

	public void activatePipeSign(Direction dir, PlayerEntity player) {
		if (dir.ordinal() < 6) {
			if (signItem[dir.ordinal()] != null) {
				signItem[dir.ordinal()].activate(player);
			}
		}
	}

	public List<Pair<Direction, IPipeSign>> getPipeSigns() {
		List<Pair<Direction, IPipeSign>> list = new ArrayList<>();
		for (int i = 0; i < 6; i++) {
			if (signItem[i] != null) {
				list.add(new Pair<>(DirectionUtil.getOrientation(i), signItem[i]));
			}
		}
		return list;
	}

	public void handleSignPacket(List<Integer> types) {
		if (!MainProxy.isClient(getWorld())) {
			return;
		}
		for (int i = 0; i < 6; i++) {
			int integer = types.get(i);
			if (integer >= 0) {
				Class<? extends IPipeSign> type = ItemPipeSignCreator.signTypes.get(integer);
				if (signItem[i] == null || signItem[i].getClass() != type) {
					try {
						signItem[i] = type.newInstance();
						signItem[i].init(this, DirectionUtil.getOrientation(i));
					} catch (InstantiationException | IllegalAccessException e) {
						throw new RuntimeException(e);
					}
				}
			} else {
				signItem[i] = null;
			}
		}
	}

	@Nullable
	public IPipeSign getPipeSign(@Nullable Direction dir) {
		if (dir == null) return null;
		return signItem[dir.ordinal()];
	}

	@Override
	public void writeData(LPDataOutput output) {
		output.writeBoolean(isOpaque());
	}

	@Override
	public void readData(LPDataInput input) {
		isOpaqueClientSide = input.readBoolean();
	}

	@Override
	public boolean isOpaque() {
		if (MainProxy.isClient(getWorld())) {
			return Configs.OPAQUE || isOpaqueClientSide;
		} else {
			return Configs.OPAQUE || this.getUpgradeManager().isOpaque();
		}
	}

	@Override
	public void addStatusInformation(List<StatusEntry> status) {
		StatusEntry entry = new StatusEntry();
		entry.name = "Send Queue";
		entry.subEntry = new ArrayList<>();
		for (Triplet<IRoutedItem, Direction, ItemSendMode> part : _sendQueue) {
			StatusEntry subEntry = new StatusEntry();
			subEntry.name = part.toString();
			entry.subEntry.add(subEntry);
		}
		status.add(entry);
		entry = new StatusEntry();
		entry.name = "In Transit To Me";
		entry.subEntry = new ArrayList<>();
		for (ItemRoutingInformation part : _inTransitToMe) {
			StatusEntry subEntry = new StatusEntry();
			subEntry.name = part.toString();
			entry.subEntry.add(subEntry);
		}
		status.add(entry);
	}

	@Override
	@Nonnull
	public DebugLogController getDebug() {
		return debug;
	}

	@Override
	public void setPreventRemove(boolean flag) {
		preventRemove = flag;
	}

	@Override
	public boolean isRoutedPipe() {
		return true;
	}

	@Override
	public double getDistanceTo(int destinationint, Direction ignore, ItemIdentifier ident, boolean isActive, double traveled, double max,
			List<DoubleCoordinates> visited) {
		if (!stillNeedReplace) {
			if (getRouterId() == destinationint) {
				return 0;
			}
			ExitRoute route = getRouter().getExitFor(destinationint, isActive, ident);
			if (route != null && route.exitOrientation != ignore) {
				if (route.distanceToDestination + traveled >= max) {
					return Integer.MAX_VALUE;
				}
				return route.distanceToDestination;
			}
		}
		return Integer.MAX_VALUE;
	}

	protected void triggerConnectionCheck() {
		recheckConnections = true;
	}

	@Override
	public CacheHolder getCacheHolder() {
		if (cacheHolder == null) {
			if (container instanceof ILPTEInformation && ((ILPTEInformation) container).getObject() != null) {
				cacheHolder = ((ILPTEInformation) container).getObject().getCacheHolder();
			} else {
				cacheHolder = new CacheHolder();
			}
		}
		return cacheHolder;
	}

	@Override
	public IHighlightPlacementRenderer getHighlightRenderer() {
		return LogisticsRenderPipe.secondRenderer;
	}

	public enum ItemSendMode {
		Normal,
		Fast
	}
}
