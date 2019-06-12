package logisticspipes.network.packets.block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;

import logisticspipes.blocks.stats.LogisticsStatisticsTileEntity;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.PipeItemsCraftingLogistics;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.routing.ExitRoute;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.item.ItemIdentifierStack;

@StaticResolve
public class RequestRunningCraftingTasks extends CoordinatesPacket {

	public RequestRunningCraftingTasks(int id) {
		super(id);
	}

	@Override
	public void processPacket(EntityPlayer player) {
		LogisticsStatisticsTileEntity tile = this.getTile(player.getEntityWorld(), LogisticsStatisticsTileEntity.class);
		CoreRoutedPipe pipe = tile.getConnectedPipe();
		if (pipe == null) {
			return;
		}

		List<ItemIdentifierStack> items = new ArrayList<>();

		for (ExitRoute r : pipe.getRouter().getIRoutersByCost()) {
			if (r == null) {
				continue;
			}
			if (r.destination.getPipe() instanceof PipeItemsCraftingLogistics) {
				PipeItemsCraftingLogistics crafting = (PipeItemsCraftingLogistics) r.destination.getPipe();
				// TODO PROVIDE REFACTOR
				List<ItemIdentifierStack> content = Collections.emptyList(); // crafting.getItemOrderManager().getContentList(player.getEntityWorld());
				items.addAll(content);
			}
		}
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(RunningCraftingTasks.class).setIdentList(items), player);
	}

	@Override
	public ModernPacket template() {
		return new RequestRunningCraftingTasks(getId());
	}
}
