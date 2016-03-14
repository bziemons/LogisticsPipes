package logisticspipes.network.packets.debuggui;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.ticks.DebugGuiTickHandler;
import logisticspipes.ticks.DebugGuiTickHandler.VarType;

import net.minecraft.entity.player.EntityPlayer;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(chain = true)
public class DebugInfoUpdate extends AbstractPacket {

	@Getter
	@Setter
	private Integer[] path;

	@Getter
	@Setter
	private VarType information;

	public DebugInfoUpdate(int id) {
		super(id);
	}

	@Override
	public void readData(LPDataInput data) throws IOException {
		ByteArrayInputStream bis = new ByteArrayInputStream(data.readLengthAndBytes());
		ObjectInput in = new ObjectInputStream(bis);
		try {
			information = (VarType) in.readObject();
		} catch (ClassNotFoundException e) {
			throw new UnsupportedOperationException(e);
		}
		int size = data.readInt();
		path = new Integer[size];
		for (int i = 0; i < size; i++) {
			path[i] = data.readInt();
		}
	}

	@Override
	public void processPacket(EntityPlayer player) {
		try {
			DebugGuiTickHandler.instance().handleContentUpdatePacket(path, getInformation());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void writeData(LPDataOutput data) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = new ObjectOutputStream(bos);
		out.writeObject(getInformation());
		byte[] bytes = bos.toByteArray();
		data.writeLengthAndBytes(bytes);
		data.writeInt(path.length);
		for (Integer element : path) {
			data.writeInt(element);
		}
	}

	@Override
	public AbstractPacket template() {
		return new DebugInfoUpdate(getId());
	}

	@Override
	public boolean isCompressable() {
		return true;
	}
}
