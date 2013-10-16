package org.kvj.vimtouch.ext.impl.read;

import org.kvj.vimtouch.ext.FieldReader;
import org.kvj.vimtouch.ext.FieldReaderException;
import org.kvj.vimtouch.ext.IncomingTransfer;
import org.kvj.vimtouch.ext.Transferable.FieldType;

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
