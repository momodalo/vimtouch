package net.momodalo.app.vimtouch.ext.impl.read;

import net.momodalo.app.vimtouch.ext.FieldReader;
import net.momodalo.app.vimtouch.ext.FieldReaderException;
import net.momodalo.app.vimtouch.ext.IncomingTransfer;
import net.momodalo.app.vimtouch.ext.Transferable.FieldType;

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
