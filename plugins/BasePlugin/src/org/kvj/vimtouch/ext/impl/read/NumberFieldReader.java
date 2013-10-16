package org.kvj.vimtouch.ext.impl.read;

import org.kvj.vimtouch.ext.FieldReader;
import org.kvj.vimtouch.ext.FieldReaderException;
import org.kvj.vimtouch.ext.IncomingTransfer;
import org.kvj.vimtouch.ext.Transferable.FieldType;

public abstract class NumberFieldReader implements FieldReader<Number> {

	@Override
	public FieldType getType() {
		return FieldType.Number;
	}

	@Override
	public Number read(IncomingTransfer t) throws FieldReaderException {
		String value = t.nextPiece();
		try {
			return Long.parseLong(value, 10);
		} catch (Exception e) {
		}
		throw new FieldReaderException("Invalid number: " + value);
	}

}
