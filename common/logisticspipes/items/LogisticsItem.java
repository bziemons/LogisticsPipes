/*
 * Copyright (c) Krapht, 2011
 * <p>
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.items;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.lwjgl.input.Keyboard;

import logisticspipes.LogisticsPipes;
import logisticspipes.interfaces.ILogisticsItem;
import network.rs485.logisticspipes.util.TextUtil;

public class LogisticsItem extends Item implements ILogisticsItem {

	public LogisticsItem() {
		super(new Item.Properties().group(LogisticsPipes.LP_ITEM_GROUP));
	}

	protected LogisticsItem(Item.Properties itemProperties) {
		super(itemProperties);
	}

	@Override
	public String getModelPath() {
		String modelFile = getRegistryName().getPath();
		String dir = getModelSubdir();
		if (!dir.isEmpty()) {
			if (modelFile.startsWith(String.format("%s_", dir))) {
				modelFile = modelFile.substring(dir.length() + 1);
			}
			return String.format("%s/%s", dir, modelFile).replaceAll("/+", "/");
		}
		return modelFile;
	}

	public String getModelSubdir() {
		return "";
	}

	public int getModelCount() {
		return 1;
	}

	@Nonnull
	@Override
	public String getTranslationKey(@Nonnull ItemStack stack) {
//		if (getHasSubtypes()) {
//			return String.format("%s.%d", super.getTranslationKey(stack), stack.getMetadata());
//		}
		return super.getTranslationKey(stack);
	}

	/**
	 * Adds all keys from the translation file in the format:
	 * item.className.tip([0-9]*) Tips start from 1 and increment. Sparse rows
	 * should be left empty (ie empty line must still have a key present) Shift
	 * shows full tooltip, without it you just get the first line.
	 */
	@Override
	@OnlyIn(Dist.CLIENT)
	public void addInformation(@Nonnull ItemStack stack, @Nullable World worldIn, @Nonnull List<ITextComponent> tooltip, @Nonnull ITooltipFlag flagIn) {
		super.addInformation(stack, worldIn, tooltip, flagIn);
//		if (addShiftInfo()) {
//			TextUtil.addTooltipInformation(stack, tooltip, Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));
//		}
	}

	public boolean addShiftInfo() {
		return true;
	}

}
