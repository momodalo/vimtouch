package net.momodalo.app.vimtouch.ext.impl.event;

import net.momodalo.app.vimtouch.ext.IncomingTransfer;
import net.momodalo.app.vimtouch.ext.OutgoingTransfer;
import net.momodalo.app.vimtouch.ext.Transferable;
import net.momodalo.app.vimtouch.ext.impl.read.NumberFieldReader;

public class EventRequest implements Transferable {

	private int subscription = 0;

	@Override
	public void readFrom(IncomingTransfer t) {
		t.readAs("subscription", new NumberFieldReader() {
			@Override
			public void set(Number value) {
				subscription = value.intValue();
			}
		});
	}

	@Override
	public void writeTo(OutgoingTransfer t) {
	}

	public int getSubscription() {
		return subscription;
	}

}
