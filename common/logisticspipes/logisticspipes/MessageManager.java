package logisticspipes.logisticspipes;

import java.util.LinkedList;

import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.orderer.ComponentList;
import logisticspipes.network.packets.orderer.MissingItems;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.ItemMessage;
import net.minecraft.entity.player.EntityPlayer;
import cpw.mods.fml.common.network.Player;

public class MessageManager {
	
	public static void errors(EntityPlayer player, LinkedList<ItemMessage> errors) {
//TODO 	MainProxy.sendPacketToPlayer(new PacketItems(NetworkConstants.MISSING_ITEMS, errors,true).getPacket(), (Player)player);
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(MissingItems.class).setItems(errors).setFlag(true), (Player)player);
	}

	public static void requested(EntityPlayer player, LinkedList<ItemMessage> items) {
//TODO	MainProxy.sendPacketToPlayer(new PacketItems(NetworkConstants.MISSING_ITEMS, items,false).getPacket(), (Player)player);
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(MissingItems.class).setItems(items).setFlag(false), (Player)player);
	}

	public static void simulated(EntityPlayer player, LinkedList<ItemMessage> used, LinkedList<ItemMessage> missing) {
//TODO 	MainProxy.sendPacketToPlayer(new PacketSimulate(NetworkConstants.COMPONENT_LIST, used,missing).getPacket(), (Player)player);		
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(ComponentList.class).setUsed(used).setMissing(missing), (Player)player);
	}
}
