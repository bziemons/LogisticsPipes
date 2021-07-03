package logisticspipes.entity;

import java.net.SocketAddress;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.crypto.SecretKey;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.INetHandler;
import net.minecraft.network.IPacket;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketDirection;
import net.minecraft.network.ProtocolType;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

public class FakeNetServerHandler extends ServerPlayNetHandler {

	public static class NetworkManagerFake extends NetworkManager {

		public NetworkManagerFake() {
			super(PacketDirection.CLIENTBOUND);
		}

		@Override
		public void tick() {}

		@Override
		public void closeChannel(ITextComponent message) {}

		@Override
		public boolean isEncrypted() { return false; }

		@Override
		public boolean hasNoChannel() { return true; }

		@Override
		public void handleDisconnection() {
			super.handleDisconnection();
		}

		@Override
		public float getPacketsSent() {
			return super.getPacketsSent();
		}

		@Override
		public PacketDirection getDirection() {
			return super.getDirection();
		}

		@Override
		public void channelActive(ChannelHandlerContext p_channelActive_1_) { }

		@Override
		public void setConnectionState(ProtocolType newState) { }

		@Override
		public void channelInactive(ChannelHandlerContext p_channelInactive_1_) { }

		@Override
		public void exceptionCaught(ChannelHandlerContext p_exceptionCaught_1_, @Nonnull Throwable p_exceptionCaught_2_) { }

		@Override
		public void setNetHandler(INetHandler handler) { }

		@Override
		public void sendPacket(IPacket<?> packetIn) { }

		@Override
		public void sendPacket(IPacket<?> packetIn,
				@Nullable GenericFutureListener<? extends Future<? super Void>> p_201058_2_) { }

		@Override
		public float getPacketsReceived() { return 0f; }

		@Nonnull
		@Override
		public SocketAddress getRemoteAddress() {
			return null;
		}

		@Override
		public boolean isLocalChannel() {
			return false;
		}

		@Override
		public void enableEncryption(SecretKey key) { }

		@Override
		public boolean isChannelOpen() {
			return false;
		}

		@Nonnull
		@Override
		public INetHandler getNetHandler() {
			return null;
		}

		@Nonnull
		@Override
		public ITextComponent getExitMessage() {
			return null;
		}

		@Override
		public void setCompressionThreshold(int threshold) { }

		@Override
		public void disableAutoRead() { }

		@Nonnull
		@Override
		public Channel channel() {
			return null;
		}

	}

	public FakeNetServerHandler(MinecraftServer server, ServerPlayerEntity playerIn) {
		super(server, new NetworkManagerFake(), playerIn);
	}

	@Override
	public void disconnect(@Nonnull final ITextComponent textComponent) { }

	@Override
	public void setPlayerLocation(double x, double y, double z, float yaw, float pitch) { }

	@Override
	public void onDisconnect(ITextComponent reason) { }

}
