package logisticspipes.pipes.basic.debug;

import java.util.ArrayList;
import java.util.List;

import network.rs485.logisticspipes.network.LPChannel;

import logisticspipes.network.packets.debug.SendNewLogLine;
import logisticspipes.network.packets.debug.SendNewLogWindow;
import logisticspipes.network.packets.debug.UpdateStatusEntries;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.utils.PlayerCollectionList;

import net.minecraft.entity.player.EntityPlayer;

public class DebugLogController {

	private static int nextID = 0;
	private final int ID = DebugLogController.nextID++;
	public final CoreUnroutedPipe pipe;
	public boolean debugThisPipe = false;
	private List<StatusEntry> oldList = new ArrayList<>();
	private PlayerCollectionList players = new PlayerCollectionList();

	public DebugLogController(CoreUnroutedPipe pipe) {
		this.pipe = pipe;
	}

	public void log(String info) {
		if (players.isEmptyWithoutCheck()) {
			return;
		}
		LPChannel.sendToPlayerList(PacketHandler.getPacket(SendNewLogLine.class).setWindowID(ID).setLine(info), players);
	}

	public void tick() {
		if (players.isEmpty()) {
			return;
		}
		generateStatus();
	}

	public void generateStatus() {
		List<StatusEntry> status = new ArrayList<>();
		pipe.addStatusInformation(status);
		if (!status.equals(oldList)) {
			LPChannel.sendToPlayerList(PacketHandler.getPacket(UpdateStatusEntries.class).setWindowID(ID).setStatus(status), players);
			oldList = status;
		}
	}

	public void openForPlayer(EntityPlayer player) {
		players.add(player);
		List<StatusEntry> status = new ArrayList<>();
		pipe.addStatusInformation(status);
		LPChannel.sendPacketToPlayer(PacketHandler.getPacket(SendNewLogWindow.class).setWindowID(ID).setTitle(pipe.toString()), player);
		LPChannel.sendPacketToPlayer(PacketHandler.getPacket(UpdateStatusEntries.class).setWindowID(ID).setStatus(status), player);
	}
}
