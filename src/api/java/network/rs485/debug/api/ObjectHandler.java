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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static network.rs485.debug.api.DataClass.ArrayFieldVarType;
import static network.rs485.debug.api.DataClass.ArrayVarType;
import static network.rs485.debug.api.DataClass.BasicVarType;
import static network.rs485.debug.api.DataClass.ExtendedVarType;
import static network.rs485.debug.api.DataClass.FieldPart;
import static network.rs485.debug.api.DataClass.MethodPart;
import static network.rs485.debug.api.DataClass.NullVarType;
import static network.rs485.debug.api.DataClass.VarType;

public class ObjectHandler implements IDataConnection {

	private final IServerDebugGuiEntry parent;
	private Object object;
	private IDataConnection outgoingData;
	private IObjectIdentification objectIdent;
	private VarType var;
	private OutputStream stream;
	private VarTypeDataOutputStream dataOut;

	public ObjectHandler(Object object, final IDataConnection outgoingData, IObjectIdentification objectIdent, IServerDebugGuiEntry parent) throws IOException {
		this.object = object;
		this.outgoingData = outgoingData;
		this.objectIdent = objectIdent;
		this.parent = parent;
		stream = new ByteArrayOutputStream() {

			@Override
			public void flush() throws IOException {
				outgoingData.passData(this.toByteArray());
				this.reset();
			}
		};
		dataOut = new VarTypeDataOutputStream(stream);
		var = resolveType(object, null, object.getClass().getSimpleName(), true, null);
		sendStructureUpdate(new Integer[] {}, var);
	}

	private VarType getVarForPath(Integer[] path) {
		VarType pos = var;
		for (int i = 0; i < path.length; i++) {
			if (pos instanceof ExtendedVarType) {
				if (i + 1 < path.length) {
					if (path[i] == 0) {
						pos = ((ExtendedVarType) pos).objectType.get(path[i + 1]).type;
					} else if (path[i] == 1) {
						VarType r = null;
						for (int j = 0; j < ((ExtendedVarType) pos).methodType.size(); j++) {
							if (((ExtendedVarType) pos).methodType.get(j).i.equals(path[i + 1])) {
								r = ((ExtendedVarType) pos).methodType.get(j).lastResult;
								break;
							}
						}
						if (r == null) {
							new Exception(
									"List unsorted for some reason. Accessing " + pos.name + " at i=" + i + ". Closing gui. (" + Arrays.toString(path) + ")")
									.printStackTrace();
							return null;
						}
						pos = r;
					}
				}
				i++;
			} else if (pos instanceof ArrayVarType) {
				pos = ((ArrayVarType) pos).objectType.get(path[i]).content;
			} else {
				new Exception("List unsorted for some reason. Accessing '" + (pos != null ? pos.name + "' / " + pos.getClass() : "null'") + " at i=" + i
						+ ". Closing gui. (" + Arrays.toString(path) + ")").printStackTrace();
				return null;
			}
		}
		return pos;
	}

	private void handleVarChangePacket(Integer[] path, String content) {
		VarType pos = getVarForPath(path);
		if (pos == null) return;
		if (pos.getParent() instanceof FieldPart) {
			FieldPart f = (FieldPart) pos.getParent();
			try {
				f.field.set(f.getParent().watched.get(), getCasted(f.field.getType(), content));
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (pos.getParent() instanceof ArrayFieldVarType) {
			ArrayFieldVarType a = (ArrayFieldVarType) pos.getParent();
			try {
				setArray(a.getParent().watched.get(), path[path.length - 1], getCasted(a.getParent().watched.get().getClass().getComponentType(), content));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private Object getCasted(Class<?> clazz, String content) {
		if (clazz == int.class || clazz == Integer.class) {
			return Integer.valueOf(content);
		} else if (clazz == byte.class || clazz == Byte.class) {
			return Byte.valueOf(content);
		} else if (clazz == short.class || clazz == Short.class) {
			return Short.valueOf(content);
		} else if (clazz == long.class || clazz == Long.class) {
			return Long.valueOf(content);
		} else if (clazz == float.class || clazz == Float.class) {
			return Float.valueOf(content);
		} else if (clazz == double.class || clazz == Double.class) {
			return Double.valueOf(content);
		} else if (clazz == char.class || clazz == Character.class) {
			return content.charAt(0);
		} else if (clazz == boolean.class || clazz == Boolean.class) {
			return Boolean.valueOf(content);
		} else if (clazz == UUID.class) {
			return UUID.fromString(content);
		} else {
			System.out.println("What type is that: " + clazz.getSimpleName() + "?");
		}
		return content;
	}

	private void executeMethod(Integer[] path) throws IOException {
		VarType pos = getVarForPath(path);
		if (pos == null) return;
		if (pos.getParent() instanceof MethodPart) {
			MethodPart method = (MethodPart) pos.getParent();
			Object obj = method.getParent().getWatched().get();
			String result;
			try {
				Object oResult = method.getMethod().invoke(obj);
				//method.setWatched(oResult);
				method.lastResult = resolveType(oResult, method.lastResult, "result", true, method);
				sendStructureUpdate(path, getVarForPath(path));
				result = String.valueOf(oResult);
			} catch (InvocationTargetException e) {
				e.printStackTrace();
				result = e.getTargetException().getClass() + ", Message: " + e.getTargetException().getMessage();
			} catch (Exception e) {
				e.printStackTrace();
				result = "Internal Exception";
			}
			updateCallResult(path, result);
		}
	}

	private void expandGuiAt(Integer[] tree) throws IOException {
		VarType pos = getVarForPath(tree);
		VarType prevPos = pos.getParent();
		if (pos == null) return;
		if (pos instanceof ExtendedVarType) {
			((ExtendedVarType) pos).extended = true;
			pos = resolveType(((ExtendedVarType) pos).watched.get(), pos, ((ExtendedVarType) pos).name, true, prevPos);
			if (prevPos != null) {
				if (prevPos instanceof ExtendedVarType) {
					((ExtendedVarType) prevPos).objectType.get(tree[tree.length - 1]).type = pos;
				} else if (prevPos instanceof ArrayVarType) {
					((ArrayVarType) prevPos).objectType.get(tree[tree.length - 1]).content = pos;
				} else if (prevPos instanceof FieldPart) {
					((FieldPart) prevPos).type = pos;
				} else if (prevPos instanceof ArrayFieldVarType) {
					((ArrayFieldVarType) prevPos).content = pos;
				}
			} else {
				var = pos;
			}
		}
		sendStructureUpdate(tree, pos);
	}

	private boolean isPrimitive(Class<?> clazz) {
		return clazz == Integer.class || clazz == Boolean.class || clazz == Double.class || clazz == Float.class || clazz == Long.class || clazz == UUID.class
				|| clazz == Byte.class || clazz == String.class;
	}

	private VarType resolveType(Object toInspect, VarType prev, String name, boolean extended, VarType parent) {
		if (toInspect == null) {
			NullVarType type = new NullVarType(parent);
			type.name = name;
			return type;
		}
		Class<?> clazz = toInspect.getClass();
		if (objectIdent.handleObject(toInspect) != null) {
			BasicVarType type = new BasicVarType(parent);
			type.name = name;
			type.watched = "(" + clazz.getSimpleName() + "): " + objectIdent.handleObject(toInspect);
			return type;
		}
		if (clazz.isPrimitive() || isPrimitive(clazz) || objectIdent.toStringObject(toInspect)) {
			BasicVarType type = new BasicVarType(parent);
			type.name = name;
			type.watched = toInspect.toString();
			return type;
		}
		if (clazz.isArray()) {
			ArrayVarType type = new ArrayVarType(parent);
			type.watched = new WeakReference<Object>(toInspect);
			type.name = name;
			type.typeName = clazz.getSimpleName();
			Object[] array = getArray(type.watched.get());
			for (int i = 0; i < array.length; i++) {
				Object o = array[i];
				VarType tmp = null;
				if (prev instanceof ArrayVarType) {
					tmp = ((ArrayVarType) prev).objectType.get(i);
				}
				ArrayFieldVarType field = new ArrayFieldVarType(type);
				field.content = resolveType(o, tmp, i + ": ", false, field);
				field.i = i;
				type.objectType.put(i, field);
			}
			return type;
		}
		ExtendedVarType type = new ExtendedVarType(parent);
		type.watched = new WeakReference<Object>(toInspect);
		type.name = name;
		type.extended = extended;
		type.typeName = clazz.getSimpleName();
		if (prev instanceof ExtendedVarType) {
			type.extended = ((ExtendedVarType) prev).extended;
		}
		if (type.extended) {
			int field = 0;
			int method = 0;
			while (!clazz.equals(Object.class)) {
				try {
					Field[] fields = clazz.getDeclaredFields();
					for (int i = 0; i < fields.length; i++) {
						Field f = fields[i];
						try {
							f.setAccessible(true);
							Object content = f.get(toInspect);
							VarType tmp = null;
							if (prev instanceof ExtendedVarType) {
								FieldPart part = ((ExtendedVarType) prev).objectType.get(i);
								if (part != null) {
									tmp = part.type;
								}
							}
							FieldPart fieldPart = new FieldPart(type);
							VarType subType = resolveType(content, tmp, f.getName(), false, fieldPart);
							fieldPart.field = f;
							fieldPart.name = f.getName();
							fieldPart.type = subType;
							fieldPart.i = field;
							type.objectType.put(field++, fieldPart);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				} catch (NoClassDefFoundError e) {
					e.printStackTrace(); //Don't crash if there is a Field of an unknown type
				}
				try {
					Method[] methods = clazz.getDeclaredMethods();
					for (Method m : methods) {
						try {
							m.setAccessible(true);
							MethodPart methodPart = new MethodPart(type);
							methodPart.lastResult = new NullVarType(methodPart);
							//methodPart.lastResult.setName("result");
							methodPart.method = m;
							methodPart.name = m.getName();
							methodPart.i = method;
							List<String> params = new ArrayList<String>();
							for (Class<?> par : m.getParameterTypes()) {
								params.add(par.getSimpleName());
							}
							methodPart.param = params.toArray(new String[] {});
							type.methodType.put(method++, methodPart);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				} catch (NoClassDefFoundError e) {
					e.printStackTrace(); //Don't crash if there is a Method with an unknown type
				}
				clazz = clazz.getSuperclass();
			}
		}
		return type;
	}

	private Object[] getArray(Object val) {
		if (val instanceof Object[]) {
			return (Object[]) val;
		}
		int arrlength = Array.getLength(val);
		if (arrlength > 10000) { //Limit to 10000 to avoid high system load (More being displayed also can't be handled by the user)
			arrlength = 10000;
		}
		Object[] outputArray = new Object[arrlength];
		for (int i = 0; i < arrlength; ++i) {
			outputArray[i] = Array.get(val, i);
		}
		return outputArray;
	}

	private void setArray(Object val, Integer pos, Object casted) {
		if (val instanceof Object[]) {
			((Object[]) val)[pos] = casted;
		} else {
			Array.set(val, pos, casted);
		}
	}

	private VarType handleUpdate(VarType type, LinkedList<Integer> path, Object newObject, VarType parent) throws IOException {
		boolean isModified = false;
		if (type instanceof BasicVarType) {
			BasicVarType bType = (BasicVarType) type;
			if (bType.watched != null && newObject != null) {
				String s = objectIdent.handleObject(newObject);
				if (s == null) {
					s = newObject.toString();
				} else {
					s = "(" + newObject.getClass().getSimpleName() + "): " + s;
				}
				if (!bType.watched.equals(s)) {
					isModified = true;
					bType.watched = s;
				}
			} else if (newObject == null) {
				isModified = true;
			}
			if (isModified) {
				VarType newType = resolveType(newObject, type, type.name, true, parent);
				if (newType.getClass() == type.getClass()) {
					type = newType;
					Integer[] integers = path.toArray(new Integer[] {});
					updateContent(type, integers);
				} else {
					type = newType;
					sendStructureUpdate(path.toArray(new Integer[] {}), type);
				}
			}
			return bType;
		} else if (type instanceof NullVarType) {
			if (newObject != null) {
				type = resolveType(newObject, type, type.name, true, parent);
				sendStructureUpdate(path.toArray(new Integer[] {}), type);
			}
			return type;
		} else if (type instanceof ArrayVarType) {
			ArrayVarType aType = (ArrayVarType) type;
			if (aType.watched.get() != null) {
				if (aType.watched.get() != newObject) {
					isModified = true;
				}
			} else if (newObject != null) {
				aType.watched = new WeakReference<Object>(newObject);
				isModified = true;
			}
			if (!isModified) {
				for (int i = 0; i < aType.objectType.size(); i++) {
					ArrayFieldVarType fieldVarType = aType.objectType.get(i);
					path.addLast(fieldVarType.i);
					fieldVarType.content = handleUpdate(fieldVarType.content, path, getArray(aType.watched.get())[i], aType);
					path.removeLast();
				}
			} else {
				type = resolveType(newObject, type, type.name, true, parent);
				sendStructureUpdate(path.toArray(new Integer[] {}), type);
			}
			return type;
		} else if (type instanceof ExtendedVarType) {
			ExtendedVarType eType = (ExtendedVarType) type;
			if (eType.watched != null) {
				if (eType.watched.get() != null) {
					if (!eType.watched.get().equals(newObject)) {
						eType.watched = new WeakReference<Object>(newObject);
						isModified = true;
					}
				}
			}
			if (newObject != null && !isModified) {
				for (int i = 0; i < eType.objectType.size(); i++) {
					FieldPart part = eType.objectType.get(i);
					Object content = null;
					try {
						content = part.field.get(newObject);
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
						isModified = true;
					} catch (IllegalAccessException e) {
						e.printStackTrace();
						isModified = true;
					}
					path.addLast(0);
					path.addLast(part.i);
					part.type = handleUpdate(part.type, path, content, part);
					path.removeLast();
					path.removeLast();
				}
				for (int i = 0; i < eType.methodType.size(); i++) {
					MethodPart part = eType.methodType.get(i);
					Object content = null;
					if (part.lastResult instanceof ExtendedVarType) {
						content = ((ExtendedVarType) part.lastResult).getWatched().get();
					} else if (part.lastResult instanceof ArrayVarType) {
						content = ((ArrayVarType) part.lastResult).getWatched().get();
					}
					path.addLast(1);
					path.addLast(part.i);
					part.lastResult = handleUpdate(part.lastResult, path, content, part);
					path.removeLast();
					path.removeLast();
				}
			} else {
				type = resolveType(newObject, type, type.name, true, parent);
				sendStructureUpdate(path.toArray(new Integer[] {}), type);
			}
			return type;
		} else {
			System.out.println("Unknown Type: " + type);
			return null;
		}
	}

	private void sendStructureUpdate(Integer[] path, VarType vatThis) throws IOException {
		dataOut.writeShort(1);
		dataOut.writeIntegerArray(path);
		dataOut.writeVarType(vatThis);
		dataOut.flush();
	}

	private void updateContent(VarType type, Integer[] integers) throws IOException {
		dataOut.writeShort(2);
		dataOut.writeIntegerArray(integers);
		dataOut.writeVarType(type);
		dataOut.flush();
	}

	private void updateCallResult(Integer[] path, String result) throws IOException {
		dataOut.writeShort(3);
		dataOut.writeIntegerArray(path);
		dataOut.writeUTF(result);
		dataOut.flush();
	}

	@Override
	public void passData(byte[] packet) throws IOException {
		VarTypeDataInputStream input = new VarTypeDataInputStream(new ByteArrayInputStream(packet));
		short id = input.readShort();
		switch (id) {
			case 1: // Expansion Event
				Integer[] path1 = input.readIntegerArray();
				expandGuiAt(path1);
				break;
			case 2:
				Integer[] path2 = input.readIntegerArray();
				String s = input.readUTF();
				handleVarChangePacket(path2, s);
				break;
			case 3: //Close
				closeCon();
				break;
			case 4: //Close
				Integer[] path3 = input.readIntegerArray();
				executeMethod(path3);
				break;
			default:
				System.out.println("Error got packet ID: " + id);
		}
	}

	@Override
	public void closeCon() throws IOException {
		parent.remove(this);
		outgoingData.closeCon();
	}

	public void run() {
		try {
			var = handleUpdate(var, new LinkedList<Integer>(), object, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
