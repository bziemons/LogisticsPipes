package logisticspipes.modules.abstractmodules;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import lombok.Getter;

import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.interfaces.IQueueCCEvent;
import logisticspipes.interfaces.IWorldProvider;
import logisticspipes.interfaces.routing.ISaveState;
import logisticspipes.proxy.computers.interfaces.CCCommand;
import logisticspipes.proxy.computers.interfaces.CCType;
import logisticspipes.proxy.computers.interfaces.ILPCCTypeHolder;
import logisticspipes.proxy.computers.objects.CCSinkResponder;
import logisticspipes.utils.item.ItemIdentifierStack;
import network.rs485.logisticspipes.logistic.IDestination;
import network.rs485.logisticspipes.logistic.Interests;

@CCType(name = "LogisticsModule")
public abstract class LogisticsModule implements IDestination, ISaveState, ILPCCTypeHolder {

	private Object ccType;

	protected IWorldProvider _world;
	protected IPipeServiceProvider _service;

	/**
	 * Registers the Inventory and ItemSender to the module
	 *
	 * @param world   that the module is in.
	 * @param service Inventory access, power and utility functions provided by the
	 *                pipe
	 */
	public void registerHandler(IWorldProvider world, IPipeServiceProvider service) {
		_world = world;
		_service = service;
	}

	@Getter
	protected ModulePositionType slot;
	@Getter
	protected int positionInt;

	/**
	 * Registers the slot type the module is in
	 */
	public void registerPosition(ModulePositionType slot, int positionInt) {
		this.slot = slot;
		this.positionInt = positionInt;
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
	 * typically returns the coord of the pipe that holds it.
	 */
	public abstract int getX();

	/**
	 * typically returns the coord of the pipe that holds it.
	 */
	public abstract int getY();

	/**
	 * typically returns the coord of the pipe that holds it.
	 */
	public abstract int getZ();

	/**
	 * Returns submodules. Normal modules don't have submodules
	 *
	 * @param slot of the requested module
	 * @return
	 */
	public abstract LogisticsModule getSubModule(int slot);

	/**
	 * A tick for the Module
	 */
	public abstract void tick();

	/**
	 * is this module a valid destination for bounced items.
	 */
	public abstract boolean recievePassive();

	/**
	 * Returns the module's interests.
	 *
	 * @return a {@link Stream} of {@link Interests}.
	 */
	public abstract Stream<Interests> streamInterests();

	/**
	 * Returns whether the module should be displayed the effect when as an
	 * item.
	 *
	 * @return True to show effect False to no effect (default)
	 */
	public boolean hasEffect() {
		return false;
	}

	public List<CCSinkResponder> queueCCSinkEvent(ItemIdentifierStack item) {
		return new ArrayList<>(0);
	}

	public void registerCCEventQueuer(IQueueCCEvent eventQueuer) {}

	@CCCommand(description = "Returns if the Pipe has a gui")
	public boolean hasGui() {
		return false;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + "(" + getX() + ", " + getY() + ", " + getZ() + ")";
	}

	/**
	 * typically used when the neighboring block changes
	 */
	public void clearCache() {}

	@Override
	public void setCCType(Object type) {
		ccType = type;
	}

	@Override
	public Object getCCType() {
		return ccType;
	}
}
