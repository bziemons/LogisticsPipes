package logisticspipes.utils;

import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.logisticspipes.IRoutedItem;

public final class SinkReply {

	public enum FixedPriority {
		DefaultRoute,
		ModBasedItemSink,
		OreDictItemSink,
		EnchantmentItemSink,
		Terminus,
		ItemSink,
		PassiveSupplier,
	}

	public enum BufferMode {
		NONE,
		BUFFERED,
		DESTINATION_BUFFERED,
	}

	public final FixedPriority fixedPriority;
	public final int customPriority;
	public final int energyUse;
	public final int maxNumberOfItems;
	public final BufferMode bufferMode;
	public final IAdditionalTargetInformation addInfo;

	public SinkReply(FixedPriority fixedPriority, int customPriority, int energyUse, int maxNumberOfItems, IAdditionalTargetInformation addInfo) {
		this.fixedPriority = fixedPriority;
		this.customPriority = customPriority;
		this.energyUse = energyUse;
		this.maxNumberOfItems = maxNumberOfItems;
		bufferMode = BufferMode.NONE;
		this.addInfo = addInfo;
	}

	public SinkReply(SinkReply base, int maxNumberOfItems) {
		fixedPriority = base.fixedPriority;
		customPriority = base.customPriority;
		energyUse = base.energyUse;
		this.maxNumberOfItems = maxNumberOfItems;
		bufferMode = BufferMode.NONE;
		addInfo = base.addInfo;
	}

	public SinkReply(SinkReply base, int maxNumberOfItems, BufferMode bufferMode) {
		fixedPriority = base.fixedPriority;
		customPriority = base.customPriority;
		energyUse = base.energyUse;
		this.maxNumberOfItems = maxNumberOfItems;
		this.bufferMode = bufferMode;
		addInfo = base.addInfo;
	}

	public void setTransportMode(IRoutedItem item) {
		if (fixedPriority == FixedPriority.DefaultRoute) {
			item.setTransportMode(IRoutedItem.TransportMode.Default);
		} else {
			item.setTransportMode(IRoutedItem.TransportMode.Passive);
		}
	}
}
