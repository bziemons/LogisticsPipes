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

package network.rs485.logisticspipes.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class SynchronizedByteBuf {

	private final ByteBuf buffer;
	private final ReentrantLock writerLock = new ReentrantLock();
	private int writerIndex;

	public SynchronizedByteBuf(int initialCapacity, int maxCapacity) {
		buffer = Unpooled.buffer(initialCapacity, maxCapacity);
		writerIndex = buffer.writerIndex();
	}

	public void writerAccess(Consumer<ByteBuf> writerViewConsumer) {
		ByteBuf duplo = buffer.duplicate();
		writerLock.lock();
		try {
			duplo.writerIndex(writerIndex);
			writerViewConsumer.accept(duplo);
			writerIndex = duplo.writerIndex();
		} finally {
			writerLock.unlock();
		}
	}

	public boolean writeToOutputStream(OutputStream outputStream, int maxBytes) throws IOException {
		// Read index synchronization is done via the buffer's read index

		if (buffer.refCnt() == 0) {
			throw new IllegalStateException("Wrong thread to access the buffer");
		}

		boolean more = false;

		writerLock.lock();
		try {
			buffer.writerIndex(writerIndex);
			if (buffer.readableBytes() > maxBytes) {
				more = true;
			}
			buffer.readBytes(outputStream, Math.min(buffer.readableBytes(), maxBytes));
			buffer.discardReadBytes();
			writerIndex = buffer.writerIndex();
		} finally {
			writerLock.unlock();
		}
		return more;
	}

	public void release() {
		buffer.release();
	}
}
