package org.kvj.vimtouch.ext.impl.event;


import org.kvj.vimtouch.ext.IncomingTransfer;
import org.kvj.vimtouch.ext.OutgoingTransfer;
import org.kvj.vimtouch.ext.Transferable;
import org.kvj.vimtouch.ext.impl.read.NumberFieldReader;

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
