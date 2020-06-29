package logisticspipes.modules;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import logisticspipes.LogisticsPipes;
import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.interfaces.IQueueCCEvent;
import logisticspipes.interfaces.ISlotUpgradeManager;
import logisticspipes.interfaces.IWorldProvider;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.computers.interfaces.CCCommand;
import logisticspipes.proxy.computers.interfaces.CCType;
import logisticspipes.proxy.computers.interfaces.ILPCCTypeHolder;
import logisticspipes.proxy.computers.objects.CCSinkResponder;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import network.rs485.logisticspipes.api.LogisticsModule;
import network.rs485.logisticspipes.api.RoutedLogisticsPipe;
import network.rs485.logisticspipes.module.Gui;

@CCType(name = "LogisticsModule")
public abstract class AbstractModule implements LogisticsModule, ILPCCTypeHolder {

	public final UUID moduleId = UUID.randomUUID();

	/**
	 * Registers the Inventory and ItemSender to the module
	 *
	 * @param world   that the module is in.
	 * @param service Inventory access, power and utility functions provided by the
	 *                pipe
	 */
	public void registerHandler(IWorldProvider world, IPipeServiceProvider service) {
	}

	protected ModulePositionType slot;
	protected int positionInt;

	/**
	 * Registers the slot type the module is in
	 */
	public void registerPosition(ModulePositionType slot, int positionInt) {
		this.slot = slot;
		this.positionInt = positionInt;
	}

	@Nonnull
	public BlockPos getBlockPos() {
		if (slot.isInWorld()) {
			return getPipe().getPos();
		} else {
			if (LogisticsPipes.isDEBUG()) {
				throw new IllegalStateException("Module is not in world, but getBlockPos was called");
			}
			return BlockPos.ORIGIN;
		}
	}

	public ModulePositionType getSlot() {
		return this.slot;
	}

	public int getPositionInt() {
		return this.positionInt;
	}

	@NotNull
	@Override
	public RoutedLogisticsPipe getPipe() {
		return (CoreRoutedPipe) pipe;
	}

	public enum ModulePositionType {
		SLOT(true),
		IN_HAND(false),
		IN_PIPE(true);

		@Getter
		private final boolean inWorld;

		ModulePositionType(boolean inWorld) {
			this.inWorld = inWorld;
		}
	}

	/**
	 * Gives an sink answer on the given itemstack
	 *
	 * @param stack              to sink
	 * @param item               to sink
	 * @param bestPriority       best priority seen so far
	 * @param bestCustomPriority best custom subpriority
	 * @param allowDefault       is a default only sink allowed to sink this?
	 * @param includeInTransit   inclide the "in transit" items? -- true for a destination
	 *                           search, false for a sink check.
	 * @param forcePassive       check for passive routing only, in case this method is redirected to other sinks
	 * @return SinkReply whether the module sinks the item or not
	 */
	public SinkReply sinksItem(@Nonnull ItemStack stack, ItemIdentifier item, int bestPriority, int bestCustomPriority, boolean allowDefault, boolean includeInTransit, boolean forcePassive) {
		return null;
	}

	/**
	 * A tick for the Module
	 */
	public abstract void tick();

	/**
	 * Is this module interested in all items, or just some specific ones?
	 *
	 * @return true: this module will be checked against every item request
	 * false: only requests involving items collected by {@link #collectSpecificInterests(Collection)} will be checked
	 */
	public abstract boolean hasGenericInterests();

	/**
	 * Collects the items which this module is capable of providing or supplying
	 * (or is otherwise interested in)
	 * @param itemidCollection the collection to add the interests to
	 */
	public void collectSpecificInterests(@Nonnull Collection<ItemIdentifier> itemidCollection) {}

	public abstract boolean interestedInAttachedInventory();

	/**
	 * is this module interested in receiving any damage variant of items in the
	 * attached inventory?
	 */
	public abstract boolean interestedInUndamagedID();

	/**
	 * is this module a valid destination for bounced items.
	 */
	public abstract boolean recievePassive();

	/**
	 * Returns whether the module should be displayed the effect when as an
	 * item.
	 *
	 * @return True to show effect False to no effect (default)
	 */
	public boolean hasEffect() {
		return false;
	}

	public Stream<CCSinkResponder> queueCCSinkEvent(ItemIdentifierStack item) {
		return Stream.empty();
	}

	public void registerCCEventQueuer(IQueueCCEvent eventQueuer) {}

	@CCCommand(description = "Returns true if the Pipe has a gui")
	public boolean hasGui() {
		return this instanceof Gui;
	}

	@Nonnull
	public AbstractModule getModule() {
		return this;
	}

	@Nullable
	protected ISlotUpgradeManager getUpgradeManager() {
		if (pipe == null) {
			return null;
		}
		return pipe.getUpgradeManager(slot, positionInt);
	}

	/**
	 * typically used when the neighboring block changes
	 */
	public void clearCache() {}

	@Override
	public String toString() {
		try {
			return getClass().getName() + " at " + getPipe().getPos() + " in " + getPipe().getWorld();
		} catch (NullPointerException e) {
			return super.toString();
		}
	}

}
