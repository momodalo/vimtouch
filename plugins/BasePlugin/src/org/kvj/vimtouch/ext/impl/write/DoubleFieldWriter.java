package org.kvj.vimtouch.ext.impl.write;

import org.kvj.vimtouch.ext.FieldWriter;
import org.kvj.vimtouch.ext.OutgoingTransfer;
import org.kvj.vimtouch.ext.Transferable.FieldType;

public class DoubleFieldWriter implements FieldWriter<Double> {

	@Override
	public FieldType getType() {
		return FieldType.Double;
	}

	@Override
	public void write(OutgoingTransfer t, Double value) {
		t.getBuffer().append(value.toString());
		t.getBuffer().append(',');
	}

}
