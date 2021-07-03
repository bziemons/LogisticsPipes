package logisticspipes.items;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.client.util.InputMappings;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.World;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import logisticspipes.interfaces.IItemAdvancedExistance;
import logisticspipes.proxy.SimpleServiceLocator;
import network.rs485.logisticspipes.util.TextUtil;

public class LogisticsItemCard extends LogisticsItem implements IItemAdvancedExistance {

	public static final int FREQ_CARD = 0;
	public static final int SEC_CARD = 1;

	public LogisticsItemCard() {
		hasSubtypes = true;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void addInformation(@Nonnull ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
		super.addInformation(stack, worldIn, tooltip, flagIn);
		if (!stack.hasTag()) {
			tooltip.add(TextUtil.translate("tooltip.logisticsItemCard"));
		} else {
			final CompoundNBT tag = Objects.requireNonNull(stack.getTag());
			if (tag.contains("UUID")) {
				if (stack.getDamage() == LogisticsItemCard.FREQ_CARD) {
					tooltip.add("Freq. Card");
				} else if (stack.getDamage() == LogisticsItemCard.SEC_CARD) {
					tooltip.add("Sec. Card");
				}
				if (InputMappings.isKeyDown(Minecraft.getInstance().mainWindow.getHandle(), 340)) {
					tooltip.add("Id: " + tag.getString("UUID"));
					if (stack.getDamage() == LogisticsItemCard.SEC_CARD) {
						UUID id = UUID.fromString(tag.getString("UUID"));
						tooltip.add("Authorization: " + (SimpleServiceLocator.securityStationManager.isAuthorized(id) ? "Authorized" : "Deauthorized"));
					}
				}
			}
		}
	}

	@Override
	public boolean getShareTag() {
		return true;
	}

	@Override
	public int getItemStackLimit() {
		return 64;
	}

	@Override
	public boolean canExistInNormalInventory(@Nonnull ItemStack stack) {
		return true;
	}

	@Override
	public boolean canExistInWorld(@Nonnull ItemStack stack) {
		return stack.getDamage() != LogisticsItemCard.SEC_CARD;
	}
}
