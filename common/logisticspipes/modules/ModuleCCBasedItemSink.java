package logisticspipes.modules;

import java.util.List;

import net.minecraft.nbt.NBTTagCompound;

import logisticspipes.interfaces.IQueueCCEvent;
import logisticspipes.modules.abstractmodules.LogisticsModule;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.computers.objects.CCSinkResponder;
import logisticspipes.utils.OneList;
import logisticspipes.utils.item.ItemIdentifierStack;

public class ModuleCCBasedItemSink extends LogisticsModule {

	private IQueueCCEvent eventQueuer;

	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound) {}

	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound) {}

	@Override
	public void registerCCEventQueuer(IQueueCCEvent eventQueuer) {
		this.eventQueuer = eventQueuer;
	}

	@Override
	public int getX() {
		return _service.getX();
	}

	@Override
	public int getY() {
		return _service.getY();
	}

	@Override
	public int getZ() {
		return _service.getZ();
	}

	@Override
	public LogisticsModule getSubModule(int slot) {
		return null;
	}

	@Override
	public void tick() {}

	@Override
	public boolean recievePassive() {
		return false;
	}

	@Override
	public List<CCSinkResponder> queueCCSinkEvent(ItemIdentifierStack item) {
		CCSinkResponder resonse = new CCSinkResponder(item, _service.getSourceID(), eventQueuer);
		eventQueuer.queueEvent("ItemSink", new Object[] { SimpleServiceLocator.ccProxy.getAnswer(resonse) });
		return new OneList<>(resonse);
	}

}
