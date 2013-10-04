package net.momodalo.app.vimtouch.ext;

import net.momodalo.app.vimtouch.ext.Transferable.FieldType;

public interface FieldReader<T> {

	public FieldType getType();

	public T read(IncomingTransfer t) throws FieldReaderException;

	public void set(T value);
}
