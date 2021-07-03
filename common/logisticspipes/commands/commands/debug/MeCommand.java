package logisticspipes.commands.commands.debug;

import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.StringTextComponent;

import logisticspipes.commands.abstracts.ICommandHandler;

public class MeCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "me", "self" };
	}

	@Override
	public boolean isCommandUsableBy(ICommandSource sender) {
		return sender instanceof PlayerEntity;
	}

	@Override
	public String[] getDescription() {
		return new String[] { "Start debugging the CommandSender" };
	}

	@Override
	public void executeCommand(ICommandSource sender, String[] args) {
		DebugGuiController.instance().startWatchingOf(sender, (PlayerEntity) sender);
		sender.sendMessage(new StringTextComponent("Starting SelfDebuging"));
	}
}
