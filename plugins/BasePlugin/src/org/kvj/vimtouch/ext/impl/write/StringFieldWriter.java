package org.kvj.vimtouch.ext.impl.write;

import org.kvj.vimtouch.ext.FieldWriter;
import org.kvj.vimtouch.ext.OutgoingTransfer;
import org.kvj.vimtouch.ext.Transferable.FieldType;

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
