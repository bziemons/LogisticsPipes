/*
 * Copyright (c) 2019  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 *
 * This file can instead be distributed under the license terms of the
 * MIT license:
 *
 * Copyright (c) 2019  RS485
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

package network.rs485.debug;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.FutureTask;

import lombok.SneakyThrows;

import network.rs485.debug.api.IDataConnection;
import network.rs485.debug.api.IObjectIdentification;
import network.rs485.debug.api.IServerDebugGuiEntry;
import network.rs485.debug.api.ObjectHandler;

public class ServerDebugGuiEntry extends IServerDebugGuiEntry {

	private static ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<Runnable>();
	private static ArrayList<ObjectHandler> objects = new ArrayList<ObjectHandler>();

	@Override
	public IDataConnection startServerDebugging(Object object, IDataConnection outgoingData, IObjectIdentification objectIdent) throws IOException {
		ObjectHandler handler = new ObjectHandler(object, outgoingData, objectIdent, this);
		objects.add(handler);
		return handler;
	}

	@Override
	public void remove(ObjectHandler objectHandler) {
		objects.remove(objectHandler);
	}

	@SneakyThrows
	public void exec() {
		while (!queue.isEmpty()) {
			Runnable poll = queue.poll();
			poll.run();
			if (poll instanceof FutureTask) {
				((FutureTask) poll).get();
			}
		}
		for (ObjectHandler handler : objects) {
			handler.run();
		}
		while (!queue.isEmpty()) {
			Runnable poll = queue.poll();
			poll.run();
			if (poll instanceof FutureTask) {
				((FutureTask) poll).get();
			}
		}
	}
}
