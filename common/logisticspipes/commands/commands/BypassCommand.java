package logisticspipes.commands.commands;

import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.StringTextComponent;

import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.commands.LogisticsPipesCommand;
import logisticspipes.commands.abstracts.ICommandHandler;

public class BypassCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "bypass", "bp" };
	}

	@Override
	public boolean isCommandUsableBy(ICommandSource sender) {
		return sender instanceof PlayerEntity && LogisticsPipesCommand.isOP(sender);
	}

	@Override
	public String[] getDescription() {
		return new String[] { "Allows to enable/disable the", "security station bypass token" };
	}

	@Override
	public void executeCommand(ICommandSource sender, String[] args) {
		if (!LogisticsSecurityTileEntity.byPassed.contains((PlayerEntity) sender)) {
			LogisticsSecurityTileEntity.byPassed.add((PlayerEntity) sender);
			sender.sendMessage(new StringTextComponent("Enabled"));
		} else {
			LogisticsSecurityTileEntity.byPassed.remove((PlayerEntity) sender);
			sender.sendMessage(new StringTextComponent("Disabled"));
		}
	}
}
