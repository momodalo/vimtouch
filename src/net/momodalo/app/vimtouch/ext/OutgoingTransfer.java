package net.momodalo.app.vimtouch.ext;

import java.util.List;
import java.util.Map;

import net.momodalo.app.vimtouch.ext.Transferable.FieldType;
import net.momodalo.app.vimtouch.ext.impl.write.DoubleFieldWriter;
import net.momodalo.app.vimtouch.ext.impl.write.ListFieldWriter;
import net.momodalo.app.vimtouch.ext.impl.write.MapFieldWriter;
import net.momodalo.app.vimtouch.ext.impl.write.NumberFieldWriter;
import net.momodalo.app.vimtouch.ext.impl.write.StringFieldWriter;
import net.momodalo.app.vimtouch.ext.impl.write.TransferableFieldWriter;

public class OutgoingTransfer {

	private StringBuilder buffer = null;

	public void beginWrite() {
		buffer = new StringBuilder();
	}

	public StringBuilder getBuffer() {
		return buffer;
	}

	public void writeInt(int value) {
		buffer.append(value);
		buffer.append(',');
	}

	public void writeStr(String value) {
		writeInt(value.length());
		buffer.append(value);
		buffer.append(',');
	}

	public <T> void writeField(String name, T value, FieldWriter<T> writer) {
		if (null != name) {
			writeStr(name);
		}
		if (null == value) {
			// No value
			buffer.append(FieldType.Null.getCode());
			buffer.append(',');
			return;
		}
		buffer.append(writer.getType().getCode());
		writer.write(this, value);
	}

	public void writeString(String name, String value) {
		writeField(name, value, new StringFieldWriter());
	}

	public void writeNumber(String name, Number value) {
		writeField(name, value, new NumberFieldWriter());
	}

	public void writeDouble(String name, Double value) {
		writeField(name, value, new DoubleFieldWriter());
	}

	public <T> void writeList(String name, List<T> list, FieldWriter<T> writer) {
		writeField(name, list, new ListFieldWriter<T>(writer));
	}

	public <T> void writeMap(String name, Map<String, T> map,
			FieldWriter<T> writer) {
		writeField(name, map, new MapFieldWriter<T>(writer));
	}

	public void writeObject(String name, Transferable obj) {
		writeField(name, obj, new TransferableFieldWriter());
	}

	public void endWrite() {
		writeStr("");
	}
}
