package logisticspipes.items;

import javax.annotation.Nonnull;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import logisticspipes.LogisticsPipes;
import logisticspipes.blocks.BlockDummy;
import logisticspipes.blocks.LogisticsSolidBlock;
import logisticspipes.interfaces.ILogisticsItem;

public class LogisticsSolidBlockItem extends BlockItem implements ILogisticsItem {

	private final LogisticsSolidBlock.Type type;

	public LogisticsSolidBlockItem(LogisticsSolidBlock block) {
		super(block, new Item.Properties().group(LogisticsPipes.LP_ITEM_GROUP));
		type = block.getType();
		BlockDummy.updateItemMap.put(type.getMeta(), this);
	}

	@Nonnull
	@Override
	public ITextComponent getDisplayName(@Nonnull ItemStack stack) {
		return new TranslationTextComponent(getTranslationKey(stack) + ".name");
	}

	public LogisticsSolidBlock.Type getType() {
		return this.type;
	}
}
