package logisticspipes.commands.commands.debug;

import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.StringTextComponent;

import logisticspipes.commands.abstracts.ICommandHandler;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.debuggui.DebugAskForTarget;
import logisticspipes.proxy.MainProxy;

public class TargetCommand implements ICommandHandler {

	@Override
	public String[] getNames() {
		return new String[] { "target", "look", "watch" };
	}

	@Override
	public boolean isCommandUsableBy(ICommandSource sender) {
		return sender instanceof PlayerEntity;
	}

	@Override
	public String[] getDescription() {
		return new String[] { "Starts debugging the TileEntity", "or Entitiy you are currently looking at." };
	}

	@Override
	public void executeCommand(ICommandSource sender, String[] args) {
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(DebugAskForTarget.class), (PlayerEntity) sender);
		sender.sendMessage(new StringTextComponent("Asking for Target."));
	}
}
