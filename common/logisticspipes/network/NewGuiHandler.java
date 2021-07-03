package logisticspipes.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import logisticspipes.LogisticsPipes;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.network.abstractguis.PopupGuiProvider;
import logisticspipes.network.exception.TargetNotFoundException;
import logisticspipes.network.packets.gui.OpenGUIPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.gui.SubGuiScreen;
import network.rs485.logisticspipes.util.LPDataIOWrapper;

public class NewGuiHandler {

	public static List<GuiProvider> guilist;
	public static Map<Class<? extends GuiProvider>, GuiProvider> guimap;

	private NewGuiHandler() { }

	@SuppressWarnings("unchecked") // Suppressed because this cast should never fail.
	public static <T extends GuiProvider> T getGui(Class<T> clazz) {
		return (T) NewGuiHandler.guimap.get(clazz).template();
	}

	public static void initialize() {
		// FIXME: cannot find GUIs with static resolver anymore
		Set<Class<? extends GuiProvider>> classes = Collections.emptySet();

		loadGuiProviders(classes);

		if (NewGuiHandler.guilist == null || NewGuiHandler.guilist.isEmpty()) {
			throw new RuntimeException("Cannot load GuiProvider Classes");
		}
	}

	private static void loadGuiProviders(Set<Class<? extends GuiProvider>> classesIn) {
		List<Class<? extends GuiProvider>> classes = classesIn.stream()
				.sorted(Comparator.comparing(Class::getCanonicalName))
				.collect(Collectors.toList());

		NewGuiHandler.guilist = new ArrayList<>(classes.size());
		NewGuiHandler.guimap = new HashMap<>(classes.size());

		int currentId = 0;
		for (Class<? extends GuiProvider> cls : classes) {
			try {
				final GuiProvider instance = (GuiProvider) cls.getConstructors()[0].newInstance(currentId);
				NewGuiHandler.guilist.add(instance);
				NewGuiHandler.guimap.put(cls, instance);
				currentId++;
			} catch (Throwable ignoredButPrinted) {
				ignoredButPrinted.printStackTrace();
			}
		}
	}

	public static void openGui(GuiProvider guiProvider, PlayerEntity oPlayer) {
		if (!(oPlayer instanceof ServerPlayerEntity)) {
			throw new UnsupportedOperationException("Gui can only be opened on the server side");
		}
		ServerPlayerEntity player = (ServerPlayerEntity) oPlayer;
		Container container = guiProvider.getContainer(player);
		if (container == null) {
			if (guiProvider instanceof PopupGuiProvider) {
				OpenGUIPacket packet = PacketHandler.getPacket(OpenGUIPacket.class);
				packet.setGuiID(guiProvider.getId());
				packet.setWindowID(-2);
				packet.setGuiData(LPDataIOWrapper.collectData(guiProvider::writeData));
				MainProxy.sendPacketToPlayer(packet, player);
			}
			return;
		}
		player.getNextWindowId();
		player.closeContainer();
		int windowId = player.currentWindowId;

		OpenGUIPacket packet = PacketHandler.getPacket(OpenGUIPacket.class);
		packet.setGuiID(guiProvider.getId());
		packet.setWindowID(windowId);
		packet.setGuiData(LPDataIOWrapper.collectData(guiProvider::writeData));
		MainProxy.sendPacketToPlayer(packet, player);

		player.openContainer = container;
		// FIXME: windowId cannot be reassigned on Container
		//player.openContainer.windowId = windowId;
		player.openContainer.addListener(player);
		net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.player.PlayerContainerEvent.Open(player, player.openContainer));
	}

	@OnlyIn(Dist.CLIENT)
	public static void openGui(OpenGUIPacket packet, PlayerEntity player) {
		int guiID = packet.getGuiID();
		GuiProvider provider = NewGuiHandler.guilist.get(guiID).template();
		LPDataIOWrapper.provideData(packet.getGuiData(), provider::readData);

		if (provider instanceof PopupGuiProvider && packet.getWindowID() == -2) {
			if (Minecraft.getInstance().currentScreen instanceof LogisticsBaseGuiScreen) {
				LogisticsBaseGuiScreen<?> baseGUI = (LogisticsBaseGuiScreen<?>) Minecraft.getInstance().currentScreen;
				SubGuiScreen newSub;
				try {
					newSub = (SubGuiScreen) provider.getClientGui(player);
				} catch (TargetNotFoundException e) {
					throw e;
				} catch (Exception e) {
					LogisticsPipes.getLOGGER().error(packet.getClass().getName());
					LogisticsPipes.getLOGGER().error(packet.toString());
					throw new RuntimeException(e);
				}
				if (newSub != null) {
					if (!baseGUI.hasSubGui()) {
						baseGUI.setSubGui(newSub);
					} else {
						SubGuiScreen canidate = baseGUI.getSubGui();
						while (canidate.hasSubGui()) {
							canidate = canidate.getSubGui();
						}
						canidate.setSubGui(newSub);
					}
				}
			}
		} else {
			ContainerScreen<?> screen;
			try {
				screen = (ContainerScreen<?>) provider.getClientGui(player);
			} catch (TargetNotFoundException e) {
				throw e;
			} catch (Exception e) {
				LogisticsPipes.getLOGGER().error(packet.getClass().getName());
				LogisticsPipes.getLOGGER().error(packet.toString());
				throw new RuntimeException(e);
			}
			// FIXME: windowId cannot be reassigned
//			screen.getContainer().windowId = packet.getWindowID();
			Minecraft.getInstance().displayGuiScreen(screen);
		}
	}
}
