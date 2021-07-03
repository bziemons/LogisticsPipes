package logisticspipes.items;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Direction;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.guis.LogisticsPlayerSettingsGuiProvider;
import logisticspipes.proxy.MainProxy;

public class ItemPipeController extends LogisticsItem {

	public ItemPipeController() {
		super();
	}

	@Nonnull
	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, @Nonnull Hand handIn) {
		ItemStack stack = player.getHeldItem(handIn);
		if (MainProxy.isClient(world)) {
			return new ActionResult<>(ActionResultType.PASS, stack);
		}
		useItem(player, world);
		return new ActionResult<>(ActionResultType.SUCCESS, stack);
	}

	@Nonnull
	@Override
	public ActionResultType onItemUse(PlayerEntity player, World world, BlockPos pos, Hand hand, Direction facing, float hitX, float hitY, float hitZ) {
		if (MainProxy.isClient(world)) {
			return ActionResultType.PASS;
		}
		useItem(player, world);
		return ActionResultType.SUCCESS;
	}

	private void useItem(PlayerEntity player, World world) {
		NewGuiHandler.getGui(LogisticsPlayerSettingsGuiProvider.class).open(player);
	}
}
