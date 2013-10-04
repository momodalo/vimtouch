package net.momodalo.app.vimtouch.ext.impl.write;

import java.util.List;

import net.momodalo.app.vimtouch.ext.FieldWriter;
import net.momodalo.app.vimtouch.ext.OutgoingTransfer;
import net.momodalo.app.vimtouch.ext.Transferable.FieldType;

public class ListFieldWriter<T> implements FieldWriter<List<T>> {

	private FieldWriter<T> itemWriter;

	public ListFieldWriter(FieldWriter<T> itemWriter) {
		this.itemWriter = itemWriter;
	}

	@Override
	public FieldType getType() {
		return FieldType.List;
	}

	@Override
	public void write(OutgoingTransfer t, List<T> value) {
		t.writeInt(value.size());
		for (T item : value) {
			t.writeField(null, item, itemWriter);
		}
	}
}
