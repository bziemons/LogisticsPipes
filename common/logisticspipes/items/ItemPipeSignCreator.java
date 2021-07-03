package logisticspipes.items;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Direction;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.pipes.signs.CraftingPipeSign;
import logisticspipes.pipes.signs.IPipeSign;
import logisticspipes.pipes.signs.ItemAmountPipeSign;
import logisticspipes.proxy.MainProxy;

public class ItemPipeSignCreator extends LogisticsItem {

	public static final List<Class<? extends IPipeSign>> signTypes = new ArrayList<>();

	//private TextureAtlasSprite[] itemIcon = new TextureAtlasSprite[2];

	public ItemPipeSignCreator() {
		super();
		setMaxStackSize(1);
		setMaxDamage(250);
		setHasSubtypes(true);
	}

	@Override
	public boolean isEnchantable(@Nonnull ItemStack stack) {
		return false;
	}

	@Override
	public boolean canApplyAtEnchantingTable(@Nonnull ItemStack stack, Enchantment enchantment) {
		return false;
	}

	@Nonnull
	@Override
	public ActionResultType onItemUse(PlayerEntity player, World world, BlockPos pos, Hand hand, Direction facing, float hitX, float hitY, float hitZ) {
		if (MainProxy.isClient(world)) {
			return ActionResultType.FAIL;
		}
		ItemStack itemStack = player.inventory.getCurrentItem();
		if (itemStack.isEmpty() || itemStack.getDamage() > this.getMaxDamage()) {
			return ActionResultType.FAIL;
		}
		TileEntity tile = world.getTileEntity(pos);
		if (!(tile instanceof LogisticsTileGenericPipe)) {
			return ActionResultType.FAIL;
		}

		if (!itemStack.hasTag()) {
			itemStack.setTag(new CompoundNBT());
		}
		itemStack.getTag().putInt("PipeClicked", 0);

		int mode = itemStack.getTag().getInt("CreatorMode");

		if (facing == null) {
			return ActionResultType.FAIL;
		}

		if (!(((LogisticsTileGenericPipe) tile).pipe instanceof CoreRoutedPipe)) {
			return ActionResultType.FAIL;
		}

		CoreRoutedPipe pipe = (CoreRoutedPipe) ((LogisticsTileGenericPipe) tile).pipe;
		if (pipe == null) {
			return ActionResultType.FAIL;
		}
		if (!player.isSneaking()) {
			if (pipe.hasPipeSign(facing)) {
				pipe.activatePipeSign(facing, player);
				return ActionResultType.SUCCESS;
			} else if (mode >= 0 && mode < ItemPipeSignCreator.signTypes.size()) {
				Class<? extends IPipeSign> signClass = ItemPipeSignCreator.signTypes.get(mode);
				try {
					IPipeSign sign = signClass.newInstance();
					if (sign.isAllowedFor(pipe)) {
						itemStack.damageItem(1, player);
						sign.addSignTo(pipe, facing, player);
						return ActionResultType.SUCCESS;
					} else {
						return ActionResultType.FAIL;
					}
				} catch (InstantiationException | IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			} else {
				return ActionResultType.FAIL;
			}
		} else {
			if (pipe.hasPipeSign(facing)) {
				pipe.removePipeSign(facing, player);
				itemStack.damageItem(-1, player);
			}
			return ActionResultType.SUCCESS;
		}
	}

	@Override
	public int getMetadata(@Nonnull ItemStack stack) {
		if (stack.isEmpty() || !stack.hasTag()) return 0;
		int mode = Objects.requireNonNull(stack.getTag()).getInt("CreatorMode");
		return Math.min(mode, ItemPipeSignCreator.signTypes.size() - 1);
	}

	@Override
	public int getModelCount() {
		return signTypes.size();
	}

	@Nonnull
	@Override
	public ActionResult<ItemStack> onItemRightClick(final World world, final PlayerEntity player, @Nonnull final Hand hand) {
		ItemStack stack = player.inventory.getCurrentItem();
		if (MainProxy.isClient(world)) {
			return ActionResult.newResult(ActionResultType.PASS, stack);
		}
		if (player.isSneaking()) {
			if (!stack.hasTag()) {
				stack.setTag(new CompoundNBT());
			}
			if (!stack.getTag().contains("PipeClicked")) {
				int mode = stack.getTag().getInt("CreatorMode");
				mode++;
				if (mode >= ItemPipeSignCreator.signTypes.size()) {
					mode = 0;
				}
				stack.getTag().putInt("CreatorMode", mode);
			}
		}
		if (stack.hasTag()) {
			stack.getTag().remove("PipeClicked");
		}
		return ActionResult.newResult(ActionResultType.SUCCESS, stack);
	}

	public static void registerPipeSignTypes() {
		// Never change this order. It defines the id each signType has.
		ItemPipeSignCreator.signTypes.add(CraftingPipeSign.class);
		ItemPipeSignCreator.signTypes.add(ItemAmountPipeSign.class);
	}
}
