package logisticspipes.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.interfaces.IClientInformationProvider;
import logisticspipes.interfaces.IHUDModuleHandler;
import logisticspipes.interfaces.IHUDModuleRenderer;
import logisticspipes.interfaces.IModuleInventoryReceive;
import logisticspipes.interfaces.IModuleWatchReciver;
import logisticspipes.modules.abstractmodules.LogisticsGuiModule;
import logisticspipes.modules.abstractmodules.LogisticsModule;
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
import logisticspipes.pipes.PipeLogisticsChassi.ChassiTargetInformation;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.pipes.basic.debug.StatusEntry;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.ISimpleInventoryEventHandler;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;

public class ModuleActiveSupplier extends LogisticsGuiModule implements IClientInformationProvider, IHUDModuleHandler, IModuleWatchReciver, IModuleInventoryReceive, ISimpleInventoryEventHandler {

	private final PlayerCollectionList localModeWatchers = new PlayerCollectionList();

	private boolean _lastRequestFailed = false;

	public ModuleActiveSupplier() {
		dummyInventory.addListener(this);
	}

	@Override
	public SinkReply sinksItem(ItemIdentifier item, int bestPriority, int bestCustomPriority, boolean allowDefault, boolean includeInTransit) {
		return null;
	}

	@Override
	public LogisticsModule getSubModule(int slot) {
		return null;
	}

	@Override
	public List<String> getClientInformation() {
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
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(ModuleInventory.class).setIdentList(ItemIdentifierStack.getListFromInventory(dummyInventory)).setModulePos(this), player);
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
	public void handleInvContent(Collection<ItemIdentifierStack> list) {
		dummyInventory.handleItemIdentifierList(list);
	}

	@Override
	public void InventoryChanged(IInventory inventory) {
		if (MainProxy.isServer(_world.getWorld())) {
			MainProxy.sendToPlayerList(PacketHandler.getPacket(ModuleInventory.class).setIdentList(ItemIdentifierStack.getListFromInventory(dummyInventory)).setModulePos(this), localModeWatchers);
		}
	}

	@Override
	public boolean hasGenericInterests() {
		return false;
	}

	@Override
	public boolean interestedInAttachedInventory() {
		return false;
	}

	@Override
	public boolean interestedInUndamagedID() {
		return false;
	}

	@Override
	public boolean recievePassive() {
		return true;
	}

	public void setRequestFailed(boolean value) {
		_lastRequestFailed = value;
	}

	private ItemIdentifierInventory dummyInventory = new ItemIdentifierInventory(9, "", 127);

	private final HashMap<ItemIdentifier, Integer> _requestedItems = new HashMap<>();

	public enum SupplyMode {
		Partial,
		Full,
		Bulk50,
		Bulk100,
		Infinite
	}

	public enum PatternMode {
		Partial,
		Full,
		Bulk50,
		Bulk100;
	}

	private SupplyMode _requestMode = SupplyMode.Bulk50;
	private PatternMode _patternMode = PatternMode.Bulk50;
	@Getter
	@Setter
	private boolean isLimited = true;

	public int[] slotArray = new int[9];

	/*** GUI ***/
	public ItemIdentifierInventory getDummyInventory() {
		return dummyInventory;
	}

	@Override
	public void tick() {
		if (!_service.isNthTick(100)) {
			return;
		}

		_requestedItems.values().stream().filter(amount -> amount > 0).forEach(amount -> _service.spawnParticle(Particles.VioletParticle, 2));

		// TODO PROVIDE REFACTOR: request available items
	}

	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound) {
		dummyInventory.readFromNBT(nbttagcompound, "");
		if (nbttagcompound.hasKey("requestmode")) {
			_requestMode = SupplyMode.values()[nbttagcompound.getShort("requestmode")];
		}
		if (nbttagcompound.hasKey("patternmode")) {
			_patternMode = PatternMode.values()[nbttagcompound.getShort("patternmode")];
		}
		if (nbttagcompound.hasKey("limited")) {
			setLimited(nbttagcompound.getBoolean("limited"));
		}
		if (nbttagcompound.hasKey("requestpartials")) {
			boolean oldPartials = nbttagcompound.getBoolean("requestpartials");
			if (oldPartials) {
				_requestMode = SupplyMode.Partial;
			} else {
				_requestMode = SupplyMode.Full;
			}
		}
		for (int i = 0; i < 9; i++) {
			slotArray[i] = nbttagcompound.getInteger("slotArray_" + i);
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound) {
		dummyInventory.writeToNBT(nbttagcompound, "");
		nbttagcompound.setShort("requestmode", (short) _requestMode.ordinal());
		nbttagcompound.setShort("patternmode", (short) _patternMode.ordinal());
		nbttagcompound.setBoolean("limited", isLimited());
		for (int i = 0; i < 9; i++) {
			nbttagcompound.setInteger("slotArray_" + i, slotArray[i]);
		}
	}

	private void decreaseRequested(ItemIdentifierStack item) {
		int remaining = item.getStackSize();
		//see if we can get an exact match
		Integer count = _requestedItems.get(item.getItem());
		if (count != null) {
			_service.getDebug().log("Supplier: Exact match. Still missing: " + Math.max(0, count - remaining));
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
				_service.getDebug().log("Supplier: Fuzzy match with" + e + ". Still missing: " + Math.max(0, expected - remaining));
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
		_service.getDebug().log("Supplier: supplier got unexpected item " + item.toString());
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

	@Override
	protected ModuleCoordinatesGuiProvider getPipeGuiProvider() {
		return NewGuiHandler.getGui(ActiveSupplierSlot.class).setPatternUpgarde(hasPatternUpgrade()).setSlotArray(slotArray).setMode((_service.getUpgradeManager(slot, positionInt).hasPatternUpgrade() ? getPatternMode() : getSupplyMode()).ordinal()).setLimit(isLimited);
	}

	@Override
	protected ModuleInHandGuiProvider getInHandGuiProvider() {
		return NewGuiHandler.getGui(ActiveSupplierInHand.class);
	}

	public boolean hasPatternUpgrade() {
		if (_service != null && _service.getUpgradeManager(slot, positionInt) != null) {
			return _service.getUpgradeManager(slot, positionInt).hasPatternUpgrade();
		}
		return false;
	}
}
