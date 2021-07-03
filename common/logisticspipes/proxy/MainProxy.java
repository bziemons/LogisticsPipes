package logisticspipes.proxy;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.server.ServerWorld;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.SidedProvider;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.thread.EffectiveSide;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import com.google.common.collect.Maps;
import io.netty.channel.embedded.EmbeddedChannel;
import lombok.Getter;

import logisticspipes.LPConstants;
import logisticspipes.LPItems;
import logisticspipes.LogisticsEventListener;
import logisticspipes.LogisticsPipes;
import logisticspipes.entity.FakePlayerLP;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.PacketInboundHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.proxy.interfaces.IProxy;
import logisticspipes.proxy.side.ClientProxy;
import logisticspipes.proxy.side.ServerProxy;
import logisticspipes.routing.pathfinder.IPipeInformationProvider;
import logisticspipes.utils.PlayerCollectionList;

@Mod.EventBusSubscriber(modid = LPConstants.LP_MOD_ID)
public class MainProxy {

	private MainProxy() {}

	public static IProxy proxy = DistExecutor.runForDist(() -> ClientProxy::new, () -> ServerProxy::new);

	public static final ResourceLocation CHANNEL_RESOURCE =
			new ResourceLocation(LPConstants.LP_MOD_ID, MainProxy.networkChannelName);

	public static final String CHANNEL_VERSION = "1.0";

	@Getter
	private static int globalTick;
	public static EnumMap<Dist, EmbeddedChannel> channels;

	private static final Map<Dimension, FakePlayerLP> fakePlayers = Maps.newHashMap();

	public static final String networkChannelName = "LogisticsPipes";

	public static boolean isClient(@Nullable IWorld blockAccess) {
		if (blockAccess instanceof World) {
			World world = (World) blockAccess;
			try {
				return world.isRemote;
			} catch (NullPointerException n) {
				LogisticsPipes.getLOGGER().fatal("isClient called with a null world - using slow thread based fallback");
				n.printStackTrace();
			}
		}
		return EffectiveSide.get().isClient();
	}

	public static boolean isServer(@Nullable IWorld blockAccess) {
		if (blockAccess instanceof World) {
			World world = (World) blockAccess;
			try {
				return !world.isRemote;
			} catch (NullPointerException n) {
				LogisticsPipes.getLOGGER().fatal("isServer called with a null world - using slow thread based fallback");
				n.printStackTrace();
			}
		}
		return EffectiveSide.get().isServer();
	}

	/**
	 * Simple function to run code on the server and which can be replaced by the DistExecutor later.
	 */
	public static void runOnServer(@Nullable IWorld world, @Nonnull Supplier<Runnable> runnableConsumer) {
		DistExecutor.runWhenOn(Dist.DEDICATED_SERVER, runnableConsumer);
	}

	public static void runOnClient(@Nullable IBlockAccess world, @Nonnull Supplier<Runnable> runnableConsumer) {
		if (isClient(world)) runnableConsumer.get().run();
	}

	public static World getClientMainWorld() {
		return MainProxy.proxy.getWorld();
	}

	public static void createChannels() {
		// FIXME
//		MainProxy.channels = NetworkRegistry
//				.newSimpleChannel(CHANNEL_RESOURCE, () -> CHANNEL_VERSION, CHANNEL_VERSION::equals,
//						CHANNEL_VERSION::equals);
//		MainProxy.channels = NetworkRegistry.INSTANCE.newChannel(MainProxy.networkChannelName, new PacketHandler());
//		for (Side side : Side.values()) {
//			FMLEmbeddedChannel channel = MainProxy.channels.get(side);
//			String type = channel.findChannelHandlerNameForType(PacketHandler.class);
//			channel.pipeline().addAfter(type, PacketInboundHandler.class.getName(), new PacketInboundHandler());
//		}
	}

	public static void sendPacketToServer(ModernPacket packet) {
		if (EffectiveSide.get().isServer()) {
			System.err.println("sendPacketToServer called serverside !");
			new Exception().printStackTrace();
			return;
		}
		if (packet.isCompressable() || MainProxy.needsToBeCompressed(packet)) {
			SimpleServiceLocator.clientBufferHandler.addPacketToCompressor(packet);
		} else {
			// FIXME
//			MainProxy.channels.get(Dist.CLIENT).attr(FMLOutboundHandler.FML_MESSAGETARGET).set(OutboundTarget.TOSERVER);
//			MainProxy.channels.get(Dist.CLIENT).writeOutbound(packet);
		}
	}

	public static void sendPacketToPlayer(ModernPacket packet, PlayerEntity player) {
		if (!MainProxy.isServer(player.world)) {
			System.err.println("sendPacketToPlayer called clientside !");
			new Exception().printStackTrace();
			return;
		}
		if (packet.isCompressable() || MainProxy.needsToBeCompressed(packet)) {
			SimpleServiceLocator.serverBufferHandler.addPacketToCompressor(packet, player);
		} else {
			// FIXME
//			MainProxy.channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.PLAYER);
//			MainProxy.channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(player);
//			MainProxy.channels.get(Side.SERVER).writeOutbound(packet);
		}
	}

	// ignores dimension; more stringent check done inside sendPacketToAllWatching
	public static boolean isAnyoneWatching(BlockPos pos, int dimensionID) {
		return isAnyoneWatching(pos.getX(), pos.getZ(), dimensionID);
	}

	// ignores dimension; more stringent check done inside sendPacketToAllWatching
	public static boolean isAnyoneWatching(int X, int Z, int dimensionID) {
		ChunkPos chunk = new ChunkPos(X >> 4, Z >> 4);
		PlayerCollectionList players = LogisticsEventListener.watcherList.get(chunk);
		if (players == null) {
			return false;
		}
		return !players.isEmptyWithoutCheck();
	}

	// FIXME
//	public static void sendPacketToAllWatchingChunk(LogisticsModule module, ModernPacket packet) {
//		if (module.getSlot().isInWorld()) {
//			final World world = module.getWorld();
//			if (world == null) {
//				if (LogisticsPipes.isDEBUG()) {
//					throw new IllegalStateException("sendPacketToAllWatchingChunk called without a world provider on the module");
//				}
//				return;
//			}
//			final BlockPos pos = module.getBlockPos();
//			sendPacketToAllWatchingChunk(pos.getX(), pos.getZ(), world.getDimension(), packet);
//		} else {
//			if (LogisticsPipes.isDEBUG()) {
//				throw new IllegalStateException("sendPacketToAllWatchingChunk for module in hand was called");
//			}
//		}
//	}
//
//	public static void sendPacketToAllWatchingChunk(TileEntity tile, ModernPacket packet) {
//		sendPacketToAllWatchingChunk(tile.getPos().getX(), tile.getPos().getZ(), tile.getWorld().getDimension(), packet);
//	}

	// FIXME
//	public static void sendPacketToAllWatchingChunk(int X, int Z, int dimensionId, ModernPacket packet) {
//		if (!EffectiveSide.get().isServer()) {
//			System.err.println("sendPacketToAllWatchingChunk called clientside !");
//			new Exception().printStackTrace();
//			return;
//		}
//		ChunkPos chunk = new ChunkPos(X >> 4, Z >> 4);
//		PlayerCollectionList players = LogisticsEventListener.watcherList.get(chunk);
//		if (players != null) {
//			for (PlayerEntity player : players.players()) {
//				if (player.world.getDimension() == dimensionId) {
//					MainProxy.sendPacketToPlayer(packet, player);
//				}
//			}
//		}
//	}

	public static void sendToPlayerList(ModernPacket packet, PlayerCollectionList players) {
		if (players.isEmpty()) {
			return;
		}
		sendToPlayerList(packet, players.players());
	}

	public static void sendToPlayerList(ModernPacket packet, Iterable<PlayerEntity> players) {

		if (!EffectiveSide.get().isServer()) {
			System.err.println("sendToPlayerList called clientside !");
			new Exception().printStackTrace();
			return;
		}
		if (packet.isCompressable() || MainProxy.needsToBeCompressed(packet)) {
			for (PlayerEntity player : players) {
				SimpleServiceLocator.serverBufferHandler.addPacketToCompressor(packet, player);
			}
		} else {
			for (PlayerEntity player : players) {
				MainProxy.sendPacketToPlayer(packet, player);
			}
		}
	}

	public static void sendToPlayerList(ModernPacket packet, Stream<PlayerEntity> players) {
		if (!EffectiveSide.get().isServer()) {
			System.err.println("sendToPlayerList called clientside !");
			new Exception().printStackTrace();
			return;
		}
		if (packet.isCompressable() || MainProxy.needsToBeCompressed(packet)) {
			players.forEach(player -> SimpleServiceLocator.serverBufferHandler.addPacketToCompressor(packet, player));
		} else {
			players.forEach(player -> MainProxy.sendPacketToPlayer(packet, player));
		}
	}

	// FIXME
//	public static void sendToAllPlayers(ModernPacket packet) {
//		if (!EffectiveSide.get().isServer()) {
//			System.err.println("sendToAllPlayers called clientside !");
//			new Exception().printStackTrace();
//			return;
//		}
//		if (packet.isCompressable() || MainProxy.needsToBeCompressed(packet)) {
//			for (World world : DimensionManager.getWorlds()) {
//				for (Object playerObject : world.playerEntities) {
//					PlayerEntity player = (PlayerEntity) playerObject;
//					SimpleServiceLocator.serverBufferHandler.addPacketToCompressor(packet, player);
//				}
//			}
//		} else {
//			MainProxy.channels.get(Side.SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.ALL);
//			MainProxy.channels.get(Side.SERVER).writeOutbound(packet);
//		}
//	}

	private static boolean needsToBeCompressed(ModernPacket packet) {
		/*if(packet.getData() != null) {
			if(packet.getData().length > 32767) {
				return true; // Packet is to big
			}
		}*/
		return false;
	}

	public static FakePlayer getFakePlayer(World world) {
		Dimension dim = world.getDimension();
		if (fakePlayers.containsKey(dim))
			return fakePlayers.get(dim);
		if (world instanceof ServerWorld) {
			FakePlayerLP fp = new FakePlayerLP((ServerWorld) world);
			fakePlayers.put(dim, fp);
			return fp;
		}
		return null;
	}

	public static void addTick() {
		MainProxy.globalTick++;
	}

	public static ItemEntity dropItems(World world, @Nonnull ItemStack stack, int xCoord, int yCoord, int zCoord) {
		ItemEntity item = new ItemEntity(world, xCoord, yCoord, zCoord, stack);
		world.spawnEntity(item);
		return item;
	}

	public static boolean checkPipesConnections(TileEntity from, TileEntity to, Direction way) {
		return MainProxy.checkPipesConnections(from, to, way, false);
	}

	public static boolean checkPipesConnections(TileEntity from, TileEntity to, Direction way, boolean ignoreSystemDisconnection) {
		if (from == null || to == null) {
			return false;
		}
		IPipeInformationProvider fromInfo = SimpleServiceLocator.pipeInformationManager.getInformationProviderFor(from);
		IPipeInformationProvider toInfo = SimpleServiceLocator.pipeInformationManager.getInformationProviderFor(to);
		if (fromInfo == null && toInfo == null) {
			return false;
		}
		if (fromInfo != null) {
			if (!fromInfo.canConnect(to, way, ignoreSystemDisconnection)) {
				return false;
			}
		}
		if (toInfo != null) {
			return toInfo.canConnect(from, way.getOpposite(), ignoreSystemDisconnection);
		}
		return true;
	}

	public static boolean isPipeControllerEquipped(PlayerEntity player) {
		return player != null &&
				!player.getItemStackFromSlot(EquipmentSlotType.MAINHAND).isEmpty() &&
				player.getItemStackFromSlot(EquipmentSlotType.MAINHAND).getItem() == LPItems.pipeController;
	}

	@SubscribeEvent
	public static void onWorldUnload(WorldEvent.Unload event) {
		fakePlayers.entrySet().removeIf(entry -> entry.getValue().world == event.getWorld());
	}

	@Nullable
	public static Dimension getDimension(PlayerEntity player, int dimension) {
		final MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
		Dimension dim = null;
		for (final ServerWorld w : server.getWorlds()) {
			if (w.dimension.getType().getId() == dimension) {
				dim = w.dimension;
				break;
			}
		}
		return dim;
	}

}
