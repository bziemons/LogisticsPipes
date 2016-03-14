/*
 * Copyright (c) 2015  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 *
 * This file can instead be distributed under the license terms of the MIT license:
 *
 * Copyright (c) 2015  RS485
 *
 * This MIT license was reworded to only match this file. If you use the regular MIT license in your project, replace this copyright notice (this line and any lines below and NOT the copyright line above) with the lines from the original MIT license located here: http://opensource.org/licenses/MIT
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this file and associated documentation files (the "Source Code"), to deal in the Source Code without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Source Code, and to permit persons to whom the Source Code is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Source Code, which also can be distributed under the MIT.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package network.rs485.logisticspipes.network;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import network.rs485.logisticspipes.util.LPDataWrapper;

import net.minecraft.entity.player.EntityPlayer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import lombok.Getter;

public abstract class AbstractPacket {

	public static final Map<Integer, StackTraceElement[]> DEBUG_MAP = new HashMap<>();
	private static int nextDebugId = 0;

	@Getter
	private int debugId;

	protected abstract void reset0();

	void reset() {
		reset0();
	}

	protected abstract void read0(DataInput packetInput) throws IOException;

	void read(ByteBuf packetBytes) {
		boolean withDebug = true;
		try (ByteBufInputStream byteInputStream = new ByteBufInputStream(packetBytes)) {
			withDebug = byteInputStream.readBoolean();
			if (withDebug) {
				debugId = nextDebugId++;
				ObjectInputStream objectInputStream = new ObjectInputStream(byteInputStream);
				AbstractPacket.DEBUG_MAP.put(debugId, (StackTraceElement[]) objectInputStream.readObject());
			}

			read0(byteInputStream);
		} catch (IOException | ClassNotFoundException e) {
			String postfix = "";
			if (withDebug) {
				postfix = " (with debug info)";
			}
			throw new RuntimeException("Could not read packet " + this.getClass().getSimpleName() + " from data" + postfix, e);
		}
	}

	protected abstract void write0(DataOutput packetOutput) throws IOException;

	void write(ByteBuf byteBuffer) {
		LPDataWrapper.getInstance().setByteBuf(byteBuffer);

		try (ByteBufOutputStream byteOutputStream = new ByteBufOutputStream(byteBuffer)) {
			byteOutputStream.writeBoolean(false);
			write0(byteOutputStream);
		} catch (IOException e) {
			throw new RuntimeException("Could not write packet " + this.getClass().getSimpleName() + " to byte buffer", e);
		}
	}

	void writeDebug(ByteBuf byteBuffer, StackTraceElement[] caller) {
		try (ByteBufOutputStream byteOutputStream = new ByteBufOutputStream(byteBuffer)) {
			byteOutputStream.writeBoolean(true);
			{
				ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteOutputStream);
				objectOutputStream.writeObject(caller);
				objectOutputStream.flush();
			}
			write0(byteOutputStream);
		} catch (IOException e) {
			throw new RuntimeException("Could not write packet " + this.getClass().getSimpleName() + " to byte buffer (with debug info)", e);
		}
	}

	public abstract int getApproximateSize();

	public abstract boolean isOrderImportant();

	public abstract boolean needsRetry();

	public abstract boolean shouldBeCompressed();

	public abstract void processPacket(EntityPlayer player);
}
