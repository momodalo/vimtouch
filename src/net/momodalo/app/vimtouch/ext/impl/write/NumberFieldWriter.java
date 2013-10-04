package net.momodalo.app.vimtouch.ext.impl.write;

import net.momodalo.app.vimtouch.ext.FieldWriter;
import net.momodalo.app.vimtouch.ext.OutgoingTransfer;
import net.momodalo.app.vimtouch.ext.Transferable.FieldType;

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
