package logisticspipes.logisticspipes;

import net.minecraft.util.Direction;

import logisticspipes.modules.LogisticsModule;
import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.item.ItemIdentifierStack;

public class ChassisTransportLayer extends TransportLayer {

	private final PipeLogisticsChassis _chassiPipe;

	public ChassisTransportLayer(PipeLogisticsChassis chassiPipe) {
		_chassiPipe = chassiPipe;
	}

	@Override
	public Direction itemArrived(IRoutedItem item, Direction blocked) {
		if (item.getItemIdentifierStack() != null) {
			_chassiPipe.recievedItem(item.getItemIdentifierStack().getStackSize());
		}
		return _chassiPipe.getPointedDirection();
	}

	@Override
	public boolean stillWantItem(IRoutedItem item) {
		LogisticsModule module = _chassiPipe.getLogisticsModule();
		if (module == null) {
			_chassiPipe.notifyOfItemArival(item.getInfo());
			return false;
		}
		if (!_chassiPipe.isEnabled()) {
			_chassiPipe.notifyOfItemArival(item.getInfo());
			return false;
		}
		final ItemIdentifierStack itemidStack = item.getItemIdentifierStack();
		SinkReply reply = module.sinksItem(itemidStack.makeNormalStack(), itemidStack.getItem(), -1, 0, true, false, false);
		if (reply == null || reply.maxNumberOfItems < 0) {
			_chassiPipe.notifyOfItemArival(item.getInfo());
			return false;
		}

		if (reply.maxNumberOfItems > 0 && itemidStack.getStackSize() > reply.maxNumberOfItems) {
			Direction o = _chassiPipe.getPointedDirection();
			if (o == null) {
				o = Direction.UP;
			}

			item.split(reply.maxNumberOfItems, o);
		}
		return true;
	}

}
