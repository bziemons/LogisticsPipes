package logisticspipes.routing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.logisticspipes.IRoutedItem.TransportMode;
import logisticspipes.proxy.MainProxy;
import logisticspipes.routing.order.IDistanceTracker;
import logisticspipes.utils.item.ItemIdentifierStack;
import network.rs485.logisticspipes.util.items.ItemStackLoader;

public class ItemRoutingInformation {

	public static class DelayComparator implements Comparator<ItemRoutingInformation> {

		@Override
		public int compare(ItemRoutingInformation o1, ItemRoutingInformation o2) {
			return (int) (o2.getTimeOut() - o1.getTimeOut()); // cast will never overflow because the delta is in 1/20ths of a second.
		}
	}

	@Override
	public ItemRoutingInformation clone() {
		ItemRoutingInformation that = new ItemRoutingInformation();
		that.destinationint = destinationint;
		that.destinationUUID = destinationUUID;
		that.arrived = arrived;
		that.bufferCounter = bufferCounter;
		that._doNotBuffer = _doNotBuffer;
		that._transportMode = _transportMode;
		that.jamlist = new ArrayList<>(jamlist);
		that.tracker = tracker;
		that.targetInfo = targetInfo;
		that.item = new ItemIdentifierStack(getItem());
		return that;
	}

	public int destinationint = -1;
	public UUID destinationUUID;
	public boolean arrived;
	public int bufferCounter = 0;
	public boolean _doNotBuffer;
	public TransportMode _transportMode = TransportMode.Unknown;
	public List<Integer> jamlist = new ArrayList<>();
	public IDistanceTracker tracker = null;
	public IAdditionalTargetInformation targetInfo;

	private long delay = 640 + MainProxy.getGlobalTick();

	@Getter
	@Setter
	private ItemIdentifierStack item;

	public void readFromNBT(CompoundNBT tag) {
		if (tag.contains("destinationUUID")) {
			destinationUUID = UUID.fromString(CompoundNBT.getString("destinationUUID"));
		}
		arrived = CompoundNBT.getBoolean("arrived");
		bufferCounter = tag.getInt("bufferCounter");
		_transportMode = TransportMode.values()[tag.getInt("transportMode")];
		ItemStack stack = ItemStackLoader.loadAndFixItemStackFromNBT(CompoundNBT.getCompound("Item"));
		setItem(ItemIdentifierStack.getFromStack(stack));
	}

	public void writeToNBT(CompoundNBT tag) {
		if (destinationUUID != null) {
			tag.putString("destinationUUID", destinationUUID.toString());
		}
		tag.putBoolean("arrived", arrived);
		tag.putInt("bufferCounter", bufferCounter);
		tag.putInt("transportMode", _transportMode.ordinal());

		CompoundNBT tag2 = new CompoundNBT();
		getItem().makeNormalStack().writeToNBT(CompoundNBT2);
		tag.put("Item", CompoundNBT2);
	}

	// the global LP tick in which getTickToTimeOut returns 0.
	public long getTimeOut() {
		return delay;
	}

	// how many ticks until this times out
	public long getTickToTimeOut() {
		return delay - MainProxy.getGlobalTick();
	}

	public void resetDelay() {
		delay = 640 + MainProxy.getGlobalTick();
		if (tracker != null) {
			tracker.setDelay(delay);
		}
	}

	public void setItemTimedout() {
		delay = MainProxy.getGlobalTick() - 1;
		if (tracker != null) {
			tracker.setDelay(delay);
		}
	}

	@Override
	public String toString() {
		return String.format("(%s, %d, %s, %s, %s, %d, %s)", item, destinationint, destinationUUID, _transportMode, jamlist, delay, tracker);
	}

	public void storeToNBT(CompoundNBT tag) {
		UUID uuid = UUID.randomUUID();
		tag.putString("StoreUUID", uuid.toString());
		this.writeToNBT(CompoundNBT);
		storeMap.put(uuid, this);
	}

	public static ItemRoutingInformation restoreFromNBT(CompoundNBT tag) {
		if (tag.contains("StoreUUID")) {
			UUID uuid = UUID.fromString(CompoundNBT.getString("StoreUUID"));
			if (storeMap.containsKey(uuid)) {
				ItemRoutingInformation result = storeMap.get(uuid);
				storeMap.remove(uuid);
				return result;
			}
		}
		ItemRoutingInformation info = new ItemRoutingInformation();
		info.readFromNBT(CompoundNBT);
		return info;
	}

	private static final Map<UUID, ItemRoutingInformation> storeMap = new HashMap<>();
}
