package net.momodalo.app.vimtouch.ext.impl.write;

import net.momodalo.app.vimtouch.ext.FieldWriter;
import net.momodalo.app.vimtouch.ext.OutgoingTransfer;
import net.momodalo.app.vimtouch.ext.Transferable.FieldType;

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
