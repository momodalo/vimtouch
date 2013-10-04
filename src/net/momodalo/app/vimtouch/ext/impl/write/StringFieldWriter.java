package net.momodalo.app.vimtouch.ext.impl.write;

import net.momodalo.app.vimtouch.ext.FieldWriter;
import net.momodalo.app.vimtouch.ext.OutgoingTransfer;
import net.momodalo.app.vimtouch.ext.Transferable.FieldType;

public class StringFieldWriter implements FieldWriter<String> {

	@Override
	public FieldType getType() {
		return FieldType.String;
	}

	@Override
	public void write(OutgoingTransfer t, String value) {
		t.writeStr(value);
	}

}
