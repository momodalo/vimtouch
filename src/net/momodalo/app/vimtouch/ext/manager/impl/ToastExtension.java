package net.momodalo.app.vimtouch.ext.manager.impl;

import net.momodalo.app.vimtouch.VimTouch;
import net.momodalo.app.vimtouch.ext.manager.impl.ToastExtension.ToastInput;
import net.momodalo.app.vimtouch.ext.manager.impl.ToastExtension.ToastOutput;

import org.kvj.vimtouch.ext.IncomingTransfer;
import org.kvj.vimtouch.ext.OutgoingTransfer;
import org.kvj.vimtouch.ext.Transferable;
import org.kvj.vimtouch.ext.impl.read.StringFieldReader;
import org.kvj.vimtouch.ext.manager.IntegrationExtension;
import org.kvj.vimtouch.ext.manager.IntegrationExtensionException;

import android.widget.Toast;

public class ToastExtension implements
		IntegrationExtension<ToastInput, ToastOutput> {

	public static class ToastInput implements Transferable {

		String message = null;
		String type = "short";

		@Override
		public void readFrom(IncomingTransfer t) {
			t.readAs("message", new StringFieldReader() {

				@Override
				public void set(String value) {
					message = value;
				}
			});
			t.readAs("type", new StringFieldReader() {

				@Override
				public void set(String value) {
					type = value;
				}
			});
		}

		@Override
		public void writeTo(OutgoingTransfer t) {
		}

	}

	public static class ToastOutput implements Transferable {

		@Override
		public void readFrom(IncomingTransfer t) {
		}

		@Override
		public void writeTo(OutgoingTransfer t) {
		}

	}

	private VimTouch vimTouch;

	public ToastExtension(VimTouch vimTouch) {
		this.vimTouch = vimTouch;
	}

	@Override
	public String getType() {
		return "toast";
	}

	@Override
	public ToastInput newInput() {
		return new ToastInput();
	}

	@Override
	public ToastOutput process(final ToastInput input)
			throws IntegrationExtensionException {
		vimTouch.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(
						vimTouch,
						input.message,
						"long".equals(input.type) ? Toast.LENGTH_LONG
								: Toast.LENGTH_SHORT).show();
			}
		});
		return new ToastOutput();
	}
}
