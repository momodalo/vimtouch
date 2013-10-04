package net.momodalo.app.vimtouch.ext.impl.write;

import net.momodalo.app.vimtouch.ext.FieldWriter;
import net.momodalo.app.vimtouch.ext.OutgoingTransfer;
import net.momodalo.app.vimtouch.ext.Transferable;
import net.momodalo.app.vimtouch.ext.Transferable.FieldType;

public class TransferableFieldWriter implements FieldWriter<Transferable> {

	@Override
	public FieldType getType() {
		return FieldType.Transferable;
	}

	@Override
	public void write(OutgoingTransfer t, Transferable value) {
		// for (Package p : value.getClass().getPackage().) {
		// t.getBuffer().append(p.getName().charAt(0));
		// t.getBuffer().append('.');
		// }
		t.writeStr(value.getClass().getSimpleName());
		value.writeTo(t);
		t.endWrite();
	}

}
