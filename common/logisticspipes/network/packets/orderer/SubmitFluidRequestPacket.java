package logisticspipes.network.packets.orderer;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.dimension.Dimension;

import logisticspipes.LogisticsPipes;
import logisticspipes.interfaces.routing.IRequestFluid;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.RequestPacket;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.request.RequestHandler;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class SubmitFluidRequestPacket extends RequestPacket {

	public SubmitFluidRequestPacket(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new SubmitFluidRequestPacket(getId());
	}

	@Override
	public void processPacket(PlayerEntity player) {
		Dimension dim = MainProxy.getDimension(player, getDimension());
		if (dim == null) {
			LogisticsPipes.getLOGGER().warn("Could not find dimension for packet " + this);
			return;
		}
		final BlockPos pos = new BlockPos(getPosX(), getPosY(), getPosZ());
		final LogisticsTileGenericPipe pipe = MainProxy.proxy.getPipeInDimensionAt(dim, pos, player);
		if (pipe == null || !(pipe.pipe instanceof CoreRoutedPipe) || !(pipe.pipe instanceof IRequestFluid)) {
			return;
		}
		RequestHandler.requestFluid(player, getStack(), (CoreRoutedPipe) pipe.pipe, (IRequestFluid) pipe.pipe);
	}
}
