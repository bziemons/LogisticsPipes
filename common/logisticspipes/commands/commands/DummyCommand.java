package logisticspipes.commands.commands;

import net.minecraft.command.ICommandSource;

import logisticspipes.commands.abstracts.ICommandHandler;

public class DummyCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "dummy" };
	}

	@Override
	public boolean isCommandUsableBy(ICommandSource sender) {
		return true;
	}

	@Override
	public String[] getDescription() {
		return new String[] { "#This Command doesn't do anything" };
	}

	@Override
	public void executeCommand(ICommandSource sender, String[] args) {}
}
