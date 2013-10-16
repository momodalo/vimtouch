package org.kvj.vimtouch.ext.impl.write;

import org.kvj.vimtouch.ext.FieldWriter;
import org.kvj.vimtouch.ext.OutgoingTransfer;
import org.kvj.vimtouch.ext.Transferable.FieldType;

public class NumberFieldWriter implements FieldWriter<Number> {

	@Override
	public FieldType getType() {
		return FieldType.Number;
	}

	@Override
	public void write(OutgoingTransfer t, Number value) {
		t.writeInt(value.intValue());
	}

}
