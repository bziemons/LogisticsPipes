package logisticspipes.modules;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.jetbrains.annotations.NotNull;

import logisticspipes.interfaces.IInventoryUtil;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.abstractguis.ModuleCoordinatesGuiProvider;
import logisticspipes.network.abstractguis.ModuleInHandGuiProvider;
import logisticspipes.network.guis.pipe.ChassiGuiProvider;
import logisticspipes.pipes.PipeLogisticsChassi;
import logisticspipes.pipes.PipeLogisticsChassi.ChassiTargetInformation;
import logisticspipes.proxy.computers.objects.CCSinkResponder;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import network.rs485.logisticspipes.api.LogisticsModule;
import network.rs485.logisticspipes.api.RoutedLogisticsPipe;
import network.rs485.logisticspipes.module.Gui;

public class ChassiModule extends AbstractModule implements Gui {

	private final LogisticsModule[] modules;
	private final PipeLogisticsChassi parentChassis;

	public ChassiModule(int moduleCount, PipeLogisticsChassi parentChassis) {
		modules = new AbstractModule[moduleCount];
		this.parentChassis = parentChassis;
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

	public Stream<LogisticsModule> modules() {
		return Arrays.stream(modules).filter(Objects::nonNull);
	}

	@Override
	public SinkReply sinksItem(@Nonnull ItemStack stack, ItemIdentifier item, int bestPriority, int bestCustomPriority, boolean allowDefault, boolean includeInTransit, boolean forcePassive) {
		SinkReply bestresult = null;
		final Iterator<LogisticsModule> moduleIterator = modules().iterator();
		while (moduleIterator.hasNext()) {
			LogisticsModule module = moduleIterator.next();
			if (module instanceof AbstractModule) {
				if (!forcePassive || ((AbstractModule) module).recievePassive()) {
					SinkReply result = ((AbstractModule) module).sinksItem(stack, item, bestPriority, bestCustomPriority, allowDefault, includeInTransit, forcePassive);
					if (result != null && result.maxNumberOfItems >= 0) {
						bestresult = result;
						bestPriority = result.fixedPriority.ordinal();
						bestCustomPriority = result.customPriority;
					}
				}
			}
		}

		if (bestresult == null) {
			return null;
		}
		//Always deny items when we can't put the item anywhere
		IInventoryUtil invUtil = parentChassis.getSneakyInventory(ModulePositionType.SLOT, ((ChassiTargetInformation) bestresult.addInfo).getModuleSlot());
		if (invUtil == null) {
			return null;
		}
		int roomForItem;
		if (includeInTransit) {
			int onRoute = parentChassis.countOnRoute(item);
			final ItemStack copy = stack.copy();
			copy.setCount(onRoute + item.getMaxStackSize());
			roomForItem = invUtil.roomForItem(copy);
			roomForItem -= onRoute;
		} else {
			roomForItem = invUtil.roomForItem(stack);
		}
		if (roomForItem < 1) {
			return null;
		}

		if (bestresult.maxNumberOfItems == 0) {
			return new SinkReply(bestresult, roomForItem);
		}
		return new SinkReply(bestresult, Math.min(bestresult.maxNumberOfItems, roomForItem));
	}

	@Override
	public void readFromNBT(@Nonnull NBTTagCompound nbttagcompound) {
		for (int i = 0; i < modules.length; i++) {
			if (modules[i] != null) {
				if (nbttagcompound.hasKey("slot" + i)) {
					modules[i].readFromNBT(nbttagcompound.getCompoundTag("slot" + i));
				}
			}
		}
	}

	@Override
	public void writeToNBT(@Nonnull NBTTagCompound tag) {
		for (int i = 0; i < modules.length; i++) {
			if (modules[i] != null) {
				NBTTagCompound slotTag = new NBTTagCompound();
				modules[i].writeToNBT(slotTag);
				tag.setTag("slot" + i, slotTag);
			}
		}
	}

	@Override
	public void tick() {
		modules().forEach(LogisticsModule::tick);
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
		return modules().anyMatch(LogisticsModule::recievePassive);
	}

	@Override
	public Stream<CCSinkResponder> queueCCSinkEvent(ItemIdentifierStack item) {
		return modules().flatMap(logisticsModule -> logisticsModule.queueCCSinkEvent(item));
	}

	@Nonnull
	@Override
	public ModuleCoordinatesGuiProvider getPipeGuiProvider() {
		return NewGuiHandler.getGui(ChassiGuiProvider.class).setFlag(parentChassis.getUpgradeManager().hasUpgradeModuleUpgrade());
	}

	@Nonnull
	@Override
	public ModuleInHandGuiProvider getInHandGuiProvider() {
		throw new UnsupportedOperationException("Chassis GUI can never be opened in hand");
	}

	@NotNull
	@Override
	public RoutedLogisticsPipe getPipe() {
		return null;
	}

	@NotNull
	@Override
	public UUID getModuleId() {
		return null;
	}
}
