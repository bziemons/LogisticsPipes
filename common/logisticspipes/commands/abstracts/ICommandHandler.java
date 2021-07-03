package logisticspipes.commands.abstracts;

import net.minecraft.command.ICommandSource;

public interface ICommandHandler {

	String[] getNames();

	boolean isCommandUsableBy(ICommandSource sender);

	String[] getDescription();

	void executeCommand(ICommandSource sender, String[] args);
}
