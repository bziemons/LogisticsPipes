package logisticspipes.network.packets;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.entity.player.PlayerEntity;

import net.minecraftforge.common.DimensionManager;

import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class PlayerListRequest extends ModernPacket {

	public PlayerListRequest(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new PlayerListRequest(getId());
	}

	@Override
	public void processPacket(PlayerEntity player) {
		Stream<?> allPlayers = Arrays.stream(DimensionManager.getWorlds()).map(worldServer -> worldServer.playerEntities).flatMap(Collection::stream);
		Stream<PlayerEntity> allPlayerEntities = allPlayers.filter(o -> o instanceof PlayerEntity).map(o -> (PlayerEntity) o);
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(PlayerList.class)
				.setStringList(allPlayerEntities.map(PlayerEntity -> PlayerEntity.getGameProfile().getName()).collect(Collectors.toList())), player);
	}

	@Override
	public void readData(LPDataInput input) {}

	@Override
	public void writeData(LPDataOutput output) {}
}
