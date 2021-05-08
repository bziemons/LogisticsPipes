package logisticspipes.interfaces;

import logisticspipes.request.resources.IResource;
import network.rs485.logisticspipes.logistic.TempOrders;

public interface IRequestWatcher {

	void handleOrderList(IResource stack, TempOrders orders);

	void handleClientSideListInfo(int id, IResource stack, TempOrders orders);

	void handleClientSideRemove(int id);
}
