package logisticspipes.commands.commands;

import net.minecraft.command.ICommandSource;
import net.minecraft.util.text.StringTextComponent;

import logisticspipes.commands.abstracts.ICommandHandler;
import logisticspipes.ticks.RoutingTableUpdateThread;

public class RoutingThreadCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "routingthread", "rt" };
	}

	@Override
	public boolean isCommandUsableBy(ICommandSource sender) {
		return true;
	}

	@Override
	public String[] getDescription() {
		return new String[] { "Display Routing thread status information" };
	}

	@Override
	public void executeCommand(ICommandSource sender, String[] args) {
		sender.sendMessage(new StringTextComponent("RoutingTableUpdateThread: Queued: " + RoutingTableUpdateThread.size()));
		sender.sendMessage(new StringTextComponent("RoutingTableUpdateThread: Average: " + RoutingTableUpdateThread.getAverage() + "ns"));
	}
}
