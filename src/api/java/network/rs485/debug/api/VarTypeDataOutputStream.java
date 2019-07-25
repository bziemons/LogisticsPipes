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

package network.rs485.debug.api;

import java.io.DataOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import static network.rs485.debug.api.DataClass.VarType;

public class VarTypeDataOutputStream extends DataOutputStream {

	private HashMap<VarType, Integer> hashCodes = new HashMap<VarType, Integer>();

	/**
	 * Creates a new data output stream to write data to the specified
	 * underlying output stream. The counter <code>written</code> is
	 * set to zero.
	 *
	 * @param out the underlying output stream, to be saved for later
	 *            use.
	 * @see FilterOutputStream#out
	 */
	public VarTypeDataOutputStream(OutputStream out) {
		super(out);
	}

	public void writeVarType(VarType var) throws IOException {
		writeBoolean(var != null);
		if (var == null) return;
		if (hashCodes.containsKey(var)) {
			writeBoolean(false);
			writeInt(hashCodes.get(var));
		} else {
			writeBoolean(true);
			int hash = var.hashCode();
			hashCodes.put(var, hash);
			writeInt(hash);
			writeInt(var.getClass().getAnnotation(DataID.class).value());
			writeVarType(var.getParent());
			var.writeData(this);
		}
	}

	public void writeIntegerArray(Integer[] array) throws IOException {
		writeInt(array.length);
		for (int i = 0; i < array.length; i++) {
			writeInt(array[i]);
		}
	}

	public <T, V> void writeMap(Map<T, V> map, IPartWriter<T> part1, IPartWriter<V> part2) throws IOException {
		for (Map.Entry<T, V> e : map.entrySet()) {
			writeBoolean(true);
			part1.writeObject(this, e.getKey());
			part2.writeObject(this, e.getValue());
		}
		writeBoolean(false);
	}

	public void writeStringArray(String[] array) throws IOException {
		writeInt(array.length);
		for (int i = 0; i < array.length; i++) {
			writeUTF(array[i]);
		}
	}
}
