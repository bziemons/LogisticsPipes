package logisticspipes.network.packets.orderer;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.dimension.Dimension;

import logisticspipes.LogisticsPipes;
import logisticspipes.network.abstractpackets.IntegerCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.request.RequestHandler;

// FIXME: @StaticResolve
public class OrdererRefreshRequestPacket extends IntegerCoordinatesPacket {

	public OrdererRefreshRequestPacket(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new OrdererRefreshRequestPacket(getId());
	}

	@Override
	public void processPacket(PlayerEntity player) {
		Dimension dim = MainProxy.getDimension(player, (getInteger() - (getInteger() % 10)) / 10);
		if (dim == null) {
			LogisticsPipes.getLOGGER().warn("Could not find dimension for packet " + this);
			return;
		}
		final BlockPos pos = new BlockPos(getPosX(), getPosY(), getPosZ());
		final LogisticsTileGenericPipe pipe = MainProxy.proxy.getPipeInDimensionAt(dim,
				pos, player);
		if (pipe == null || !(pipe.pipe instanceof CoreRoutedPipe)) {
			return;
		}
		RequestHandler.DisplayOptions option;
		switch (getInteger() % 10) {
			case 0:
				option = RequestHandler.DisplayOptions.Both;
				break;
			case 1:
				option = RequestHandler.DisplayOptions.SupplyOnly;
				break;
			case 2:
				option = RequestHandler.DisplayOptions.CraftOnly;
				break;
			default:
				option = RequestHandler.DisplayOptions.Both;
				break;
		}
		RequestHandler.refresh(player, (CoreRoutedPipe) pipe.pipe, option);
	}

}
