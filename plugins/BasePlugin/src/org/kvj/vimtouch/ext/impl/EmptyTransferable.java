package org.kvj.vimtouch.ext.impl;

import org.kvj.vimtouch.ext.IncomingTransfer;
import org.kvj.vimtouch.ext.OutgoingTransfer;
import org.kvj.vimtouch.ext.Transferable;

public class EmptyTransferable implements Transferable {

	@Override
	public void readFrom(IncomingTransfer t) {
	}

	@Override
	public void writeTo(OutgoingTransfer t) {
	}

}
