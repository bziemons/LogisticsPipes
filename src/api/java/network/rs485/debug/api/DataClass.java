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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataClass {

	public static Class<? extends VarType> getClassForId(int id) {
		for (Class<?> c : DataClass.class.getDeclaredClasses()) {
			if (c.getAnnotation(DataID.class) != null && c.getAnnotation(DataID.class).value() == id) {
				return (Class<? extends VarType>) c;
			}
		}
		return null;
	}

	@DataID(1)
	public static abstract class VarType {

		private static final long serialVersionUID = -8013762524305750292L;
		String name;
		private transient DebugTreeWidgetItem node;

		private VarType parent;

		public VarType(VarType parent) {
			this.parent = parent;
		}

		public VarType getParent() {
			return parent;
		}

		public void setParent(VarType parent) {
			this.parent = parent;
		}

		public List<Integer> trace() {
			return new ArrayList<Integer>();
		}

		@Override
		public String toString() {
			return this.getClass().getSimpleName() + "{" +
					"name='" + name + '\'' +
					", parent=" + parent +
					'}';
		}

		public void writeData(VarTypeDataOutputStream data) throws IOException {
			data.writeUTF(String.valueOf(name));
		}

		public void read(VarTypeDataInputStream data) throws IOException, InstantiationException, IllegalAccessException {
			name = data.readUTF();
		}

		public DebugTreeWidgetItem setNode(DebugTreeWidgetItem node) {
			if (this.node != null) {
				if (this.node.aboutToBeReplacedBy(node)) {
					this.node = node;
				}
			} else {
				this.node = node;
			}
			return this.node;
		}
	}

	@DataID(2)
	public static class BasicVarType extends VarType {

		private static final long serialVersionUID = 4416098847612633006L;
		String watched;

		public BasicVarType(VarType parent) {
			super(parent);
		}

		public String getWatched() {
			return watched;
		}

		public void writeData(VarTypeDataOutputStream data) throws IOException {
			super.writeData(data);
			data.writeUTF(watched);
		}

		public void read(VarTypeDataInputStream data) throws IOException, IllegalAccessException, InstantiationException {
			super.read(data);
			watched = data.readUTF();
		}
	}

	@DataID(3)
	public static class NullVarType extends VarType {

		private static final long serialVersionUID = -3673576543767748173L;

		public NullVarType(VarType parent) {
			super(parent);
		}
	}

	@DataID(4)
	public static class ExtendedVarType extends VarType {

		private static final long serialVersionUID = -5243734594523844526L;
		Map<Integer, FieldPart> objectType = new HashMap<Integer, FieldPart>();
		Map<Integer, MethodPart> methodType = new HashMap<Integer, MethodPart>();
		transient WeakReference<Object> watched;
		boolean extended;
		String typeName;

		public ExtendedVarType(VarType parent) {
			super(parent);
		}

		public WeakReference<Object> getWatched() {
			return watched;
		}

		public void writeData(VarTypeDataOutputStream data) throws IOException {
			super.writeData(data);
			data.writeMap(objectType, DataOutputStream::writeInt, VarTypeDataOutputStream::writeVarType);
			data.writeMap(methodType, DataOutputStream::writeInt, VarTypeDataOutputStream::writeVarType);
			data.writeBoolean(extended);
			data.writeUTF(typeName);
		}

		public void read(VarTypeDataInputStream data) throws IOException, IllegalAccessException, InstantiationException {
			super.read(data);
			objectType = data.readMap(DataInputStream::readInt, data1 -> (FieldPart) data1.readVarType());
			methodType = data.readMap(DataInputStream::readInt, data12 -> (MethodPart) data12.readVarType());
			extended = data.readBoolean();
			typeName = data.readUTF();
		}
	}

	@DataID(5)
	public static class ArrayVarType extends VarType {

		private static final long serialVersionUID = -6335674162049738144L;
		public String typeName;
		Map<Integer, ArrayFieldVarType> objectType = new HashMap<Integer, ArrayFieldVarType>();
		transient WeakReference<Object> watched;

		public ArrayVarType(VarType parent) {
			super(parent);
		}

		public WeakReference<Object> getWatched() {
			return watched;
		}

		public void writeData(VarTypeDataOutputStream data) throws IOException {
			super.writeData(data);
			data.writeMap(objectType, DataOutputStream::writeInt, VarTypeDataOutputStream::writeVarType);
			data.writeUTF(typeName);
		}

		public void read(VarTypeDataInputStream data) throws IOException, IllegalAccessException, InstantiationException {
			super.read(data);
			objectType = data.readMap(DataInputStream::readInt, data1 -> (ArrayFieldVarType) data1.readVarType());
			typeName = data.readUTF();
		}
	}

	@DataID(6)
	public static abstract class PositionedVarType extends VarType {

		private static final long serialVersionUID = -1685367418049733144L;
		public Integer i;

		public PositionedVarType(VarType parent) {
			super(parent);
		}

		public void writeData(VarTypeDataOutputStream data) throws IOException {
			super.writeData(data);
			data.writeInt(i);
		}

		public void read(VarTypeDataInputStream data) throws IOException, InstantiationException, IllegalAccessException {
			super.read(data);
			i = data.readInt();
		}
	}

	@DataID(7)
	public static class ArrayFieldVarType extends PositionedVarType {

		private static final long serialVersionUID = -1643567412049738144L;
		public VarType content;
		private ArrayVarType parent;

		public ArrayFieldVarType(ArrayVarType parent) {
			super(parent);
			this.parent = parent;
		}

		@Override
		public ArrayVarType getParent() {
			return parent;
		}

		@Override
		public void setParent(VarType parent) {
			super.setParent(parent);
			this.parent = (ArrayVarType) parent;
		}

		public List<Integer> trace() {
			ArrayList<Integer> list = new ArrayList<Integer>();
			list.add(i);
			return list;
		}

		public void writeData(VarTypeDataOutputStream data) throws IOException {
			super.writeData(data);
			data.writeVarType(content);
		}

		public void read(VarTypeDataInputStream data) throws IOException, InstantiationException, IllegalAccessException {
			super.read(data);
			content = data.readVarType();
		}
	}

	@DataID(8)
	public static class FieldPart extends PositionedVarType {

		private static final long serialVersionUID = 3891140976403054537L;
		VarType type;
		transient Field field;
		private ExtendedVarType parent;

		public FieldPart(ExtendedVarType parent) {
			super(parent);
			this.parent = parent;
		}

		@Override
		public ExtendedVarType getParent() {
			return parent;
		}

		@Override
		public void setParent(VarType parent) {
			super.setParent(parent);
			this.parent = (ExtendedVarType) parent;
		}

		public List<Integer> trace() {
			ArrayList<Integer> list = new ArrayList<Integer>();
			list.add(0);
			list.add(i);
			return list;
		}

		public void writeData(VarTypeDataOutputStream data) throws IOException {
			super.writeData(data);
			data.writeVarType(type);
		}

		public void read(VarTypeDataInputStream data) throws IOException, InstantiationException, IllegalAccessException {
			super.read(data);
			type = data.readVarType();
		}
	}

	@DataID(9)
	public static class MethodPart extends PositionedVarType {

		private static final long serialVersionUID = -5704715096482038636L;
		String[] param;
		transient Method method;
		transient Object watched;
		VarType lastResult;
		private ExtendedVarType parent;

		public MethodPart(ExtendedVarType parent) {
			super(parent);
			this.parent = parent;
		}

		@Override
		public ExtendedVarType getParent() {
			return parent;
		}

		@Override
		public void setParent(VarType parent) {
			super.setParent(parent);
			this.parent = (ExtendedVarType) parent;
		}

		public Object getWatched() {
			return watched;
		}

		public Method getMethod() {
			return method;
		}

		public List<Integer> trace() {
			ArrayList<Integer> list = new ArrayList<Integer>();
			list.add(1);
			list.add(i);
			return list;
		}

		public void writeData(VarTypeDataOutputStream data) throws IOException {
			super.writeData(data);
			data.writeStringArray(param);
			data.writeVarType(lastResult);
		}

		public void read(VarTypeDataInputStream data) throws IOException, IllegalAccessException, InstantiationException {
			super.read(data);
			param = data.readStringArray();
			lastResult = data.readVarType();
		}
	}
}
