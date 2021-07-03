package logisticspipes.pipes.basic;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullSupplier;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.items.IItemHandler;

import dan200.computercraft.api.peripheral.IComputerAccess;
import lombok.Getter;
import org.apache.logging.log4j.Level;

import logisticspipes.LPConstants;
import logisticspipes.LogisticsPipes;
import logisticspipes.api.ILPPipe;
import logisticspipes.api.ILPPipeTile;
import logisticspipes.asm.ModDependentField;
import logisticspipes.asm.ModDependentMethod;
import logisticspipes.blocks.LogisticsSolidTileEntity;
import logisticspipes.interfaces.IClientState;
import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.logic.LogicController;
import logisticspipes.logic.interfaces.ILogicControllerTile;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.packets.block.PipeSolidSideCheck;
import logisticspipes.network.packets.pipe.PipeTileStatePacket;
import logisticspipes.pipes.PipeItemsFirewall;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.opencomputers.IOCTile;
import logisticspipes.proxy.opencomputers.asm.BaseWrapperClass;
import logisticspipes.renderer.IIconProvider;
import logisticspipes.renderer.LogisticsTileRenderController;
import logisticspipes.renderer.state.PipeRenderState;
import logisticspipes.routing.pathfinder.IPipeInformationProvider;
import logisticspipes.transport.LPTravelingItem;
import logisticspipes.transport.PipeFluidTransportLogistics;
import logisticspipes.utils.LPPositionSet;
import logisticspipes.utils.OrientationsUtil;
import logisticspipes.utils.ReflectionHelper;
import logisticspipes.utils.StackTraceUtil;
import logisticspipes.utils.StackTraceUtil.Info;
import logisticspipes.utils.TileBuffer;
import logisticspipes.utils.item.ItemIdentifier;
import network.rs485.logisticspipes.connection.ConnectionType;
import network.rs485.logisticspipes.connection.PipeInventoryConnectionChecker;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;
import network.rs485.logisticspipes.world.DoubleCoordinates;
import network.rs485.logisticspipes.world.DoubleCoordinatesType;
import network.rs485.logisticspipes.world.WorldCoordinatesWrapper;

public class LogisticsTileGenericPipe extends TileEntity implements ITickableTileEntity, IOCTile, ILPPipeTile, IPipeInformationProvider, ILogicControllerTile {

	public static PipeInventoryConnectionChecker pipeInventoryConnectionChecker = new PipeInventoryConnectionChecker();
	public final PipeRenderState renderState;
	public final CoreState coreState = new CoreState();
	@ModDependentField(modId = LPConstants.computerCraftModID)
	public HashMap<IComputerAccess, Direction> connections;
	@ModDependentField(modId = LPConstants.computerCraftModID)
	public IComputerAccess currentPC;
	public int statePacketId = 0;
	public Object OPENPERIPHERAL_IGNORE; //Tell OpenPeripheral to ignore this class
	public Set<DoubleCoordinates> subMultiBlock = new HashSet<>();
	public boolean[] turtleConnect = new boolean[7];
	public LogicController logicController = new LogicController();
	public boolean[] pipeConnectionsBuffer = new boolean[6];
	public boolean[] pipeBCConnectionsBuffer = new boolean[6];
	public boolean[] pipeTDConnectionsBuffer = new boolean[6];
	public CoreUnroutedPipe pipe;
	private LogisticsTileRenderController renderController;
	private boolean addedToNetwork = false;
	private boolean sendInitPacket = true;
	@Getter
	private boolean initialized = false;
	private boolean deletePipe = false;
	private TileBuffer[] tileBuffer;
	private boolean sendClientUpdate = false;
	private boolean blockNeighborChange = false;
	private boolean refreshRenderState = false;
	private boolean pipeBound = false;
	private AxisAlignedBB renderBox;
	private EnumMap<Direction, ItemInsertionHandler> itemInsertionHandlers;

	public LogisticsTileGenericPipe() {
		if (SimpleServiceLocator.ccProxy.isCC()) {
			connections = new HashMap<>();
		}
		SimpleServiceLocator.openComputersProxy.initLogisticsTileGenericPipe(this);
		bcCapProvider = SimpleServiceLocator.buildCraftProxy.getIBCPipeCapabilityProvider(this);
		imcmpltgpCompanion = SimpleServiceLocator.mcmpProxy.createMCMPCompanionFor(this);
		itemInsertionHandlers = new EnumMap<>(Direction.class);
		Arrays.stream(Direction.values()).forEach(face -> itemInsertionHandlers.put(face, new ItemInsertionHandler(this, face)));
		ItemInsertionHandler itemInsertionHandlerNull = new ItemInsertionHandler(this, null);
		renderState = new PipeRenderState();
	}

	@Override
	public void invalidate() {
		if (pipe == null) {
			tileEntityInvalid = true;
			initialized = false;
			tileBuffer = null;
			super.invalidate();
		} else if (!pipe.preventRemove()) {
			tileEntityInvalid = true;
			initialized = false;
			tileBuffer = null;
			pipe.invalidate();
			super.invalidate();
			SimpleServiceLocator.openComputersProxy.handleInvalidate(this);
			tdPart.invalidate();
		}
	}

	@Override
	public void validate() {
		super.validate();
		initialized = false;
		tileBuffer = null;
		bindPipe();
		if (pipe != null) {
			pipe.validate();
		}
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		if (pipe != null) {
			pipe.onChunkUnloaded();
		}
		SimpleServiceLocator.openComputersProxy.handleChunkUnload(this);
		tdPart.onChunkUnloaded();
	}

	@Override
	public void update() {
		imcmpltgpCompanion.update();
		final Info superDebug = StackTraceUtil.addSuperTraceInformation(() -> "Time: " + getWorld().getWorldTime());
		final Info debug = StackTraceUtil.addTraceInformation(() -> "(" + getX() + ", " + getY() + ", " + getZ() + ")", superDebug);
		if (sendInitPacket && MainProxy.isServer(getWorld())) {
			sendInitPacket = false;
			getRenderController().sendInit();
		}
		if (!world.isRemote) {
			if (deletePipe) {
				world.setBlockToAir(getPos());
			}

			if (pipe == null) {
				debug.end();
				return;
			}

			if (!initialized) {
				initialize(pipe);
			}
		}

		if (!LogisticsBlockGenericPipe.isValid(pipe)) {
			debug.end();
			return;
		}

		pipe.updateEntity();

		if (world.isRemote) {
			debug.end();
			return;
		}

		if (blockNeighborChange) {
			computeConnections();
			pipe.onNeighborBlockChange();
			blockNeighborChange = false;
			refreshRenderState = true;

			if (MainProxy.isServer(world)) {
				MainProxy.sendPacketToAllWatchingChunk(this, PacketHandler.getPacket(PipeSolidSideCheck.class).setTilePos(this));
			}
		}

		//Sideblocks need to be checked before this
		//Network needs to be after this

		if (refreshRenderState) {
			refreshRenderState();

			if (renderState.isDirty()) {
				renderState.clean();
				sendUpdateToClient();
			}

			refreshRenderState = false;
		}

		if (sendClientUpdate) {
			sendClientUpdate = false;
			MainProxy.sendPacketToAllWatchingChunk(this, getLPDescriptionPacket());
		}

		getRenderController().onUpdate();
		if (!addedToNetwork) {
			addedToNetwork = true;
			SimpleServiceLocator.openComputersProxy.addToNetwork(this);
		}
		debug.end();
	}

	private void refreshRenderState() {
		// Pipe connections;
		for (Direction o : Direction.values()) {
			renderState.pipeConnectionMatrix.setConnected(o, pipeConnectionsBuffer[o.ordinal()]);
			renderState.pipeConnectionMatrix.setBCConnected(o, pipeBCConnectionsBuffer[o.ordinal()]);
			renderState.pipeConnectionMatrix.setTDConnected(o, pipeTDConnectionsBuffer[o.ordinal()]);
		}
		// Pipe Textures
		for (int i = 0; i < 7; i++) {
			Direction o = Direction.getFront(i);
			renderState.textureMatrix.setIconIndex(o, pipe.getIconIndex(o));
		}
		//New Pipe Texture States
		renderState.textureMatrix.refreshStates(pipe);
	}

	@Override
	public boolean isMultipartAllowedInPipe() {
		return !isMultiBlock() && (pipe == null || pipe.isMultipartAllowedInPipe());
	}

	@Nonnull
	@Override
	public CompoundNBT getUpdateTag() {
		sendInitPacket = true;
		CompoundNBT nbt = super.getUpdateTag();
		try {
			PacketHandler.addPacketToNBT(getLPDescriptionPacket(), nbt);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return nbt;
	}

	@Override
	public TileEntity getTileEntity() {
		return null;
	}

	@Override
	public void deserializeNBT(CompoundNBT nbt) {

	}

	@Override
	public CompoundNBT serializeNBT() {
		return null;
	}

	@Override
	public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {

	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void handleUpdateTag(@Nonnull CompoundNBT tag) {
		PacketHandler.queueAndRemovePacketFromNBT(tag);
		super.handleUpdateTag(tag);
	}

	@Override
	public SUpdateTileEntityPacket getUpdatePacket() {
		CompoundNBT nbt = new CompoundNBT();
		SUpdateTileEntityPacket superPacket = super.getUpdatePacket();
		if (superPacket != null) {
			nbt.setTag("LogisticsPipes:SuperUpdatePacket", ReflectionHelper.getPrivateField(SUpdateTileEntityPacket.class, superPacket, "nbt", "field_148860_e"));
		}
		try {
			PacketHandler.addPacketToNBT(getLPDescriptionPacket(), nbt);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new SUpdateTileEntityPacket(getPos(), 1, nbt);
	}

	@Override
	public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket packet) {
		PacketHandler.queueAndRemovePacketFromNBT(packet.getNbtCompound());
		if (packet.getNbtCompound().contains("LogisticsPipes:SuperUpdatePacket")) {
			super.onDataPacket(net, new SUpdateTileEntityPacket(getPos(), 0, packet.getNbtCompound().getCompound("LogisticsPipes:SuperUpdatePacket")));
		}
	}

	@Override
	public void addInfoToCrashReport(@Nonnull CrashReportCategory reportCategory) {
		try {
			super.addInfoToCrashReport(reportCategory);
		} catch (Exception e) {
			if (LogisticsPipes.isDEBUG()) {
				e.printStackTrace();
			}
		}
		reportCategory.func_71507_a("LP-Version", LogisticsPipes.getVersionString());
		if (pipe != null) {
			reportCategory.func_71507_a("Pipe", pipe.getClass().getCanonicalName());
			if (pipe.transport != null) {
				reportCategory.func_71507_a("Transport", pipe.transport.getClass().getCanonicalName());
			} else {
				reportCategory.func_71507_a("Transport", "null");
			}

			if (pipe instanceof CoreRoutedPipe) {
				try {
					((CoreRoutedPipe) pipe).addCrashReport(reportCategory);
				} catch (Exception e) {
					reportCategory.addCrashSectionThrowable("Internal LogisticsPipes Error", e);
				}
			}
		}
	}

	public void scheduleNeighborChange() {
		DistExecutor.runWhenOn(Dist.DEDICATED_SERVER, () -> () -> pipe.triggerConnectionCheck());
		blockNeighborChange = true;
		boolean[] connected = new boolean[6];
		new WorldCoordinatesWrapper(this).allNeighborTileEntities().stream().filter(adjacent -> SimpleServiceLocator.ccProxy.isTurtle(adjacent.getTileEntity())).forEach(adjacent -> connected[adjacent.getDirection().ordinal()] = true);
		for (int i = 0; i < 6; i++) {
			if (!connected[i]) {
				turtleConnect[i] = false;
			}
		}
	}

	/* IPipeInformationProvider */

	@Override
	public CompoundNBT write(CompoundNBT tag) {
		tag = super.write(tag);

		/*
		for (int i = 0; i < Direction.values().length; i++) {
			final String key = "redstoneInputSide[" + i + "]";
			nbt.setByte(key, (byte) redstoneInputSide[i]);
		}
		 */

		if (pipe != null) {
			tag.putInt("pipeId", Registry.ITEM.getId(pipe.item));
			pipe.writeToNBT(tag);
		} else {
			tag.putInt("pipeId", coreState.pipeId);
		}

		CompoundNBT logicNBT = new CompoundNBT();
		logicController.writeToNBT(logicNBT);
		tag.put("logicController", logicNBT);
		return tag;
	}

	@Override
	public void read(CompoundNBT tag) {if (pipe != null) {
		StackTraceElement[] trace = Thread.currentThread().getStackTrace();
		if (trace.length > 2 && trace[2].getMethodName().equals("handle") && trace[2].getClassName().equals("com.xcompwiz.lookingglass.network.packet.PacketTileEntityNBT")) {
			System.out.println("Prevented false data injection by LookingGlass");
			return;
		}
	}
		super.read(tag);

		if (!tag.contains("pipeId") && MainProxy.isClient(world)) return;

		coreState.pipeId = tag.getInt("pipeId");
		pipe = LogisticsBlockGenericPipe.createPipe(Item.getItemById(coreState.pipeId));
		bindPipe();

		if (pipe != null) {
			pipe.readFromNBT(tag);
			pipe.finishInit();
		} else {
			LogisticsPipes.getLOGGER().warn("Pipe failed to load from NBT at " + getPos());
			deletePipe = true;
		}

		logicController.readFromNBT(tag.getCompound("logicController"));
	}

	public boolean canPipeConnect(TileEntity with, Direction side) {
		if (MainProxy.isClient(world)) {
			//XXX why is this ever called client side, its not *used* for anything.
			return false;
		}
		if (with == null) {
			return false;
		}

		if (!LogisticsBlockGenericPipe.isValid(pipe)) {
			return false;
		}

		if (with instanceof LogisticsTileGenericPipe) {
			CoreUnroutedPipe otherPipe = ((LogisticsTileGenericPipe) with).pipe;

			if (!(LogisticsBlockGenericPipe.isValid(otherPipe))) {
				return false;
			}

			if (!(otherPipe.canPipeConnect(this, side.getOpposite()))) {
				return false;
			}

		}
		return pipe.canPipeConnect(with, side);
	}

	@Nonnull
	public ItemStack insertItem(Direction from, @Nonnull ItemStack stack) {
		int used = injectItem(stack, true, from);
		if (used == stack.getCount()) {
			return ItemStack.EMPTY;
		} else {
			stack = stack.copy();
			stack.shrink(used);
			return stack;
		}
	}

	public void addLaser(Direction dir, float length, int color, boolean reverse, boolean renderBall) {
		getRenderController().addLaser(dir, length, color, reverse, renderBall);
	}

	public void removeLaser(Direction dir, int color, boolean isBall) {
		getRenderController().removeLaser(dir, color, isBall);
	}

	public LogisticsTileRenderController getRenderController() {
		if (renderController == null) {
			renderController = new LogisticsTileRenderController(this);
		}
		return renderController;
	}

	@Override
	public boolean isCorrect(ConnectionType type) {
		return true;
	}

	@Override
	public int getX() {
		return getPos().getX();
	}

	@Override
	public int getY() {
		return getPos().getY();
	}

	@Override
	public int getZ() {
		return getPos().getZ();
	}

	@Override
	public boolean isRouterInitialized() {
		return isInitialized() && (!isRoutingPipe() || !getRoutingPipe().stillNeedReplace());
	}

	@Override
	public boolean isRoutingPipe() {
		return pipe instanceof CoreRoutedPipe;
	}

	@Override
	public CoreRoutedPipe getRoutingPipe() {
		if (pipe instanceof CoreRoutedPipe) {
			return (CoreRoutedPipe) pipe;
		}
		throw new RuntimeException("This is no routing pipe");
	}

	@Override
	public boolean isFirewallPipe() {
		return pipe instanceof PipeItemsFirewall;
	}

	@Override
	public IFilter getFirewallFilter() {
		if (pipe instanceof PipeItemsFirewall) {
			return ((PipeItemsFirewall) pipe).getFilter();
		}
		throw new RuntimeException("This is no firewall pipe");
	}

	public TileEntity getTile() {
		return this;
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
	public boolean isOutputClosed(Direction direction) {
		return false;
	}

	@Override
	public boolean isItemPipe() {
		return true;
	}

	@Override
	public boolean isFluidPipe() {
		return pipe != null && pipe.isFluidPipe();
	}

	@Override
	public boolean isPowerPipe() {
		return false;
	}

	@Override
	public boolean canConnect(TileEntity to, Direction direction, boolean flag) {
		if (pipe == null) {
			return false;
		}
		return pipe.canPipeConnect(to, direction, flag);
	}

	@Override
	public double getDistance() {
		if (this.pipe != null && this.pipe.transport != null) {
			return this.pipe.transport.getPipeLength();
		}
		return 1;
	}

	@Override
	public double getDistanceWeight() {
		if (this.pipe != null && this.pipe.transport != null) {
			return this.pipe.transport.getDistanceWeight();
		}
		return 1;
	}

	public int injectItem(@Nonnull ItemStack payload, boolean doAdd, Direction from) {
		if (LogisticsBlockGenericPipe.isValid(pipe) && pipe.transport != null && isPipeConnectedCached(from)) {
			if (doAdd && MainProxy.isServer(getWorld())) {
				ItemStack leftStack = payload.copy();
				int lastIterLeft;
				do {
					lastIterLeft = leftStack.getCount();
					LPTravelingItem.LPTravelingItemServer travelingItem = SimpleServiceLocator.routedItemHelper.createNewTravelItem(leftStack);
					leftStack.setCount(pipe.transport.injectItem(travelingItem, from.getOpposite()));
				} while (leftStack.getCount() != lastIterLeft && leftStack.getCount() != 0);
				return payload.getCount() - leftStack.getCount();
			}
		}
		return 0;
	}

	public boolean isPipeConnectedCached(Direction side) {
		if (MainProxy.isClient(this.world)) {
			return renderState.pipeConnectionMatrix.isConnected(side);
		} else {
			return pipeConnectionsBuffer[side.ordinal()];
		}
	}

	public boolean isOpaque() {
		return pipe.isOpaque();
	}

	public void initialize(CoreUnroutedPipe pipe) {
		blockType = getBlockType();

		if (pipe == null) {
			LogisticsPipes.getLOGGER().warn("Pipe failed to initialize at " + getPos().toString() + ", deleting");
			world.setBlockToAir(getPos());
			return;
		}

		this.pipe = pipe;

		/*
		for (Direction o : Direction.values()) {
			TileEntity tile = getTile(o);

			if (tile instanceof LogisticsTileGenericPipe) {
				((LogisticsTileGenericPipe) tile).scheduleNeighborChange();
			}
		}*/

		bindPipe();

		computeConnections();
		scheduleRenderUpdate();

		if (pipe.needsInit()) {
			pipe.initialize();
		}

		initialized = true;
	}

	private void bindPipe() {
		if (!pipeBound && pipe != null) {
			pipe.setTile(this);
			coreState.pipeId = Item.getIdFromItem(pipe.item);
			pipeBound = true;
		}
	}

	/* SMP */

	public ModernPacket getLPDescriptionPacket() {
		bindPipe();

		PipeTileStatePacket packet = PacketHandler.getPacket(PipeTileStatePacket.class);

		packet.setTilePos(this);

		packet.setCoreState(coreState);
		packet.setRenderState(renderState);
		packet.setPipe(pipe);
		packet.setStatePacketId(statePacketId++);

		return packet;
	}

	public void afterStateUpdated() {
		if (pipe == null && coreState.pipeId != 0) {
			initialize(LogisticsBlockGenericPipe.createPipe(Registry.ITEM.getByValue(coreState.pipeId)));
		}

		if (pipe == null) {
			return;
		}

		world.markBlockRangeForRenderUpdate(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());

		if (renderState.needsRenderUpdate()) {
			world.markBlockRangeForRenderUpdate(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
			renderState.clean();
		}
	}

	public void sendUpdateToClient() {
		sendClientUpdate = true;
	}

	public TileBuffer[] getTileCache() {
		if (tileBuffer == null && pipe != null) {
			tileBuffer = TileBuffer.makeBuffer(world, pos, pipe.transport.delveIntoUnloadedChunks());
		}
		return tileBuffer;
	}

	public void blockCreated(Direction from, Block block, TileEntity tile) {
		TileBuffer[] cache = getTileCache();
		if (cache != null) {
			cache[from.getOpposite().ordinal()].set(block, tile);
		}
	}

	@Override
	public TileEntity getNextConnectedTile(Direction to) {
		if (this.pipe.isMultiBlock()) {
			return ((CoreMultiBlockPipe) this.pipe).getConnectedEndTile(to);
		}
		return getTile(to, false);
	}

	public TileEntity getTile(Direction to) {
		return getTile(to, false);
	}

	public TileEntity getTile(Direction to, boolean force) {
		TileBuffer[] cache = getTileCache();
		if (cache != null) {
			if (force) {
				cache[to.ordinal()].refresh();
			}
			return cache[to.ordinal()].getTile();
		} else {
			return null;
		}
	}

	public Block getBlock(Direction to) {
		TileBuffer[] cache = getTileCache();
		if (cache != null) {
			return cache[to.ordinal()].getBlock();
		} else {
			return null;
		}
	}

	private void computeConnections() {
		TileBuffer[] cache = getTileCache();
		if (cache == null) {
			return;
		}

		boolean[] pipeTDConnectionsBufferOld = pipeTDConnectionsBuffer.clone();

		for (Direction side : Direction.values()) {
			TileBuffer t = cache[side.ordinal()];
			t.refresh();

			pipeConnectionsBuffer[side.ordinal()] = canPipeConnect(t.getTile(), side);
			if (pipeConnectionsBuffer[side.ordinal()]) {
				pipeBCConnectionsBuffer[side.ordinal()] = SimpleServiceLocator.buildCraftProxy.isBuildCraftPipe(t.getTile());
				pipeTDConnectionsBuffer[side.ordinal()] = SimpleServiceLocator.thermalDynamicsProxy.isItemDuct(t.getTile());
			} else {
				pipeBCConnectionsBuffer[side.ordinal()] = false;
				pipeTDConnectionsBuffer[side.ordinal()] = false;
			}
		}
		if (!Arrays.equals(pipeTDConnectionsBufferOld, pipeTDConnectionsBuffer)) {
			tdPart.connectionsChanged();
		}
	}

	@Nullable
	private NonNullSupplier<IFluidHandler> supplyFluidHandler(@Nullable Direction side) {
		if (side != null && LogisticsBlockGenericPipe.isValid(pipe) && pipe.transport instanceof PipeFluidTransportLogistics) {
			return () -> ((PipeFluidTransportLogistics) pipe.transport).getIFluidHandler(side);
		} else {
			return null;
		}
	}

	@Nullable
	private NonNullSupplier<IItemHandler> supplyItemHandler(@Nullable Direction side) {
		if (side != null) {
			TileEntity tile = getTile(side);
			if (tile != null && pipeInventoryConnectionChecker.shouldLPProvideInventoryTo(tile)) {
				return () -> itemInsertionHandlers.get(side);
			}
		}
		return null;
	}

	@Nonnull
	@Override
	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @javax.annotation.Nullable Direction side) {
		LazyOptional<T> opt = LogisticsPipes.ITEM_HANDLER_CAPABILITY
				.orEmpty(cap, LazyOptional.of(supplyItemHandler(side)));
		if (opt.isPresent()) return opt;

		opt = LogisticsPipes.FLUID_HANDLER_CAPABILITY
				.orEmpty(cap, LazyOptional.of(supplyFluidHandler(side)));
		if (opt.isPresent()) return opt;

		return super.getCapability(cap, side);
	}

	public void scheduleRenderUpdate() {
		refreshRenderState = true;
	}

	@OnlyIn(Dist.CLIENT)
	public IIconProvider getPipeIcons() {
		if (pipe == null) {
			return null;
		}
		return pipe.getIconProvider();
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 64 * 4 * 64 * 4;
	}

	public Block getBlock() {
		return getBlockType();
	}

	public boolean isUsableByPlayer(PlayerEntity player) {
		return world.getTileEntity(pos) == this;
	}

	@Override
	public boolean isRemoved() {
		if (pipe != null && pipe.preventRemove()) {
			return false;
		}
		return super.isRemoved();
	}

	@Override
	public LogicController getLogicController() {
		return logicController;
	}

	@Override
	public ILPPipe getLPPipe() {
		return pipe;
	}

	@Override
	public BlockPos getBlockPos() {
		return getPos();
	}

	@Nonnull
	@OnlyIn(Dist.CLIENT)
	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		if (renderBox != null) {
			return renderBox;
		}
		if (pipe == null) {
			return new AxisAlignedBB(getPos(), getPos().add(1, 1, 1));
		}
		if (!pipe.isMultiBlock()) {
			renderBox = new AxisAlignedBB(getPos(), getPos().add(1, 1, 1));
		} else {
			LPPositionSet<DoubleCoordinatesType<CoreMultiBlockPipe.SubBlockTypeForShare>> set = ((CoreMultiBlockPipe) pipe).getRotatedSubBlocks();
			set.addToAll(pipe.getLPPosition());
			set.add(new DoubleCoordinatesType<>(getPos(), CoreMultiBlockPipe.SubBlockTypeForShare.NON_SHARE));
			set.add(new DoubleCoordinatesType<>(getPos().getX() + 1, getPos().getY() + 1, getPos().getZ() + 1, CoreMultiBlockPipe.SubBlockTypeForShare.NON_SHARE));
			renderBox = new AxisAlignedBB(set.getMinXD() - 1, set.getMinYD() - 1, set.getMinZD() - 1, set.getMaxXD() + 1, set.getMaxYD() + 1, set.getMaxZD() + 1);
		}
		return renderBox;
	}

	@Override
	public boolean canRenderBreaking() {
		return false;
	}

	@Override
	public boolean hasFastRenderer() {
		return false;
	}

	@Override
	public void requestModelDataUpdate() {

	}

	@Nonnull
	@Override
	public IModelData getModelData() {
		return null;
	}

	@Override
	public double getDistanceTo(int destinationint, Direction ignore, ItemIdentifier ident, boolean isActive, double traveled, double max, List<DoubleCoordinates> visited) {
		if (pipe == null || traveled > max) {
			return Integer.MAX_VALUE;
		}
		double result = pipe.getDistanceTo(destinationint, ignore, ident, isActive, traveled + getDistance(), max, visited);
		if (result == Integer.MAX_VALUE) {
			return result;
		}
		return result + (int) getDistance();
	}

	@Override
	public boolean acceptItem(LPTravelingItem item, TileEntity from) {
		if (LogisticsBlockGenericPipe.isValid(pipe) && pipe.transport != null) {
			pipe.transport.injectItem(item, item.output);
			return true;
		}
		return false;
	}

	@Override
	public void refreshTileCacheOnSide(Direction side) {
		TileBuffer[] cache = getTileCache();
		if (cache != null) {
			cache[side.ordinal()].refresh();
		}
	}

	public boolean nonNull() {
		return Objects.nonNull(pipe);
	}

	@Override
	public boolean isMultiBlock() {
		return nonNull() && pipe.isMultiBlock();
	}

	@Override
	public Stream<TileEntity> getPartsOfPipe() {
		return this.subMultiBlock.stream().map(pos -> pos.getTileEntity(world));
	}

	@Override
	public void tick() {

	}

	public static class CoreState implements IClientState {

		public int pipeId = -1;

		@Override
		public void writeData(LPDataOutput output) {
			output.writeInt(pipeId);

		}

		@Override
		public void readData(LPDataInput input) {
			pipeId = input.readInt();

		}
	}
}
