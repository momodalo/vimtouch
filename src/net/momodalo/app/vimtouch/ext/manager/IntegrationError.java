package net.momodalo.app.vimtouch.ext.manager;

import net.momodalo.app.vimtouch.ext.IncomingTransfer;
import net.momodalo.app.vimtouch.ext.OutgoingTransfer;
import net.momodalo.app.vimtouch.ext.Transferable;
import net.momodalo.app.vimtouch.ext.impl.read.StringFieldReader;

public class IntegrationError implements Transferable {

	private String message = null;

	public IntegrationError() {
	}

	public IntegrationError(String message) {
		this.message = message;
	}

	@Override
	public void readFrom(IncomingTransfer t) {
		t.readAs("error", new StringFieldReader() {

			@Override
			public void set(String value) {
				message = value;
			}
		});
	}

	@Override
	public void writeTo(OutgoingTransfer t) {
		t.writeString("error", message);
	}

	public String getMessage() {
		return message;
	}

}
