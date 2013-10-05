package net.momodalo.app.vimtouch.ext.impl;

import net.momodalo.app.vimtouch.ext.IncomingTransfer;
import net.momodalo.app.vimtouch.ext.OutgoingTransfer;
import net.momodalo.app.vimtouch.ext.Transferable;

public class EmptyTransferable implements Transferable {

	@Override
	public void readFrom(IncomingTransfer t) {
	}

	@Override
	public void writeTo(OutgoingTransfer t) {
	}

}
