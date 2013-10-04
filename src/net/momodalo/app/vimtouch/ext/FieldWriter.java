package net.momodalo.app.vimtouch.ext;

import net.momodalo.app.vimtouch.ext.Transferable.FieldType;


public interface FieldWriter<T> {

	public FieldType getType();

	public void write(OutgoingTransfer t, T value);
}
