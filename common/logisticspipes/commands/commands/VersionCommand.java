package logisticspipes.commands.commands;

import net.minecraft.command.ICommandSource;
import net.minecraft.util.text.StringTextComponent;

import logisticspipes.LogisticsPipes;
import logisticspipes.commands.abstracts.ICommandHandler;
import logisticspipes.ticks.VersionChecker;

public class VersionCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "version", "v" };
	}

	@Override
	public boolean isCommandUsableBy(ICommandSource sender) {
		return true;
	}

	@Override
	public String[] getDescription() {
		return new String[] { "Display the used LP version", "and shows, if an update is available" };
	}

	@Override
	public void executeCommand(ICommandSource sender, String[] args) {
		sender.sendMessage(new StringTextComponent(LogisticsPipes.getVersionString()));

		VersionChecker versionChecker = LogisticsPipes.versionChecker;
		sender.sendMessage(new StringTextComponent(versionChecker.getVersionCheckerStatus()));

		if (versionChecker.isVersionCheckDone() && versionChecker.getVersionInfo().isNewVersionAvailable()) {
			sender.sendMessage(new StringTextComponent("Use \"/logisticspipes changelog\" to see a changelog."));
		}
	}
}
