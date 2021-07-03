package logisticspipes.commands.commands;

import net.minecraft.command.ICommandSource;
import net.minecraft.util.text.StringTextComponent;

import logisticspipes.commands.abstracts.ICommandHandler;
import logisticspipes.utils.string.ChatColor;

public class ClearCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "clear" };
	}

	@Override
	public boolean isCommandUsableBy(ICommandSource sender) {
		return true;
	}

	@Override
	public String[] getDescription() {
		return new String[] { "Clears the chat window from every content", ChatColor.GRAY + "add '" + ChatColor.YELLOW + "all" + ChatColor.GRAY + "' to also clear the send messages" };
	}

	@Override
	public void executeCommand(ICommandSource sender, String[] args) {
		if (args.length <= 0 || !args[0].equalsIgnoreCase("all")) {
			sender.sendMessage(new StringTextComponent("%LPSTORESENDMESSAGE%"));
			sender.sendMessage(new StringTextComponent("%LPCLEARCHAT%"));
			sender.sendMessage(new StringTextComponent("%LPRESTORESENDMESSAGE%"));
		} else {
			sender.sendMessage(new StringTextComponent("%LPCLEARCHAT%"));
		}
	}
}
