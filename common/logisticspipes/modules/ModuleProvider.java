package logisticspipes.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

import logisticspipes.gui.hud.modules.HUDProviderModule;
import logisticspipes.interfaces.IClientInformationProvider;
import logisticspipes.interfaces.IHUDModuleHandler;
import logisticspipes.interfaces.IHUDModuleRenderer;
import logisticspipes.interfaces.IInventoryUtil;
import logisticspipes.interfaces.IModuleInventoryReceive;
import logisticspipes.interfaces.IModuleWatchReciver;
import logisticspipes.interfaces.ISlotUpgradeManager;
import logisticspipes.logisticspipes.ExtractionMode;
import logisticspipes.modules.abstractmodules.LogisticsModule;
import logisticspipes.modules.abstractmodules.LogisticsSneakyDirectionModule;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractguis.ModuleCoordinatesGuiProvider;
import logisticspipes.network.abstractguis.ModuleInHandGuiProvider;
import logisticspipes.network.guis.module.inhand.ProviderModuleInHand;
import logisticspipes.network.guis.module.inpipe.ProviderModuleGuiProvider;
import logisticspipes.network.packets.hud.HUDStartModuleWatchingPacket;
import logisticspipes.network.packets.hud.HUDStopModuleWatchingPacket;
import logisticspipes.network.packets.module.ModuleInventory;
import logisticspipes.network.packets.modules.ExtractorModuleMode;
import logisticspipes.pipes.basic.CoreRoutedPipe.ItemSendMode;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.computers.interfaces.CCCommand;
import logisticspipes.proxy.computers.interfaces.CCType;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;

@CCType(name = "Provider Module")
public class ModuleProvider extends LogisticsSneakyDirectionModule implements IClientInformationProvider, IHUDModuleHandler, IModuleWatchReciver, IModuleInventoryReceive {

	private final ItemIdentifierInventory _filterInventory = new ItemIdentifierInventory(9, "Items to provide (or empty for all)", 1);
	private EnumFacing _sneakyDirection = null;

	private boolean isActive = false;

	protected final int ticksToActiveAction = 6;
	protected final int ticksToPassiveAction = 100;
	private final Map<ItemIdentifier, Integer> displayMap = new TreeMap<>();
	public final ArrayList<ItemIdentifierStack> displayList = new ArrayList<>();
	private final ArrayList<ItemIdentifierStack> oldList = new ArrayList<>();
	private final PlayerCollectionList localModeWatchers = new PlayerCollectionList();

	protected int currentTick = 0;
	protected boolean isExcludeFilter = false;
	protected ExtractionMode _extractionMode = ExtractionMode.Normal;
	private IHUDModuleRenderer HUD = new HUDProviderModule(this);

	public ModuleProvider() {}

	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound) {
		_filterInventory.readFromNBT(nbttagcompound, "");
		isActive = nbttagcompound.getBoolean("isActive");
		isExcludeFilter = nbttagcompound.getBoolean("filterisexclude");
		_extractionMode = ExtractionMode.getMode(nbttagcompound.getInteger("extractionMode"));
		if (nbttagcompound.hasKey("sneakydirection")) {
			_sneakyDirection = EnumFacing.values()[nbttagcompound.getInteger("sneakydirection")];
		} else if (nbttagcompound.hasKey("sneakyorientation")) {
			//convert sneakyorientation to sneakydirection
			int t = nbttagcompound.getInteger("sneakyorientation");
			switch (t) {
				default:
				case 0:
					_sneakyDirection = null;
					break;
				case 1:
					_sneakyDirection = EnumFacing.UP;
					break;
				case 2:
					_sneakyDirection = EnumFacing.SOUTH;
					break;
				case 3:
					_sneakyDirection = EnumFacing.DOWN;
					break;
			}
		}

	}

	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound) {
		_filterInventory.writeToNBT(nbttagcompound, "");
		nbttagcompound.setBoolean("isActive", isActive);
		nbttagcompound.setBoolean("filterisexclude", isExcludeFilter);
		nbttagcompound.setInteger("extractionMode", _extractionMode.ordinal());
		if (_sneakyDirection != null) {
			nbttagcompound.setInteger("sneakydirection", _sneakyDirection.ordinal());
		}
	}

	@Override
	public EnumFacing getSneakyDirection() {
		return _sneakyDirection;
	}

	@Override
	public void setSneakyDirection(EnumFacing sneakyDirection) {
		_sneakyDirection = sneakyDirection;
		if(MainProxy.isServer(this._world.getWorld())) {
			MainProxy.sendToPlayerList(PacketHandler.getPacket(ExtractorModuleMode.class).setDirection(_sneakyDirection).setModulePos(this), localModeWatchers);
		}
	}

	@Override
	protected ModuleCoordinatesGuiProvider getPipeGuiProvider() {
		return NewGuiHandler.getGui(ProviderModuleGuiProvider.class).setExtractorMode(getExtractionMode().ordinal()).setExclude(isExcludeFilter);
		//.setIsActive(isActive)
		//.setSneakyDirection(_sneakyDirection);
	}

	@Override
	protected ModuleInHandGuiProvider getInHandGuiProvider() {
		return NewGuiHandler.getGui(ProviderModuleInHand.class);
	}

	protected int neededEnergy() {
		return (int) (1 * Math.pow(1.1, getUpgradeManager().getItemExtractionUpgrade()) * Math.pow(1.2, getUpgradeManager().getItemStackExtractionUpgrade()))	;
	}

	protected int itemsToExtract() {
		return 8 * (int) Math.pow(2, getUpgradeManager().getItemExtractionUpgrade());
	}

	protected int stacksToExtract() {
		return 1 + getUpgradeManager().getItemStackExtractionUpgrade();
	}

	protected ItemSendMode itemSendMode() {
		return getUpgradeManager().getItemExtractionUpgrade() > 0 ? ItemSendMode.Fast : ItemSendMode.Normal;
	}

	protected ISlotUpgradeManager getUpgradeManager() {
		if (_service == null) {
			return null;
		}
		return _service.getUpgradeManager(slot, positionInt);
	}

	@Override
	public LogisticsModule getSubModule(int slot) {
		return null;
	}

	@Override
	public void tick() {
		currentTick = 0;
		checkUpdate(null);
		// TODO PROVIDE REFACTOR: send requested items
	}

	public boolean filterAllowsItem(ItemIdentifier item) {
		if (!hasFilter()) {
			return true;
		}
		boolean isFiltered = itemIsFiltered(item);
		return isExcludeFilter ^ isFiltered;
	}

	public void onBlockRemoval() {
		// TODO PROVIDE REFACTOR: cancel unsent requests
	}

	private int getTotalItemCount(ItemIdentifier item) {

		IInventoryUtil inv = _service.getPointedInventory(_extractionMode, true);
		if (inv == null) {
			return 0;
		}

		if (!filterAllowsItem(item)) {
			return 0;
		}

		return inv.itemCount(item);
	}

	private boolean hasFilter() {
		return !_filterInventory.isEmpty();
	}

	private boolean itemIsFiltered(ItemIdentifier item) {
		return _filterInventory.containsItem(item);
	}

	/*** GUI STUFF ***/

	@CCCommand(description = "Returns the FilterInventory of this Module")
	public IInventory getFilterInventory() {
		return _filterInventory;
	}

	public boolean isExcludeFilter() {
		return isExcludeFilter;
	}

	public void setFilterExcluded(boolean isExcludeFilter) {
		this.isExcludeFilter = isExcludeFilter;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setIsActive(boolean isActive) {
		this.isActive = isActive;
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

	@Override
	public List<String> getClientInformation() {
		List<String> list = new ArrayList<>();
		list.add(!isExcludeFilter ? "Included" : "Excluded");
		list.add("Mode: " + _extractionMode.getExtractionModeString());
		list.add("Filter: ");
		list.add("<inventory>");
		list.add("<that>");
		return list;
	}

	private void checkUpdate(EntityPlayer player) {
		if (localModeWatchers.size() == 0 && player == null) {
			return;
		}
		displayList.clear();
		displayMap.clear();
		// TODO PROVIDE REFACTOR
		//getAllItems(displayMap, new ArrayList<>(0));
		displayList.ensureCapacity(displayMap.size());
		displayList.addAll(displayMap.entrySet().stream()
				.map(item -> new ItemIdentifierStack(item.getKey(), item.getValue()))
				.collect(Collectors.toList()));
		if (!oldList.equals(displayList)) {
			oldList.clear();
			oldList.ensureCapacity(displayList.size());
			oldList.addAll(displayList);
			MainProxy.sendToPlayerList(PacketHandler.getPacket(ModuleInventory.class).setIdentList(displayList).setModulePos(this).setCompressable(true), localModeWatchers);
		} else if (player != null) {
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(ModuleInventory.class).setIdentList(displayList).setModulePos(this).setCompressable(true), player);
		}
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
		checkUpdate(player);
	}

	@Override
	public void stopWatching(EntityPlayer player) {
		localModeWatchers.remove(player);
	}

	@Override
	public IHUDModuleRenderer getHUDRenderer() {
		return HUD;
	}

	@Override
	public void handleInvContent(Collection<ItemIdentifierStack> list) {
		displayList.clear();
		displayList.addAll(list);
	}

	@Override
	public boolean recievePassive() {
		return false;
	}

}
