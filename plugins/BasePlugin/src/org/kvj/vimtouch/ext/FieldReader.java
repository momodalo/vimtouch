package org.kvj.vimtouch.ext;

import org.kvj.vimtouch.ext.Transferable.FieldType;

public interface FieldReader<T> {

	public FieldType getType();

	public T read(IncomingTransfer t) throws FieldReaderException;

	public void set(T value);
}
