package logisticspipes.items;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;

import logisticspipes.api.ILPPipeConfigTool;
import logisticspipes.api.ILPPipeTile;

public class ItemPipeManager extends LogisticsItem implements ILPPipeConfigTool {

	public ItemPipeManager() {
		super();
	}

	@Override
	public boolean canWrench(PlayerEntity player, @Nonnull ItemStack wrench, ILPPipeTile pipe) {
		return true;
	}

	@Override
	public void wrenchUsed(PlayerEntity player, @Nonnull ItemStack wrench, ILPPipeTile pipe) {}

	@Override
	public boolean doesSneakBypassUse(@Nonnull ItemStack stack, IWorld world, BlockPos pos, PlayerEntity player) {
		return true;
	}
}
