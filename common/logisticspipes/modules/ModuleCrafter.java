package logisticspipes.modules;

import java.util.List;
import java.util.concurrent.DelayQueue;
import java.util.stream.Collectors;

import net.minecraft.block.Block;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.CapabilityItemHandler;

import logisticspipes.interfaces.IHUDModuleHandler;
import logisticspipes.interfaces.IHUDModuleRenderer;
import logisticspipes.interfaces.IInventoryUtil;
import logisticspipes.interfaces.IModuleWatchReciver;
import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.interfaces.ISlotUpgradeManager;
import logisticspipes.interfaces.IWorldProvider;
import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.items.ItemUpgrade;
import logisticspipes.modules.abstractmodules.LogisticsGuiModule;
import logisticspipes.modules.abstractmodules.LogisticsModule;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractguis.ModuleCoordinatesGuiProvider;
import logisticspipes.network.abstractguis.ModuleInHandGuiProvider;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.guis.module.inhand.CraftingModuleInHand;
import logisticspipes.network.guis.module.inpipe.CraftingModuleSlot;
import logisticspipes.network.packets.block.CraftingPipeNextAdvancedSatellitePacket;
import logisticspipes.network.packets.block.CraftingPipePrevAdvancedSatellitePacket;
import logisticspipes.network.packets.cpipe.CPipeNextSatellite;
import logisticspipes.network.packets.cpipe.CPipePrevSatellite;
import logisticspipes.network.packets.cpipe.CPipeSatelliteId;
import logisticspipes.network.packets.cpipe.CPipeSatelliteImport;
import logisticspipes.network.packets.cpipe.CPipeSatelliteImportBack;
import logisticspipes.network.packets.cpipe.CraftingAdvancedSatelliteId;
import logisticspipes.network.packets.cpipe.CraftingPipeOpenConnectedGuiPacket;
import logisticspipes.network.packets.hud.HUDStartModuleWatchingPacket;
import logisticspipes.network.packets.hud.HUDStopModuleWatchingPacket;
import logisticspipes.network.packets.pipe.CraftingPipePriorityDownPacket;
import logisticspipes.network.packets.pipe.CraftingPipePriorityUpPacket;
import logisticspipes.network.packets.pipe.CraftingPipeUpdatePacket;
import logisticspipes.network.packets.pipe.CraftingPriority;
import logisticspipes.network.packets.pipe.FluidCraftingAdvancedSatelliteId;
import logisticspipes.network.packets.pipe.FluidCraftingAmount;
import logisticspipes.network.packets.pipe.FluidCraftingPipeAdvancedSatelliteNextPacket;
import logisticspipes.network.packets.pipe.FluidCraftingPipeAdvancedSatellitePrevPacket;
import logisticspipes.pipes.PipeFluidSatellite;
import logisticspipes.pipes.PipeItemsCraftingLogistics;
import logisticspipes.pipes.PipeItemsSatelliteLogistics;
import logisticspipes.pipes.PipeLogisticsChassi.ChassiTargetInformation;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.interfaces.ICraftingRecipeProvider;
import logisticspipes.proxy.interfaces.IFuzzyRecipeProvider;
import logisticspipes.request.resources.DictResource;
import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.IRouter;
import logisticspipes.routing.pathfinder.IPipeInformationProvider.ConnectionPipeType;
import logisticspipes.utils.CacheHolder.CacheTypes;
import logisticspipes.utils.DelayedGeneric;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.tuples.Pair;
import network.rs485.logisticspipes.world.WorldCoordinatesWrapper;
import network.rs485.logisticspipes.world.WorldCoordinatesWrapper.AdjacentTileEntity;

public class ModuleCrafter extends LogisticsGuiModule implements IHUDModuleHandler, IModuleWatchReciver {

	// for reliable transport
	protected final DelayQueue<DelayedGeneric<Pair<ItemIdentifierStack, IAdditionalTargetInformation>>> _lostItems = new DelayQueue<>();
	protected final PlayerCollectionList localModeWatchers = new PlayerCollectionList();
	public int satelliteId = 0;
	public int[] advancedSatelliteIdArray = new int[9];
	public DictResource[] fuzzyCraftingFlagArray = new DictResource[9];
	public DictResource outputFuzzyFlags = new DictResource((ItemIdentifierStack) null);
	public int priority = 0;
	public int[] liquidSatelliteIdArray = new int[ItemUpgrade.MAX_LIQUID_CRAFTER];
	public int liquidSatelliteId = 0;
	public boolean[] craftingSigns = new boolean[6];
	public boolean cleanupModeIsExclude = true;
	// from PipeItemsCraftingLogistics
	protected ItemIdentifierInventory _dummyInventory = new ItemIdentifierInventory(11, "Requested items", 127);
	protected ItemIdentifierInventory _liquidInventory = new ItemIdentifierInventory(ItemUpgrade.MAX_LIQUID_CRAFTER, "Fluid items", 1, true);
	protected ItemIdentifierInventory _cleanupInventory = new ItemIdentifierInventory(ItemUpgrade.MAX_CRAFTING_CLEANUP * 3, "Cleanup Filer Items", 1);
	protected int[] amount = new int[ItemUpgrade.MAX_LIQUID_CRAFTER];
	protected SinkReply _sinkReply;
	private PipeItemsCraftingLogistics _pipe;
	private boolean cachedAreAllOrderesToBuffer;
	private List<AdjacentTileEntity> cachedCrafters = null;

	public ModuleCrafter() {
		for (int i = 0; i < fuzzyCraftingFlagArray.length; i++) {
			fuzzyCraftingFlagArray[i] = new DictResource((ItemIdentifierStack) null);
		}
	}

	public ModuleCrafter(PipeItemsCraftingLogistics parent) {
		_pipe = parent;
		_service = parent;
		_world = parent;
		registerPosition(ModulePositionType.IN_PIPE, 0);
		for (int i = 0; i < fuzzyCraftingFlagArray.length; i++) {
			fuzzyCraftingFlagArray[i] = new DictResource((ItemIdentifierStack) null);
		}
	}

	/**
	 * assumes that the invProvider is also IRequest items.
	 */
	@Override
	public void registerHandler(IWorldProvider world, IPipeServiceProvider service) {
		super.registerHandler(world, service);
	}

	@Override
	public void registerPosition(ModulePositionType slot, int positionInt) {
		super.registerPosition(slot, positionInt);
		_sinkReply = new SinkReply(FixedPriority.ItemSink, 0, 1, 0, new ChassiTargetInformation(getPositionInt()));
	}

	protected int spaceFor(ItemIdentifier item, boolean includeInTransit) {
		Pair<String, ItemIdentifier> key = new Pair<>("spaceFor", item);
		Object cache = _service.getCacheHolder().getCacheFor(CacheTypes.Inventory, key);
		if (cache != null) {
			int count = (Integer) cache;
			if (includeInTransit) {
				count -= _service.countOnRoute(item);
			}
			return count;
		}
		WorldCoordinatesWrapper worldCoordinates = new WorldCoordinatesWrapper(getWorld(), _service.getX(), _service.getY(), _service.getZ());

		//@formatter:off
		int count = worldCoordinates.getConnectedAdjacentTileEntities(ConnectionPipeType.ITEM)
				.filter(adjacent -> adjacent.tileEntity.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, adjacent.direction.getOpposite()))
		//@formatter:on
				.map(adjacentTileEntity -> {
					EnumFacing dir = adjacentTileEntity.direction.getOpposite();
					if (getUpgradeManager().hasSneakyUpgrade()) {
						dir = getUpgradeManager().getSneakyOrientation();
					}
					IInventoryUtil inv = SimpleServiceLocator.inventoryUtilFactory.getInventoryUtil(adjacentTileEntity.tileEntity, dir);
					return inv.roomForItem(item, 9999); // ToDo: Magic number
				}).reduce(Integer::sum).orElse(0);

		_service.getCacheHolder().setCache(CacheTypes.Inventory, key, count);
		if (includeInTransit) {
			count -= _service.countOnRoute(item);
		}
		return count;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int amount) {
		priority = amount;
	}

	@Override
	public LogisticsModule getSubModule(int slot) {
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
		return _dummyInventory.getIDStackInSlot(9);
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

	public IRouter getSatelliteRouter(int x) {
		if (x == -1) {
			for (final PipeItemsSatelliteLogistics satellite : PipeItemsSatelliteLogistics.AllSatellites) {
				if (satellite.satelliteId == satelliteId) {
					CoreRoutedPipe satPipe = satellite;
					if (satPipe == null || satPipe.stillNeedReplace() || satPipe.getRouter() == null) {
						continue;
					}
					return satPipe.getRouter();
				}
			}
		} else {
			for (final PipeItemsSatelliteLogistics satellite : PipeItemsSatelliteLogistics.AllSatellites) {
				if (satellite.satelliteId == advancedSatelliteIdArray[x]) {
					CoreRoutedPipe satPipe = satellite;
					if (satPipe == null || satPipe.stillNeedReplace() || satPipe.getRouter() == null) {
						continue;
					}
					return satPipe.getRouter();
				}
			}
		}
		return null;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound) {
		//		super.readFromNBT(nbttagcompound);
		_dummyInventory.readFromNBT(nbttagcompound, "");
		_liquidInventory.readFromNBT(nbttagcompound, "FluidInv");
		_cleanupInventory.readFromNBT(nbttagcompound, "CleanupInv");
		satelliteId = nbttagcompound.getInteger("satelliteid");

		priority = nbttagcompound.getInteger("priority");
		for (int i = 0; i < 9; i++) {
			advancedSatelliteIdArray[i] = nbttagcompound.getInteger("advancedSatelliteId" + i);
		}
		if (nbttagcompound.hasKey("fuzzyCraftingFlag0")) {
			for (int i = 0; i < 9; i++) {
				int flags = nbttagcompound.getByte("fuzzyCraftingFlag" + i);
				DictResource dict = fuzzyCraftingFlagArray[i];
				if ((flags & 0x1) != 0) {
					dict.use_od = true;
				}
				if ((flags & 0x2) != 0) {
					dict.ignore_dmg = true;
				}
				if ((flags & 0x4) != 0) {
					dict.ignore_nbt = true;
				}
				if ((flags & 0x8) != 0) {
					dict.use_category = true;
				}
			}
		}
		if (nbttagcompound.hasKey("fuzzyFlags")) {
			NBTTagList lst = nbttagcompound.getTagList("fuzzyFlags", Constants.NBT.TAG_COMPOUND);
			for (int i = 0; i < 9; i++) {
				NBTTagCompound comp = lst.getCompoundTagAt(i);
				fuzzyCraftingFlagArray[i].ignore_dmg = comp.getBoolean("ignore_dmg");
				fuzzyCraftingFlagArray[i].ignore_nbt = comp.getBoolean("ignore_nbt");
				fuzzyCraftingFlagArray[i].use_od = comp.getBoolean("use_od");
				fuzzyCraftingFlagArray[i].use_category = comp.getBoolean("use_category");
			}
		}
		if (nbttagcompound.hasKey("outputFuzzyFlags")) {
			NBTTagCompound comp = nbttagcompound.getCompoundTag("outputFuzzyFlags");
			outputFuzzyFlags.ignore_dmg = comp.getBoolean("ignore_dmg");
			outputFuzzyFlags.ignore_nbt = comp.getBoolean("ignore_nbt");
			outputFuzzyFlags.use_od = comp.getBoolean("use_od");
			outputFuzzyFlags.use_category = comp.getBoolean("use_category");
		}
		for (int i = 0; i < 6; i++) {
			craftingSigns[i] = nbttagcompound.getBoolean("craftingSigns" + i);
		}
		if (nbttagcompound.hasKey("FluidAmount")) {
			amount = nbttagcompound.getIntArray("FluidAmount");
		}
		if (amount.length < ItemUpgrade.MAX_LIQUID_CRAFTER) {
			amount = new int[ItemUpgrade.MAX_LIQUID_CRAFTER];
		}
		for (int i = 0; i < ItemUpgrade.MAX_LIQUID_CRAFTER; i++) {
			liquidSatelliteIdArray[i] = nbttagcompound.getInteger("liquidSatelliteIdArray" + i);
		}
		for (int i = 0; i < ItemUpgrade.MAX_LIQUID_CRAFTER; i++) {
			liquidSatelliteIdArray[i] = nbttagcompound.getInteger("liquidSatelliteIdArray" + i);
		}
		liquidSatelliteId = nbttagcompound.getInteger("liquidSatelliteId");
		cleanupModeIsExclude = nbttagcompound.getBoolean("cleanupModeIsExclude");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound) {
		//	super.writeToNBT(nbttagcompound);
		_dummyInventory.writeToNBT(nbttagcompound, "");
		_liquidInventory.writeToNBT(nbttagcompound, "FluidInv");
		_cleanupInventory.writeToNBT(nbttagcompound, "CleanupInv");
		nbttagcompound.setInteger("satelliteid", satelliteId);

		nbttagcompound.setInteger("priority", priority);
		for (int i = 0; i < 9; i++) {
			nbttagcompound.setInteger("advancedSatelliteId" + i, advancedSatelliteIdArray[i]);
		}
		NBTTagList lst = new NBTTagList();
		for (int i = 0; i < 9; i++) {
			NBTTagCompound comp = new NBTTagCompound();
			comp.setBoolean("ignore_dmg", fuzzyCraftingFlagArray[i].ignore_dmg);
			comp.setBoolean("ignore_nbt", fuzzyCraftingFlagArray[i].ignore_nbt);
			comp.setBoolean("use_od", fuzzyCraftingFlagArray[i].use_od);
			comp.setBoolean("use_category", fuzzyCraftingFlagArray[i].use_category);
			lst.appendTag(comp);
		}
		nbttagcompound.setTag("fuzzyFlags", lst);
		{
			NBTTagCompound comp = new NBTTagCompound();
			comp.setBoolean("ignore_dmg", outputFuzzyFlags.ignore_dmg);
			comp.setBoolean("ignore_nbt", outputFuzzyFlags.ignore_nbt);
			comp.setBoolean("use_od", outputFuzzyFlags.use_od);
			comp.setBoolean("use_category", outputFuzzyFlags.use_category);
			nbttagcompound.setTag("outputFuzzyFlags", comp);
		}
		for (int i = 0; i < 6; i++) {
			nbttagcompound.setBoolean("craftingSigns" + i, craftingSigns[i]);
		}
		for (int i = 0; i < ItemUpgrade.MAX_LIQUID_CRAFTER; i++) {
			nbttagcompound.setInteger("liquidSatelliteIdArray" + i, liquidSatelliteIdArray[i]);
		}
		nbttagcompound.setIntArray("FluidAmount", amount);
		nbttagcompound.setInteger("liquidSatelliteId", liquidSatelliteId);
		nbttagcompound.setBoolean("cleanupModeIsExclude", cleanupModeIsExclude);
	}

	public ModernPacket getCPipePacket() {
		return PacketHandler.getPacket(CraftingPipeUpdatePacket.class).setAmount(amount).setLiquidSatelliteIdArray(liquidSatelliteIdArray).setLiquidSatelliteId(liquidSatelliteId).setSatelliteId(satelliteId).setAdvancedSatelliteIdArray(advancedSatelliteIdArray)
				.setPriority(priority).setModulePos(this);
	}

	public void handleCraftingUpdatePacket(CraftingPipeUpdatePacket packet) {
		amount = packet.getAmount();
		liquidSatelliteIdArray = packet.getLiquidSatelliteIdArray();
		liquidSatelliteId = packet.getLiquidSatelliteId();
		satelliteId = packet.getSatelliteId();
		advancedSatelliteIdArray = packet.getAdvancedSatelliteIdArray();
		priority = packet.getPriority();
	}

	@Override
	protected ModuleCoordinatesGuiProvider getPipeGuiProvider() {
		return NewGuiHandler.getGui(CraftingModuleSlot.class).setAdvancedSat(getUpgradeManager().isAdvancedSatelliteCrafter()).setLiquidCrafter(getUpgradeManager().getFluidCrafter()).setAmount(amount).setHasByproductExtractor(getUpgradeManager().hasByproductExtractor()).setFuzzy(
				getUpgradeManager().isFuzzyUpgrade())
				.setCleanupSize(getUpgradeManager().getCrafterCleanup()).setCleanupExclude(cleanupModeIsExclude);
	}

	@Override
	protected ModuleInHandGuiProvider getInHandGuiProvider() {
		return NewGuiHandler.getGui(CraftingModuleInHand.class).setAmount(amount).setCleanupExclude(cleanupModeIsExclude);
	}

	/**
	 * Simply get the dummy inventory
	 *
	 * @return the dummy inventory
	 */
	public ItemIdentifierInventory getDummyInventory() {
		return _dummyInventory;
	}

	public ItemIdentifierInventory getFluidInventory() {
		return _liquidInventory;
	}

	public IInventory getCleanupInventory() {
		return _cleanupInventory;
	}

	public void setDummyInventorySlot(int slot, ItemStack itemstack) {
		_dummyInventory.setInventorySlotContents(slot, itemstack);
	}

	public void importFromCraftingTable(EntityPlayer player) {
		if (MainProxy.isClient(getWorld())) {
			// Send packet asking for import
			final CoordinatesPacket packet = PacketHandler.getPacket(CPipeSatelliteImport.class).setModulePos(this);
			MainProxy.sendPacketToServer(packet);
		} else {
			WorldCoordinatesWrapper worldCoordinates = new WorldCoordinatesWrapper(getWorld(), getX(), getY(), getZ());

			for (AdjacentTileEntity adjacent : worldCoordinates.getConnectedAdjacentTileEntities(ConnectionPipeType.ITEM).collect(Collectors.toList())) {
				for (ICraftingRecipeProvider provider : SimpleServiceLocator.craftingRecipeProviders) {
					if (provider.importRecipe(adjacent.tileEntity, _dummyInventory)) {
						if (provider instanceof IFuzzyRecipeProvider) {
							((IFuzzyRecipeProvider) provider).importFuzzyFlags(adjacent.tileEntity, _dummyInventory, fuzzyCraftingFlagArray, outputFuzzyFlags);
						}
						// ToDo: break only out of the inner loop?
						break;
					}
				}
			}
			// Send inventory as packet
			final CoordinatesPacket packet = PacketHandler.getPacket(CPipeSatelliteImportBack.class).setInventory(_dummyInventory).setModulePos(this);
			if (player != null) {
				MainProxy.sendPacketToPlayer(packet, player);
			}
			MainProxy.sendPacketToAllWatchingChunk(getX(), getZ(), getWorld().provider.getDimension(), packet);
		}
	}

	protected World getWorld() {
		return _world.getWorld();
	}

	public void priorityUp(EntityPlayer player) {
		priority++;
		if (MainProxy.isClient(player.world)) {
			MainProxy.sendPacketToServer(PacketHandler.getPacket(CraftingPipePriorityUpPacket.class).setModulePos(this));
		} else if (player != null && MainProxy.isServer(player.world)) {
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(CraftingPriority.class).setInteger(priority).setModulePos(this), player);
		}
	}

	public void priorityDown(EntityPlayer player) {
		priority--;
		if (MainProxy.isClient(player.world)) {
			MainProxy.sendPacketToServer(PacketHandler.getPacket(CraftingPipePriorityDownPacket.class).setModulePos(this));
		} else if (player != null && MainProxy.isServer(player.world)) {
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(CraftingPriority.class).setInteger(priority).setModulePos(this), player);
		}
	}

	public ItemIdentifierStack getByproductItem() {
		return _dummyInventory.getIDStackInSlot(10);
	}

	public ItemIdentifierStack getMaterials(int slotnr) {
		return _dummyInventory.getIDStackInSlot(slotnr);
	}

	public FluidIdentifier getFluidMaterial(int slotnr) {
		ItemIdentifierStack stack = _liquidInventory.getIDStackInSlot(slotnr);
		if (stack == null) {
			return null;
		}
		return FluidIdentifier.get(stack.getItem());
	}

	public void setNextSatellite(EntityPlayer player, int i) {
		if (MainProxy.isClient(player.world)) {
			MainProxy.sendPacketToServer(PacketHandler.getPacket(CraftingPipeNextAdvancedSatellitePacket.class).setInteger(i).setModulePos(this));
		} else {
			advancedSatelliteIdArray[i] = getNextConnectSatelliteId(false, i);
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(CraftingAdvancedSatelliteId.class).setInteger2(i).setInteger(advancedSatelliteIdArray[i]).setModulePos(this), player);
		}
	}

	public void setPrevSatellite(EntityPlayer player, int i) {
		if (MainProxy.isClient(player.world)) {
			MainProxy.sendPacketToServer(PacketHandler.getPacket(CraftingPipePrevAdvancedSatellitePacket.class).setInteger(i).setModulePos(this));
		} else {
			advancedSatelliteIdArray[i] = getNextConnectSatelliteId(true, i);
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(CraftingAdvancedSatelliteId.class).setInteger2(i).setInteger(advancedSatelliteIdArray[i]).setModulePos(this), player);
		}
	}

	public void changeFluidAmount(int change, int slot, EntityPlayer player) {
		if (MainProxy.isClient(player.world)) {
			MainProxy.sendPacketToServer(PacketHandler.getPacket(FluidCraftingAmount.class).setInteger2(slot).setInteger(change).setModulePos(this));
		} else {
			amount[slot] += change;
			if (amount[slot] <= 0) {
				amount[slot] = 0;
			}
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(FluidCraftingAmount.class).setInteger2(slot).setInteger(amount[slot]).setModulePos(this), player);
		}
	}

	public void setPrevFluidSatellite(EntityPlayer player, int i) {
		if (MainProxy.isClient(player.world)) {
			MainProxy.sendPacketToServer(PacketHandler.getPacket(FluidCraftingPipeAdvancedSatellitePrevPacket.class).setInteger(i).setModulePos(this));
		} else {
			if (i == -1) {
				liquidSatelliteId = getNextConnectFluidSatelliteId(true, i);
				MainProxy.sendPacketToPlayer(PacketHandler.getPacket(FluidCraftingAdvancedSatelliteId.class).setInteger2(i).setInteger(liquidSatelliteId).setModulePos(this), player);
			} else {
				liquidSatelliteIdArray[i] = getNextConnectFluidSatelliteId(true, i);
				MainProxy.sendPacketToPlayer(PacketHandler.getPacket(FluidCraftingAdvancedSatelliteId.class).setInteger2(i).setInteger(liquidSatelliteIdArray[i]).setModulePos(this), player);
			}
		}
	}

	public void setNextFluidSatellite(EntityPlayer player, int i) {
		if (MainProxy.isClient(player.world)) {
			MainProxy.sendPacketToServer(PacketHandler.getPacket(FluidCraftingPipeAdvancedSatelliteNextPacket.class).setInteger(i).setModulePos(this));
		} else {
			if (i == -1) {
				liquidSatelliteId = getNextConnectFluidSatelliteId(false, i);
				MainProxy.sendPacketToPlayer(PacketHandler.getPacket(FluidCraftingAdvancedSatelliteId.class).setInteger2(i).setInteger(liquidSatelliteId).setModulePos(this), player);
			} else {
				liquidSatelliteIdArray[i] = getNextConnectFluidSatelliteId(false, i);
				MainProxy.sendPacketToPlayer(PacketHandler.getPacket(FluidCraftingAdvancedSatelliteId.class).setInteger2(i).setInteger(liquidSatelliteIdArray[i]).setModulePos(this), player);
			}
		}
	}

	public void defineFluidAmount(int integer, int slot) {
		if (MainProxy.isClient(getWorld())) {
			amount[slot] = integer;
		}
	}

	public int[] getFluidAmount() {
		return amount;
	}

	public void setFluidAmount(int[] amount) {
		if (MainProxy.isClient(getWorld())) {
			this.amount = amount;
		}
	}

	public void setFluidSatelliteId(int integer, int slot) {
		if (slot == -1) {
			liquidSatelliteId = integer;
		} else {
			liquidSatelliteIdArray[slot] = integer;
		}
	}

	public IRouter getFluidSatelliteRouter(int x) {
		if (x == -1) {
			for (final PipeFluidSatellite satellite : PipeFluidSatellite.AllSatellites) {
				if (satellite.satelliteId == liquidSatelliteId) {
					CoreRoutedPipe satPipe = satellite;
					if (satPipe == null || satPipe.stillNeedReplace() || satPipe.getRouter() == null) {
						continue;
					}
					return satPipe.getRouter();
				}
			}
		} else {
			for (final PipeFluidSatellite satellite : PipeFluidSatellite.AllSatellites) {
				if (satellite.satelliteId == liquidSatelliteIdArray[x]) {
					CoreRoutedPipe satPipe = satellite;
					if (satPipe == null || satPipe.stillNeedReplace() || satPipe.getRouter() == null) {
						continue;
					}
					return satPipe.getRouter();
				}
			}
		}
		return null;
	}

	public void openAttachedGui(EntityPlayer player) {
		if (MainProxy.isClient(player.world)) {
			if (player instanceof EntityPlayerMP) {
				player.closeScreen();
			} else if (player instanceof EntityPlayerSP) {
				player.closeScreen();
			}
			MainProxy.sendPacketToServer(PacketHandler.getPacket(CraftingPipeOpenConnectedGuiPacket.class).setModulePos(this));
			return;
		}

		// hack to avoid wrenching blocks
		int savedEquipped = player.inventory.currentItem;
		boolean foundSlot = false;
		// try to find a empty slot
		for (int i = 0; i < 9; i++) {
			if (player.inventory.getStackInSlot(i) == null) {
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

		WorldCoordinatesWrapper worldCoordinates = new WorldCoordinatesWrapper(getWorld(), getX(), getY(), getZ());

		worldCoordinates.getConnectedAdjacentTileEntities(ConnectionPipeType.ITEM).anyMatch(adjacent -> {
			boolean found = SimpleServiceLocator.craftingRecipeProviders.stream().anyMatch(provider -> provider.canOpenGui(adjacent.tileEntity));

			if (!found) {
				found = (adjacent.tileEntity instanceof IInventory);
			}

			if (found) {
				Block block = getWorld().getBlockState(adjacent.tileEntity.getPos()).getBlock();
				if (block != null && block
						.onBlockActivated(getWorld(), adjacent.tileEntity.getPos(), adjacent.tileEntity.getWorld().getBlockState(adjacent.tileEntity.getPos()), player,
								EnumHand.MAIN_HAND, EnumFacing.UP, 0, 0, 0)) {
					return true;
				}
			}
			return false;
		});
		player.inventory.currentItem = savedEquipped;
	}

	public boolean areAllOrderesToBuffer() {
		return cachedAreAllOrderesToBuffer;
	}

	protected int neededEnergy() {
		return (int) (10 * Math.pow(1.1, getUpgradeManager().getItemExtractionUpgrade()) * Math.pow(1.2, getUpgradeManager().getItemStackExtractionUpgrade()))	;
	}

	protected int itemsToExtract() {
		return (int) Math.pow(2, getUpgradeManager().getItemExtractionUpgrade());
	}

	protected int stacksToExtract() {
		return 1 + getUpgradeManager().getItemStackExtractionUpgrade();
	}

	public List<AdjacentTileEntity> locateCrafters() {
		if (cachedCrafters == null) {
			//@formatter:off
			cachedCrafters = new WorldCoordinatesWrapper(getWorld(), getX(), getY(), getZ())
					.getConnectedAdjacentTileEntities(ConnectionPipeType.ITEM)
					.filter(adjacent ->
							adjacent.tileEntity instanceof IInventory
							|| SimpleServiceLocator.inventoryUtilFactory.getInventoryUtil(adjacent.tileEntity, adjacent.direction.getOpposite()) != null
					).collect(Collectors.toList());
			//formatter:on
		}

		return cachedCrafters;
	}

	public void clearCraftersCache() {
		cachedCrafters = null;
	}

	@Override
	public void clearCache() {
		clearCraftersCache();
	}

	public void importCleanup() {
		for (int i = 0; i < 10; i++) {
			_cleanupInventory.setInventorySlotContents(i, _dummyInventory.getStackInSlot(i));
		}
		for (int i = 10; i < _cleanupInventory.getSizeInventory(); i++) {
			_cleanupInventory.setInventorySlotContents(i, (ItemStack) null);
		}
		_cleanupInventory.compactFirst(10);
		_cleanupInventory.recheckStackLimit();
		cleanupModeIsExclude = false;
	}

	public void toogleCleaupMode() {
		cleanupModeIsExclude = !cleanupModeIsExclude;
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
}
