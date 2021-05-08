package logisticspipes.modules;

import java.lang.ref.WeakReference;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.DelayQueue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;

import net.minecraftforge.common.util.Constants;

import com.google.common.collect.ImmutableList;
import lombok.Getter;

import logisticspipes.LogisticsPipes;
import logisticspipes.interfaces.IGuiOpenControler;
import logisticspipes.interfaces.IHUDModuleHandler;
import logisticspipes.interfaces.IHUDModuleRenderer;
import logisticspipes.interfaces.IModuleWatchReciver;
import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.interfaces.ISlotUpgradeManager;
import logisticspipes.interfaces.IWorldProvider;
import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.items.ItemUpgrade;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractguis.ModuleCoordinatesGuiProvider;
import logisticspipes.network.abstractguis.ModuleInHandGuiProvider;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.guis.module.inhand.CraftingModuleInHand;
import logisticspipes.network.guis.module.inpipe.CraftingModuleSlot;
import logisticspipes.network.packets.cpipe.CPipeSatelliteImport;
import logisticspipes.network.packets.cpipe.CPipeSatelliteImportBack;
import logisticspipes.network.packets.cpipe.CraftingPipeOpenConnectedGuiPacket;
import logisticspipes.network.packets.hud.HUDStartModuleWatchingPacket;
import logisticspipes.network.packets.hud.HUDStopModuleWatchingPacket;
import logisticspipes.network.packets.pipe.CraftingPipeUpdatePacket;
import logisticspipes.network.packets.pipe.FluidCraftingAmount;
import logisticspipes.pipes.PipeFluidSatellite;
import logisticspipes.pipes.PipeItemsSatelliteLogistics;
import logisticspipes.pipes.PipeLogisticsChassis.ChassiTargetInformation;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.interfaces.IFuzzyRecipeProvider;
import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.IRouter;
import logisticspipes.utils.CacheHolder.CacheTypes;
import logisticspipes.utils.DelayedGeneric;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.tuples.Pair;
import network.rs485.logisticspipes.connection.AdjacentUtilKt;
import network.rs485.logisticspipes.module.Gui;
import network.rs485.logisticspipes.property.BitSetProperty;
import network.rs485.logisticspipes.property.BooleanProperty;
import network.rs485.logisticspipes.property.IBitSet;
import network.rs485.logisticspipes.property.IntListProperty;
import network.rs485.logisticspipes.property.IntegerProperty;
import network.rs485.logisticspipes.property.InventoryProperty;
import network.rs485.logisticspipes.property.Property;
import network.rs485.logisticspipes.property.UUIDListProperty;
import network.rs485.logisticspipes.property.UUIDProperty;
import network.rs485.logisticspipes.property.UUIDPropertyKt;
import network.rs485.logisticspipes.util.FuzzyUtil;

public class ModuleCrafter extends LogisticsModule
		implements IHUDModuleHandler, IModuleWatchReciver, IGuiOpenControler, Gui {

	public final InventoryProperty dummyInventory = new InventoryProperty(
			new ItemIdentifierInventory(11, "Requested items", 127), "");
	public final InventoryProperty liquidInventory = new InventoryProperty(
			new ItemIdentifierInventory(ItemUpgrade.MAX_LIQUID_CRAFTER, "Fluid items", 1, true), "FluidInv");
	public final InventoryProperty cleanupInventory = new InventoryProperty(
			new ItemIdentifierInventory(ItemUpgrade.MAX_CRAFTING_CLEANUP * 3, "Cleanup Filter Items", 1), "CleanupInv");
	public final UUIDProperty satelliteUUID = new UUIDProperty(null, "satelliteUUID");
	public final UUIDListProperty advancedSatelliteUUIDList = new UUIDListProperty("advancedSatelliteUUIDList");
	public final UUIDProperty liquidSatelliteUUID = new UUIDProperty(null, "liquidSatelliteId");
	public final UUIDListProperty liquidSatelliteUUIDList = new UUIDListProperty("liquidSatelliteUUIDList");
	public final IntegerProperty priority = new IntegerProperty(0, "priority");
	public final IntListProperty liquidAmounts = new IntListProperty("FluidAmount");
	public final BooleanProperty cleanupModeIsExclude = new BooleanProperty(true, "cleanupModeIsExclude");
	public final BitSetProperty fuzzyFlags = new BitSetProperty(new BitSet(4 * (9 + 1)), "fuzzyBitSet");
	private final List<Property<?>> properties = ImmutableList.<Property<?>>builder()
			.add(dummyInventory)
			.add(liquidInventory)
			.add(cleanupInventory)
			.add(satelliteUUID)
			.add(advancedSatelliteUUIDList)
			.add(liquidSatelliteUUID)
			.add(liquidSatelliteUUIDList)
			.add(priority)
			.add(liquidAmounts)
			.add(cleanupModeIsExclude)
			.add(fuzzyFlags)
			.build();

	// for reliable transport
	protected final DelayQueue<DelayedGeneric<Pair<ItemIdentifierStack, IAdditionalTargetInformation>>> _lostItems = new DelayQueue<>();
	protected final PlayerCollectionList localModeWatchers = new PlayerCollectionList();
	protected final PlayerCollectionList guiWatcher = new PlayerCollectionList();

	public ClientSideSatelliteNames clientSideSatelliteNames = new ClientSideSatelliteNames();

	protected SinkReply _sinkReply;

	@Nullable
	private IRequestItems _invRequester;
	private WeakReference<TileEntity> lastAccessedCrafter = new WeakReference<>(null);
	private boolean cachedAreAllOrderesToBuffer;
	private UpgradeSatelliteFromIDs updateSatelliteFromIDs = null;

	public ModuleCrafter() {
		advancedSatelliteUUIDList.ensureSize(9);
		liquidAmounts.ensureSize(ItemUpgrade.MAX_LIQUID_CRAFTER);
		liquidSatelliteUUIDList.ensureSize(ItemUpgrade.MAX_LIQUID_CRAFTER);
	}

	public static String getName() {
		return "crafter";
	}

	@Nonnull
	@Override
	public String getLPName() {
		return getName();
	}

	@Nonnull
	@Override
	public List<Property<?>> getProperties() {
		return properties;
	}

	/**
	 * assumes that the invProvider is also IRequest items.
	 */
	@Override
	public void registerHandler(IWorldProvider world, IPipeServiceProvider service) {
		super.registerHandler(world, service);
	}

	@Override
	public void registerPosition(@Nonnull ModulePositionType slot, int positionInt) {
		super.registerPosition(slot, positionInt);
		_sinkReply = new SinkReply(FixedPriority.ItemSink, 0, true, false, 1, 0,
				new ChassiTargetInformation(getPositionInt()));
	}

	protected int spaceFor(@Nonnull ItemStack stack, ItemIdentifier item, boolean includeInTransit) {
		Pair<String, ItemIdentifier> key = new Pair<>("spaceFor", item);
		final IPipeServiceProvider service = _service;
		if (service == null) return 0;
		Object cache = service.getCacheHolder().getCacheFor(CacheTypes.Inventory, key);
		int onRoute = 0;
		if (includeInTransit) {
			onRoute = service.countOnRoute(item);
		}
		if (cache != null) {
			return ((Integer) cache) - onRoute;
		}

		if (includeInTransit) {
			stack = stack.copy();
			stack.grow(onRoute);
		}
		final ISlotUpgradeManager upgradeManager = Objects.requireNonNull(getUpgradeManager());
		final ItemStack finalStack = stack;
		final Integer count = AdjacentUtilKt.sneakyInventoryUtils(service.getAvailableAdjacent(), upgradeManager)
				.stream().map(invUtil -> invUtil.roomForItem(finalStack)).reduce(Integer::sum).orElse(0);

		service.getCacheHolder().setCache(CacheTypes.Inventory, key, count);
		return count - onRoute;
	}

	private UUID getUUIDForSatelliteName(String name) {
		for (PipeItemsSatelliteLogistics pipe : PipeItemsSatelliteLogistics.AllSatellites) {
			if (pipe.getSatellitePipeName().equals(name)) {
				return pipe.getRouter().getId();
			}
		}
		return null;
	}

	private UUID getUUIDForFluidSatelliteName(String name) {
		for (PipeFluidSatellite pipe : PipeFluidSatellite.AllSatellites) {
			if (pipe.getSatellitePipeName().equals(name)) {
				return pipe.getRouter().getId();
			}
		}
		return null;
	}

	public void onAllowedRemoval() {
		// TODO PROVIDE REFACTOR: cancel all orders
	}

	@Override
	public void tick() {
		if (_lostItems.isEmpty()) {
			return;
		}

		// TODO PROVIDE REFACTOR: check orders and fulfill them
	}

	@Override
	public boolean recievePassive() {
		return false;
	}

	protected ISlotUpgradeManager getUpgradeManager() {
		if (_service == null) {
			return null;
		}
		return _service.getUpgradeManager(slot, positionInt);
	}

	public ItemIdentifierStack getCraftedItem() {
		return dummyInventory.getIDStackInSlot(9);
	}

	protected int getNextConnectSatelliteId(boolean prev, int x) {
		int closestIdFound = prev ? 0 : Integer.MAX_VALUE;
		if (_service == null) {
			return prev ? Math.max(0, satelliteId - 1) : satelliteId + 1;
		}
		for (final PipeItemsSatelliteLogistics satellite : PipeItemsSatelliteLogistics.AllSatellites) {
			CoreRoutedPipe satPipe = satellite;
			if (satPipe == null || satPipe.stillNeedReplace() || satPipe.getRouter() == null || satPipe.isFluidPipe()) {
				continue;
			}
			IRouter satRouter = satPipe.getRouter();
			List<ExitRoute> routes = _service.getRouter().getDistanceTo(satRouter);
			if (routes != null && !routes.isEmpty()) {
				boolean filterFree = false;
				for (ExitRoute route : routes) {
					if (route.filters.isEmpty()) {
						filterFree = true;
						break;
					}
				}
				if (!filterFree) {
					continue;
				}
				if (x == -1) {
					if (!prev && satellite.satelliteId > satelliteId && satellite.satelliteId < closestIdFound) {
						closestIdFound = satellite.satelliteId;
					} else if (prev && satellite.satelliteId < satelliteId && satellite.satelliteId > closestIdFound) {
						closestIdFound = satellite.satelliteId;
					}
				} else {
					if (!prev && satellite.satelliteId > advancedSatelliteIdArray[x] && satellite.satelliteId < closestIdFound) {
						closestIdFound = satellite.satelliteId;
					} else if (prev && satellite.satelliteId < advancedSatelliteIdArray[x] && satellite.satelliteId > closestIdFound) {
						closestIdFound = satellite.satelliteId;
					}
				}
			}
		}
		if (closestIdFound == Integer.MAX_VALUE) {
			if (x == -1) {
				return satelliteId;
			} else {
				return advancedSatelliteIdArray[x];
			}
		}
		return closestIdFound;
	}

	protected int getNextConnectFluidSatelliteId(boolean prev, int x) {
		int closestIdFound = prev ? 0 : Integer.MAX_VALUE;
		for (final PipeFluidSatellite satellite : PipeFluidSatellite.AllSatellites) {
			CoreRoutedPipe satPipe = satellite;
			if (satPipe == null || satPipe.stillNeedReplace() || satPipe.getRouter() == null || !satPipe.isFluidPipe()) {
				continue;
			}
			IRouter satRouter = satPipe.getRouter();
			List<ExitRoute> routes = _service.getRouter().getDistanceTo(satRouter);
			if (routes != null && !routes.isEmpty()) {
				boolean filterFree = false;
				for (ExitRoute route : routes) {
					if (route.filters.isEmpty()) {
						filterFree = true;
						break;
					}
				}
				if (!filterFree) {
					continue;
				}
				if (x == -1) {
					if (!prev && satellite.satelliteId > liquidSatelliteId && satellite.satelliteId < closestIdFound) {
						closestIdFound = satellite.satelliteId;
					} else if (prev && satellite.satelliteId < liquidSatelliteId && satellite.satelliteId > closestIdFound) {
						closestIdFound = satellite.satelliteId;
					}
				} else {
					if (!prev && satellite.satelliteId > liquidSatelliteIdArray[x] && satellite.satelliteId < closestIdFound) {
						closestIdFound = satellite.satelliteId;
					} else if (prev && satellite.satelliteId < liquidSatelliteIdArray[x] && satellite.satelliteId > closestIdFound) {
						closestIdFound = satellite.satelliteId;
					}
				}
			}
		}
		if (closestIdFound == Integer.MAX_VALUE) {
			if (x == -1) {
				return liquidSatelliteId;
			} else {
				return liquidSatelliteIdArray[x];
			}
		}
		return closestIdFound;
	}

	public void setNextSatellite(EntityPlayer player) {
		if (MainProxy.isClient(player.world)) {
			final CoordinatesPacket packet = PacketHandler.getPacket(CPipeNextSatellite.class).setModulePos(this);
			MainProxy.sendPacketToServer(packet);
		} else {
			satelliteId = getNextConnectSatelliteId(false, -1);
			final CoordinatesPacket packet = PacketHandler.getPacket(CPipeSatelliteId.class).setPipeId(satelliteId).setModulePos(this);
			MainProxy.sendPacketToPlayer(packet, player);
		}

	}

	// This is called by the packet PacketCraftingPipeSatelliteId
	public void setSatelliteId(int satelliteId, int x) {
		if (x == -1) {
			this.satelliteId = satelliteId;
		} else {
			advancedSatelliteIdArray[x] = satelliteId;
		}
	}

	public void setPrevSatellite(EntityPlayer player) {
		if (MainProxy.isClient(player.world)) {
			final CoordinatesPacket packet = PacketHandler.getPacket(CPipePrevSatellite.class).setModulePos(this);
			MainProxy.sendPacketToServer(packet);
		} else {
			satelliteId = getNextConnectSatelliteId(true, -1);
			final CoordinatesPacket packet = PacketHandler.getPacket(CPipeSatelliteId.class).setPipeId(satelliteId).setModulePos(this);
			MainProxy.sendPacketToPlayer(packet, player);
		}
	}

	private IRouter getSatelliteRouter(int x) {
		final UUID satelliteUUID = x == -1 ? this.satelliteUUID.getValue() : advancedSatelliteUUIDList.get(x);
		final int satelliteRouterId = SimpleServiceLocator.routerManager.getIDforUUID(satelliteUUID);
		return SimpleServiceLocator.routerManager.getRouter(satelliteRouterId);
	}

	@Override
	public void readFromNBT(@Nonnull NBTTagCompound tag) {
		super.readFromNBT(tag);

		// FIXME: remove after 1.12
		for (int i = 0; i < 9; i++) {
			String advancedSatelliteUUIDArrayString = tag.getString("advancedSatelliteUUID" + i);
			if (!advancedSatelliteUUIDArrayString.isEmpty()) {
				advancedSatelliteUUIDList.set(i, UUID.fromString(advancedSatelliteUUIDArrayString));
			}
		}

		// FIXME: remove after 1.12
		for (int i = 0; i < ItemUpgrade.MAX_LIQUID_CRAFTER; i++) {
			String liquidSatelliteUUIDArrayString = tag.getString("liquidSatelliteUUIDArray" + i);
			if (!liquidSatelliteUUIDArrayString.isEmpty()) {
				liquidSatelliteUUIDList.set(i, UUID.fromString(liquidSatelliteUUIDArrayString));
			}
		}

		// FIXME: remove after 1.12
		if (tag.hasKey("fuzzyFlags")) {
			NBTTagList lst = tag.getTagList("fuzzyFlags", Constants.NBT.TAG_COMPOUND);
			for (int i = 0; i < 9; i++) {
				FuzzyUtil.INSTANCE.readFromNBT(inputFuzzy(i), lst.getCompoundTagAt(i));
			}
		}
		// FIXME: remove after 1.12
		if (tag.hasKey("outputFuzzyFlags")) {
			FuzzyUtil.INSTANCE.readFromNBT(outputFuzzy(), tag.getCompoundTag("outputFuzzyFlags"));
		}

		// FIXME: remove after 1.12
		if (tag.hasKey("satelliteid")) {
			updateSatelliteFromIDs = new UpgradeSatelliteFromIDs();
			updateSatelliteFromIDs.satelliteId = tag.getInteger("satelliteid");
			for (int i = 0; i < 9; i++) {
				updateSatelliteFromIDs.advancedSatelliteIdArray[i] = tag.getInteger("advancedSatelliteId" + i);
			}
			for (int i = 0; i < ItemUpgrade.MAX_LIQUID_CRAFTER; i++) {
				updateSatelliteFromIDs.liquidSatelliteIdArray[i] = tag.getInteger("liquidSatelliteIdArray" + i);
			}
			updateSatelliteFromIDs.liquidSatelliteId = tag.getInteger("liquidSatelliteId");
		}
	}

	public IBitSet outputFuzzy() {
		final int startIdx = 4 * 9; // after the 9th slot
		return fuzzyFlags.get(startIdx, startIdx + 3);
	}

	public IBitSet inputFuzzy(int slot) {
		final int startIdx = 4 * slot;
		return fuzzyFlags.get(startIdx, startIdx + 3);
	}

	public ModernPacket getCPipePacket() {
		return PacketHandler.getPacket(CraftingPipeUpdatePacket.class).setAmount(liquidAmounts.getArray())
				.setLiquidSatelliteNameArray(getSatelliteNamesForUUIDs(liquidSatelliteUUIDList))
				.setLiquidSatelliteName(getSatelliteNameForUUID(liquidSatelliteUUID.getValue()))
				.setSatelliteName(getSatelliteNameForUUID(satelliteUUID.getValue()))
				.setAdvancedSatelliteNameArray(getSatelliteNamesForUUIDs(advancedSatelliteUUIDList))
				.setPriority(priority.getValue()).setModulePos(this);
	}

	private String getSatelliteNameForUUID(UUID uuid) {
		if (UUIDPropertyKt.isZero(uuid)) {
			return "";
		}
		int simpleId = SimpleServiceLocator.routerManager.getIDforUUID(uuid);
		IRouter router = SimpleServiceLocator.routerManager.getRouter(simpleId);
		if (router != null) {
			CoreRoutedPipe pipe = router.getPipe();
			if (pipe instanceof PipeItemsSatelliteLogistics) {
				return ((PipeItemsSatelliteLogistics) pipe).getSatellitePipeName();
			} else if (pipe instanceof PipeFluidSatellite) {
				return ((PipeFluidSatellite) pipe).getSatellitePipeName();
			}
		}
		return "UNKNOWN NAME";
	}

	private String[] getSatelliteNamesForUUIDs(UUIDListProperty list) {
		return list.stream().map(this::getSatelliteNameForUUID).toArray(String[]::new);
	}

	public void handleCraftingUpdatePacket(CraftingPipeUpdatePacket packet) {
		if (MainProxy.isClient(getWorld())) {
			liquidAmounts.replaceContent(packet.getAmount());
			clientSideSatelliteNames.liquidSatelliteNameArray = packet.getLiquidSatelliteNameArray();
			clientSideSatelliteNames.liquidSatelliteName = packet.getLiquidSatelliteName();
			clientSideSatelliteNames.satelliteName = packet.getSatelliteName();
			clientSideSatelliteNames.advancedSatelliteNameArray = packet.getAdvancedSatelliteNameArray();
			priority.setValue(packet.getPriority());
		} else {
			throw new UnsupportedOperationException();
		}
	}

	@Nonnull
	@Override
	public ModuleCoordinatesGuiProvider getPipeGuiProvider() {
		return NewGuiHandler.getGui(CraftingModuleSlot.class)
				.setAdvancedSat(getUpgradeManager().isAdvancedSatelliteCrafter())
				.setLiquidCrafter(getUpgradeManager().getFluidCrafter())
				.setAmount(liquidAmounts.getArray())
				.setHasByproductExtractor(getUpgradeManager().hasByproductExtractor())
				.setFuzzy(getUpgradeManager().isFuzzyUpgrade())
				.setCleanupSize(getUpgradeManager().getCrafterCleanup())
				.setCleanupExclude(cleanupModeIsExclude.getValue());
	}

	@Nonnull
	@Override
	public ModuleInHandGuiProvider getInHandGuiProvider() {
		return NewGuiHandler.getGui(CraftingModuleInHand.class).setAmount(liquidAmounts.getArray())
				.setCleanupExclude(cleanupModeIsExclude.getValue());
	}

	public void importFromCraftingTable(@Nullable EntityPlayer player) {
		if (MainProxy.isClient(getWorld())) {
			// Send packet asking for import
			final CoordinatesPacket packet = PacketHandler.getPacket(CPipeSatelliteImport.class).setModulePos(this);
			MainProxy.sendPacketToServer(packet);
		} else {
			final IPipeServiceProvider service = _service;
			if (service == null) return;
			service.getAvailableAdjacent().neighbors().keySet().stream().flatMap(
					neighbor -> SimpleServiceLocator.craftingRecipeProviders.stream()
							.filter(provider -> provider.importRecipe(neighbor.getTileEntity(), dummyInventory))
							.map(provider1 -> new Pair<>(neighbor, provider1))).findFirst()
					.ifPresent(neighborProviderPair -> {
						if (neighborProviderPair.getValue2() instanceof IFuzzyRecipeProvider) {
							((IFuzzyRecipeProvider) neighborProviderPair.getValue2())
									.importFuzzyFlags(neighborProviderPair.getValue1().getTileEntity(),
											dummyInventory.getSlotAccess(), fuzzyFlags);
						}
					});

			// Send inventory as packet
			final CoordinatesPacket packet = PacketHandler.getPacket(CPipeSatelliteImportBack.class)
					.setInventory(dummyInventory).setModulePos(this);
			if (player != null) {
				MainProxy.sendPacketToPlayer(packet, player);
			}
			MainProxy.sendPacketToAllWatchingChunk(this, packet);
		}
	}

	public ItemIdentifierStack getByproductItem() {
		return dummyInventory.getIDStackInSlot(10);
	}

	public FluidIdentifier getFluidMaterial(int slotnr) {
		ItemIdentifierStack stack = liquidInventory.getIDStackInSlot(slotnr);
		if (stack == null) {
			return null;
		}
		return FluidIdentifier.get(stack.getItem());
	}

	public void changeFluidAmount(int change, int slot, EntityPlayer player) {
		if (MainProxy.isClient(player.world)) {
			MainProxy.sendPacketToServer(
					PacketHandler.getPacket(FluidCraftingAmount.class).setInteger2(slot).setInteger(change)
							.setModulePos(this));
		} else {
			liquidAmounts.increase(slot, change);
			if (liquidAmounts.get(slot) <= 0) {
				liquidAmounts.set(slot, 0);
			}
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(FluidCraftingAmount.class).setInteger2(slot)
					.setInteger(liquidAmounts.get(slot)).setModulePos(this), player);
		}
	}

	private IRouter getFluidSatelliteRouter(int x) {
		final UUID liquidSatelliteUUID = x == -1 ? this.liquidSatelliteUUID.getValue() : liquidSatelliteUUIDList.get(x);
		final int satelliteRouterId = SimpleServiceLocator.routerManager.getIDforUUID(liquidSatelliteUUID);
		return SimpleServiceLocator.routerManager.getRouter(satelliteRouterId);
	}

	/**
	 * Triggers opening the first possible crafting provider or inventory GUI by using onBlockActivated.
	 *
	 * @return true, if a GUI was opened (server-side only)
	 */
	public boolean openAttachedGui(EntityPlayer player) {
		if (MainProxy.isClient(player.world)) {
			if (player instanceof EntityPlayerMP) {
				player.closeScreen();
			} else if (player instanceof EntityPlayerSP) {
				player.closeScreen();
			}
			MainProxy.sendPacketToServer(
					PacketHandler.getPacket(CraftingPipeOpenConnectedGuiPacket.class).setModulePos(this));
			return false;
		}

		final IPipeServiceProvider service = _service;
		if (service == null) return false;
		final IWorldProvider worldProvider = _world;
		if (worldProvider == null) return false;

		// hack to avoid wrenching blocks
		int savedEquipped = player.inventory.currentItem;
		boolean foundSlot = false;
		// try to find a empty slot
		for (int i = 0; i < 9; i++) {
			if (player.inventory.getStackInSlot(i).isEmpty()) {
				foundSlot = true;
				player.inventory.currentItem = i;
				break;
			}
		}
		// okay, anything that's a block?
		if (!foundSlot) {
			for (int i = 0; i < 9; i++) {
				ItemStack is = player.inventory.getStackInSlot(i);
				if (is.getItem() instanceof ItemBlock) {
					foundSlot = true;
					player.inventory.currentItem = i;
					break;
				}
			}
		}
		// give up and select whatever is right of the current slot
		if (!foundSlot) {
			player.inventory.currentItem = (player.inventory.currentItem + 1) % 9;
		}

		final boolean guiOpened = service.getAvailableAdjacent().neighbors().keySet().stream().anyMatch(neighbor -> {
			if (neighbor.canHandleItems() || SimpleServiceLocator.craftingRecipeProviders.stream()
					.anyMatch(provider -> provider.canOpenGui(neighbor.getTileEntity()))) {
				final BlockPos pos = neighbor.getTileEntity().getPos();
				IBlockState blockState = worldProvider.getWorld().getBlockState(pos);
				return !blockState.getBlock().isAir(blockState, worldProvider.getWorld(), pos) && blockState.getBlock()
						.onBlockActivated(worldProvider.getWorld(), pos,
								neighbor.getTileEntity().getWorld().getBlockState(pos), player, EnumHand.MAIN_HAND,
								EnumFacing.UP, 0, 0, 0);
			} else {
				return false;
			}
		});
		if (!guiOpened) {
			LogisticsPipes.log.warn("Ignored open attached GUI request at " + player.world + " @ " + getBlockPos());
		}
		player.inventory.currentItem = savedEquipped;
		return guiOpened;
	}

	public boolean areAllOrderesToBuffer() {
		return cachedAreAllOrderesToBuffer;
	}

	protected int neededEnergy() {
		return (int) (10 * Math.pow(1.1, getUpgradeManager().getItemExtractionUpgrade()) * Math
				.pow(1.2, getUpgradeManager().getItemStackExtractionUpgrade()));
	}

	protected int itemsToExtract() {
		return (int) Math.pow(2, getUpgradeManager().getItemExtractionUpgrade());
	}

	protected int stacksToExtract() {
		return 1 + getUpgradeManager().getItemStackExtractionUpgrade();
	}

	public void importCleanup() {
		for (int i = 0; i < 10; i++) {
			final ItemIdentifierStack identStack = dummyInventory.getIDStackInSlot(i);
			if (identStack == null) {
				cleanupInventory.clearInventorySlotContents(i);
			} else {
				cleanupInventory.setInventorySlotContents(i, new ItemIdentifierStack(identStack));
			}
		}
		for (int i = 10; i < cleanupInventory.getSizeInventory(); i++) {
			cleanupInventory.clearInventorySlotContents(i);
		}
		cleanupInventory.getSlotAccess().compactFirst(10);
		cleanupInventory.recheckStackLimit();
		cleanupModeIsExclude.setValue(false);
	}

	@Override
	public void startHUDWatching() {
		MainProxy.sendPacketToServer(PacketHandler.getPacket(HUDStartModuleWatchingPacket.class).setModulePos(this));
	}

	@Override
	public void stopHUDWatching() {
		MainProxy.sendPacketToServer(PacketHandler.getPacket(HUDStopModuleWatchingPacket.class).setModulePos(this));
	}

	@Override
	public void startWatching(EntityPlayer player) {
		localModeWatchers.add(player);
	}

	@Override
	public void stopWatching(EntityPlayer player) {
		localModeWatchers.remove(player);
	}

	@Override
	public IHUDModuleRenderer getHUDRenderer() {
		// TODO Auto-generated method stub
		return null;
	}

	private void updateSatellitesOnClient() {
		MainProxy.sendToPlayerList(getCPipePacket(), guiWatcher);
	}

	public void setSatelliteUUID(@Nullable UUID pipeID) {
		if (pipeID == null) {
			satelliteUUID.zero();
		} else {
			satelliteUUID.setValue(pipeID);
		}
		updateSatellitesOnClient();
		updateSatelliteFromIDs = null;
	}

	public void setAdvancedSatelliteUUID(int i, @Nullable UUID pipeID) {
		if (pipeID == null) {
			advancedSatelliteUUIDList.zero(i);
		} else {
			advancedSatelliteUUIDList.set(i, pipeID);
		}
		updateSatellitesOnClient();
		updateSatelliteFromIDs = null;
	}

	public void setFluidSatelliteUUID(@Nullable UUID pipeID) {
		if (pipeID == null) {
			liquidSatelliteUUID.zero();
		} else {
			liquidSatelliteUUID.setValue(pipeID);
		}
		updateSatellitesOnClient();
		updateSatelliteFromIDs = null;
	}

	public void setAdvancedFluidSatelliteUUID(int i, @Nullable UUID pipeID) {
		if (pipeID == null) {
			liquidSatelliteUUIDList.zero(i);
		} else {
			liquidSatelliteUUIDList.set(i, pipeID);
		}
		updateSatellitesOnClient();
		updateSatelliteFromIDs = null;
	}

	@Override
	public void guiOpenedByPlayer(EntityPlayer player) {
		guiWatcher.add(player);
	}

	@Override
	public void guiClosedByPlayer(EntityPlayer player) {
		guiWatcher.remove(player);
	}

	public static class CraftingChassisInformation extends ChassiTargetInformation {

		@Getter
		private final int craftingSlot;

		public CraftingChassisInformation(int craftingSlot, int moduleSlot) {
			super(moduleSlot);
			this.craftingSlot = craftingSlot;
		}
	}

	// FIXME: Remove after 1.12
	private static class UpgradeSatelliteFromIDs {

		public int satelliteId;
		public int[] advancedSatelliteIdArray = new int[9];
		public int[] liquidSatelliteIdArray = new int[ItemUpgrade.MAX_LIQUID_CRAFTER];
		public int liquidSatelliteId;
	}

	public static class ClientSideSatelliteNames {

		public @Nonnull
		String satelliteName = "";
		public @Nonnull
		String[] advancedSatelliteNameArray = {};
		public @Nonnull
		String liquidSatelliteName = "";
		public @Nonnull
		String[] liquidSatelliteNameArray = {};
	}

	public boolean hasByproductUpgrade() {
		return getUpgradeManager().hasByproductExtractor();
	}

	public boolean hasFuzzyUpgrade() { return getUpgradeManager().isFuzzyUpgrade(); }
}
