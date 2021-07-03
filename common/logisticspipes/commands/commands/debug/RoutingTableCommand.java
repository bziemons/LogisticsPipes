package logisticspipes.commands.commands.debug;

import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.StringTextComponent;

import logisticspipes.commands.abstracts.ICommandHandler;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.routingdebug.RoutingUpdateAskForTarget;
import logisticspipes.proxy.MainProxy;

public class RoutingTableCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "rt", "routing" };
	}

	@Override
	public boolean isCommandUsableBy(ICommandSource sender) {
		return sender instanceof PlayerEntity;
	}

	@Override
	public String[] getDescription() {
		return new String[] { "Starts debugging the Routing Table", "update of the pipe you are currently looking at." };
	}

	@Override
	public void executeCommand(ICommandSource sender, String[] args) {
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(RoutingUpdateAskForTarget.class), (PlayerEntity) sender);
		sender.sendMessage(new StringTextComponent("Asking for Target."));
	}
}
