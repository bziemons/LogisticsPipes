package logisticspipes.modules;

import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;

import logisticspipes.interfaces.IInventoryUtil;
import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.interfaces.IWorldProvider;
import logisticspipes.modules.abstractmodules.LogisticsModule;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.routing.pathfinder.IPipeInformationProvider.ConnectionPipeType;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.SinkReply.FixedPriority;
import logisticspipes.utils.item.ItemIdentifier;
import network.rs485.logisticspipes.world.WorldCoordinatesWrapper;

//IHUDModuleHandler,
public class ModuleSatellite extends LogisticsModule {

	private final CoreRoutedPipe pipe;

	public ModuleSatellite(CoreRoutedPipe pipeItemsSatelliteLogistics) {
		pipe = pipeItemsSatelliteLogistics;
	}

	@Override
	public void registerHandler(IWorldProvider world, IPipeServiceProvider service) {}

	@Override
	public final int getX() {
		return pipe.getX();
	}

	@Override
	public final int getY() {
		return pipe.getY();
	}

	@Override
	public final int getZ() {
		return pipe.getZ();
	}

	private SinkReply _sinkReply = new SinkReply(FixedPriority.ItemSink, 0, 1, 0, null);

	private int spaceFor(ItemIdentifier item, boolean includeInTransit) {
		WorldCoordinatesWrapper worldCoordinates = new WorldCoordinatesWrapper(pipe.container);

		//@formatter:off
		int count = worldCoordinates.getConnectedAdjacentTileEntities(ConnectionPipeType.ITEM)
				.filter(adjacent -> adjacent.tileEntity instanceof IInventory)
		//@formatter:on
				.map(adjacent -> {
					IInventoryUtil util = SimpleServiceLocator.inventoryUtilFactory.getInventoryUtil(adjacent);
					return util.roomForItem(item, 9999);
				}).reduce(Integer::sum).orElse(0);

		if (includeInTransit) {
			count -= pipe.countOnRoute(item);
		}
		return count;
	}

	@Override
	public LogisticsModule getSubModule(int slot) {
		return null;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound) {}

	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound) {}

	@Override
	public void tick() {}

	@Override
	public boolean recievePassive() {
		return false;
	}

}
