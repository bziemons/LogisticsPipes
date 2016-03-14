package logisticspipes.network.packets.pipe;

import java.lang.ref.WeakReference;

import network.rs485.logisticspipes.network.LPChannel;

import logisticspipes.network.abstractpackets.IntegerPacket;

import logisticspipes.transport.LPTravelingItem;
import logisticspipes.transport.LPTravelingItem.LPTravelingItemServer;

import net.minecraft.entity.player.EntityPlayer;

import logisticspipes.utils.StaticResolve;

@StaticResolve
public class PipeContentRequest extends IntegerPacket {

	public PipeContentRequest(int id) {
		super(id);
	}

	@Override
	public void processPacket(EntityPlayer player) {
		WeakReference<LPTravelingItemServer> ref = LPTravelingItem.serverList.get(getInteger());
		LPTravelingItemServer item = null;
		if (ref != null) {
			item = ref.get();
		}
		if (item != null) {
			LPChannel.sendPacketToPlayer(PacketHandler.getPacket(PipeContentPacket.class).setItem(item.getItemIdentifierStack()).setTravelId(item.getId()),
					player);
		}
	}

	@Override
	public AbstractPacket template() {
		return new PipeContentRequest(getId());
	}
}
