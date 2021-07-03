package logisticspipes.logic;

import java.util.UUID;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;

import lombok.Getter;

public abstract class BaseLogicTask {

	//Graphical Interface
	@Getter
	protected int posX;
	@Getter
	protected int posY;
	@Getter
	protected String name = getTypeName();
	@Getter
	protected String comment = "";

	//Saving and Server/Client sync
	@Getter
	protected UUID uuid;

	public BaseLogicTask(CompoundNBT nbt) {
		posX = nbt.getInt("posX");
		posY = nbt.getInt("posY");
		name = nbt.getString("name");
		comment = nbt.getString("comment");
		uuid = UUID.fromString(nbt.getString("uuid"));
	}

	public BaseLogicTask(int posX, int posY) {
		this.posX = posX;
		this.posY = posY;
		uuid = UUID.randomUUID();
	}

	public final CompoundNBT getCompoundNBT() {
		CompoundNBT nbt = new CompoundNBT();
		addToNBT(nbt);
		return nbt;
	}

	protected void addToNBT(CompoundNBT nbt) {
		nbt.putInt("posX", posX);
		nbt.putInt("posY", posY);
		nbt.putString("name", name);
		nbt.putString("comment", comment);
		nbt.putString("uuid", uuid.toString());
	}

	public abstract int getAmountOfInput();

	public abstract int getAmountOfOutput();

	public abstract LogicParameterType getInputParameterType(int i);

	public abstract LogicParameterType getOutputParameterType(int i);

	public abstract void setInputParameter(int i, Object value);

	public abstract boolean isCalculated();

	public abstract Object getResult(int i);

	public abstract void resetState();

	public abstract String getTypeName();

	public abstract void syncTick(TileEntity tile);
}
