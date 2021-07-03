package logisticspipes.items;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import logisticspipes.interfaces.IItemAdvancedExistance;

public class LogisticsBrokenItem extends LogisticsItem implements IItemAdvancedExistance {

	private static final String PREFIX = "tooltip.brokenItem.";

	@Override
	public boolean canExistInNormalInventory(@Nonnull ItemStack stack) {
		return false;
	}

	@Override
	public boolean canExistInWorld(@Nonnull ItemStack stack) {
		return false;
	}

//	@Override
//	@OnlyIn(Dist.CLIENT)
//	public void addInformation(@Nonnull ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
//		tooltip.add(" - " + TextUtil.translate(LogisticsBrokenItem.PREFIX + "1"));
//		tooltip.add(" - " + TextUtil.translate(LogisticsBrokenItem.PREFIX + "2"));
//		tooltip.add("    " + TextUtil.translate(LogisticsBrokenItem.PREFIX + "3"));
//	}
}
