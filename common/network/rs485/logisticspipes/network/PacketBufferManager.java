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
import java.util.function.Consumer;

import network.rs485.logisticspipes.network.exception.FaultyPacketException;

import lombok.AccessLevel;
import lombok.Getter;

final class PacketBufferManager {

	@Getter(AccessLevel.PACKAGE)
	private static final PacketBufferManager instance = new PacketBufferManager();

	@Getter(AccessLevel.PACKAGE)
	private PacketBufferMapAccessor outPacketBuffer;
	@Getter(AccessLevel.PACKAGE)
	private PacketBufferMapAccessor inPacketBuffer;

	private PacketBufferManager() {
		outPacketBuffer = new PacketBufferMapAccessor();
		inPacketBuffer = new PacketBufferMapAccessor();
	}

	class PacketBufferMapAccessor {

		private HashMap<Class, PacketBuffer> packetBufferMap = new HashMap<>();

		@SuppressWarnings("unchecked")
		private <T extends AbstractPacket> PacketBuffer<T> getOrMake(Class<T> clazz) {
			PacketBuffer<T> packetBuffer = packetBufferMap.get(clazz);
			if (packetBuffer == null) {
				packetBuffer = new PacketBuffer<>();
				packetBufferMap.put(clazz, packetBuffer);
			}
			return packetBuffer;
		}

		public <T extends AbstractPacket> int getBufferSize(Class<T> clazz) {
			return getOrMake(clazz).size();
		}

		public <T extends AbstractPacket> void resizeBuffer(Class<T> clazz, int newSize) {
			getOrMake(clazz).resize(newSize);
		}

		public <T extends AbstractPacket> boolean isEmpty(Class<T> clazz) {
			return getOrMake(clazz).isEmpty();
		}

		public <T extends AbstractPacket> boolean isFull(Class<T> clazz) {
			return getOrMake(clazz).isFull();
		}

		public <T extends AbstractPacket> void writePacketForClass(Class<T> clazz, Consumer<T> packetWriter) {
			PacketBuffer<T> packetBuffer = getOrMake(clazz);
			while (true) {
				if (!packetBuffer.isFull()) break;
			}

			T packet = packetBuffer.getNextWritePacket();
			if (packet == null) {
				try {
					packet = clazz.newInstance();
				} catch (InstantiationException | IllegalAccessException e) {
					throw new FaultyPacketException("Exception when instantiating packet " + clazz.getSimpleName(), e);
				}
				packetBuffer.setWritePacket(packet);
			}

			// reset packet
			packet.reset();
			// write to packet
			packetWriter.accept(packet);
			// move pointers
			packetBuffer.nextFill();
		}

		public <T extends AbstractPacket> void readPacketForClass(Class<T> clazz, Consumer<T> packetConsumer) {
			PacketBuffer<T> packetBuffer = getOrMake(clazz);
			while (true) {
				if (!packetBuffer.isEmpty()) break;
			}

			// get packet
			T packet = packetBuffer.getNextReadPacket();
			// read from packet
			packetConsumer.accept(packet);
			// move pointers
			packetBuffer.nextRead();
		}
	}
}
