package logisticspipes.network.packets.orderer;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;

import logisticspipes.LPItems;
import logisticspipes.network.PacketHandler;
import network.rs485.logisticspipes.network.packets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.pipes.PipeItemsRequestLogisticsMk2;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class DiskRequestConectPacket extends CoordinatesPacket {

	public DiskRequestConectPacket(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new DiskRequestConectPacket(getId());
	}

	@Override
	public void processPacket(PlayerEntity player) {
		final LogisticsTileGenericPipe pipe = this.getPipe(player.world);
		if (pipe == null) {
			return;
		}
		if (pipe.pipe instanceof PipeItemsRequestLogisticsMk2) {
			if (((PipeItemsRequestLogisticsMk2) pipe.pipe).getDisk() != null) {
				if (((PipeItemsRequestLogisticsMk2) pipe.pipe).getDisk().getItem().equals(LPItems.disk)) {
					if (!((PipeItemsRequestLogisticsMk2) pipe.pipe).getDisk().hasTag()) {
						((PipeItemsRequestLogisticsMk2) pipe.pipe).getDisk().setTag(new CompoundNBT());
					}
				}
			}
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(DiscContent.class).setStack(((PipeItemsRequestLogisticsMk2) pipe.pipe).getDisk()).setBlockPos(pipe.getPos()), player);
		}
		if (pipe.pipe instanceof PipeBlockRequestTable) {
			if (((PipeBlockRequestTable) pipe.pipe).diskInv.getStackInSlot(0) != null) {
				if (((PipeBlockRequestTable) pipe.pipe).diskInv.getStackInSlot(0).getItem().equals(LPItems.disk)) {
					if (!((PipeBlockRequestTable) pipe.pipe).diskInv.getStackInSlot(0).hasTag()) {
						((PipeBlockRequestTable) pipe.pipe).diskInv.getStackInSlot(0).setTag(new CompoundNBT());
					}
				}
			}
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(DiscContent.class).setStack(((PipeBlockRequestTable) pipe.pipe).diskInv.getStackInSlot(0)).setBlockPos(pipe.getPos()), player);
		}
	}
}
