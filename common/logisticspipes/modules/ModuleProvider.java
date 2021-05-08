package logisticspipes.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

import com.google.common.collect.ImmutableList;

import logisticspipes.gui.hud.modules.HUDProviderModule;
import logisticspipes.interfaces.IClientInformationProvider;
import logisticspipes.interfaces.IHUDModuleHandler;
import logisticspipes.interfaces.IHUDModuleRenderer;
import logisticspipes.interfaces.IInventoryUtil;
import logisticspipes.interfaces.IModuleInventoryReceive;
import logisticspipes.interfaces.IModuleWatchReciver;
import logisticspipes.interfaces.ISlotUpgradeManager;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractguis.ModuleCoordinatesGuiProvider;
import logisticspipes.network.abstractguis.ModuleInHandGuiProvider;
import logisticspipes.network.guis.module.inhand.ProviderModuleInHand;
import logisticspipes.network.guis.module.inpipe.ProviderModuleGuiProvider;
import logisticspipes.network.packets.hud.HUDStartModuleWatchingPacket;
import logisticspipes.network.packets.hud.HUDStopModuleWatchingPacket;
import logisticspipes.network.packets.module.ModuleInventory;
import logisticspipes.network.packets.modules.SneakyModuleDirectionUpdate;
import logisticspipes.pipes.basic.CoreRoutedPipe.ItemSendMode;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.computers.interfaces.CCCommand;
import logisticspipes.proxy.computers.interfaces.CCType;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;
import network.rs485.logisticspipes.connection.NeighborTileEntity;
import network.rs485.logisticspipes.inventory.IItemIdentifierInventory;
import network.rs485.logisticspipes.inventory.ProviderMode;
import network.rs485.logisticspipes.module.Gui;
import network.rs485.logisticspipes.module.SneakyDirection;
import network.rs485.logisticspipes.property.BooleanProperty;
import network.rs485.logisticspipes.property.EnumProperty;
import network.rs485.logisticspipes.property.InventoryProperty;
import network.rs485.logisticspipes.property.NullableEnumProperty;
import network.rs485.logisticspipes.property.Property;

@CCType(name = "Provider Module")
public class ModuleProvider extends LogisticsModule implements SneakyDirection, ILegacyActiveModule,
		IClientInformationProvider, IHUDModuleHandler, IModuleWatchReciver, IModuleInventoryReceive, Gui {

	public final ArrayList<ItemIdentifierStack> displayList = new ArrayList<>();
	public final InventoryProperty filterInventory = new InventoryProperty(
			new ItemIdentifierInventory(9, "Items to provide (or empty for all)", 1), "");
	public final BooleanProperty isActive = new BooleanProperty(false, "isActive");
	public final BooleanProperty isExclusionFilter = new BooleanProperty(false, "filterisexclude");
	public final EnumProperty<ProviderMode> providerMode =
			new EnumProperty<>(ProviderMode.DEFAULT, "extractionMode", ProviderMode.values());
	public final NullableEnumProperty<EnumFacing> sneakyDirection =
			new NullableEnumProperty<>(null, "sneakydirection", EnumFacing.values());
	public final ImmutableList<Property<?>> propertyList = ImmutableList.<Property<?>>builder()
			.add(filterInventory)
			.add(isActive)
			.add(isExclusionFilter)
			.add(providerMode)
			.add(sneakyDirection)
			.build();
	private final Map<ItemIdentifier, Integer> displayMap = new TreeMap<>();
	private final ArrayList<ItemIdentifierStack> oldList = new ArrayList<>();
	private final PlayerCollectionList localModeWatchers = new PlayerCollectionList();
	private final IHUDModuleRenderer HUD = new HUDProviderModule(this);

	public ModuleProvider() {}

	public static String getName() {
		return "provider";
	}

	@Nonnull
	@Override
	public String getLPName() {
		return getName();
	}

	/**
	 * Returns a list of all the properties of this module.
	 */
	@Nonnull
	@Override
	public List<Property<?>> getProperties() {
		return propertyList;
	}

	@Override
	public EnumFacing getSneakyDirection() {
		return sneakyDirection.getValue();
	}

	@Override
	public void setSneakyDirection(EnumFacing direction) {
		sneakyDirection.setValue(direction);
		MainProxy.runOnServer(getWorld(), () -> () ->
				MainProxy.sendToPlayerList(
						PacketHandler.getPacket(SneakyModuleDirectionUpdate.class)
								.setDirection(sneakyDirection.getValue())
								.setModulePos(this),
						localModeWatchers
				)
		);
	}

	protected int neededEnergy() {
		return (int) (1 * Math.pow(1.1, getUpgradeManager().getItemExtractionUpgrade()) * Math
				.pow(1.2, getUpgradeManager().getItemStackExtractionUpgrade()));
	}

	protected int itemsToExtract() {
		return 8 * (int) Math.pow(2, getUpgradeManager().getItemExtractionUpgrade());
	}

	protected int stacksToExtract() {
		return 1 + getUpgradeManager().getItemStackExtractionUpgrade();
	}

	public ItemSendMode itemSendMode() {
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

	public boolean filterBlocksItem(ItemIdentifier item) {
		if (filterInventory.isEmpty()) {
			return false;
		}
		boolean isFiltered = filterInventory.containsItem(item);
		return isExclusionFilter.getValue() == isFiltered;
	}

	public void onBlockRemoval() {
		// TODO PROVIDE REFACTOR: cancel unsent requests
	}

		service.getItemOrderManager().sendFailed();
		return 0;
	}

	public int getTotalItemCount(final ItemIdentifier item) {
		if (filterBlocksItem(item)) return 0;
		return inventoriesWithMode().map(invUtil -> invUtil.itemCount(item)).reduce(Integer::sum).orElse(0);
	}

	/*** GUI STUFF ***/

	@CCCommand(description = "Returns the FilterInventory of this Module")
	public IItemIdentifierInventory getFilterInventory() {
		return filterInventory;
	}

	@Override
	public @Nonnull
	List<String> getClientInformation() {
		List<String> list = new ArrayList<>();
		list.add(!(boolean) isExclusionFilter.getValue() ? "Included" : "Excluded");
		list.add("Mode: " + providerMode.getValue().name());
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
			MainProxy.sendToPlayerList(
					PacketHandler.getPacket(ModuleInventory.class).setIdentList(displayList).setModulePos(this)
							.setCompressable(true), localModeWatchers);
		} else if (player != null) {
			MainProxy.sendPacketToPlayer(
					PacketHandler.getPacket(ModuleInventory.class).setIdentList(displayList).setModulePos(this)
							.setCompressable(true), player);
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
	public void handleInvContent(@Nonnull Collection<ItemIdentifierStack> list) {
		displayList.clear();
		displayList.addAll(list);
	}

	@Override
	public boolean recievePassive() {
		return false;
	}

	@Nonnull
	@Override
	public ModuleCoordinatesGuiProvider getPipeGuiProvider() {
		return NewGuiHandler.getGui(ProviderModuleGuiProvider.class).setExtractorMode(providerMode.getValue().ordinal())
				.setExclude(isExclusionFilter.getValue());
	}

	@Nonnull
	@Override
	public ModuleInHandGuiProvider getInHandGuiProvider() {
		return NewGuiHandler.getGui(ProviderModuleInHand.class);
	}

	private IInventoryUtil getInventoryUtilWithMode(NeighborTileEntity<TileEntity> neighbor) {
		return SimpleServiceLocator.inventoryUtilFactory
				.getHidingInventoryUtil(neighbor.getTileEntity(), neighbor.getOurDirection(), providerMode.getValue());
	}

}
