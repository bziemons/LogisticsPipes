package logisticspipes.items;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

public class ItemParts extends LogisticsItem {

	public ItemParts() {
		super();
		setHasSubtypes(true);
	}

	@Override
	public int getModelCount() {
		return 4;
	}

	@Override
	public String getModelSubdir() {
		return "parts";
	}

	@Override
	public void getSubItems(@Nonnull ItemGroup group, @Nonnull NonNullList<ItemStack> items) {
		if (isInGroup(group)) {
			items.add(new ItemStack(this, 1, 0));
			items.add(new ItemStack(this, 1, 1));
			items.add(new ItemStack(this, 1, 2));
			items.add(new ItemStack(this, 1, 3));
		}
	}

}
