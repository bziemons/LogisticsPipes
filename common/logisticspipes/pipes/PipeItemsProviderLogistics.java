/**
 * Copyright (c) Krapht, 2011
 * 
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.pipes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;

import logisticspipes.LogisticsPipes;
import logisticspipes.gui.hud.HUDProvider;
import logisticspipes.interfaces.IChangeListener;
import logisticspipes.interfaces.IChestContentReceiver;
import logisticspipes.interfaces.IHeadUpDisplayRenderer;
import logisticspipes.interfaces.IHeadUpDisplayRendererProvider;
import logisticspipes.interfaces.IInventoryUtil;
import logisticspipes.interfaces.IOrderManagerContentReceiver;
import logisticspipes.logisticspipes.ExtractionMode;
import logisticspipes.modules.abstractmodules.LogisticsModule;
import logisticspipes.network.GuiIDs;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.hud.ChestContent;
import logisticspipes.network.packets.hud.HUDStartWatchingPacket;
import logisticspipes.network.packets.hud.HUDStopWatchingPacket;
import logisticspipes.network.packets.modules.ProviderPipeInclude;
import logisticspipes.network.packets.modules.ProviderPipeMode;
import logisticspipes.network.packets.orderer.OrdererManagerContent;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.routing.pathfinder.IPipeInformationProvider.ConnectionPipeType;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;
import network.rs485.logisticspipes.world.WorldCoordinatesWrapper;
import network.rs485.logisticspipes.world.WorldCoordinatesWrapper.AdjacentTileEntity;

public class PipeItemsProviderLogistics extends CoreRoutedPipe implements IOrderManagerContentReceiver, IHeadUpDisplayRendererProvider, IChestContentReceiver, IChangeListener {

	public final PlayerCollectionList localModeWatchers = new PlayerCollectionList();

	private final Map<ItemIdentifier, Integer> displayMap = new TreeMap<>();
	public final ArrayList<ItemIdentifierStack> displayList = new ArrayList<>();
	private final ArrayList<ItemIdentifierStack> oldList = new ArrayList<>();

	public final LinkedList<ItemIdentifierStack> oldManagerList = new LinkedList<>();
	public final LinkedList<ItemIdentifierStack> itemListOrderer = new LinkedList<>();
	private final HUDProvider HUD = new HUDProvider(this);

	private boolean doContentUpdate = true;

	public PipeItemsProviderLogistics(Item item) {
		super(item);
	}

	@Override
	public void onAllowedRemoval() {
		// TODO PROVIDE REFACTOR: fail current orders that did not leave the pipe yet
	}

	public int getTotalItemCount(ItemIdentifier item) {

		if (!isEnabled()) {
			return 0;
		}

		//Check if configurations allow for this item
		if (hasFilter() && ((isExcludeFilter() && itemIsFiltered(item)) || (!isExcludeFilter() && !itemIsFiltered(item)))) {
			return 0;
		}

		//@formatter:off
		return new WorldCoordinatesWrapper(container).getConnectedAdjacentTileEntities(ConnectionPipeType.ITEM)
				.filter(adjacent -> !SimpleServiceLocator.pipeInformationManager.isItemPipe(adjacent.tileEntity))
				.map(this::getAdaptedInventoryUtil)
				.filter(Objects::nonNull)
				.map(util -> util.itemCount(item))
				.reduce(Integer::sum).orElse(0);
		//@formatter:on
	}

	protected int neededEnergy() {
		return 1;
	}

	protected int itemsToExtract() {
		return 8;
	}

	private IInventoryUtil getAdaptedInventoryUtil(AdjacentTileEntity adjacent) {
		ExtractionMode mode = getExtractionMode();
		switch (mode) {
			case LeaveFirst:
				return SimpleServiceLocator.inventoryUtilFactory.getHidingInventoryUtil(adjacent.tileEntity, adjacent.direction.getOpposite(), false, false, 1, 0);
			case LeaveLast:
				return SimpleServiceLocator.inventoryUtilFactory.getHidingInventoryUtil(adjacent.tileEntity, adjacent.direction.getOpposite(), false, false, 0, 1);
			case LeaveFirstAndLast:
				return SimpleServiceLocator.inventoryUtilFactory.getHidingInventoryUtil(adjacent.tileEntity, adjacent.direction.getOpposite(), false, false, 1, 1);
			case Leave1PerStack:
				return SimpleServiceLocator.inventoryUtilFactory.getHidingInventoryUtil(adjacent.tileEntity, adjacent.direction.getOpposite(), true, false, 0, 0);
			case Leave1PerType:
				return SimpleServiceLocator.inventoryUtilFactory.getHidingInventoryUtil(adjacent.tileEntity, adjacent.direction.getOpposite(), false, true, 0, 0);
			default:
				break;
		}
		return SimpleServiceLocator.inventoryUtilFactory.getHidingInventoryUtil(adjacent.tileEntity, adjacent.direction.getOpposite(), false, false, 0, 0);
	}

	@Override
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_PROVIDER_TEXTURE;
	}

	@Override
	public void enabledUpdateEntity() {
		super.enabledUpdateEntity();

		if (isNthTick(6)) {
			updateInv(null);
		}

		if (doContentUpdate) {
			checkContentUpdate(null);
		}

		// TODO PROVIDE REFACTOR: send next orders every 6 ticks
	}

	@Override
	public LogisticsModule getLogisticsModule() {
		return null;
	}

	@Override
	public ItemSendMode getItemSendMode() {
		return ItemSendMode.Normal;
	}

	@Override
	public void startWatching() {
		MainProxy.sendPacketToServer(
				PacketHandler.getPacket(HUDStartWatchingPacket.class).setInteger(1 /*TODO*/).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
	}

	@Override
	public void stopWatching() {
		MainProxy.sendPacketToServer(PacketHandler.getPacket(HUDStopWatchingPacket.class).setInteger(1 /*TODO*/).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
	}

	private void updateInv(EntityPlayer player) {
		if (localModeWatchers.size() == 0 && player == null) {
			return;
		}
		displayList.clear();
		displayMap.clear();
		// TODO PROVIDE REFACTOR: fill displayMap
		displayList.ensureCapacity(displayMap.size());
		displayList.addAll(displayMap.entrySet().stream()
				.map(item -> new ItemIdentifierStack(item.getKey(), item.getValue()))
				.collect(Collectors.toList()));
		if (!oldList.equals(displayList)) {
			oldList.clear();
			oldList.ensureCapacity(displayList.size());
			oldList.addAll(displayList);
			MainProxy.sendToPlayerList(PacketHandler.getPacket(ChestContent.class).setIdentList(displayList).setPosX(getX()).setPosY(getY()).setPosZ(getZ()), localModeWatchers);
		} else if (player != null) {
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(ChestContent.class).setIdentList(displayList).setPosX(getX()).setPosY(getY()).setPosZ(getZ()), player);
		}
	}

	@Override
	public void listenedChanged() {
		doContentUpdate = true;
	}

	private void checkContentUpdate(EntityPlayer player) {
		doContentUpdate = false;
		// TODO PROVIDE REFACTOR: send some providing info packet to player
		LinkedList<ItemIdentifierStack> all = new LinkedList<>(); // _orderManager.getContentList(getWorld());
		if (!oldManagerList.equals(all)) {
			oldManagerList.clear();
			oldManagerList.addAll(all);
			MainProxy.sendToPlayerList(PacketHandler.getPacket(OrdererManagerContent.class).setIdentList(all).setPosX(getX()).setPosY(getY()).setPosZ(getZ()), localModeWatchers);
		} else if (player != null) {
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(OrdererManagerContent.class).setIdentList(all).setPosX(getX()).setPosY(getY()).setPosZ(getZ()), player);
		}
	}

	@Override
	public void playerStartWatching(EntityPlayer player, int mode) {
		if (mode == 1) {
			localModeWatchers.add(player);
			updateInv(player);
			checkContentUpdate(player);
		} else {
			super.playerStartWatching(player, mode);
		}
	}

	@Override
	public void playerStopWatching(EntityPlayer player, int mode) {
		super.playerStopWatching(player, mode);
		localModeWatchers.remove(player);
	}

	@Override
	public void setReceivedChestContent(Collection<ItemIdentifierStack> list) {
		displayList.clear();
		displayList.ensureCapacity(list.size());
		displayList.addAll(list);
	}

	@Override
	public IHeadUpDisplayRenderer getRenderer() {
		return HUD;
	}

	@Override
	public void setOrderManagerContent(Collection<ItemIdentifierStack> list) {
		itemListOrderer.clear();
		itemListOrderer.addAll(list);
	}

	// import from logic
	private ItemIdentifierInventory providingInventory = new ItemIdentifierInventory(9, "", 1);
	private boolean _filterIsExclude;
	private ExtractionMode _extractionMode = ExtractionMode.Normal;

	@Override
	public void onWrenchClicked(EntityPlayer entityplayer) {
		entityplayer.openGui(LogisticsPipes.instance, GuiIDs.GUI_ProviderPipe_ID, getWorld(), getX(), getY(), getZ());
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(ProviderPipeMode.class).setInteger(getExtractionMode().ordinal()).setPosX(getX()).setPosY(getY()).setPosZ(getZ()), entityplayer);
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(ProviderPipeInclude.class).setInteger(isExcludeFilter() ? 1 : 0).setPosX(getX()).setPosY(getY()).setPosZ(getZ()), entityplayer);
	}

	/*** GUI ***/
	public ItemIdentifierInventory getprovidingInventory() {
		return providingInventory;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound) {
		super.readFromNBT(nbttagcompound);
		providingInventory.readFromNBT(nbttagcompound, "");
		_filterIsExclude = nbttagcompound.getBoolean("filterisexclude");
		_extractionMode = ExtractionMode.getMode(nbttagcompound.getInteger("extractionMode"));
	}

	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound) {
		super.writeToNBT(nbttagcompound);
		providingInventory.writeToNBT(nbttagcompound, "");
		nbttagcompound.setBoolean("filterisexclude", _filterIsExclude);
		nbttagcompound.setInteger("extractionMode", _extractionMode.ordinal());
	}

	/** INTERFACE TO PIPE **/
	public boolean hasFilter() {
		return !providingInventory.isEmpty();
	}

	public boolean itemIsFiltered(ItemIdentifier item) {
		return providingInventory.containsItem(item);
	}

	public boolean isExcludeFilter() {
		return _filterIsExclude;
	}

	public void setFilterExcluded(boolean isExcluded) {
		_filterIsExclude = isExcluded;
	}

	public ExtractionMode getExtractionMode() {
		return _extractionMode;
	}

	public void setExtractionMode(int id) {
		_extractionMode = ExtractionMode.getMode(id);
	}

	public void nextExtractionMode() {
		_extractionMode = _extractionMode.next();
	}

}
