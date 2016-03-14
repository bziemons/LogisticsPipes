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

import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;

class PacketBuffer<T extends AbstractPacket> {

	public static final int INITIAL_SIZE = 2;

	private int fillIdx;
	private int readIdx;
	private AtomicReferenceArray<T> buffer;
	private ReentrantLock resizeLock;

	PacketBuffer() {
		fillIdx = 0;
		readIdx = -1;
		buffer = new AtomicReferenceArray<>(INITIAL_SIZE);
		resizeLock = new ReentrantLock();
	}

	public boolean isEmpty() {
		return readIdx == -1;
	}

	public boolean isFull() {
		return fillIdx == -1;
	}

	public int size() {
		return buffer.length();
	}

	public void resize(int newSize) {
		resizeLock.lock();
		try {
			final int size = size();
			if (newSize != size && newSize > 1) {
				AtomicReferenceArray<T> temp = new AtomicReferenceArray<>(newSize);
				final int min = Math.min(size, newSize);
				for (int i = 0; i < min; i++) {
					// copy references
					temp.set(i, buffer.get(i));
				}

				// replace old buffer array
				buffer = temp;

				if (newSize < size) {
					if (fillIdx < readIdx) {
						if (readIdx >= newSize) nextRead(false);
						if (fillIdx >= newSize) nextFill(false);
					} else {
						if (fillIdx >= newSize) nextFill(false);
						if (readIdx >= newSize) nextRead(false);
					}
				} else {
					if (fillIdx < readIdx) {
						fillIdx = size;
					}
				}
			}
		} finally {
			resizeLock.unlock();
		}
	}

	public T getNextReadPacket() {
		if (isEmpty()) throw new UnsupportedOperationException("Cannot read, because nothing is there");

		return buffer.get(readIdx);
	}

	public T getNextWritePacket() {
		if (isFull()) throw new UnsupportedOperationException("Cannot write, because buffer is full");

		return buffer.get(fillIdx);
	}

	/**
	 * Only necessary, if writeNextPacket returns null
	 */
	public void setWritePacket(T packet) {
		if (isFull()) throw new UnsupportedOperationException("Cannot set packet, because buffer is full");

		buffer.set(fillIdx, packet);
	}

	public void nextRead() {
		assert isEmpty() != isFull();
		nextRead(true);
	}

	private void nextRead(boolean safe) {
		if (fillIdx == -1) {
			fillIdx = readIdx;
		}

		readIdx++;

		if (safe) {
			resizeLock.lock();
			try {
				if (readIdx >= buffer.length()) {
					readIdx = 0;
				}
			} finally {
				resizeLock.unlock();
			}
		} else {
			if (readIdx >= buffer.length()) {
				readIdx = 0;
			}
		}

		if (readIdx == fillIdx) {
			// buffer is empty
			readIdx = -1;
		}
	}

	public void nextFill() {
		assert isEmpty() != isFull();
		nextFill(true);
	}

	private void nextFill(boolean safe) {
		if (readIdx == -1) {
			readIdx = fillIdx;
		}

		fillIdx++;

		if (safe) {
			resizeLock.lock();
			try {
				if (fillIdx >= buffer.length()) {
					fillIdx = 0;
				}
			} finally {
				resizeLock.unlock();
			}
		} else {
			if (fillIdx >= buffer.length()) {
				fillIdx = 0;
			}
		}

		if (fillIdx == readIdx) {
			// buffer is full
			fillIdx = -1;
		}
	}
}
