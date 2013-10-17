package net.momodalo.app.vimtouch.ext.manager.impl;

import java.util.ArrayList;
import java.util.List;

import net.momodalo.app.vimtouch.VimTouch;
import net.momodalo.app.vimtouch.ext.manager.impl.QuickbarExtension.QuickbarInput;

import org.kvj.vimtouch.ext.IncomingTransfer;
import org.kvj.vimtouch.ext.OutgoingTransfer;
import org.kvj.vimtouch.ext.Transferable;
import org.kvj.vimtouch.ext.impl.EmptyTransferable;
import org.kvj.vimtouch.ext.impl.read.BooleanFieldReader;
import org.kvj.vimtouch.ext.impl.read.ListFieldReader;
import org.kvj.vimtouch.ext.impl.read.StringFieldReader;
import org.kvj.vimtouch.ext.manager.IntegrationExtension;
import org.kvj.vimtouch.ext.manager.IntegrationExtensionException;

public class QuickbarExtension implements
		IntegrationExtension<QuickbarInput, EmptyTransferable> {

	private VimTouch vimTouch = null;

	public QuickbarExtension(VimTouch vimTouch) {
		this.vimTouch = vimTouch;
	}

	public static class QuickbarInput implements Transferable {

		private List<String> items = new ArrayList<String>();
		private boolean setDefault = false;

		@Override
		public void readFrom(IncomingTransfer t) {
			t.readAs("items", new ListFieldReader<String>(
					new StringFieldReader() {

						@Override
						public void set(String value) {
						}
					}) {

				@Override
				public void set(List<String> value) {
				}

				@Override
				public void add(String value) {
					items.add(value);
				}
			});
			t.readAs("default", new BooleanFieldReader() {

				@Override
				public void set(Boolean value) {
					setDefault = value;
				}
			});
		}

		@Override
		public void writeTo(OutgoingTransfer t) {
		}

	}

	@Override
	public String getType() {
		return "quickbar";
	}

	@Override
	public QuickbarInput newInput() {
		return new QuickbarInput();
	}

	@Override
	public EmptyTransferable process(final QuickbarInput input)
			throws IntegrationExtensionException {
		vimTouch.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (input.setDefault) {
					vimTouch.updateButtons();
				} else {
					vimTouch.setCustomButtons(input.items);
				}
			}
		});
		return new EmptyTransferable();
	}
}
