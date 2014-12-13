package kvj.app.vimtouch.ext.manager.impl;

import kvj.app.vimtouch.VimTouch;
import kvj.app.vimtouch.ext.manager.impl.InputExtension.InputExtensionInput;

import org.kvj.vimtouch.ext.IncomingTransfer;
import org.kvj.vimtouch.ext.OutgoingTransfer;
import org.kvj.vimtouch.ext.Transferable;
import org.kvj.vimtouch.ext.impl.EmptyTransferable;
import org.kvj.vimtouch.ext.impl.read.StringFieldReader;
import org.kvj.vimtouch.ext.manager.IntegrationExtension;
import org.kvj.vimtouch.ext.manager.IntegrationExtensionException;

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
