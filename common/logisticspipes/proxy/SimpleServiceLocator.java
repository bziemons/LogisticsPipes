/*
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.proxy;

import logisticspipes.interfaces.ISecurityStationManager;
import logisticspipes.interfaces.routing.IChannelConnectionManager;
import logisticspipes.interfaces.routing.IChannelManagerProvider;
import logisticspipes.logistics.ILogisticsFluidManager;
import logisticspipes.logistics.ILogisticsManager;
import logisticspipes.proxy.interfaces.ICCLProxy;
import logisticspipes.proxy.interfaces.IPowerProxy;
import logisticspipes.proxy.progressprovider.MachineProgressProvider;
import logisticspipes.proxy.specialconnection.SpecialPipeConnection;
import logisticspipes.proxy.specialconnection.SpecialTileConnection;
import logisticspipes.proxy.specialtankhandler.SpecialTankHandler;
import logisticspipes.renderer.newpipe.GLRenderListHandler;
import logisticspipes.routing.RouterManager;
import logisticspipes.routing.pathfinder.PipeInformationManager;
import logisticspipes.ticks.ClientPacketBufferHandlerThread;
import logisticspipes.ticks.ServerPacketBufferHandlerThread;
import logisticspipes.utils.InventoryUtilFactory;
import logisticspipes.utils.RoutedItemHelper;
import logisticspipes.utils.TankUtilFactory;

public final class SimpleServiceLocator {

	private SimpleServiceLocator() {}

	public static IChannelConnectionManager connectionManager;

	public static void setChannelConnectionManager(final IChannelConnectionManager conMngr) {
		SimpleServiceLocator.connectionManager = conMngr;
	}

	public static ISecurityStationManager securityStationManager;

	public static void setSecurityStationManager(final ISecurityStationManager secStationMngr) {
		SimpleServiceLocator.securityStationManager = secStationMngr;
	}

	public static RouterManager routerManager;

	public static void setRouterManager(final RouterManager routerMngr) {
		SimpleServiceLocator.routerManager = routerMngr;
	}

	public static ILogisticsManager logisticsManager;

	public static void setLogisticsManager(final ILogisticsManager logisticsMngr) {
		SimpleServiceLocator.logisticsManager = logisticsMngr;
	}

	public static ILogisticsFluidManager logisticsFluidManager;

	public static void setLogisticsFluidManager(final ILogisticsFluidManager logisticsMngr) {
		SimpleServiceLocator.logisticsFluidManager = logisticsMngr;
	}

	public static InventoryUtilFactory inventoryUtilFactory;

	public static void setInventoryUtilFactory(final InventoryUtilFactory invUtilFactory) {
		SimpleServiceLocator.inventoryUtilFactory = invUtilFactory;
	}

	public static TankUtilFactory tankUtilFactory;

	public static void setTankUtilFactory(final TankUtilFactory tankUtilFactory) {
		SimpleServiceLocator.tankUtilFactory = tankUtilFactory;
	}

	public static SpecialPipeConnection specialpipeconnection;

	public static void setSpecialConnectionHandler(final SpecialPipeConnection special) {
		SimpleServiceLocator.specialpipeconnection = special;
	}

	public static SpecialTileConnection specialtileconnection;

	public static void setSpecialConnectionHandler(final SpecialTileConnection special) {
		SimpleServiceLocator.specialtileconnection = special;
	}

	public static SpecialTankHandler specialTankHandler;

	public static void setSpecialTankHandler(SpecialTankHandler proxy) {
		SimpleServiceLocator.specialTankHandler = proxy;
	}

	public static ClientPacketBufferHandlerThread clientBufferHandler;

	public static void setClientPacketBufferHandlerThread(ClientPacketBufferHandlerThread proxy) {
		SimpleServiceLocator.clientBufferHandler = proxy;
	}

	public static ServerPacketBufferHandlerThread serverBufferHandler;

	public static void setServerPacketBufferHandlerThread(ServerPacketBufferHandlerThread proxy) {
		SimpleServiceLocator.serverBufferHandler = proxy;
	}

	public static PipeInformationManager pipeInformationManager;

	public static void setPipeInformationManager(PipeInformationManager manager) {
		SimpleServiceLocator.pipeInformationManager = manager;
	}

	public static MachineProgressProvider machineProgressProvider;

	public static void setMachineProgressProvider(MachineProgressProvider provider) {
		SimpleServiceLocator.machineProgressProvider = provider;
	}

	public static RoutedItemHelper routedItemHelper;

	public static void setRoutedItemHelper(RoutedItemHelper helper) {
		SimpleServiceLocator.routedItemHelper = helper;
	}

	public static GLRenderListHandler renderListHandler;

	public static void setRenderListHandler(GLRenderListHandler handler) {
		SimpleServiceLocator.renderListHandler = handler;
	}

	public static IPowerProxy powerProxy;

	public static void setPowerProxy(IPowerProxy proxy) {
		SimpleServiceLocator.powerProxy = proxy;
	}

	public static ICCLProxy cclProxy;

	public static void setCCLProxy(ICCLProxy proxy) {
		SimpleServiceLocator.cclProxy = proxy;
	}

	public static ConfigToolHandler configToolHandler;

	public static void setConfigToolHandler(ConfigToolHandler configToolHandler) {
		SimpleServiceLocator.configToolHandler = configToolHandler;
	}

	public static IChannelManagerProvider channelManagerProvider;

	public static void setChannelManagerProvider(IChannelManagerProvider managerProvider) {
		SimpleServiceLocator.channelManagerProvider = managerProvider;
	}
}
