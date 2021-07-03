/**
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.items;

import javax.annotation.Nonnull;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import lombok.Getter;
import org.apache.logging.log4j.Level;

import logisticspipes.LPBlocks;
import logisticspipes.LogisticsPipes;
import logisticspipes.interfaces.ITubeOrientation;
import logisticspipes.pipes.basic.CoreMultiBlockPipe;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.pipes.basic.LogisticsBlockGenericPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericSubMultiBlock;
import logisticspipes.renderer.IIconProvider;
import logisticspipes.utils.LPPositionSet;
import network.rs485.logisticspipes.world.DoubleCoordinates;
import network.rs485.logisticspipes.world.DoubleCoordinatesType;

/**
 * A logistics pipe Item
 */
public class ItemLogisticsPipe extends LogisticsItem {

	private int newPipeIconIndex;
	private int newPipeRenderList = -1;
	@Getter
	private CoreUnroutedPipe dummyPipe;

	public ItemLogisticsPipe() {
		super();
	}

	@Nonnull
	@Override
	public ActionResultType onItemUse(PlayerEntity player, World worldIn, BlockPos pos, Hand hand, Direction facing, float hitX, float hitY, float hitZ) {
		Block block = LPBlocks.pipe;

		BlockState BlockState = worldIn.getBlockState(pos);
		Block worldBlock = BlockState.getBlock();

		if (!worldBlock.isReplaceable(worldIn, pos)) {
			pos = pos.offset(facing);
		}

		ItemStack itemstack = player.getHeldItem(hand);

		if (itemstack.isEmpty()) {
			return ActionResultType.FAIL;
		}

		if (!dummyPipe.isMultiBlock()) {
			if (player.canPlayerEdit(pos, facing, itemstack) && worldIn.mayPlace(block, pos, false, facing, null)) {
				CoreUnroutedPipe pipe = LogisticsBlockGenericPipe.createPipe(this);

				if (pipe == null) {
					LogisticsPipes.getLOGGER().log(Level.WARN, "Pipe failed to create during placement at {0},{1},{2}", new Object[] { pos.getX(), pos.getY(), pos.getZ() });
					return ActionResultType.PASS;
				}

				if (LogisticsBlockGenericPipe.placePipe(pipe, worldIn, pos, block, null)) {
					BlockState state = worldIn.getBlockState(pos);
					if (state.getBlock() == block) {
						//setTileEntityNBT(world, player, pos, stack);
						block.onBlockPlacedBy(worldIn, pos, state, player, itemstack);

						if (player instanceof ServerPlayerEntity)
							CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayerEntity) player, pos, itemstack);

						BlockState newBlockState = worldIn.getBlockState(pos);
						SoundType soundtype = newBlockState.getBlock().getSoundType(newBlockState, worldIn, pos, player);
						worldIn.playSound(player, pos, soundtype.getPlaceSound(), SoundCategory.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F,
								soundtype.getPitch() * 0.8F);

						itemstack.shrink(1);
					}
				}

				return ActionResultType.SUCCESS;
			} else {
				return ActionResultType.FAIL;
			}
		} else {
			CoreMultiBlockPipe multiPipe = (CoreMultiBlockPipe) dummyPipe;
			boolean isFreeSpace = true;
			DoubleCoordinates placeAt = new DoubleCoordinates(pos);
			LPPositionSet<DoubleCoordinatesType<CoreMultiBlockPipe.SubBlockTypeForShare>> globalPos = new LPPositionSet<>(DoubleCoordinatesType.class);
			globalPos.add(new DoubleCoordinatesType<>(placeAt, CoreMultiBlockPipe.SubBlockTypeForShare.NON_SHARE));
			LPPositionSet<DoubleCoordinatesType<CoreMultiBlockPipe.SubBlockTypeForShare>> positions = multiPipe.getSubBlocks();
			ITubeOrientation orientation = multiPipe.getTubeOrientation(player, pos.getX(), pos.getZ());
			if (orientation == null) {
				return ActionResultType.FAIL;
			}
			orientation.rotatePositions(positions);
			positions.stream().map(iPos -> iPos.add(placeAt)).forEach(globalPos::add);
			globalPos.addToAll(orientation.getOffset());
			placeAt.add(orientation.getOffset());

			for (DoubleCoordinatesType<CoreMultiBlockPipe.SubBlockTypeForShare> iPos : globalPos) {
				if (!player.canPlayerEdit(iPos.getBlockPos(), facing, itemstack) || !worldIn.mayPlace(block, iPos.getBlockPos(), false, facing, null)) {
					TileEntity tile = worldIn.getTileEntity(iPos.getBlockPos());
					boolean canPlace = false;
					if (tile instanceof LogisticsTileGenericSubMultiBlock) {
						if (CoreMultiBlockPipe.canShare(((LogisticsTileGenericSubMultiBlock) tile).getSubTypes(), iPos.getType())) {
							canPlace = true;
						}
					}
					if (!canPlace) {
						isFreeSpace = false;
						break;
					}
				}
			}
			if (isFreeSpace) {
				CoreUnroutedPipe pipe = LogisticsBlockGenericPipe.createPipe(this);

				if (pipe == null) {
					LogisticsPipes.getLOGGER().log(Level.WARN, "Pipe failed to create during placement at {0},{1},{2}", new Object[] { pos.getX(), pos.getY(), pos.getZ() });
					return ActionResultType.SUCCESS;
				}

				if (LogisticsBlockGenericPipe.placePipe(pipe, worldIn, placeAt.getBlockPos(), block, orientation)) {
					BlockState state = worldIn.getBlockState(placeAt.getBlockPos());
					if (state.getBlock() == block) {
						//setTileEntityNBT(world, player, pos, stack);
						block.onBlockPlacedBy(worldIn, pos, state, player, itemstack);

						if (player instanceof ServerPlayerEntity)
							CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayerEntity) player, placeAt.getBlockPos(), itemstack);

						BlockState newBlockState = worldIn.getBlockState(placeAt.getBlockPos());
						SoundType soundtype = newBlockState.getBlock().getSoundType(newBlockState, worldIn, placeAt.getBlockPos(), player);
						worldIn.playSound(player, placeAt.getBlockPos(), soundtype.getPlaceSound(), SoundCategory.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F,
								soundtype.getPitch() * 0.8F);

						itemstack.shrink(1);
					}
				}

				return ActionResultType.SUCCESS;
			} else {
				return ActionResultType.FAIL;
			}
		}
	}

	@OnlyIn(Dist.CLIENT)
	public void setPipesIcons(IIconProvider iconProvider) {
	}

	public void setPipeIconIndex(int index, int newIndex) {
		newPipeIconIndex = newIndex;
	}

	public int getNewPipeIconIndex() {
		return newPipeIconIndex;
	}

	public int getNewPipeRenderList() {
		return newPipeRenderList;
	}

	public void setNewPipeRenderList(int list) {
		if (newPipeRenderList != -1) {
			throw new UnsupportedOperationException("Can't reset this");
		}
		newPipeRenderList = list;
	}

	public void setDummyPipe(CoreUnroutedPipe pipe) {
		dummyPipe = pipe;
	}
}
