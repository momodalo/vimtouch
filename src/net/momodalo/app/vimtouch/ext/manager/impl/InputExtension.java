package net.momodalo.app.vimtouch.ext.manager.impl;

import net.momodalo.app.vimtouch.VimTouch;
import net.momodalo.app.vimtouch.ext.IncomingTransfer;
import net.momodalo.app.vimtouch.ext.OutgoingTransfer;
import net.momodalo.app.vimtouch.ext.Transferable;
import net.momodalo.app.vimtouch.ext.impl.EmptyTransferable;
import net.momodalo.app.vimtouch.ext.impl.read.StringFieldReader;
import net.momodalo.app.vimtouch.ext.manager.IntegrationExtension;
import net.momodalo.app.vimtouch.ext.manager.IntegrationExtensionException;
import net.momodalo.app.vimtouch.ext.manager.impl.InputExtension.InputExtensionInput;

public class InputExtension implements
		IntegrationExtension<InputExtensionInput, EmptyTransferable> {

	public static class InputExtensionInput implements Transferable {

		String request = "";

		@Override
		public void readFrom(IncomingTransfer t) {
			t.readAs("request", new StringFieldReader() {

				@Override
				public void set(String value) {
					request = value;
				}
			});
		}

		@Override
		public void writeTo(OutgoingTransfer t) {
		}

	}

	protected static final String TAG = "InputExtension";

	private VimTouch vimTouch = null;

	public InputExtension(VimTouch vimTouch) {
		this.vimTouch = vimTouch;
	}

	@Override
	public String getType() {
		return "input";
	}

	@Override
	public InputExtensionInput newInput() {
		return new InputExtensionInput();
	}

	@Override
	public EmptyTransferable process(InputExtensionInput input)
			throws IntegrationExtensionException {
		if ("keyboard_show".equals(input.request)) {
			vimTouch.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					vimTouch.showIme();
				}
			});
		}
		if ("keyboard_hide".equals(input.request)) {
			vimTouch.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					vimTouch.hideIme();
				}
			});
		}
		return new EmptyTransferable();
	}
}
