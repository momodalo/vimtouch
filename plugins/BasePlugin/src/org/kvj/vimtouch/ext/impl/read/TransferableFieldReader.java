package org.kvj.vimtouch.ext.impl.read;

import org.kvj.vimtouch.ext.FieldReader;
import org.kvj.vimtouch.ext.FieldReaderException;
import org.kvj.vimtouch.ext.IncomingTransfer;
import org.kvj.vimtouch.ext.Transferable;
import org.kvj.vimtouch.ext.Transferable.FieldType;

abstract public class TransferableFieldReader<T extends Transferable>
		implements FieldReader<T> {

	@Override
	public FieldType getType() {
		return FieldType.Transferable;
	}

	@Override
	public T read(IncomingTransfer t) throws FieldReaderException {
		String className = t.nextString();
		T object = create();
		IncomingTransfer it = new IncomingTransfer(t.getData());
		it.setIndex(t.getIndex());
		object.readFrom(it);
		it.read();
		t.setIndex(it.getIndex());
		return object;
	}

	abstract public T create();

}
