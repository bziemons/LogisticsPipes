package logisticspipes.blocks;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.IProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.chunk.BlockStateContainer;

import lombok.Getter;

import logisticspipes.LogisticsPipes;
import logisticspipes.blocks.crafting.LogisticsCraftingTableTileEntity;
import logisticspipes.blocks.powertile.LogisticsIC2PowerProviderTileEntity;
import logisticspipes.blocks.powertile.LogisticsRFPowerProviderTileEntity;
import logisticspipes.blocks.stats.LogisticsStatisticsTileEntity;
import logisticspipes.interfaces.IGuiTileEntity;
import logisticspipes.interfaces.IRotationProvider;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;

public class LogisticsSolidBlock extends Block {

	public static final IProperty<Integer> rotationProperty = IntegerProperty.create("rotation", 0, 3);
	public static final IProperty<Boolean> active = BooleanProperty.create("active");
	public static final Map<Direction, IProperty<Boolean>> connectionPropertys = Arrays.stream(Direction.values()).collect(Collectors.toMap(key -> key, key -> BooleanProperty.create("connection_" + key.ordinal())));

	@Getter
	private final Type type;

	public enum Type {
		LOGISTICS_POWER_JUNCTION(1, LogisticsPowerJunctionTileEntity::new),
		LOGISTICS_SECURITY_STATION(2, LogisticsSecurityTileEntity::new),
		LOGISTICS_AUTOCRAFTING_TABLE(3, LogisticsCraftingTableTileEntity::new),
		LOGISTICS_FUZZYCRAFTING_TABLE(4, LogisticsCraftingTableTileEntity::new),
		LOGISTICS_STATISTICS_TABLE(5, LogisticsStatisticsTileEntity::new),

		// Power Provider
		LOGISTICS_RF_POWERPROVIDER(10, LogisticsRFPowerProviderTileEntity::new),
		LOGISTICS_IC2_POWERPROVIDER(11, LogisticsIC2PowerProviderTileEntity::new),
		LOGISTICS_BC_POWERPROVIDER(12),

		LOGISTICS_PROGRAM_COMPILER(14, LogisticsProgramCompilerTileEntity::new),

		LOGISTICS_BLOCK_FRAME(15);

		@Getter
		boolean hasActiveTexture;

		@Nullable
		private final Supplier<TileEntity> teConstructor;

		Type(int meta) {
			this(meta, null, false);
		}

		Type(int meta, @Nullable Supplier<TileEntity> teConstructor) {
			this(meta, teConstructor, false);
		}

		Type(int meta, @Nullable Supplier<TileEntity> teConstructor, boolean hasActiveTexture) {
			this.meta = meta;
			this.teConstructor = teConstructor;
			this.hasActiveTexture = hasActiveTexture;
		}

		public boolean hasTE() {
			return teConstructor != null;
		}

		public TileEntity createTE() {
			if (!hasTE()) throw new UnsupportedOperationException("This block type has no tile entity!");

			assert teConstructor != null;
			return teConstructor.get();
		}

	}

	public LogisticsSolidBlock(Type type) {
		super(Material.IRON);
		this.type = type;
		setHardness(6.0F);
		setCreativeTab(LogisticsPipes.LP_ITEM_GROUP);
		BlockDummy.updateBlockMap.put(type.getMeta(), this);
	}

	@Override
	public void onNeighborChange(IWorld world, BlockPos pos, BlockPos neigbour) {
		super.onNeighborChange(world, pos, neigbour);
		TileEntity tile = world.getTileEntity(pos);
		if (tile instanceof LogisticsSolidTileEntity) {
			((LogisticsSolidTileEntity) tile).notifyOfBlockChange();
		}
	}

	@Override
	public boolean onBlockActivated(World worldIn, BlockPos pos, BlockState state, PlayerEntity playerIn, Hand hand, Direction facing, float hitX, float hitY, float hitZ) {
		if (!playerIn.isSneaking()) {
			TileEntity tile = worldIn.getTileEntity(pos);
			if (tile instanceof IGuiTileEntity) {
				if (MainProxy.isServer(playerIn.world)) {
					((IGuiTileEntity) tile).getGuiProvider().setTilePos(tile).open(playerIn);
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, EntityLivingBase placer, @Nonnull ItemStack stack) {
		super.onBlockPlacedBy(world, pos, state, placer, stack);
		TileEntity tile = world.getTileEntity(pos);
		if (tile instanceof LogisticsCraftingTableTileEntity) {
			((LogisticsCraftingTableTileEntity) tile).placedBy(placer);
		}
		if (tile instanceof IRotationProvider) {
			((IRotationProvider) tile).setFacing(placer.getHorizontalFacing().getOpposite());
		}
	}

	@Override
	public void breakBlock(World worldIn, @Nonnull BlockPos pos, @Nonnull BlockState state) {
		TileEntity tile = worldIn.getTileEntity(pos);
		if (tile instanceof LogisticsSolidTileEntity) {
			((LogisticsSolidTileEntity) tile).onBlockBreak();
		}
		super.breakBlock(worldIn, pos, state);
	}

	@Nullable
	@Override
	public TileEntity createTileEntity(@Nonnull World world, @Nonnull BlockState state) {
		if (!type.hasTE()) return null;
		return type.createTE();
	}

	@Override
	public boolean hasTileEntity(BlockState state) {
		return type.hasTE();
	}

	@Nonnull
	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer.Builder(this)
				.add(rotationProperty)
				.add(active)
				.add(connectionPropertys.values().toArray(new IProperty[0]))
				.build();
	}

	@Override
	public int getMetaFromState(BlockState state) {
		return 0;
	}

	@Nonnull
	@Override
	public BlockState getActualState(@Nonnull BlockState state, IWorld worldIn, BlockPos pos) {
		state = super.getActualState(state, worldIn, pos);
		TileEntity tile = worldIn.getTileEntity(pos);
		if (tile instanceof LogisticsSolidTileEntity) {
			LogisticsSolidTileEntity ste = (LogisticsSolidTileEntity) tile;
			int rotation = ste.getRotation();
			state = state
					.withProperty(rotationProperty, Math.min(Math.max(rotation, 0), 3))
					.withProperty(active, ste.isActive());
		}

		if (tile != null) {
			for (Direction side : Direction.values()) {
				boolean render = true;
				TileEntity sideTile = worldIn.getTileEntity(pos.offset(side));
				if (sideTile instanceof LogisticsTileGenericPipe) {
					LogisticsTileGenericPipe tilePipe = (LogisticsTileGenericPipe) sideTile;
					if (tilePipe.renderState.pipeConnectionMatrix.isConnected(side.getOpposite())) {
						render = false;
					}
				}
				state = state.withProperty(connectionPropertys.get(side), render);
			}
		}

		return state;
	}
}
