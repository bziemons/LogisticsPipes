package logisticspipes.commands.commands.debug;

import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.StringTextComponent;

import logisticspipes.commands.abstracts.ICommandHandler;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.debug.PipeDebugLogAskForTarget;
import logisticspipes.network.packets.pipe.PipeDebugAskForTarget;
import logisticspipes.proxy.MainProxy;

public class PipeCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "pipe" };
	}

	@Override
	public boolean isCommandUsableBy(ICommandSource sender) {
		return sender instanceof PlayerEntity;
	}

	@Override
	public String[] getDescription() {
		return new String[] { "Set the pipe into debug mode" };
	}

	@Override
	public void executeCommand(ICommandSource sender, String[] args) {
		if (args.length != 1) {
			sender.sendMessage(new StringTextComponent("Wrong amount of arguments"));
			return;
		}
		if (args[0].equalsIgnoreCase("help")) {
			sender.sendMessage(new StringTextComponent("client, server, both or console"));
		} else if (args[0].equalsIgnoreCase("both")) {
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(PipeDebugAskForTarget.class).setServer(true), (PlayerEntity) sender);
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(PipeDebugAskForTarget.class).setServer(false), (PlayerEntity) sender);
			sender.sendMessage(new StringTextComponent("Asking for Target."));
		} else if (args[0].equalsIgnoreCase("console") || args[0].equalsIgnoreCase("c")) {
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(PipeDebugLogAskForTarget.class), (PlayerEntity) sender);
			sender.sendMessage(new StringTextComponent("Asking for Target."));
		} else {
			boolean isClient = args[0].equalsIgnoreCase("client");
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(PipeDebugAskForTarget.class).setServer(!isClient), (PlayerEntity) sender);
			sender.sendMessage(new StringTextComponent("Asking for Target."));
		}
	}
}
