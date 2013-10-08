package net.momodalo.app.vimtouch.ext.impl.read;

import net.momodalo.app.vimtouch.ext.FieldReader;
import net.momodalo.app.vimtouch.ext.FieldReaderException;
import net.momodalo.app.vimtouch.ext.IncomingTransfer;
import net.momodalo.app.vimtouch.ext.Transferable.FieldType;

public abstract class BooleanFieldReader implements FieldReader<Boolean> {

	@Override
	public FieldType getType() {
		return FieldType.Boolean;
	}

	@Override
	public Boolean read(IncomingTransfer t) throws FieldReaderException {
		int value = t.nextInt();
		return value != 0;
	}

}
