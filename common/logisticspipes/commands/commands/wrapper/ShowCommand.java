package logisticspipes.commands.commands.wrapper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.command.ICommandSource;
import net.minecraft.util.text.StringTextComponent;

import logisticspipes.asm.wrapper.AbstractWrapper;
import logisticspipes.asm.wrapper.LogisticsWrapperHandler;
import logisticspipes.asm.wrapper.WrapperState;
import logisticspipes.commands.LogisticsPipesCommand;
import logisticspipes.commands.abstracts.ICommandHandler;

public class ShowCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "show" };
	}

	@Override
	public boolean isCommandUsableBy(ICommandSource sender) {
		return LogisticsPipesCommand.isOP(sender);
	}

	@Override
	public String[] getDescription() {
		return new String[] { "Reenables the given proxy", "if it was disabled by an exception." };
	}

	@Override
	public void executeCommand(ICommandSource sender, String[] args) {
		if (args.length != 1) {
			sender.sendMessage(new StringTextComponent("Wrong amount of arguments"));
			return;
		}
		String name = args[0];
		List<AbstractWrapper> list = new ArrayList<>(LogisticsWrapperHandler.wrapperController);
		Iterator<AbstractWrapper> iter = list.iterator();
		while (iter.hasNext()) {
			AbstractWrapper item = iter.next();
			if (item.getState() != WrapperState.Exception) {
				iter.remove();
			}
		}
		iter = list.iterator();
		while (iter.hasNext()) {
			AbstractWrapper item = iter.next();
			if (!(item.getName() + item.getTypeName()).startsWith(name)) {
				iter.remove();
			}
		}
		if (list.size() > 1) {
			sender.sendMessage(new StringTextComponent("Possible: "));
			for (AbstractWrapper can : list) {
				sender.sendMessage(new StringTextComponent(can.getName() + can.getTypeName()));
			}
		} else if (list.isEmpty()) {
			sender.sendMessage(new StringTextComponent("No match found"));
		} else {
			AbstractWrapper wrapper = list.get(0);
			Throwable t = wrapper.getReason();
			if (t == null) {
				sender.sendMessage(new StringTextComponent("null"));
				return;
			} else {
				sender.sendMessage(new StringTextComponent("-----------------------------------------------------"));
				sender.sendMessage(new StringTextComponent(t.toString()));
				for (StackTraceElement s : t.getStackTrace()) {
					sender.sendMessage(new StringTextComponent(" " + s.toString()));
				}
				t.printStackTrace();
			}
		}
	}
}
