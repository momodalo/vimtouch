package org.kvj.vimtouch.ext.impl.read;

import org.kvj.vimtouch.ext.FieldReader;
import org.kvj.vimtouch.ext.FieldReaderException;
import org.kvj.vimtouch.ext.IncomingTransfer;
import org.kvj.vimtouch.ext.Transferable.FieldType;

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
