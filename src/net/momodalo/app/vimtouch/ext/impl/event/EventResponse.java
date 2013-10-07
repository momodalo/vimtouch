package net.momodalo.app.vimtouch.ext.impl.event;

import net.momodalo.app.vimtouch.ext.IncomingTransfer;
import net.momodalo.app.vimtouch.ext.OutgoingTransfer;
import net.momodalo.app.vimtouch.ext.Transferable;

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
