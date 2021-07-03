package logisticspipes.network.packets.orderer;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;

import logisticspipes.LPItems;
import logisticspipes.network.abstractpackets.IntegerCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.pipes.PipeItemsRequestLogisticsMk2;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.request.RequestHandler;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class DiskMacroRequestPacket extends IntegerCoordinatesPacket {

	public DiskMacroRequestPacket(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new DiskMacroRequestPacket(getId());
	}

	@Override
	public void processPacket(PlayerEntity player) {
		final LogisticsTileGenericPipe pipe = this.getPipe(player.world);
		if (pipe == null) {
			return;
		}
		if (pipe.pipe instanceof PipeItemsRequestLogisticsMk2) {
			if (((PipeItemsRequestLogisticsMk2) pipe.pipe).getDisk() == null) {
				return;
			}
			if (!((PipeItemsRequestLogisticsMk2) pipe.pipe).getDisk().getItem().equals(LPItems.disk)) {
				return;
			}
			if (!((PipeItemsRequestLogisticsMk2) pipe.pipe).getDisk().hasTag()) {
				return;
			}
			CompoundNBT nbt = ((PipeItemsRequestLogisticsMk2) pipe.pipe).getDisk().getTag();
			if (!nbt.contains("macroList")) {
				ListNBT list = new ListNBT();
				nbt.setTag("macroList", list);
			}
			ListNBT list = nbt.getList("macroList", 10);
			for (int i = 0; i < list.size(); i++) {
				if (i == getInteger()) {
					CompoundNBT itemlist = list.getCompound(i);
					RequestHandler.requestMacrolist(itemlist, (PipeItemsRequestLogisticsMk2) pipe.pipe, player);
					break;
				}
			}
		}
		if (pipe.pipe instanceof PipeBlockRequestTable) {
			if (((PipeBlockRequestTable) pipe.pipe).getDisk() == null) {
				return;
			}
			if (!((PipeBlockRequestTable) pipe.pipe).getDisk().getItem().equals(LPItems.disk)) {
				return;
			}
			if (!((PipeBlockRequestTable) pipe.pipe).getDisk().hasTag()) {
				return;
			}
			CompoundNBT nbt = ((PipeBlockRequestTable) pipe.pipe).getDisk().getTag();
			if (!nbt.contains("macroList")) {
				ListNBT list = new ListNBT();
				nbt.setTag("macroList", list);
			}
			ListNBT list = nbt.getList("macroList", 10);
			for (int i = 0; i < list.size(); i++) {
				if (i == getInteger()) {
					CompoundNBT itemlist = list.getCompound(i);
					RequestHandler.requestMacrolist(itemlist, (PipeBlockRequestTable) pipe.pipe, player);
					break;
				}
			}
		}
	}
}
