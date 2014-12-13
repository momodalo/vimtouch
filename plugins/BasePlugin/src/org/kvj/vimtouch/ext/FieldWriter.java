package org.kvj.vimtouch.ext;

import org.kvj.vimtouch.ext.Transferable.FieldType;


public interface FieldWriter<T> {

	public FieldType getType();

	public void write(OutgoingTransfer t, T value);
}
