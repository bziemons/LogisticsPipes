package logisticspipes.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;

import logisticspipes.interfaces.IClientInformationProvider;
import logisticspipes.interfaces.IHUDModuleHandler;
import logisticspipes.interfaces.IHUDModuleRenderer;
import logisticspipes.interfaces.IModuleInventoryReceive;
import logisticspipes.interfaces.IModuleWatchReciver;
import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractguis.ModuleCoordinatesGuiProvider;
import logisticspipes.network.abstractguis.ModuleInHandGuiProvider;
import logisticspipes.network.guis.module.inhand.ActiveSupplierInHand;
import logisticspipes.network.guis.module.inpipe.ActiveSupplierSlot;
import logisticspipes.network.packets.hud.HUDStartModuleWatchingPacket;
import logisticspipes.network.packets.hud.HUDStopModuleWatchingPacket;
import logisticspipes.network.packets.module.ModuleInventory;
import logisticspipes.pipefxhandlers.Particles;
import logisticspipes.pipes.basic.debug.StatusEntry;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.ISimpleInventoryEventHandler;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.tuples.Pair;
import network.rs485.logisticspipes.logistic.Interests;
import network.rs485.logisticspipes.module.Gui;
import network.rs485.logisticspipes.property.BooleanProperty;
import network.rs485.logisticspipes.property.EnumProperty;
import network.rs485.logisticspipes.property.IntListProperty;
import network.rs485.logisticspipes.property.InventoryProperty;
import network.rs485.logisticspipes.property.Property;

public class ModuleActiveSupplier extends LogisticsModule
		implements IClientInformationProvider, IHUDModuleHandler,
		IModuleWatchReciver, IModuleInventoryReceive, ISimpleInventoryEventHandler, Gui {

	public static final int SUPPLIER_SLOTS = 9;

	private final PlayerCollectionList localModeWatchers = new PlayerCollectionList();
	private final HashMap<ItemIdentifier, Integer> _requestedItems = new HashMap<>();

	// properties for the pattern upgrade
	public final IntListProperty slotAssignmentPattern = new IntListProperty("slotpattern");
	public final EnumProperty<PatternMode> patternMode =
			new EnumProperty<>(PatternMode.Bulk50, "patternmode", PatternMode.values());

	// properties for the regular configuration
	public final InventoryProperty inventory =
			new InventoryProperty(new ItemIdentifierInventory(SUPPLIER_SLOTS, "", 127), "");
	public final EnumProperty<SupplyMode> requestMode =
			new EnumProperty<>(SupplyMode.Bulk50, "requestmode", SupplyMode.values());
	public final BooleanProperty isLimited = new BooleanProperty(true, "limited");

	private final List<Property<?>> properties = ImmutableList.<Property<?>>builder()
			.add(slotAssignmentPattern)
			.add(patternMode)
			.add(inventory)
			.add(requestMode)
			.add(isLimited)
			.build();

	private boolean _lastRequestFailed = false;

	public ModuleActiveSupplier() {
		inventory.addListener(this);
		slotAssignmentPattern.ensureSize(SUPPLIER_SLOTS);
	}

	public static String getName() {
		return "active_supplier";
	}

	@Nonnull
	@Override
	public String getLPName() {
		return getName();
	}

	@NotNull
	@Override
	public List<Property<?>> getProperties() {
		return properties;
	}

	@Override
	public @Nonnull
	List<String> getClientInformation() {
		List<String> list = new ArrayList<>();
		list.add("Supplied: ");
		list.add("<inventory>");
		list.add("<that>");
		return list;
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
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(ModuleInventory.class)
						.setIdentList(ItemIdentifierStack.getListFromInventory(inventory))
						.setModulePos(this),
				player);
	}

	@Override
	public void stopWatching(EntityPlayer player) {
		localModeWatchers.remove(player);
	}

	@Override
	public IHUDModuleRenderer getHUDRenderer() {
		return null;
		//return HUD;
	}

	@Override
	public void handleInvContent(@Nonnull Collection<ItemIdentifierStack> list) {
		inventory.handleItemIdentifierList(list);
	}

	@Override
	public void InventoryChanged(IInventory inventory) {
		if (MainProxy.isServer(getWorld())) {
			MainProxy.sendToPlayerList(PacketHandler.getPacket(ModuleInventory.class)
							.setIdentList(ItemIdentifierStack.getListFromInventory(inventory))
							.setModulePos(this),
					localModeWatchers);
		}
	}

	@Override
	public Stream<Interests> streamInterests() {
		// active suppliers do not have interests, but actively request items on #tick.
		return Stream.empty();
	}

	public void setRequestFailed(boolean value) {
		_lastRequestFailed = value;
	}

	@Override
	public void tick() {
		final IPipeServiceProvider service = Objects.requireNonNull(_service);
		if (!service.isNthTick(100)) {
			return;
		}

		_requestedItems.values().stream().filter(amount -> amount > 0)
				.forEach(amount -> service.spawnParticle(Particles.VioletParticle, 2));

		// TODO PROVIDE REFACTOR: request available items
	}

	@Override
	public void readFromNBT(@Nonnull NBTTagCompound tag) {
		super.readFromNBT(tag);
		// deprecated, TODO: remove after 1.12
		final List<Pair<Integer, String>> slotArrayList = IntStream.range(0, SUPPLIER_SLOTS)
				.mapToObj((idx) -> new Pair<>(idx, "slotArray_" + idx))
				.filter((it) -> tag.hasKey(it.getValue2()))
				.collect(Collectors.toList());
		if (!slotArrayList.isEmpty()) {
			final int[] slotArray = new int[SUPPLIER_SLOTS];
			slotArrayList.forEach((pair) -> slotArray[pair.getValue1()] = tag.getInteger(pair.getValue2()));
			slotAssignmentPattern.replaceContent(slotArray);
		}
	}

	private void decreaseRequested(ItemIdentifierStack item) {
		final IPipeServiceProvider service = Objects.requireNonNull(_service);
		int remaining = item.getStackSize();
		//see if we can get an exact match
		Integer count = _requestedItems.get(item.getItem());
		if (count != null) {
			service.getDebug().log("Supplier: Exact match. Still missing: " + Math.max(0, count - remaining));
			if (count - remaining > 0) {
				_requestedItems.put(item.getItem(), count - remaining);
			} else {
				_requestedItems.remove(item.getItem());
			}
			remaining -= count;
		}
		if (remaining <= 0) {
			return;
		}
		//still remaining... was from fuzzyMatch on a crafter
		Iterator<Entry<ItemIdentifier, Integer>> it = _requestedItems.entrySet().iterator();
		while (it.hasNext()) {
			Entry<ItemIdentifier, Integer> e = it.next();
			if (e.getKey().equalsWithoutNBT(item.getItem())) {
				int expected = e.getValue();
				service.getDebug().log("Supplier: Fuzzy match with" + e + ". Still missing: " + Math
						.max(0, expected - remaining));
				if (expected - remaining > 0) {
					e.setValue(expected - remaining);
				} else {
					it.remove();
				}
				remaining -= expected;
			}
			if (remaining <= 0) {
				return;
			}
		}
		//we have no idea what this is, log it.
		service.getDebug().log("Supplier: supplier got unexpected item " + item);
	}

	public SupplyMode getSupplyMode() {
		return _requestMode;
	}

	public void setSupplyMode(SupplyMode mode) {
		_requestMode = mode;
	}

	public PatternMode getPatternMode() {
		return _patternMode;
	}

	public void setPatternMode(PatternMode mode) {
		_patternMode = mode;
	}

	public int[] getSlotsForItemIdentifier(ItemIdentifier item) {
		int size = 0;
		for (int i = 0; i < 9; i++) {
			if (dummyInventory.getIDStackInSlot(i) != null && dummyInventory.getIDStackInSlot(i).getItem().equals(item)) {
				size++;
			}
		}
		int[] array = new int[size];
		int pos = 0;
		for (int i = 0; i < 9; i++) {
			if (dummyInventory.getIDStackInSlot(i) != null && dummyInventory.getIDStackInSlot(i).getItem().equals(item)) {
				array[pos++] = i;
			}
		}
		return array;
	}

	public void addStatusInformation(List<StatusEntry> status) {
		StatusEntry entry = new StatusEntry();
		entry.name = "Requested Items";
		entry.subEntry = new ArrayList<>();
		for (Entry<ItemIdentifier, Integer> part : _requestedItems.entrySet()) {
			StatusEntry subEntry = new StatusEntry();
			subEntry.name = part.toString();
			entry.subEntry.add(subEntry);
		}
		status.add(entry);
	}

	@Nonnull
	@Override
	public ModuleCoordinatesGuiProvider getPipeGuiProvider() {
		final boolean hasPatternUpgrade = hasPatternUpgrade();
		return NewGuiHandler.getGui(ActiveSupplierSlot.class)
				.setPatternUpgarde(hasPatternUpgrade)
				.setSlotArray(slotAssignmentPattern.stream().mapToInt(Integer::intValue).toArray())
				.setMode((hasPatternUpgrade ? patternMode.getValue() : requestMode.getValue()).ordinal())
				.setLimit(isLimited.getValue());
	}

	@Nonnull
	@Override
	public ModuleInHandGuiProvider getInHandGuiProvider() {
		return NewGuiHandler.getGui(ActiveSupplierInHand.class);
	}

	public boolean hasPatternUpgrade() {
		return getUpgradeManager().hasPatternUpgrade();
	}

	public enum SupplyMode {
		Partial,
		Full,
		Bulk50,
		Bulk100,
		Infinite
	}
}
