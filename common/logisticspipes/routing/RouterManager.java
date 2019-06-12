/**
 * Copyright (c) Krapht, 2011
 * 
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.routing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.interfaces.ISecurityStationManager;
import logisticspipes.interfaces.routing.IChannelConnectionManager;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.block.SecurityStationAuthorizedList;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.routing.channels.ChannelConnection;

import net.minecraft.entity.player.EntityPlayer;

public class RouterManager implements IRouterManager, IChannelConnectionManager, ISecurityStationManager {

	private final ArrayList<IRouter> _routersClient = new ArrayList<>();
	private final ArrayList<IRouter> _routersServer = new ArrayList<>();
	private final Map<UUID, Integer> _uuidMap = new HashMap<>();

	private final WeakHashMap<LogisticsSecurityTileEntity, Void> _security = new WeakHashMap<>();
	private List<String> _authorized = new LinkedList<>();

	private final ArrayList<ChannelConnection> channelConnectedPipes = new ArrayList<>();

	@Override
	public IRouter getRouter(int id) {
		//TODO: isClient without a world is expensive
		if (id <= 0 || MainProxy.isClient()) {
			return null;
		} else {
			return _routersServer.get(id);
		}
	}

	@Override
	public IRouter getRouterUnsafe(Integer id, boolean side) {
		if (side || id <= 0) {
			return null;
		} else {
			return _routersServer.get(id);
		}
	}

	@Override
	public int getIDforUUID(UUID id) {
		if (id == null) {
			return -1;
		}
		Integer iId = _uuidMap.get(id);
		if (iId == null) {
			return -1;
		}
		return iId;
	}

	@Override
	public void removeRouter(int id) {
		//TODO: isClient without a world is expensive
		if (!MainProxy.isClient()) {
			_routersServer.set(id, null);
		}
	}

	@Override
	public IRouter getOrCreateRouter(UUID UUid, int dimension, int xCoord, int yCoord, int zCoord, boolean forceCreateDuplicate) {
		IRouter r = null;
		int id = getIDforUUID(UUid);
		if (id > 0) {
			getRouter(id);
		}
		if (r == null || !r.isAt(dimension, xCoord, yCoord, zCoord)) {
			if (MainProxy.isClient()) {
				synchronized (_routersClient) {
					for (IRouter r2 : _routersClient) {
						if (r2.isAt(dimension, xCoord, yCoord, zCoord)) {
							return r2;
						}
					}
					r = new ClientRouter(UUid, dimension, xCoord, yCoord, zCoord);
					_routersClient.add(r);
				}
			} else {
				synchronized (_routersServer) {
					if (!forceCreateDuplicate) {
						for (IRouter r2 : _routersServer) {
							if (r2 != null && r2.isAt(dimension, xCoord, yCoord, zCoord)) {
								return r2;
							}
						}
					}
					r = new ServerRouter(UUid, dimension, xCoord, yCoord, zCoord);

					int rId = r.getSimpleID();
					if (_routersServer.size() > rId) {
						_routersServer.set(rId, r);
					} else {
						_routersServer.ensureCapacity(rId + 1);
						while (_routersServer.size() <= rId) {
							_routersServer.add(null);
						}
						_routersServer.set(rId, r);
					}
					_uuidMap.put(r.getId(), r.getSimpleID());
				}
			}
		}
		return r;
	}

	@Override
	public boolean isRouter(int id) {
		if (MainProxy.isClient()) {
			return true;
		} else {
			return _routersServer.get(id) != null;
		}
	}

	/**
	 * This assumes you know what you are doing. expect exceptions to be thrown
	 * if you pass the wrong side.
	 * 
	 * @param id
	 * @param side
	 *            false for server, true for client.
	 * @return is this a router for the side.
	 */
	@Override
	public boolean isRouterUnsafe(int id, boolean side) {
		if (side) {
			return true;
		} else {
			return _routersServer.get(id) != null;
		}
	}

	@Override
	public List<IRouter> getRouters() {
		if (MainProxy.isClient()) {
			return Collections.unmodifiableList(_routersClient);
		} else {
			return Collections.unmodifiableList(_routersServer);
		}
	}

	@Override
	public boolean hasChannelConnection(IRouter router) {
		return channelConnectedPipes.stream()
				.filter(con -> con.routers.size() > 1)
				.anyMatch(con -> con.routers.contains(router.getSimpleID()));
	}

	@Override
	public boolean addChannelConnection(UUID ident, IRouter router) {
		if (MainProxy.isClient()) {
			return false;
		}
		int routerSimpleID = router.getSimpleID();
		channelConnectedPipes.forEach(con -> con.routers.remove(routerSimpleID));
		Optional<ChannelConnection> channel = channelConnectedPipes.stream().filter(con -> con.identifier.equals(ident)).findFirst();
		if(channel.isPresent()) {
			channel.get().routers.add(routerSimpleID);
		} else {
			ChannelConnection newChannel = new ChannelConnection();
			channelConnectedPipes.add(newChannel);
			newChannel.identifier = ident;
			newChannel.routers.add(routerSimpleID);
		}
		return true;
	}

	@Override
	public List<CoreRoutedPipe> getConnectedPipes(IRouter router) {
		Optional<ChannelConnection> channel = channelConnectedPipes.stream()
				.filter(con -> con.routers.contains(router.getSimpleID()))
				.findFirst();
		return channel.
				map(channelConnection ->
						channelConnection.routers.stream()
								.filter(r -> r != router.getSimpleID())
								.map(r -> getRouter(r).getPipe())
								.filter(Objects::nonNull)
								.collect(Collectors.toList())
				)
				.orElse(Collections.emptyList());
	}

	@Override
	public void removeChannelConnection(IRouter router) {
		if (MainProxy.isClient()) {
			return;
		}
		Optional<ChannelConnection> channel = channelConnectedPipes.stream()
				.filter(con -> con.routers.contains(router.getSimpleID()))
				.findFirst();
		channel.ifPresent(chan -> chan.routers.remove(router.getSimpleID()));
		if(channel.filter(chan -> chan.routers.isEmpty()).isPresent()) {
			channelConnectedPipes.remove(channel.get());
		}
	}

	@Override
	public void serverStopClean() {
		channelConnectedPipes.clear();
		_routersServer.clear();
		_uuidMap.clear();
		_security.clear();
	}

	@Override
	public void clearClientRouters() {
		synchronized (_routersClient) {
			_routersClient.clear();
		}
	}

	@Override
	public void add(LogisticsSecurityTileEntity tile) {
		_security.put(tile, null);
		authorizeUUID(tile.getSecId());
	}

	@Override
	public LogisticsSecurityTileEntity getStation(UUID id) {
		if (id == null) {
			return null;
		}
		for (LogisticsSecurityTileEntity tile : _security.keySet()) {
			if (id.equals(tile.getSecId())) {
				return tile;
			}
		}
		return null;
	}

	@Override
	public void remove(LogisticsSecurityTileEntity tile) {
		_security.remove(tile);
		deauthorizeUUID(tile.getSecId());
	}

	@Override
	public void dimensionUnloaded(int dim) {
		synchronized (_routersServer) {
			_routersServer.stream().filter(r -> r != null && r.isInDim(dim)).forEach(r -> {
				r.clearPipeCache();
			});
		}
	}

	@Override
	public void deauthorizeUUID(UUID id) {
		if (_authorized.contains(id.toString())) {
			_authorized.remove(id.toString());
		}
		sendClientAuthorizationList();
	}

	@Override
	public void authorizeUUID(UUID id) {
		if (!_authorized.contains(id.toString())) {
			_authorized.add(id.toString());
		}
		sendClientAuthorizationList();
	}

	@Override
	public boolean isAuthorized(UUID id) {
		if (_authorized.isEmpty() || id == null) {
			return false;
		}
		return _authorized.contains(id.toString());
	}

	@Override
	public boolean isAuthorized(String id) {
		if (_authorized.isEmpty() || id == null) {
			return false;
		}
		return _authorized.contains(id);
	}

	@Override
	public void setClientAuthorizationList(List<String> list) {
		_authorized = list;
	}

	@Override
	public void sendClientAuthorizationList() {
		MainProxy.sendToAllPlayers(PacketHandler.getPacket(SecurityStationAuthorizedList.class).setStringList(_authorized));
	}

	@Override
	public void sendClientAuthorizationList(EntityPlayer player) {
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(SecurityStationAuthorizedList.class).setStringList(_authorized), player);
	}

	@Override
	public void printAllRouters() {
		_routersServer.stream().filter(router -> router != null).forEach(router -> System.out.println(router.toString()));
	}
}
