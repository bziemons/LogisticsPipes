package logisticspipes.commands.commands.debug;

import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.StringTextComponent;

import logisticspipes.commands.abstracts.ICommandHandler;

public class HandCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "hand" };
	}

	@Override
	public boolean isCommandUsableBy(ICommandSource sender) {
		return sender instanceof PlayerEntity;
	}

	@Override
	public String[] getDescription() {
		return new String[] { "Start debugging the selected ItemStack" };
	}

	@Override
	public void executeCommand(ICommandSource sender, String[] args) {
		PlayerEntity player = (PlayerEntity) sender;
		ItemStack item = player.inventory.mainInventory.get(player.inventory.currentItem);
		if (!item.isEmpty()) {
			DebugGuiController.instance().startWatchingOf(item, player);
			sender.sendMessage(new StringTextComponent("Starting HandDebuging"));
		}
	}
}
