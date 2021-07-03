package logisticspipes;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import net.minecraft.block.BlockState;
import net.minecraft.client.gui.screen.inventory.ChestScreen;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.ChestTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkWatchEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import logisticspipes.config.Configs;
import logisticspipes.interfaces.IItemAdvancedExistance;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.PlayerConfigToClientPacket;
import logisticspipes.network.packets.chassis.ChestGuiClosed;
import logisticspipes.network.packets.chassis.ChestGuiOpened;
import logisticspipes.network.packets.gui.GuiReopenPacket;
import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.renderer.LogisticsGuiOverrenderer;
import logisticspipes.renderer.LogisticsHUDRenderer;
import logisticspipes.routing.ItemRoutingInformation;
import logisticspipes.routing.pathfinder.changedetection.TEControl;
import logisticspipes.ticks.LPTickHandler;
import logisticspipes.ticks.VersionChecker;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.PlayerIdentifier;
import logisticspipes.utils.QuickSortChestMarkerStorage;
import logisticspipes.utils.string.ChatColor;
import network.rs485.logisticspipes.config.ClientConfiguration;
import network.rs485.logisticspipes.config.PlayerConfiguration;
import network.rs485.logisticspipes.connection.NeighborTileEntity;
import network.rs485.logisticspipes.module.AsyncQuicksortModule;
import network.rs485.logisticspipes.util.TextUtil;
import network.rs485.logisticspipes.world.WorldCoordinatesWrapper;

@Mod.EventBusSubscriber(modid = LPConstants.LP_MOD_ID)
public class LogisticsEventListener {

	public static final WeakHashMap<PlayerEntity, List<WeakReference<AsyncQuicksortModule>>> chestQuickSortConnection = new WeakHashMap<>();
	public static Map<ChunkPos, PlayerCollectionList> watcherList = new ConcurrentHashMap<>();

	@SubscribeEvent
	public void onEntitySpawn(EntityJoinWorldEvent event) {
		if (event != null && event.getEntity() instanceof ItemEntity && event.getEntity().world != null && !event.getEntity().world.isRemote) {
			ItemStack stack = ((ItemEntity) event.getEntity()).getItem(); //Get ItemStack
			if (!stack.isEmpty() && stack.getItem() instanceof IItemAdvancedExistance && !((IItemAdvancedExistance) stack.getItem()).canExistInWorld(stack)) {
				event.setCanceled(true);
			}
			if (stack.hasTag()) {
				for (String key : Objects.requireNonNull(stack.getTag(), "nbt for stack must be non-null").keySet()) {
					if (key.startsWith("logisticspipes:routingdata")) {
						ItemRoutingInformation info = ItemRoutingInformation.restoreFromNBT(stack.getTag().getCompound(key));
						info.setItemTimedout();
						((ItemEntity) event.getEntity()).setItem(info.getItem().getItem().makeNormalStack(stack.getCount()));
						break;
					}
				}
			}
		}
	}

	@SubscribeEvent
	public void onPlayerLeftClickBlock(final PlayerInteractEvent.LeftClickBlock event) {
		if (MainProxy.isServer(event.getPlayer().world)) {
			final TileEntity tile = event.getPlayer().world.getTileEntity(event.getPos());
			if (tile instanceof LogisticsTileGenericPipe) {
				if (((LogisticsTileGenericPipe) tile).pipe instanceof CoreRoutedPipe) {
					if (!((CoreRoutedPipe) ((LogisticsTileGenericPipe) tile).pipe).canBeDestroyedByPlayer(event.getPlayer())) {
						event.setCanceled(true);
						event.getPlayer().sendMessage(new TranslationTextComponent("lp.chat.permissiondenied"));
						((LogisticsTileGenericPipe) tile).scheduleNeighborChange();
						World world = event.getPlayer().world;
						BlockPos pos = tile.getPos();
						BlockState state = world.getBlockState(pos);
						world.markAndNotifyBlock(tile.getPos(), world.getChunkAt(pos), state, state, 2);
						((CoreRoutedPipe) ((LogisticsTileGenericPipe) tile).pipe).delayTo = System.currentTimeMillis() + 200;
						((CoreRoutedPipe) ((LogisticsTileGenericPipe) tile).pipe).repeatFor = 10;
					} else {
						((CoreRoutedPipe) ((LogisticsTileGenericPipe) tile).pipe).setDestroyByPlayer();
					}
				}
			}
		}
	}

	@SubscribeEvent
	public void onPlayerLeftClickBlock(final PlayerInteractEvent.RightClickBlock event) {
		if (MainProxy.isServer(event.getPlayer().world)) {
			WorldCoordinatesWrapper worldCoordinates = new WorldCoordinatesWrapper(event.getPlayer().world, event.getPos());
			TileEntity tileEntity = worldCoordinates.getTileEntity();
			if (tileEntity instanceof ChestTileEntity || SimpleServiceLocator.ironChestProxy.isIronChest(tileEntity)) {
				List<WeakReference<AsyncQuicksortModule>> list = worldCoordinates.allNeighborTileEntities().stream()
						.filter(NeighborTileEntity::isLogisticsPipe)
						.filter(adjacent -> ((LogisticsTileGenericPipe) adjacent.getTileEntity()).pipe instanceof PipeLogisticsChassis)
						.filter(adjacent -> ((PipeLogisticsChassis) ((LogisticsTileGenericPipe) adjacent.getTileEntity()).pipe).getPointedDirection()
								== adjacent.getOurDirection())
						.map(adjacent -> (PipeLogisticsChassis) ((LogisticsTileGenericPipe) adjacent.getTileEntity()).pipe)
						.flatMap(chassis -> chassis.getModules().getModules())
						.filter(logisticsModule -> logisticsModule instanceof AsyncQuicksortModule)
						.map(logisticsModule -> new WeakReference<>((AsyncQuicksortModule) logisticsModule))
						.collect(Collectors.toList());

				if (!list.isEmpty()) {
					LogisticsEventListener.chestQuickSortConnection.put(event.getPlayer(), list);
				}
			}
		}
	}

	public static HashMap<Integer, Long> WorldLoadTime = new HashMap<>();

	@SubscribeEvent
	public void WorldLoad(WorldEvent.Load event) {
		if (MainProxy.isServer(event.getWorld())) {
			int dim = event.getWorld().getDimension().getType().getId();
			if (!LogisticsEventListener.WorldLoadTime.containsKey(dim)) {
				LogisticsEventListener.WorldLoadTime.put(dim, System.currentTimeMillis());
			}
		}
		if (MainProxy.isClient(event.getWorld())) {
			SimpleServiceLocator.routerManager.clearClientRouters();
			LogisticsHUDRenderer.instance().clear();
		}
	}

	@SubscribeEvent
	public void WorldUnload(WorldEvent.Unload event) {
		if (MainProxy.isServer(event.getWorld())) {
			int dim = event.getWorld().getDimension().getType().getId();
			SimpleServiceLocator.routerManager.dimensionUnloaded(dim);
		}
	}

	@SubscribeEvent
	public void watchChunk(ChunkWatchEvent.Watch event) {
		ChunkPos pos = event.getPos();
		if (!LogisticsEventListener.watcherList.containsKey(pos)) {
			LogisticsEventListener.watcherList.put(pos, new PlayerCollectionList());
		}
		LogisticsEventListener.watcherList.get(pos).add(event.getPlayer());
	}

	@SubscribeEvent
	public void unWatchChunk(ChunkWatchEvent.UnWatch event) {
		ChunkPos pos = event.getPos();
		if (LogisticsEventListener.watcherList.containsKey(pos)) {
			LogisticsEventListener.watcherList.get(pos).remove(event.getPlayer());
		}
	}

	@SubscribeEvent
	public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		DistExecutor.runForDist(() -> () -> {
			SimpleServiceLocator.clientBufferHandler.clear();

			if (Configs.CHECK_FOR_UPDATES) {
				LogisticsPipes.singleThreadExecutor.execute(() -> {
					VersionChecker checker = LogisticsPipes.versionChecker;

					// send player message
					String versionMessage = checker.getVersionCheckerStatus();

					if (checker.isVersionCheckDone() && checker.getVersionInfo().isNewVersionAvailable() && !checker.getVersionInfo().isImcMessageSent()) {
						event.getPlayer().sendMessage(new StringTextComponent(versionMessage));
						event.getPlayer().sendMessage(new StringTextComponent("Use \"/logisticspipes changelog\" to see a changelog."));
					} else if (!checker.isVersionCheckDone()) {
						event.getPlayer().sendMessage(new StringTextComponent(versionMessage));
					}
				});
			}
			return null;
		}, () -> () -> {
			SimpleServiceLocator.securityStationManager.sendClientAuthorizationList(event.getPlayer());

			SimpleServiceLocator.serverBufferHandler.clear(event.getPlayer());
			ClientConfiguration config = LogisticsPipes.getServerConfigManager().getPlayerConfiguration(PlayerIdentifier.get(event.getPlayer()));
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(PlayerConfigToClientPacket.class).setConfig(config), event.getPlayer());
			return null;
		});
	}

	@SubscribeEvent
	public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		SimpleServiceLocator.serverBufferHandler.clear(event.getPlayer());
	}

	@AllArgsConstructor
	private static class GuiEntry {

		@Getter
		private final int xCoord;
		@Getter
		private final int yCoord;
		@Getter
		private final int zCoord;
		@Getter
		private final int guiID;
		@Getter
		@Setter
		private boolean isActive;
	}

	@Getter(lazy = true)
	private static final Queue<GuiEntry> guiPos = new LinkedList<>();

	//Handle GuiRepoen
	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public void onGuiOpen(GuiOpenEvent event) {
		if (!LogisticsEventListener.getGuiPos().isEmpty()) {
			if (event.getGui() == null) {
				GuiEntry part = LogisticsEventListener.getGuiPos().peek();
				if (part.isActive()) {
					part = LogisticsEventListener.getGuiPos().poll();
					MainProxy.sendPacketToServer(PacketHandler.getPacket(GuiReopenPacket.class).setGuiID(part.getGuiID()).setPosX(part.getXCoord()).setPosY(part.getYCoord()).setPosZ(part.getZCoord()));
					LogisticsGuiOverrenderer.getInstance().setOverlaySlotActive(false);
				}
			} else {
				GuiEntry part = LogisticsEventListener.getGuiPos().peek();
				part.setActive(true);
			}
		}
		if (event.getGui() == null) {
			LogisticsGuiOverrenderer.getInstance().setOverlaySlotActive(false);
		}
		if (event.getGui() instanceof ChestScreen || (SimpleServiceLocator.ironChestProxy != null && SimpleServiceLocator.ironChestProxy.isChestGui(event.getGui()))) {
			MainProxy.sendPacketToServer(PacketHandler.getPacket(ChestGuiOpened.class));
		} else {
			QuickSortChestMarkerStorage.getInstance().disable();
			MainProxy.sendPacketToServer(PacketHandler.getPacket(ChestGuiClosed.class));
		}
	}

	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public static void addGuiToReopen(int xCoord, int yCoord, int zCoord, int guiID) {
		LogisticsEventListener.getGuiPos().add(new GuiEntry(xCoord, yCoord, zCoord, guiID, false));
	}

	@SubscribeEvent
	public void onBlockUpdate(BlockEvent.NeighborNotifyEvent event) {
		TEControl.handleBlockUpdate(event.getWorld(), LPTickHandler.getWorldInfo(event.getWorld()), event.getPos());
	}

	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public void onItemStackToolTip(ItemTooltipEvent event) {
		if (event.getItemStack().hasTag()) {
			for (String key : event.getItemStack().getTag().keySet()) {
				if (key.startsWith("logisticspipes:routingdata")) {
					ItemRoutingInformation info = ItemRoutingInformation.restoreFromNBT(event.getItemStack().getTag().getCompound(key));
					List<ITextComponent> list = event.getToolTip();
					list.set(0, new StringTextComponent(ChatColor.RED + "!!! " + ChatColor.WHITE + list.get(0) + ChatColor.RED + " !!!" + ChatColor.WHITE));
					list.add(1, new TranslationTextComponent("itemstackinfo.lprouteditem"));
					list.add(2, new TranslationTextComponent("itemstackinfo.lproutediteminfo"));
					list.add(3, new StringTextComponent(TextUtil.translate("itemstackinfo.lprouteditemtype") + ": " + info.getItem().toString()));
				}
			}
		}
	}

	@SubscribeEvent
	public void onItemCrafting(PlayerEvent.ItemCraftedEvent event) {
		if (event.getPlayer().isServerWorld() && !event.getCrafting().isEmpty()) {
			if (event.getCrafting().getItem().getRegistryName().getNamespace().equals(LPConstants.LP_MOD_ID)) {
				PlayerIdentifier identifier = PlayerIdentifier.get(event.getPlayer());
				PlayerConfiguration config = LogisticsPipes.getServerConfigManager().getPlayerConfiguration(identifier);
				if (!config.getHasCraftedLPItem() && !LogisticsPipes.isDEBUG()) {
					ItemStack book = new ItemStack(LPItems.itemGuideBook, 1);
					event.getPlayer().addItemStackToInventory(book);

					config.setHasCraftedLPItem(true);
					LogisticsPipes.getServerConfigManager().setPlayerConfiguration(identifier, config);
				}
			}
		}
	}
}
