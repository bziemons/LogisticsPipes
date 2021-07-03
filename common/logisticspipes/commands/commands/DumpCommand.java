package logisticspipes.commands.commands;

import net.minecraft.command.ICommandSource;
import net.minecraft.util.text.StringTextComponent;

import logisticspipes.commands.LogisticsPipesCommand;
import logisticspipes.commands.abstracts.ICommandHandler;

public class DumpCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "dump" };
	}

	@Override
	public boolean isCommandUsableBy(ICommandSource sender) {
		return LogisticsPipesCommand.isOP(sender);
	}

	@Override
	public String[] getDescription() {
		return new String[] { "Dumps the current Tread states", "into the server log" };
	}

	@Override
	public void executeCommand(ICommandSource sender, String[] args) {
		sender.sendMessage(new StringTextComponent("Dump Created"));
	}
}
