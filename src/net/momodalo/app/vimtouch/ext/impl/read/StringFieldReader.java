package net.momodalo.app.vimtouch.ext.impl.read;

import net.momodalo.app.vimtouch.ext.FieldReader;
import net.momodalo.app.vimtouch.ext.FieldReaderException;
import net.momodalo.app.vimtouch.ext.IncomingTransfer;
import net.momodalo.app.vimtouch.ext.Transferable.FieldType;

abstract public class StringFieldReader implements FieldReader<String> {

	@Override
	public FieldType getType() {
		return FieldType.String;
	}

	@Override
	public String read(IncomingTransfer t) throws FieldReaderException {
		return t.nextString();
	}

}
