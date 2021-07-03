package logisticspipes.items;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.client.util.InputMappings;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.lwjgl.glfw.GLFW;

import logisticspipes.interfaces.IItemAdvancedExistance;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.utils.FluidIdentifierStack;
import logisticspipes.utils.item.ItemIdentifierStack;

public class LogisticsFluidContainer extends LogisticsItem implements IItemAdvancedExistance {

	public LogisticsFluidContainer() {
		super(new Item.Properties().maxStackSize(1));
	}

	@Override
	public boolean canExistInNormalInventory(@Nonnull ItemStack stack) {
		return false;
	}

	@Override
	public boolean canExistInWorld(@Nonnull ItemStack stack) {
		return false;
	}

	@Nonnull
	@Override
	public String getTranslationKey(@Nonnull ItemStack stack) {
		FluidIdentifierStack fluidStack = SimpleServiceLocator.logisticsFluidManager.getFluidFromContainer(ItemIdentifierStack.getFromStack(stack));
		if (fluidStack != null) {
			String s = fluidStack.makeFluidStack().getTranslationKey();
			if (s != null) {
				return s;
			}
		}
		return super.getTranslationKey(stack);
	}

	@Override
	@Nonnull
	public String getItemStackDisplayName(@Nonnull ItemStack itemstack) {
		String unlocalizedName = getTranslationKey(itemstack);
		return I18n.translateToLocal(unlocalizedName + ".name");
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void addInformation(@Nonnull ItemStack stack, @Nullable World worldIn, @Nonnull List<ITextComponent> tooltip, @Nonnull ITooltipFlag flagIn) {
		super.addInformation(stack, worldIn, tooltip, flagIn);
		if (InputMappings.isKeyDown(Minecraft.getInstance().mainWindow.getHandle(), GLFW.GLFW_KEY_LEFT_SHIFT)) {
			FluidIdentifierStack fluidStack = SimpleServiceLocator.logisticsFluidManager.getFluidFromContainer(ItemIdentifierStack.getFromStack(stack));
			if (fluidStack != null) {
				tooltip.add(new StringTextComponent("Type:  " + fluidStack.makeFluidStack().getDisplayName()));
				tooltip.add(new StringTextComponent("Value: " + fluidStack.getAmount() + "mB"));
			}
		}
	}

	@Override
	public Collection<ItemGroup> getCreativeTabs() {
		return Collections.emptyList();
	}

}
