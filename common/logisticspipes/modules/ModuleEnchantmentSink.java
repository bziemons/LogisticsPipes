package logisticspipes.modules;

import net.minecraft.nbt.NBTTagCompound;

import logisticspipes.modules.abstractmodules.LogisticsModule;
import logisticspipes.pipes.PipeLogisticsChassi.ChassiTargetInformation;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.SinkReply.FixedPriority;

public class ModuleEnchantmentSink extends LogisticsModule {

	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound) {}

	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound) {}

	@Override
	public int getX() {
		if (slot.isInWorld()) {
			return _service.getX();
		} else {
			return 0;
		}
	}

	@Override
	public int getY() {
		if (slot.isInWorld()) {
			return _service.getY();
		} else {
			return 0;
		}
	}

	@Override
	public int getZ() {
		if (slot.isInWorld()) {
			return _service.getZ();
		} else {
			return 0;
		}
	}

	private SinkReply _sinkReply;

	@Override
	public void registerPosition(ModulePositionType slot, int positionInt) {
		super.registerPosition(slot, positionInt);
		_sinkReply = new SinkReply(FixedPriority.EnchantmentItemSink, 0, 1, 0, new ChassiTargetInformation(getPositionInt()));
	}

	@Override
	public LogisticsModule getSubModule(int slot) {
		return null;
	}

	@Override
	public void tick() {}

	@Override
	public boolean recievePassive() {
		return true;
	}

	@Override
	public boolean hasEffect() {
		return true;
	}
}
