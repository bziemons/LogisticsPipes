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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.buffer.ByteBuf;
import static io.netty.buffer.Unpooled.buffer;
import static io.netty.buffer.Unpooled.wrappedBuffer;
import lombok.Getter;

final class Sender implements Runnable {

	@Getter
	private static final Sender instance = new Sender();

	private Thread compressionThread;
	private ConcurrentHashMap<Target, ByteBuf> uncompressedBytes;
	private ConcurrentHashMap<Target, ByteBuf> compressedBytes;
	private HashMap<Target, ByteBuf> sendBytes;

	private Sender() {
		uncompressedBytes = new ConcurrentHashMap<>();
		compressedBytes = new ConcurrentHashMap<>();
		sendBytes = new HashMap<>();

		compressionThread = new Thread(this, "LP Packet Compression");
		compressionThread.setDaemon(true);
		compressionThread.start();
	}

	@Override
	public void run() {

	}

	/**
	 * Sends all packets from the queue.
	 */
	public void send() {
		ByteBuf buffer;
		for (Entry<Target, ByteBuf> e : sendBytes.entrySet()) {
			e.getKey().sendBytes(e.getValue());
		}
	}

	/**
	 * Converts the packet into bytes and adds it to a queue or sends it now.
	 *
	 * @param debugTrace the debug trace. Will be written to the packet, if not null
	 */
	public <T extends AbstractPacket> void packet(Target target, T packet, StackTraceElement[] debugTrace, boolean sendNow) {
		// create buf
		ByteBuf buf = buffer(packet.getApproximateSize());

		if (sendNow) {
			// prepend header for sending directly
			writeHeader(buf, false);
		}
		int pastHeaderIdx = buf.writerIndex();

		// write initial size
		buf.writeInt(0);
		int dataStartIdx = buf.writerIndex();

		// write data
		if (debugTrace != null) {
			packet.writeDebug(buf, debugTrace);
		} else {
			packet.write(buf);
		}

		// overwrite data length after header
		int dataLength = buf.writerIndex() - dataStartIdx;
		buf.writerIndex(pastHeaderIdx);
		buf.writeInt(dataLength);

		if (sendNow) {
			// send packet directly
			target.sendBytes(buf);
		} else {
			// determine buf map
			Map<Target, ByteBuf> bytesMap;
			if (compressionThread.isAlive() && packet.shouldBeCompressed()) {
				bytesMap = uncompressedBytes;
			} else {
				bytesMap = sendBytes;
			}

			if (bytesMap.containsKey(target)) {
				// appends packet bytes to exiting buffers in map
				ByteBuf existingBuffer = bytesMap.get(target);
				bytesMap.put(target, wrappedBuffer(existingBuffer, buf));
			} else {
				// puts buf in the map until it is released and removed
				bytesMap.put(target, wrappedBuffer(buf));
			}
		}
	}

	/**
	 * Resets the writer index and writes the header from the marked index to the given buffer.
	 */
	private void writeHeader(ByteBuf buffer, boolean isCompressed) {
		buffer.resetWriterIndex();
		buffer.writeBoolean(isCompressed);
	}
}
