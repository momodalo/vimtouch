package org.kvj.vimtouch.ext.impl.write;

import org.kvj.vimtouch.ext.FieldWriter;
import org.kvj.vimtouch.ext.OutgoingTransfer;
import org.kvj.vimtouch.ext.Transferable.FieldType;

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
