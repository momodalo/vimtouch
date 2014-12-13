package org.kvj.vimtouch.ext.impl.event;

import org.kvj.vimtouch.ext.IncomingTransfer;
import org.kvj.vimtouch.ext.OutgoingTransfer;
import org.kvj.vimtouch.ext.Transferable;

public class EventResponse implements Transferable {

	private int subscription;

	public EventResponse(int subscription) {
		this.subscription = subscription;
	}

	@Override
	public void readFrom(IncomingTransfer t) {
	}

	@Override
	public void writeTo(OutgoingTransfer t) {
		t.writeNumber("subscription", subscription);
	}
}
