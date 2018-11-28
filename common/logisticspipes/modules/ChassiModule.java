package logisticspipes.modules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.interfaces.IWorldProvider;
import logisticspipes.logisticspipes.IRoutedItem;
import logisticspipes.modules.abstractmodules.LogisticsGuiModule;
import logisticspipes.modules.abstractmodules.LogisticsModule;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.abstractguis.ModuleCoordinatesGuiProvider;
import logisticspipes.network.abstractguis.ModuleInHandGuiProvider;
import logisticspipes.network.guis.pipe.ChassiGuiProvider;
import logisticspipes.pipes.PipeLogisticsChassi;
import logisticspipes.proxy.computers.objects.CCSinkResponder;
import logisticspipes.utils.item.ItemIdentifierStack;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

import network.rs485.logisticspipes.logistic.Interests;

public class ChassiModule extends LogisticsGuiModule {

	private final LogisticsModule[] modules;
	private final PipeLogisticsChassi parentChassis;

	public ChassiModule(int moduleCount, PipeLogisticsChassi parentChassis) {
		modules = new LogisticsModule[moduleCount];
		this.parentChassis = parentChassis;
		_service = parentChassis;
		registerPosition(ModulePositionType.IN_PIPE, 0);
	}

	public void installModule(int slot, LogisticsModule module) {
		modules[slot] = module;
	}

	public void removeModule(int slot) {
		modules[slot] = null;
	}

	public LogisticsModule getModule(int slot) {
		return modules[slot];
	}

	public boolean hasModule(int slot) {
		return (modules[slot] != null);
	}

	public LogisticsModule[] getModules() {
		return modules;
	}

	public Stream<LogisticsModule> streamModules() {
		return Arrays.stream(modules).filter(Objects::nonNull);
	}

	@Override
	public Stream<Interests> streamInterests() {
		throw new UnsupportedOperationException();
	}

	@Override
	public LogisticsModule getSubModule(int slot) {
		if (slot < 0 || slot >= modules.length) {
			return null;
		}
		return modules[slot];
	}

	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound) {
		for (int i = 0; i < modules.length; i++) {
			if (modules[i] != null) {
				NBTTagCompound slot = nbttagcompound.getCompoundTag("slot" + i);
				if (slot != null) {
					modules[i].readFromNBT(slot);
				}
			}
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound) {
		for (int i = 0; i < modules.length; i++) {
			if (modules[i] != null) {
				NBTTagCompound slot = new NBTTagCompound();
				modules[i].writeToNBT(slot);
				nbttagcompound.setTag("slot" + i, slot);
			}
		}
	}

	@Override
	public void tick() {
		for (LogisticsModule module : modules) {
			if (module == null) {
				continue;
			}
			module.tick();
		}
	}

	@Override
	public void registerHandler(IWorldProvider world, IPipeServiceProvider service) {
		//Not used in Chassie Module
	}

	@Override
	public boolean recievePassive() {
		for (LogisticsModule module : modules) {
			if (module != null && module.recievePassive()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public List<CCSinkResponder> queueCCSinkEvent(ItemIdentifierStack item) {
		List<CCSinkResponder> list = new ArrayList<>();
		for (LogisticsModule module : modules) {
			if (module != null) {
				list.addAll(module.queueCCSinkEvent(item));
			}
		}
		return list;
	}

	@Override
	protected ModuleCoordinatesGuiProvider getPipeGuiProvider() {
		return NewGuiHandler.getGui(ChassiGuiProvider.class).setFlag(parentChassis.getUpgradeManager().hasUpgradeModuleUpgrade());
	}

	@Override
	protected ModuleInHandGuiProvider getInHandGuiProvider() {
		return null;
	}

	@Override
	public boolean stillWantItem(IRoutedItem item) {
		throw new UnsupportedOperationException();
	}

	@Override
	public EnumFacing itemArrived(IRoutedItem item, EnumFacing blocked) {
		throw new UnsupportedOperationException();
	}
}
