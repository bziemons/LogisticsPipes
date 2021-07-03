package logisticspipes.pipes.basic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.thread.EffectiveSide;

import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.packets.multiblock.MultiBlockCoordinatesPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.renderer.state.PipeSubRenderState;
import logisticspipes.routing.pathfinder.IPipeInformationProvider;
import logisticspipes.routing.pathfinder.ISubMultiBlockPipeInformationProvider;
import logisticspipes.utils.TileBuffer;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public class LogisticsTileGenericSubMultiBlock extends TileEntity implements ISubMultiBlockPipeInformationProvider, ITickable {

	private Set<DoubleCoordinates> mainPipePos = new HashSet<>();
	private List<LogisticsTileGenericPipe> mainPipe;
	private List<CoreMultiBlockPipe.SubBlockTypeForShare> subTypes = new ArrayList<>();
	private TileBuffer[] tileBuffer;
	public final PipeSubRenderState renderState;

	@Deprecated
	public LogisticsTileGenericSubMultiBlock() {
		renderState = new PipeSubRenderState();
	}

	public LogisticsTileGenericSubMultiBlock(DoubleCoordinates pos) {
		if (pos != null) {
			mainPipePos.add(pos);
		}
		mainPipe = null;
		renderState = new PipeSubRenderState();
	}

	@Override
	public void setPos(BlockPos posIn) {
		super.setPos(posIn);
		if (EffectiveSide.get().isClient()) {
			System.out.println("Multi Pipe Created at: " + posIn);
		}
	}

	public List<LogisticsTileGenericPipe> getMainPipe() {
		if (mainPipe == null) {
			mainPipe = new ArrayList<>();
			for (DoubleCoordinates pos : mainPipePos) {
				TileEntity tile = pos.getTileEntity(getWorld());
				if (tile instanceof LogisticsTileGenericPipe) {
					mainPipe.add((LogisticsTileGenericPipe) tile);
				}
			}
			mainPipe = Collections.unmodifiableList(mainPipe);
		}
		if (MainProxy.isServer(world)) {
			boolean allInvalid = true;
			for (LogisticsTileGenericPipe pipe : mainPipe) {
				if (!pipe.isRemoved()) {
					allInvalid = false;
					break;
				}
			}
			if (mainPipe.isEmpty() || allInvalid) {
				getWorld().setBlockToAir(getPos());
			}
		}
		if (mainPipe != null) {
			return mainPipe;
		}
		return Collections.emptyList();
	}

	public List<CoreMultiBlockPipe.SubBlockTypeForShare> getSubTypes() {
		return Collections.unmodifiableList(subTypes);
	}

	@Override
	public void update() {
		if (MainProxy.isClient(getWorld())) {
			return;
		}
		List<LogisticsTileGenericPipe> pipes = getMainPipe();
		for (LogisticsTileGenericPipe pipe : pipes) {
			pipe.subMultiBlock.add(new DoubleCoordinates(this));
		}
	}

	@Override
	public void readFromNBT(CompoundNBT nbt) {
		super.readFromNBT(nbt);
		if (nbt.contains("MainPipePos_xPos")) {
			mainPipePos.clear();
			DoubleCoordinates pos = DoubleCoordinates.readFromNBT("MainPipePos_", nbt);
			if (pos != null) {
				mainPipePos.add(pos);
			}
		}
		if (nbt.contains("MainPipePosList")) {
			ListNBT list = nbt.getList("MainPipePosList", new CompoundNBT().getId());
			for (int i = 0; i < list.size(); i++) {
				DoubleCoordinates pos = DoubleCoordinates.readFromNBT("MainPipePos_", list.getCompound(i));
				if (pos != null) {
					mainPipePos.add(pos);
				}
			}
		}
		if (nbt.contains("SubTypeList")) {
			ListNBT list = nbt.getList("SubTypeList", new StringNBT().getId());
			subTypes.clear();
			for (int i = 0; i < list.size(); i++) {
				String name = list.getStringTagAt(i);
				CoreMultiBlockPipe.SubBlockTypeForShare type = CoreMultiBlockPipe.SubBlockTypeForShare.valueOf(name);
				if (type != null) {
					subTypes.add(type);
				}
			}
		}
		mainPipe = null;
	}

	@Nonnull
	@Override
	public CompoundNBT writeToNBT(CompoundNBT nbt) {
		nbt = super.writeToNBT(nbt);
		ListNBT nbtList = new ListNBT();
		for (DoubleCoordinates pos : mainPipePos) {
			CompoundNBT compound = new CompoundNBT();
			pos.writeToNBT("MainPipePos_", compound);
			nbtList.add(compound);
		}
		nbt.setTag("MainPipePosList", nbtList);
		ListNBT nbtTypeList = new ListNBT();
		for (CoreMultiBlockPipe.SubBlockTypeForShare type : subTypes) {
			if (type == null) continue;
			nbtTypeList.add(new StringNBT(type.name()));
		}
		nbt.setTag("SubTypeList", nbtTypeList);
		return nbt;
	}

	@Nonnull
	@Override
	public CompoundNBT getUpdateTag() {
		CompoundNBT nbt = super.getUpdateTag();
		try {
			PacketHandler.addPacketToNBT(getLPDescriptionPacket(), nbt);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return nbt;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void handleUpdateTag(@Nonnull CompoundNBT tag) {
		PacketHandler.queueAndRemovePacketFromNBT(tag);
		super.handleUpdateTag(tag);
	}

	@Override
	public SUpdateTileEntityPacket getUpdatePacket() {
		CompoundNBT nbt = new CompoundNBT();
		try {
			PacketHandler.addPacketToNBT(getLPDescriptionPacket(), nbt);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new SUpdateTileEntityPacket(getPos(), 1, nbt);
	}

	@Override
	public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket packet) {
		PacketHandler.queueAndRemovePacketFromNBT(packet.getNbtCompound());
	}

	public ModernPacket getLPDescriptionPacket() {
		MultiBlockCoordinatesPacket packet = PacketHandler.getPacket(MultiBlockCoordinatesPacket.class);
		packet.setTilePos(this);
		packet.setTargetPos(mainPipePos);
		packet.setSubTypes(subTypes);
		return packet;
	}

	public void setPosition(Set<DoubleCoordinates> lpPosition, List<CoreMultiBlockPipe.SubBlockTypeForShare> subTypes) {
		mainPipePos = lpPosition;
		this.subTypes = subTypes;
		mainPipe = null;
	}

	public TileEntity getTile() {
		return this;
	}

	public TileEntity getTile(Direction to) {
		return getTile(to, false);
	}

	public TileEntity getTile(Direction to, boolean force) {
		TileBuffer[] cache = getTileCache();
		if (cache != null) {
			if (force) {
				cache[to.ordinal()].refresh();
			}
			return cache[to.ordinal()].getTile();
		} else {
			return null;
		}
	}

	public Block getBlock(Direction to) {
		TileBuffer[] cache = getTileCache();
		if (cache != null) {
			return cache[to.ordinal()].getBlock();
		} else {
			return null;
		}
	}

	public TileBuffer[] getTileCache() {
		if (tileBuffer == null) {
			tileBuffer = TileBuffer.makeBuffer(world, getPos(), true);
		}
		return tileBuffer;
	}

	@Override
	public void invalidate() {
		super.invalidate();
		tileBuffer = null;
	}

	@Override
	public void validate() {
		super.validate();
		tileBuffer = null;
	}

	public void scheduleNeighborChange() {
		tileBuffer = null;
	}

	public void addSubTypeTo(CoreMultiBlockPipe.SubBlockTypeForShare type) {
		if (type == null) throw new NullPointerException();
		subTypes.add(type);
	}

	public void addMultiBlockMainPos(DoubleCoordinates placeAt) {
		if (mainPipePos.add(placeAt)) {
			mainPipe = null;
		}
	}

	public boolean removeMainPipe(DoubleCoordinates doubleCoordinates) {
		mainPipePos.remove(doubleCoordinates);
		return mainPipePos.isEmpty();
	}

	public void removeSubType(CoreMultiBlockPipe.SubBlockTypeForShare type) {
		subTypes.remove(type);
	}

	@Override
	public IPipeInformationProvider getMainTile() {
		List<LogisticsTileGenericPipe> mainTiles = this.getMainPipe();
		if (mainTiles.size() != 1) {
			return null;
		}
		return mainTiles.get(0);
	}
}
