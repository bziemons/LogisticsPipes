package logisticspipes.items;

import java.util.Collection;
import javax.annotation.Nonnull;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import com.google.common.collect.Lists;

import logisticspipes.LogisticsPipes;
import logisticspipes.api.IHUDArmor;
import logisticspipes.interfaces.ILogisticsItem;

public class ItemHUDArmor extends ArmorItem implements IHUDArmor, ILogisticsItem {

	public ItemHUDArmor() {
		super(ArmorMaterial.LEATHER, EquipmentSlotType.HEAD, new Item.Properties().group(LogisticsPipes.LP_ITEM_GROUP));
	}

	@Nonnull
	@Override
	public ActionResult<ItemStack> onItemRightClick(@Nonnull World world, @Nonnull PlayerEntity player, @Nonnull Hand hand) {
		ItemStack stack = player.getHeldItem(hand);
		if (world.isRemote) {
			useItem((ServerPlayerEntity) player, world);
			return new ActionResult<>(ActionResultType.SUCCESS, stack);
		}
		return new ActionResult<>(ActionResultType.PASS, stack);
	}

	@Nonnull
	@Override
	public ItemStack onItemUseFinish(@Nonnull ItemStack stack, @Nonnull World world, @Nonnull LivingEntity livingEntity) {
		if (world.isRemote) {
			useItem((ServerPlayerEntity) livingEntity, world);
		}
		return stack;
	}

	private void useItem(@Nonnull ServerPlayerEntity player, @Nonnull World world) {
		// player.openGui(LogisticsPipes.instance, GuiIDs.GUI_HUD_Settings, world, player.inventory.currentItem, -1, 0);
	}

	@Nonnull
	@Override
	public Collection<ItemGroup> getCreativeTabs() {
		// is visible in the LP creative tab and the ItemArmor creative tab
		return Lists.newArrayList(getGroup(), LogisticsPipes.LP_ITEM_GROUP);
	}

	@Override
	public boolean isEnabled(@Nonnull ItemStack item) {
		return true;
	}

	@Override
	public String getArmorTexture(@Nonnull ItemStack stack, Entity entity, EquipmentSlotType slot, String type) {
		return "logisticspipes:textures/armor/LogisticsHUD_1.png";
	}

	@Nonnull
	@Override
	public ITextComponent getDisplayName(@Nonnull ItemStack stack) {
		return new TranslationTextComponent(stack.getTranslationKey() + ".name");
	}

}
