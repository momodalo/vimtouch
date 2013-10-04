package net.momodalo.app.vimtouch.ext.impl.read;

import net.momodalo.app.vimtouch.ext.FieldReader;
import net.momodalo.app.vimtouch.ext.FieldReaderException;
import net.momodalo.app.vimtouch.ext.IncomingTransfer;
import net.momodalo.app.vimtouch.ext.Transferable;
import net.momodalo.app.vimtouch.ext.Transferable.FieldType;

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
