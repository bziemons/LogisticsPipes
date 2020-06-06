package logisticspipes.transport;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import logisticspipes.pipes.PipeItemsSystemDestinationLogistics;
import logisticspipes.pipes.PipeItemsSystemEntranceLogistics;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.PipeRoutingConnectionType;
import logisticspipes.transport.LPTravelingItem.LPTravelingItemServer;
import network.rs485.grow.GROW;

public class EntrencsTransport extends PipeTransportLogistics {

	public EntrencsTransport() {
		super(true);
	}

	public PipeItemsSystemEntranceLogistics pipe;

	@Override
	public CompletableFuture<RoutingResult> resolveDestination(LPTravelingItemServer data) {
		if (data.getDestination() < 0 || data.getArrived()) {
			if (pipe.getLocalFreqUUID() != null) {
				if (pipe.useEnergy(5)) {
					CompletableFuture<List<ExitRoute>> iRoutersByCost = pipe.getRouter().getIRoutersByCost();
					for (ExitRoute router : GROW.asyncWorkAround(iRoutersByCost)) {
						if (!router.containsFlag(PipeRoutingConnectionType.canRouteTo)) {
							continue;
						}
						CoreRoutedPipe lPipe = router.destination.getPipe();
						if (lPipe instanceof PipeItemsSystemDestinationLogistics) {
							PipeItemsSystemDestinationLogistics dPipe = (PipeItemsSystemDestinationLogistics) lPipe;
							if (dPipe.getTargetUUID() != null) {
								if (dPipe.getTargetUUID().equals(pipe.getLocalFreqUUID())) {
									data.setDestination(dPipe.getRouter().getSimpleID());
									data.setArrived(false);
								}
							}
						}
					}
				}
			}
		}
		return super.resolveDestination(data);
	}
}
