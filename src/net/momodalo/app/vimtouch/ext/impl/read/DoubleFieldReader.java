package net.momodalo.app.vimtouch.ext.impl.read;

import net.momodalo.app.vimtouch.ext.FieldReader;
import net.momodalo.app.vimtouch.ext.FieldReaderException;
import net.momodalo.app.vimtouch.ext.IncomingTransfer;
import net.momodalo.app.vimtouch.ext.Transferable.FieldType;

public abstract class DoubleFieldReader implements FieldReader<Double> {

	@Override
	public FieldType getType() {
		return FieldType.Double;
	}

	@Override
	public Double read(IncomingTransfer t) throws FieldReaderException {
		String value = t.nextPiece();
		try {
			return Double.parseDouble(value);
		} catch (Exception e) {
		}
		throw new FieldReaderException("Invalid double: " + value);
	}

}
