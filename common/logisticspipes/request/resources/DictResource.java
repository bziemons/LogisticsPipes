package logisticspipes.request.resources;

import java.util.BitSet;

import net.minecraft.item.ItemStack;

import com.google.common.base.Objects;

import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.string.ChatColor;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

public class DictResource implements IResource {

	public ItemIdentifierStack stack;
	//match all items with same oredict name
	public boolean use_od = false;
	//match all items with same id
	public boolean ignore_dmg = false;
	//match all items with same id and damage
	public boolean ignore_nbt = false;
	//match all items with same oredict prefix
	public boolean use_category = false;
	private Object ccObject;

	public DictResource(ItemIdentifierStack stack) {
		this.stack = stack;
	}

	public DictResource(LPDataInput input) {
		stack = input.readItemIdentifierStack();
		BitSet bits = input.readBitSet();
		use_od = bits.get(0);
		ignore_dmg = bits.get(1);
		ignore_nbt = bits.get(2);
		use_category = bits.get(3);
	}

	@Override
	public void writeData(LPDataOutput output) {
		output.writeItemIdentifierStack(stack);
		BitSet bits = new BitSet();
		bits.set(0, use_od);
		bits.set(1, ignore_dmg);
		bits.set(2, ignore_nbt);
		bits.set(3, use_category);
		output.writeBitSet(bits);
	}

	@Override
	public ItemIdentifier getAsItem() {
		return stack.getItem();
	}

	@Override
	public int getRequestedAmount() {
		return stack.getStackSize();
	}

	@Override
	public boolean matches(ItemIdentifier other, MatchSettings settings) {
		if (use_od || use_category) {
			if (stack.getItem().getDictIdentifiers() != null && other.getDictIdentifiers() != null) {
				if (stack.getItem().getDictIdentifiers().canMatch(other.getDictIdentifiers(), true, use_category)) {
					return true;
				}
			}
		}
		ItemStack stack_n = stack.makeNormalStack();
		ItemStack other_n = other.makeNormalStack(1);
		if (stack_n.getItem() != other_n.getItem()) {
			return false;
		}
		if (stack_n.getItemDamage() != other_n.getItemDamage()) {
			if (stack_n.getHasSubtypes()) {
				return false;
			} else if (!ignore_dmg) {
				return false;
			}
		}
		if (ignore_nbt) {
			return true;
		}
		if (stack_n.hasTagCompound() ^ other_n.hasTagCompound()) {
			return false;
		}
		if (!stack_n.hasTagCompound() && !other_n.hasTagCompound()) {
			return true;
		}
		return ItemStack.areItemStackTagsEqual(stack_n, other_n);
	}

	@Override
	public IResource clone(int multiplier) {
		ItemIdentifierStack stack = this.stack.clone();
		stack.setStackSize(stack.getStackSize() * multiplier);
		DictResource clone = new DictResource(stack);
		clone.use_od = use_od;
		clone.ignore_dmg = ignore_dmg;
		clone.ignore_nbt = ignore_nbt;
		clone.use_category = use_category;
		return clone;
	}

	public DictResource clone() {
		DictResource clone = new DictResource(this.stack.clone());
		clone.use_od = use_od;
		clone.ignore_dmg = ignore_dmg;
		clone.ignore_nbt = ignore_nbt;
		clone.use_category = use_category;
		return clone;
	}

	public ItemIdentifier getItem() {
		return stack.getItem();
	}

	public ItemIdentifierStack getItemStack() {
		return stack;
	}

	@Override
	public Object getCCType() {
		return ccObject;
	}

	@Override
	public void setCCType(Object type) {
		ccObject = type;
	}

	@Override
	public String getDisplayText(ColorCode code) {
		StringBuilder builder = new StringBuilder();
		builder.append(ChatColor.GRAY);
		builder.append("{");
		if (code != ColorCode.NONE) {
			builder.append(code == ColorCode.MISSING ? ChatColor.RED : ChatColor.GREEN);
		}
		builder.append(stack.getFriendlyName());
		if (code != ColorCode.NONE) {
			builder.append(ChatColor.GRAY);
		}
		builder.append(" [");
		builder.append(use_od ? ChatColor.GREEN : ChatColor.RED);
		builder.append("OreDict");
		builder.append(ChatColor.GRAY);
		builder.append(", ");
		builder.append(use_category ? ChatColor.GREEN : ChatColor.RED);
		builder.append("OreCat");
		builder.append(ChatColor.GRAY);
		builder.append(", ");
		builder.append(ignore_dmg ? ChatColor.GREEN : ChatColor.RED);
		builder.append("IgnDmg");
		builder.append(ChatColor.GRAY);
		builder.append(", ");
		builder.append(ignore_nbt ? ChatColor.GREEN : ChatColor.RED);
		builder.append("IgnNBT");
		builder.append(ChatColor.GRAY);
		return builder.append("]}").toString();
	}

	@Override
	public ItemIdentifierStack getDisplayItem() {
		return stack;
	}

	public DictResource loadFromBitSet(BitSet bits) {
		use_od = bits.get(0);
		ignore_dmg = bits.get(1);
		ignore_nbt = bits.get(2);
		use_category = bits.get(3);
		return this;
	}

	public BitSet getBitSet() {
		BitSet bits = new BitSet();
		bits.set(0, use_od);
		bits.set(1, ignore_dmg);
		bits.set(2, ignore_nbt);
		bits.set(3, use_category);
		return bits;
	}

	public Identifier getIdentifier() {
		return new Identifier();
	}

	public class Identifier {

		private ItemIdentifier getItem() {
			return stack.getItem();
		}

		private BitSet getBitSet() {
			return DictResource.this.getBitSet();
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(getItem(), getBitSet());
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Identifier) {
				Identifier id = (Identifier) obj;
				return id.getItem().equals(getItem()) && id.getBitSet().equals(getBitSet());
			}
			return false;
		}
	}
}
