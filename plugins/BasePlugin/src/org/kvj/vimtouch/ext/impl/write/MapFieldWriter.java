package org.kvj.vimtouch.ext.impl.write;

import java.util.Map;
import java.util.Map.Entry;

import org.kvj.vimtouch.ext.FieldWriter;
import org.kvj.vimtouch.ext.OutgoingTransfer;
import org.kvj.vimtouch.ext.Transferable.FieldType;

public class MapFieldWriter<T> implements FieldWriter<Map<String, T>> {

	private FieldWriter<T> itemWriter;
	private StringFieldWriter nameWriter = new StringFieldWriter();

	public MapFieldWriter(FieldWriter<T> itemWriter) {
		this.itemWriter = itemWriter;
	}

	@Override
	public FieldType getType() {
		return FieldType.Map;
	}

	@Override
	public void write(OutgoingTransfer t, Map<String, T> value) {
		t.writeInt(value.size());
		for (Entry<String, T> item : value.entrySet()) {
			t.writeField(null, item.getKey(), nameWriter);
			t.writeField(null, item.getValue(), itemWriter);
		}
	}

}
