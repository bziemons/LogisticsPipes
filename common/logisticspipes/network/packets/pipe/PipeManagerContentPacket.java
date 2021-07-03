package logisticspipes.network.packets.pipe;

import java.util.List;

import net.minecraft.entity.player.PlayerEntity;

import lombok.Getter;
import lombok.Setter;

import network.rs485.logisticspipes.network.packets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.routing.order.ClientSideOrderInfo;
import logisticspipes.routing.order.IOrderInfoProvider;
import logisticspipes.routing.order.LogisticsOrder;
import logisticspipes.routing.order.LogisticsOrderManager;
import logisticspipes.utils.StaticResolve;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class PipeManagerContentPacket extends CoordinatesPacket {

	@Getter
	@Setter
	private LogisticsOrderManager<? extends LogisticsOrder, ?> manager;

	private List<IOrderInfoProvider> clientOrder;

	public PipeManagerContentPacket(int id) {
		super(id);
	}

	@Override
	public void processPacket(PlayerEntity player) {
		LogisticsTileGenericPipe pipe = this.getPipe(player.getEntityWorld());
		if (pipe == null || !(pipe.pipe instanceof CoreRoutedPipe)) {
			return;
		}
		CoreRoutedPipe cPipe = (CoreRoutedPipe) pipe.pipe;
		cPipe.getClientSideOrderManager().clear();
		cPipe.getClientSideOrderManager().addAll(clientOrder);
	}

	@Override
	public ModernPacket template() {
		return new PipeManagerContentPacket(getId());
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);

		// manual collection write, because generics are wrong here
		output.writeInt(manager.size());
		for (LogisticsOrder order : manager) {
			output.writeSerializable(order);
		}
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);

		clientOrder = input.readLinkedList(ClientSideOrderInfo::new);
	}
}
