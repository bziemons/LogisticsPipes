package logisticspipes.pipes.tubes;

import java.util.List;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;

import lombok.Getter;

import logisticspipes.interfaces.ITubeOrientation;
import logisticspipes.interfaces.ITubeRenderOrientation;
import logisticspipes.pipes.basic.CoreMultiBlockPipe;
import logisticspipes.renderer.newpipe.IHighlightPlacementRenderer;
import logisticspipes.renderer.newpipe.ISpecialPipeRenderer;
import logisticspipes.renderer.newpipe.tube.LineTubeRenderer;
import logisticspipes.transport.PipeMultiBlockTransportLogistics;
import logisticspipes.utils.IPositionRotateble;
import logisticspipes.utils.LPPositionSet;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;
import network.rs485.logisticspipes.world.DoubleCoordinates;
import network.rs485.logisticspipes.world.DoubleCoordinatesType;

public class HSTubeLine extends CoreMultiBlockPipe {

	@Getter
	private TubeLineOrientation orientation;

	public HSTubeLine(Item item) {
		super(new PipeMultiBlockTransportLogistics(), item);
	}

	@Override
	public void writeData(LPDataOutput output) {
		if (orientation == null) {
			output.writeBoolean(false);
		} else {
			output.writeBoolean(true);
			output.writeEnum(orientation);
		}
	}

	@Override
	public void readData(LPDataInput input) {
		if (input.readBoolean()) {
			orientation = input.readEnum(TubeLineOrientation.class);
		}
	}

	@Override
	public LPPositionSet<DoubleCoordinatesType<SubBlockTypeForShare>> getSubBlocks() {
		return new LPPositionSet<>(DoubleCoordinatesType.class);
	}

	@Override
	public LPPositionSet<DoubleCoordinatesType<SubBlockTypeForShare>> getRotatedSubBlocks() {
		LPPositionSet<DoubleCoordinatesType<SubBlockTypeForShare>> set = getSubBlocks();
		orientation.rotatePositions(set);
		return set;
	}

	@Override
	public void writeToNBT(CompoundNBT data) {
		super.writeToNBT(data);
		data.putString("orientation", orientation.name());
	}

	@Override
	public void readFromNBT(CompoundNBT data) {
		super.readFromNBT(data);
		orientation = TubeLineOrientation.valueOf(data.getString("orientation"));
	}

	@Override
	public void addCollisionBoxesToList(List<AxisAlignedBB> arraylist, AxisAlignedBB axisalignedbb) {
		DoubleCoordinates pos = getLPPosition();
		LPPositionSet<DoubleCoordinates> set = new LPPositionSet<>(DoubleCoordinates.class);
		set.addFrom(LineTubeRenderer.tubeLine.get(orientation.getRenderOrientation()).bounds().toAABB());
		set.forEach(o -> o.add(pos));
		AxisAlignedBB box = set.toABB();
		if (box != null && (axisalignedbb == null || axisalignedbb.intersects(box))) {
			arraylist.add(box);
		}
	}

	@Override
	public AxisAlignedBB getCompleteBox() {
		return LineTubeRenderer.tubeLine.get(orientation.getRenderOrientation()).bounds().toAABB();
	}

	@Override
	public ITubeOrientation getTubeOrientation(PlayerEntity player, int xPos, int zPos) {
		double x = xPos + 0.5 - player.posX;
		double z = zPos + 0.5 - player.posZ;
		double w = Math.atan2(x, z);
		double halfPI = Math.PI / 2;
		double halfhalfPI = halfPI / 2;
		w -= halfhalfPI;
		if (w < 0) {
			w += 2 * Math.PI;
		}
		Direction dir = null;
		if (0 < w && w <= halfPI) {
			dir = Direction.WEST;
		} else if (halfPI < w && w <= 2 * halfPI) {
			dir = Direction.SOUTH;
		} else if (2 * halfPI < w && w <= 3 * halfPI) {
			dir = Direction.EAST;
		} else if (3 * halfPI < w && w <= 4 * halfPI) {
			dir = Direction.NORTH;
		}
		for (TubeLineOrientation ori : TubeLineOrientation.values()) {
			if (ori.dir.equals(dir)) {
				return ori;
			}
		}
		return null;
	}

	@Override
	public float getPipeLength() {
		return 1;
	}

	@Override
	public Direction getExitForInput(Direction commingFrom) {
		return commingFrom.getOpposite();
	}

	@Override
	public TileEntity getConnectedEndTile(Direction output) {
		if (output == this.orientation.dir || output.getOpposite() == this.orientation.dir) {
			return container.getTile(output);
		}
		return null;
	}

	@Override
	public int getIconIndex(Direction direction) {
		return 0;
	}

	@Override
	public int getTextureIndex() {
		return 0;
	}

	@Override
	public boolean actAsNormalPipe() {
		return false;
	}

	@Override
	public ISpecialPipeRenderer getSpecialRenderer() {
		return LineTubeRenderer.instance;
	}

	@Override
	public IHighlightPlacementRenderer getHighlightRenderer() {
		return LineTubeRenderer.instance;
	}

	@Override
	public boolean isHSTube() {
		return true;
	}

	public enum TubeLineOrientation implements ITubeOrientation {
		NORTH(TubeLineRenderOrientation.NORTH_SOUTH, new DoubleCoordinates(0, 0, 0), Direction.NORTH),
		SOUTH(TubeLineRenderOrientation.NORTH_SOUTH, new DoubleCoordinates(0, 0, 0), Direction.SOUTH),
		EAST(TubeLineRenderOrientation.EAST_WEST, new DoubleCoordinates(0, 0, 0), Direction.EAST),
		WEST(TubeLineRenderOrientation.EAST_WEST, new DoubleCoordinates(0, 0, 0), Direction.WEST);

		@Getter
		TubeLineRenderOrientation renderOrientation;
		@Getter
		DoubleCoordinates offset;
		@Getter
		Direction dir;

		TubeLineOrientation(TubeLineRenderOrientation render, DoubleCoordinates off, Direction dir) {
			renderOrientation = render;
			offset = off;
			this.dir = dir;
		}

		@Override
		public void rotatePositions(IPositionRotateble set) {
			renderOrientation.rotateOrientation(set);
		}

		@Override
		public void setOnPipe(CoreMultiBlockPipe pipe) {
			((HSTubeLine) pipe).orientation = this;
		}
	}

	public enum TubeLineRenderOrientation implements ITubeRenderOrientation {
		NORTH_SOUTH(Direction.NORTH),
		EAST_WEST(Direction.EAST);

		@Getter
		private Direction dir;

		TubeLineRenderOrientation(Direction dir) {
			this.dir = dir;
		}

		public void rotateOrientation(IPositionRotateble set) {
			if (this == EAST_WEST) {
				set.rotateLeft();
			}
		}
	}
}
