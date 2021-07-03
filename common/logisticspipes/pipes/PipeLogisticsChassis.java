/*
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.pipes;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TranslationTextComponent;

import lombok.Getter;

import logisticspipes.LogisticsPipes;
import logisticspipes.config.Configs;
import logisticspipes.gui.hud.HudChassisPipe;
import logisticspipes.interfaces.IBufferItems;
import logisticspipes.interfaces.IHeadUpDisplayRenderer;
import logisticspipes.interfaces.IHeadUpDisplayRendererProvider;
import logisticspipes.interfaces.IInventoryUtil;
import logisticspipes.interfaces.ILegacyActiveModule;
import logisticspipes.interfaces.ISendQueueContentRecieiver;
import logisticspipes.interfaces.ISendRoutedItem;
import logisticspipes.interfaces.ISlotUpgradeManager;
import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.interfaces.routing.ICraftItems;
import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.interfaces.routing.IProvideItems;
import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.interfaces.routing.IRequireReliableTransport;
import logisticspipes.items.ItemModule;
import logisticspipes.logisticspipes.ChassisTransportLayer;
import logisticspipes.logisticspipes.ItemModuleInformationManager;
import logisticspipes.logisticspipes.TransportLayer;
import logisticspipes.modules.ChassisModule;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.modules.LogisticsModule.ModulePositionType;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.hud.HUDStartWatchingPacket;
import logisticspipes.network.packets.hud.HUDStopWatchingPacket;
import logisticspipes.network.packets.pipe.ChassisOrientationPacket;
import logisticspipes.network.packets.pipe.ChassisPipeModuleContent;
import logisticspipes.network.packets.pipe.RequestChassisOrientationPacket;
import logisticspipes.network.packets.pipe.SendQueueContent;
import logisticspipes.pipefxhandlers.Particles;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.upgrades.ModuleUpgradeManager;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.request.ICraftingTemplate;
import logisticspipes.request.IPromise;
import logisticspipes.request.RequestTree;
import logisticspipes.request.RequestTreeNode;
import logisticspipes.request.resources.DictResource;
import logisticspipes.request.resources.IResource;
import logisticspipes.routing.LogisticsPromise;
import logisticspipes.routing.order.IOrderInfoProvider.ResourceType;
import logisticspipes.routing.order.LogisticsItemOrder;
import logisticspipes.routing.order.LogisticsOrder;
import logisticspipes.security.SecuritySettings;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.ticks.HudUpdateTick;
import logisticspipes.utils.DirectionUtil;
import logisticspipes.utils.ISimpleInventoryEventHandler;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.tuples.Pair;
import network.rs485.logisticspipes.connection.Adjacent;
import network.rs485.logisticspipes.connection.ConnectionType;
import network.rs485.logisticspipes.connection.NeighborTileEntity;
import network.rs485.logisticspipes.connection.NoAdjacent;
import network.rs485.logisticspipes.connection.SingleAdjacent;
import network.rs485.logisticspipes.module.PipeServiceProviderUtilKt;
import network.rs485.logisticspipes.pipes.IChassisPipe;

public abstract class PipeLogisticsChassis extends CoreRoutedPipe
		implements ICraftItems, IBufferItems, ISimpleInventoryEventHandler, ISendRoutedItem, IProvideItems,
		IHeadUpDisplayRendererProvider, ISendQueueContentRecieiver, IChassisPipe {

	private final ChassisModule _module;
	private final ItemIdentifierInventory _moduleInventory;
	private final NonNullList<ModuleUpgradeManager> slotUpgradeManagers = NonNullList.create();
	private boolean init = false;

	// HUD
	public final LinkedList<ItemIdentifierStack> displayList = new LinkedList<>();
	public final PlayerCollectionList localModeWatchers = new PlayerCollectionList();
	private final HudChassisPipe hud;

	@Nullable
	private SingleAdjacent pointedAdjacent = null;

	public PipeLogisticsChassis(Item item) {
		super(item);
		_moduleInventory = new ItemIdentifierInventory(getChassisSize(), "Chassi pipe", 1);
		_moduleInventory.addListener(this);
		assert slotUpgradeManagers.size() == 0; // starting at index 0
		for (int i = 0; i < getChassisSize(); i++) {
			addModuleUpgradeManager();
		}
		_module = new ChassisModule(getChassisSize(), this);
		_module.registerHandler(this, this);
		hud = new HudChassisPipe(this, _moduleInventory);
	}

	/**
	 * Returns the pointed adjacent Direction or null, if this chassis does not have an attached inventory.
	 */
	@Nullable
	@Override
	public Direction getPointedOrientation() {
		if (pointedAdjacent == null) return null;
		return pointedAdjacent.getDir();
	}

	@Nonnull
	protected Adjacent getPointedAdjacentOrNoAdjacent() {
		// for public access, use getAvailableAdjacent()
		if (pointedAdjacent == null) {
			return NoAdjacent.INSTANCE;
		} else {
			return pointedAdjacent;
		}
	}

	/**
	 * Returns just the adjacent this chassis points at or no adjacent.
	 */
	@Nonnull
	@Override
	public Adjacent getAvailableAdjacent() {
		return getPointedAdjacentOrNoAdjacent();
	}

	/**
	 * Updates pointedAdjacent on {@link CoreRoutedPipe}.
	 */
	@Override
	protected void updateAdjacentCache() {
		super.updateAdjacentCache();
		final Adjacent adjacent = getAdjacent();
		if (adjacent instanceof SingleAdjacent) {
			pointedAdjacent = ((SingleAdjacent) adjacent);
		} else {
			final SingleAdjacent oldPointedAdjacent = pointedAdjacent;
			SingleAdjacent newPointedAdjacent = null;
			if (oldPointedAdjacent != null) {
				// update pointed adjacent with connection type or reset it
				newPointedAdjacent = adjacent.optionalGet(oldPointedAdjacent.getDirection()).map(connectionType -> new SingleAdjacent(this, oldPointedAdjacent.getDirection(), connectionType)).orElse(null);
			}
			if (newPointedAdjacent == null) {
				newPointedAdjacent = adjacent.neighbors().entrySet().stream().findAny().map(connectedNeighbor -> new SingleAdjacent(this, connectedNeighbor.getKey().getDirection(), connectedNeighbor.getValue())).orElse(null);
			}
			pointedAdjacent = newPointedAdjacent;
		}
	}

	@Nullable
	private Pair<NeighborTileEntity<TileEntity>, ConnectionType> nextPointedOrientation(@Nullable Direction previousDirection) {
		final Map<NeighborTileEntity<TileEntity>, ConnectionType> neighbors = getAdjacent().neighbors();
		final Stream<NeighborTileEntity<TileEntity>> sortedNeighborsStream = neighbors.keySet().stream()
				.sorted(Comparator.comparingInt(n -> n.getDirection().ordinal()));
		if (previousDirection == null) {
			return sortedNeighborsStream.findFirst().map(neighbor -> new Pair<>(neighbor, neighbors.get(neighbor))).orElse(null);
		} else {
			final List<NeighborTileEntity<TileEntity>> sortedNeighbors = sortedNeighborsStream.collect(Collectors.toList());
			if (sortedNeighbors.size() == 0) return null;
			final Optional<NeighborTileEntity<TileEntity>> nextNeighbor = sortedNeighbors.stream()
					.filter(neighbor -> neighbor.getDirection().ordinal() > previousDirection.ordinal())
					.findFirst();
			return nextNeighbor.map(neighbor -> new Pair<>(neighbor, neighbors.get(neighbor)))
					.orElse(new Pair<>(sortedNeighbors.get(0), neighbors.get(sortedNeighbors.get(0))));
		}
	}

	@Override
	public void nextOrientation() {
		final SingleAdjacent pointedAdjacent = this.pointedAdjacent;
		Pair<NeighborTileEntity<TileEntity>, ConnectionType> newNeighbor;
		if (pointedAdjacent == null) {
			newNeighbor = nextPointedOrientation(null);
		} else {
			newNeighbor = nextPointedOrientation(pointedAdjacent.getDirection());
		}
		final ChassisOrientationPacket packet = PacketHandler.getPacket(ChassisOrientationPacket.class);
		if (newNeighbor == null) {
			this.pointedAdjacent = null;
			packet.setDir(null);
		} else {
			this.pointedAdjacent = new SingleAdjacent(
					this, newNeighbor.getValue1().getDirection(), newNeighbor.getValue2());
			packet.setDir(newNeighbor.getValue1().getDirection());
		}
		MainProxy.sendPacketToAllWatchingChunk(_module, packet.setTilePos(container));
		refreshRender(true);
	}

	@Override
	public void setPointedOrientation(@Nullable Direction direction) {
		if (direction == null) {
			pointedAdjacent = null;
		} else {
			pointedAdjacent = new SingleAdjacent(this, direction, ConnectionType.UNDEFINED);
		}
	}

	@Override
	@Nonnull
	public IInventory getModuleInventory() {
		return _moduleInventory;
	}

	@Nonnull
	public ModuleUpgradeManager getModuleUpgradeManager(int slot) {
		return slotUpgradeManagers.get(slot);
	}

	@Override
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_TEXTURE;
	}

	@Override
	public TextureType getRoutedTexture(Direction connection) {
		if (getRouter().isSubPoweredExit(connection)) {
			return Textures.LOGISTICSPIPE_SUBPOWER_TEXTURE;
		}
		return Textures.LOGISTICSPIPE_CHASSI_ROUTED_TEXTURE;
	}

	@Override
	public TextureType getNonRoutedTexture(Direction connection) {
		if (pointedAdjacent != null && connection.equals(pointedAdjacent.getDirection())) {
			return Textures.LOGISTICSPIPE_CHASSI_DIRECTION_TEXTURE;
		}
		if (isPowerProvider(connection)) {
			return Textures.LOGISTICSPIPE_POWERED_TEXTURE;
		}
		return Textures.LOGISTICSPIPE_CHASSI_NOTROUTED_TEXTURE;
	}

	@Override
	public void readFromNBT(CompoundNBT tag) {
		try {
			super.readFromNBT(tag);
			_moduleInventory.readFromNBT(tag, "chassi");
			InventoryChanged(_moduleInventory);
			_module.readFromNBT(tag);
			int tmp = tag.getInt("Orientation");
			if (tmp != -1) {
				setPointedOrientation(DirectionUtil.getOrientation(tmp % 6));
			}
			for (int i = 0; i < getChassisSize(); i++) {
				if (i >= slotUpgradeManagers.size()) {
					addModuleUpgradeManager();
				}
				slotUpgradeManagers.get(i).readFromNBT(tag, Integer.toString(i));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void addModuleUpgradeManager() {
		slotUpgradeManagers.add(new ModuleUpgradeManager(this, upgradeManager));
	}

	@Override
	public void writeToNBT(CompoundNBT tag) {
		super.writeToNBT(tag);
		_moduleInventory.writeToNBT(tag, "chassi");
		_module.writeToNBT(tag);
		tag.putInt("Orientation", pointedAdjacent == null ? -1 : pointedAdjacent.getDirection().ordinal());
		for (int i = 0; i < getChassisSize(); i++) {
			slotUpgradeManagers.get(i).writeToNBT(tag, Integer.toString(i));
		}
	}

	@Override
	public void onAllowedRemoval() {
		_moduleInventory.removeListener(this);
		if (MainProxy.isServer(getWorld())) {
			for (int i = 0; i < getChassisSize(); i++) {
				LogisticsModule x = getSubModule(i);
				if (x instanceof ILegacyActiveModule) {
					ILegacyActiveModule y = (ILegacyActiveModule) x;
					y.onBlockRemoval();
				}
			}
			for (int i = 0; i < _moduleInventory.getSizeInventory(); i++) {
				ItemIdentifierStack ms = _moduleInventory.getIDStackInSlot(i);
				if (ms != null) {
					ItemStack stack = ms.makeNormalStack();
					ItemModuleInformationManager.saveInformation(stack, getSubModule(i));
					_moduleInventory.setInventorySlotContents(i, stack);
				}
			}
			_moduleInventory.dropContents(getWorld(), getX(), getY(), getZ());

			for (int i = 0; i < getChassisSize(); i++) {
				getModuleUpgradeManager(i).dropUpgrades();
			}
		}
	}

	@Override
	public void itemArrived(ItemIdentifierStack item, IAdditionalTargetInformation info) {
		if (MainProxy.isServer(getWorld())) {
			if (info instanceof ChassiTargetInformation) {
				ChassiTargetInformation target = (ChassiTargetInformation) info;
				LogisticsModule module = getSubModule(target.moduleSlot);
				if (module instanceof IRequireReliableTransport) {
					((IRequireReliableTransport) module).itemArrived(item, info);
				}
			} else {
				if (LogisticsPipes.isDEBUG() && info != null) {
					System.out.println(item);
					new RuntimeException("[ItemArrived] Information weren't ment for a chassi pipe").printStackTrace();
				}
			}
		}
	}

	@Override
	public void itemLost(ItemIdentifierStack item, IAdditionalTargetInformation info) {
		if (MainProxy.isServer(getWorld())) {
			if (info instanceof ChassiTargetInformation) {
				ChassiTargetInformation target = (ChassiTargetInformation) info;
				LogisticsModule module = getSubModule(target.moduleSlot);
				if (module instanceof IRequireReliableTransport) {
					((IRequireReliableTransport) module).itemLost(item, info);
				}
			} else {
				if (LogisticsPipes.isDEBUG()) {
					System.out.println(item);
					new RuntimeException("[ItemLost] Information weren't ment for a chassi pipe").printStackTrace();
				}
			}
		}
	}

	@Override
	public int addToBuffer(ItemIdentifierStack item, IAdditionalTargetInformation info) {
		if (MainProxy.isServer(getWorld())) {
			if (info instanceof ChassiTargetInformation) {
				ChassiTargetInformation target = (ChassiTargetInformation) info;
				LogisticsModule module = getSubModule(target.moduleSlot);
				if (module instanceof IBufferItems) {
					return ((IBufferItems) module).addToBuffer(item, info);
				}
			} else {
				if (LogisticsPipes.isDEBUG()) {
					System.out.println(item);
					new RuntimeException("[AddToBuffer] Information weren't ment for a chassi pipe").printStackTrace();
				}
			}
		}
		return item.getStackSize();
	}

	@Override
	public void InventoryChanged(IInventory inventory) {
		boolean reInitGui = false;
		for (int i = 0; i < inventory.getSizeInventory(); i++) {
			ItemStack stack = inventory.getStackInSlot(i);
			if (stack.isEmpty()) {
				if (_module.hasModule(i)) {
					_module.removeModule(i);
					reInitGui = true;
				}
				continue;
			}

			if (stack.getItem() instanceof ItemModule) {
				LogisticsModule current = _module.getModule(i);
				LogisticsModule next = ((ItemModule) stack.getItem()).getModuleForItem(stack, _module.getModule(i), this, this);
				Objects.requireNonNull(next, "getModuleForItem returned null for " + stack.toString());
				next.registerPosition(ModulePositionType.SLOT, i);
				if (current != next) {
					_module.installModule(i, next);
					if (!MainProxy.isClient(getWorld())) {
						ItemModuleInformationManager.readInformation(stack, next);
					}
				}
				inventory.setInventorySlotContents(i, stack);
			}
		}
		if (MainProxy.isServer(getWorld())) {
			if (!localModeWatchers.isEmpty()) {
				MainProxy.sendToPlayerList(PacketHandler.getPacket(ChassisPipeModuleContent.class).setIdentList(ItemIdentifierStack.getListFromInventory(_moduleInventory)).setPosX(getX()).setPosY(getY()).setPosZ(getZ()), localModeWatchers);
			}
		}
	}

	@Override
	public void ignoreDisableUpdateEntity() {
		if (!init) {
			init = true;
			if (MainProxy.isClient(getWorld())) {
				MainProxy.sendPacketToServer(PacketHandler.getPacket(RequestChassisOrientationPacket.class).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
			}
		}
	}

	@Override
	public final LogisticsModule getLogisticsModule() {
		return _module;
	}

	@Nonnull
	@Override
	public TransportLayer getTransportLayer() {
		if (_transportLayer == null) {
			_transportLayer = new ChassisTransportLayer(this);
		}
		return _transportLayer;
	}

	private boolean tryInsertingModule(PlayerEntity player) {
		for (int i = 0; i < _moduleInventory.getSizeInventory(); i++) {
			if (_moduleInventory.getIDStackInSlot(i) == null) {
				_moduleInventory.setInventorySlotContents(i, player.getItemStackFromSlot(EquipmentSlotType.MAINHAND).split(1));
				InventoryChanged(_moduleInventory);
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean handleClick(PlayerEntity player, SecuritySettings settings) {
		if (player.getItemStackFromSlot(EquipmentSlotType.MAINHAND).isEmpty()) {
			return false;
		}

		if (player.isSneaking() && SimpleServiceLocator.configToolHandler.canWrench(player, player.getItemStackFromSlot(EquipmentSlotType.MAINHAND), container)) {
			if (MainProxy.isServer(getWorld())) {
				if (settings == null || settings.openGui) {
					((PipeLogisticsChassis) container.pipe).nextOrientation();
				} else {
					player.sendMessage(new TranslationTextComponent("lp.chat.permissiondenied"));
				}
			}
			SimpleServiceLocator.configToolHandler.wrenchUsed(player, player.getItemStackFromSlot(EquipmentSlotType.MAINHAND), container);
			return true;
		}

		if (!player.isSneaking() && player.getItemStackFromSlot(EquipmentSlotType.MAINHAND).getItem() instanceof ItemModule) {
			if (MainProxy.isServer(getWorld())) {
				if (settings == null || settings.openGui) {
					return tryInsertingModule(player);
				} else {
					player.sendMessage(new TranslationTextComponent("lp.chat.permissiondenied"));
				}
			}
			return true;
		}

		return false;
	}

	/*** IProvideItems ***/
	@Override
	public void canProvide(RequestTreeNode tree, RequestTree root, List<IFilter> filters) {
		if (!isEnabled()) {
			return;
		}
		for (IFilter filter : filters) {
			if (filter.isBlocked() == filter.isFilteredItem(tree.getRequestType()) || filter.blockProvider()) {
				return;
			}
		}
		for (int i = 0; i < getChassisSize(); i++) {
			LogisticsModule x = getSubModule(i);
			if (x instanceof ILegacyActiveModule) {
				ILegacyActiveModule y = (ILegacyActiveModule) x;
				y.canProvide(tree, root, filters);
			}
		}
	}

	@Override
	public LogisticsOrder fullFill(LogisticsPromise promise, IRequestItems destination, IAdditionalTargetInformation info) {
		if (!isEnabled()) {
			return null;
		}
		for (int i = 0; i < getChassisSize(); i++) {
			LogisticsModule x = getSubModule(i);
			if (x instanceof ILegacyActiveModule) {
				ILegacyActiveModule y = (ILegacyActiveModule) x;
				LogisticsOrder result = y.fullFill(promise, destination, info);
				if (result != null) {
					spawnParticle(Particles.WhiteParticle, 2);
					return result;
				}
			}
		}
		return null;
	}

	@Override
	public void getAllItems(Map<ItemIdentifier, Integer> list, List<IFilter> filter) {
		if (!isEnabled()) {
			return;
		}
		for (int i = 0; i < getChassisSize(); i++) {
			LogisticsModule x = getSubModule(i);
			if (x instanceof ILegacyActiveModule) {
				ILegacyActiveModule y = (ILegacyActiveModule) x;
				y.getAllItems(list, filter);
			}
		}
	}

	@Override
	public ItemSendMode getItemSendMode() {
		return ItemSendMode.Normal;
	}

	@Override
	public IHeadUpDisplayRenderer getRenderer() {
		return hud;
	}

	@Override
	public void startWatching() {
		MainProxy.sendPacketToServer(PacketHandler.getPacket(HUDStartWatchingPacket.class).setInteger(1).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
	}

	@Override
	public void stopWatching() {
		MainProxy.sendPacketToServer(PacketHandler.getPacket(HUDStopWatchingPacket.class).setInteger(1).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
		hud.stopWatching();
	}

	@Override
	public void playerStartWatching(PlayerEntity player, int mode) {
		if (mode == 1) {
			localModeWatchers.add(player);
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(ChassisPipeModuleContent.class).setIdentList(ItemIdentifierStack.getListFromInventory(_moduleInventory)).setPosX(getX()).setPosY(getY()).setPosZ(getZ()), player);
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(SendQueueContent.class).setIdentList(ItemIdentifierStack.getListSendQueue(_sendQueue)).setPosX(getX()).setPosY(getY()).setPosZ(getZ()), player);
		} else {
			super.playerStartWatching(player, mode);
		}
	}

	@Override
	public void playerStopWatching(PlayerEntity player, int mode) {
		super.playerStopWatching(player, mode);
		localModeWatchers.remove(player);
	}

	public void handleModuleItemIdentifierList(Collection<ItemIdentifierStack> _allItems) {
		_moduleInventory.handleItemIdentifierList(_allItems);
	}

	@Override
	public int sendQueueChanged(boolean force) {
		if (MainProxy.isServer(getWorld())) {
			if (Configs.MULTI_THREAD_NUMBER > 0 && !force) {
				HudUpdateTick.add(getRouter());
			} else {
				if (localModeWatchers.size() > 0) {
					LinkedList<ItemIdentifierStack> items = ItemIdentifierStack.getListSendQueue(_sendQueue);
					MainProxy.sendToPlayerList(PacketHandler.getPacket(SendQueueContent.class).setIdentList(items).setPosX(getX()).setPosY(getY()).setPosZ(getZ()), localModeWatchers);
					return items.size();
				}
			}
		}
		return 0;
	}

	@Override
	public void handleSendQueueItemIdentifierList(Collection<ItemIdentifierStack> _allItems) {
		displayList.clear();
		displayList.addAll(_allItems);
	}

	public ChassisModule getModules() {
		return _module;
	}

	@Override
	public void setTile(TileEntity tile) {
		super.setTile(tile);
		for (int i = 0; i < _moduleInventory.getSizeInventory(); i++) {
			LogisticsModule current = _module.getModule(i);
			if (current != null) {
				current.registerPosition(ModulePositionType.SLOT, i);
			}
		}
	}

	@Override
	public void collectSpecificInterests(@Nonnull Collection<ItemIdentifier> itemidCollection) {
		// if we don't have a pointed inventory we can't be interested in anything
		if (getPointedAdjacentOrNoAdjacent().inventories().isEmpty()) {
			return;
		}

		for (int moduleIndex = 0; moduleIndex < getChassisSize(); moduleIndex++) {
			LogisticsModule module = getSubModule(moduleIndex);
			if (module != null && module.interestedInAttachedInventory()) {
				final ISlotUpgradeManager upgradeManager = getUpgradeManager(module.getSlot(), module.getPositionInt());
				IInventoryUtil inv = PipeServiceProviderUtilKt.availableSneakyInventories(this, upgradeManager).stream().findFirst().orElse(null);
				if (inv == null) {
					continue;
				}
				Set<ItemIdentifier> items = inv.getItems();
				itemidCollection.addAll(items);

				//also add tag-less variants ... we should probably add a module.interestedIgnoringNBT at some point
				items.stream().map(ItemIdentifier::getIgnoringNBT).forEach(itemidCollection::add);

				boolean modulesInterestedInUndamged = false;
				for (int i = 0; i < getChassisSize(); i++) {
					if (getSubModule(moduleIndex).interestedInUndamagedID()) {
						modulesInterestedInUndamged = true;
						break;
					}
				}
				if (modulesInterestedInUndamged) {
					items.stream().map(ItemIdentifier::getUndamaged).forEach(itemidCollection::add);
				}
				break; // no need to check other modules for interest in the inventory, when we know that 1 already is.
			}
		}
		for (int i = 0; i < getChassisSize(); i++) {
			LogisticsModule module = getSubModule(i);
			if (module != null) {
				module.collectSpecificInterests(itemidCollection);
			}
		}
	}

	@Override
	public boolean hasGenericInterests() {
		if (getPointedAdjacentOrNoAdjacent().inventories().isEmpty()) {
			return false;
		}
		for (int i = 0; i < getChassisSize(); i++) {
			LogisticsModule x = getSubModule(i);

			if (x != null && x.hasGenericInterests()) {
				return true;
			}
		}
		return false;
	}

	/** ICraftItems */
	public final LinkedList<LogisticsOrder> _extras = new LinkedList<>();

	@Override
	public void registerExtras(IPromise promise) {
		if (!(promise instanceof LogisticsPromise)) {
			throw new UnsupportedOperationException("Extra has to be an item for a chassis pipe");
		}
		ItemIdentifierStack stack = new ItemIdentifierStack(((LogisticsPromise) promise).item, ((LogisticsPromise) promise).numberOfItems);
		_extras.add(new LogisticsItemOrder(new DictResource(stack, null), null, ResourceType.EXTRA, null));
	}

	@Override
	public ICraftingTemplate addCrafting(IResource toCraft) {
		for (int i = 0; i < getChassisSize(); i++) {
			LogisticsModule x = getSubModule(i);

			if (x instanceof ICraftItems) {
				if (((ICraftItems) x).canCraft(toCraft)) {
					return ((ICraftItems) x).addCrafting(toCraft);
				}
			}
		}
		return null;

		// trixy code goes here to ensure the right crafter answers the right request
	}

	@Override
	public List<ItemIdentifierStack> getCraftedItems() {
		List<ItemIdentifierStack> craftables = null;
		for (int i = 0; i < getChassisSize(); i++) {
			LogisticsModule x = getSubModule(i);

			if (x instanceof ICraftItems) {
				if (craftables == null) {
					craftables = new LinkedList<>();
				}
				craftables.addAll(((ICraftItems) x).getCraftedItems());
			}
		}
		return craftables;
	}

	@Override
	public boolean canCraft(IResource toCraft) {
		for (int i = 0; i < getChassisSize(); i++) {
			LogisticsModule x = getSubModule(i);

			if (x instanceof ICraftItems) {
				if (((ICraftItems) x).canCraft(toCraft)) {
					return true;
				}
			}
		}
		return false;
	}

	@Nonnull
	@Override
	public ISlotUpgradeManager getUpgradeManager(ModulePositionType slot, int positionInt) {
		if (slot != ModulePositionType.SLOT || positionInt >= slotUpgradeManagers.size()) {
			if (LogisticsPipes.isDEBUG()) {
				new UnsupportedOperationException("Position info aren't for a chassis pipe. (" + slot + "/" + positionInt + ")").printStackTrace();
			}
			return super.getUpgradeManager(slot, positionInt);
		}
		return slotUpgradeManagers.get(positionInt);
	}

	@Override
	public int getTodo() {
		// TODO Auto-generated method stub
		// probably not needed, the chasi order manager handles the count, would need to store origin to specifically know this.
		return 0;
	}

	@Nullable
	public LogisticsModule getSubModule(int slot) {
		return _module.getModule(slot);
	}

	public static class ChassiTargetInformation implements IAdditionalTargetInformation {

		@Getter
		private final int moduleSlot;

		public ChassiTargetInformation(int slot) {
			moduleSlot = slot;
		}
	}
}
