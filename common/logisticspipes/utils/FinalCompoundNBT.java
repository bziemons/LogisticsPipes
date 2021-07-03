package logisticspipes.utils;

import java.io.DataInput;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.NBTSizeTracker;

import lombok.SneakyThrows;

public class FinalCompoundNBT extends CompoundNBT {

	private boolean constructing;

	public FinalCompoundNBT(CompoundNBT base) {
		super();
		constructing = true;
		super.merge(base);
		constructing = false;
	}

	@Override
	@Nonnull
	public Set<String> keySet() {
		return Collections.unmodifiableSet(super.keySet());
	}

	@Override
	public void putBoolean(@Nonnull String key, boolean value) {
		if (constructing) super.putBoolean(key, value);
	}

	@Override
	public void putByte(@Nonnull String key, byte value) {
		if (constructing) super.putByte(key, value);
	}

	@Override
	public void putByteArray(@Nonnull String key, @Nonnull byte[] value) {
		if (constructing) super.putByteArray(key, value);
	}

	@Override
	public void putDouble(@Nonnull String key, double value) {
		if (constructing) super.putDouble(key, value);
	}

	@Override
	public void putFloat(@Nonnull String key, float value) {
		if (constructing) super.putFloat(key, value);
	}

	@Override
	public void putIntArray(@Nonnull String key, @Nonnull int[] value) {
		if (constructing) super.putIntArray(key, value);
	}

	@Override
	public void putInt(@Nonnull String key, int value) {
		if (constructing) super.putInt(key, value);
	}

	@Override
	public void putLong(@Nonnull String key, long value) {
		if (constructing) super.putLong(key, value);
	}

	@Override
	public void putShort(@Nonnull String key, short value) {
		if (constructing) super.putShort(key, value);
	}

	@Override
	public void putString(@Nonnull String key, @Nonnull String value) {
		if (constructing) super.putString(key, value);
	}

	@Override
	public void putIntArray(@Nonnull String key, @Nonnull List<Integer> value) {
		if (constructing) super.putIntArray(key, value);
	}

	@Override
	public void putLongArray(@Nonnull String key, @Nonnull long[] value) {
		if (constructing) super.putLongArray(key, value);
	}

	@Override
	public void putLongArray(@Nonnull String key, @Nonnull List<Long> value) {
		if (constructing) super.putLongArray(key, value);
	}

	@Nullable
	@Override
	public INBT put(@Nonnull String key, @Nonnull INBT p_218657_2_) {
		return constructing ? super.put(key, p_218657_2_) : null;
	}

	@Override
	public void putUniqueId(@Nonnull String key, @Nonnull UUID value) {
		if (constructing) super.putUniqueId(key, value);
	}

	@SneakyThrows
	@Override
	@Nonnull
	public CompoundNBT merge(@Nonnull CompoundNBT other) {
		return constructing ? super.merge(other) : this;
	}

	@Override
	public void read(@Nonnull DataInput input, int depth, @Nonnull NBTSizeTracker sizeTracker) throws IOException {
		if (constructing) super.read(input, depth, sizeTracker);
	}
}
