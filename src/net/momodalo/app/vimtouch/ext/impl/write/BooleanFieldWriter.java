package net.momodalo.app.vimtouch.ext.impl.write;

import net.momodalo.app.vimtouch.ext.FieldWriter;
import net.momodalo.app.vimtouch.ext.OutgoingTransfer;
import net.momodalo.app.vimtouch.ext.Transferable.FieldType;

public class BooleanFieldWriter implements FieldWriter<Boolean> {

	@Override
	public FieldType getType() {
		return FieldType.Boolean;
	}

	@Override
	public void write(OutgoingTransfer t, Boolean value) {
		t.getBuffer().append(value ? 1 : 0);
		t.getBuffer().append(',');
	}

}
