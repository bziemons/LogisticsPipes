package logisticspipes.commands.commands;

import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.StringTextComponent;

import logisticspipes.commands.LogisticsPipesCommand;
import logisticspipes.commands.abstracts.ICommandHandler;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.RequestUpdateNamesPacket;
import logisticspipes.proxy.MainProxy;

public class TransferNamesCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "transfernames", "tn" };
	}

	@Override
	public boolean isCommandUsableBy(ICommandSource sender) {
		return sender instanceof PlayerEntity && LogisticsPipesCommand.isOP(sender);
	}

	@Override
	public String[] getDescription() {
		return new String[] { "Sends all item names form the client", "to the server to update the Language Database" };
	}

	@Override
	public void executeCommand(ICommandSource sender, String[] args) {
		sender.sendMessage(new StringTextComponent("Requesting Transfer"));
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(RequestUpdateNamesPacket.class), (PlayerEntity) sender);
		MainProxy.proxy.sendNameUpdateRequest((PlayerEntity) sender);
	}
}
