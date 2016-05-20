/*
 * Copyright (c) 2016  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 *
 * This file can instead be distributed under the license terms of the
 * MIT license:
 *
 * Copyright (c) 2016  RS485
 *
 * This MIT license was reworded to only match this file. If you use the regular
 * MIT license in your project, replace this copyright notice (this line and any
 * lines below and NOT the copyright line above) with the lines from the original
 * MIT license located here: http://opensource.org/licenses/MIT
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this file and associated documentation files (the "Source Code"), to deal in
 * the Source Code without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Source Code, and to permit persons to whom the Source Code is furnished
 * to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Source Code, which also can be
 * distributed under the MIT.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package network.rs485.logisticspipes.packet;

import java.io.IOException;
import java.util.concurrent.locks.Condition;

import net.minecraft.entity.player.EntityPlayer;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.packets.BufferTransfer;
import logisticspipes.proxy.MainProxy;
import network.rs485.logisticspipes.util.LPDataIOWrapper;

@SideOnly(Side.CLIENT)
public class ClientCompressorRunnable extends Compressor implements CompressorRunnable {
	private final Condition pauseCondition = lock.newCondition();
	private boolean pause = false;

	@Override
	public void run() {
		clear();
		BufferTransfer compressedPacket = PacketHandler.getPacket(BufferTransfer.class);

		try {
			while (true) {
				lock.lock();
				try {
					while (pause) pauseCondition.await();
					while (!newData) newDataCondition.await();
					while (pause) pauseCondition.await();

					newData = false;
				} catch (InterruptedException e) {
					e.printStackTrace();
					return;
				} finally {
					lock.unlock();
				}

				try {
					compressAndProvide(compressedData -> {
						compressedPacket.setContent(compressedData);
						MainProxy.sendPacketToServer(compressedPacket);
					});
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
			}
		} finally {
			syncBuffer.release();
			syncBuffer = null;
		}
	}

	@Override
	public void setPause(boolean pause) {
		lock.lock();
		try {
			this.pause = pause;
			pauseCondition.signal();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void outgoingPacket(ModernPacket packet, EntityPlayer ignored) {
		testBufferInitialized();
		syncBuffer.writerAccess(buffer -> {
			int packetLengthIndex = buffer.writerIndex();
			buffer.writeInt(0);

			LPDataIOWrapper.writeData(buffer, output -> {
				output.writeShort(packet.getId());
				output.writeInt(packet.getDebugId());
				packet.writeData(output);
			});

			int afterPacketIndex = buffer.writerIndex();
			buffer.writerIndex(packetLengthIndex);
			buffer.writeInt(afterPacketIndex - packetLengthIndex - Integer.BYTES);
			buffer.writerIndex(afterPacketIndex);
		});
		signalNewData();
	}
}
