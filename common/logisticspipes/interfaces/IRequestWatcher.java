package logisticspipes.interfaces;

import logisticspipes.request.resources.IResource;
import network.rs485.logisticspipes.logistic.TempOrders;

public interface IRequestWatcher {

	public void handleOrderList(IResource stack, TempOrders orders);

	public void handleClientSideListInfo(int id, IResource stack, TempOrders orders);

	public void handleClientSideRemove(int id);
}
