package org.kvj.vimtouch.ext.impl.read;

import org.kvj.vimtouch.ext.FieldReader;
import org.kvj.vimtouch.ext.FieldReaderException;
import org.kvj.vimtouch.ext.IncomingTransfer;
import org.kvj.vimtouch.ext.Transferable.FieldType;

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
