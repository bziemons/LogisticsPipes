package logisticspipes.request.resources;

import java.util.BitSet;

import net.minecraft.item.ItemStack;

import com.google.common.base.Objects;

import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

public class DictResource extends ItemResource {

	//match all items with same oredict name
	public boolean use_od = false;
	//match all items with same id
	public boolean ignore_dmg = false;
	//match all items with same id and damage
	public boolean ignore_nbt = false;
	//match all items with same oredict prefix
	public boolean use_category = false;

	public DictResource(ItemIdentifierStack stack) {
		super(stack);
	}

	public DictResource(DictResource previousResource, ItemIdentifierStack stack) {
		super(stack);
		use_od = previousResource.use_od;
		ignore_dmg = previousResource.ignore_dmg;
		ignore_nbt = previousResource.ignore_nbt;
		use_category = previousResource.use_category;
	}

	public DictResource(LPDataInput input) {
		super(input);
		loadFromBitSet(input.readBitSet());
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeBitSet(getBitSet());
	}

	public boolean matches(ItemIdentifier other) {
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
	public Object getCCType() {
		return ccObject;
	}

	@Override
	public void setCCType(Object type) {
		ccObject = type;
	}

	public void loadFromBitSet(BitSet bits) {
		use_od = bits.get(0);
		ignore_dmg = bits.get(1);
		ignore_nbt = bits.get(2);
		use_category = bits.get(3);
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
