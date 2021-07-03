package logisticspipes.ticks;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.world.IWorld;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

import com.google.common.collect.MapMaker;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import logisticspipes.LPConstants;
import logisticspipes.commands.commands.debug.DebugGuiController;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.utils.FluidIdentifier;
import network.rs485.grow.ServerTickDispatcher;
import network.rs485.logisticspipes.world.DoubleCoordinates;

@Mod.EventBusSubscriber(modid = LPConstants.LP_MOD_ID)
public class LPTickHandler {

	public static int adjChecksDone = 0;

	@SubscribeEvent
	public void clientTick(TickEvent.ClientTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			FluidIdentifier.initFromForge(true);
			SimpleServiceLocator.clientBufferHandler.clientTick();
			MainProxy.proxy.tickClient();
			DebugGuiController.instance().execClient();
		}
	}

	@SubscribeEvent
	public void serverTick(TickEvent.ServerTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			HudUpdateTick.tick();
			SimpleServiceLocator.serverBufferHandler.serverTick();
			MainProxy.proxy.tickServer();
			LPTickHandler.adjChecksDone = 0;
			DebugGuiController.instance().execServer();
			ServerTickDispatcher.INSTANCE.tick();
		}
	}

	private static Map<IWorld, LPWorldInfo> worldInfo = new MapMaker().weakKeys().makeMap();

	@SubscribeEvent
	public void worldTick(TickEvent.WorldTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}
		if (event.side.isServer()) {
			LPWorldInfo info = LPTickHandler.getWorldInfo(event.world);
			info.worldTick++;
		}
	}

	public static LPWorldInfo getWorldInfo(IWorld world) {
		LPWorldInfo info = LPTickHandler.worldInfo.get(world);
		if (info == null) {
			info = new LPWorldInfo();
			LPTickHandler.worldInfo.put(world, info);
		}
		return info;
	}

	@Data
	public static class LPWorldInfo {

		@Getter
		@Setter(value = AccessLevel.PRIVATE)
		private long worldTick = 0;
		@Getter
		private Set<DoubleCoordinates> updateQueued = new HashSet<>();

		@Getter
		@Setter
		private boolean skipBlockUpdateForWorld = false;
	}
}
