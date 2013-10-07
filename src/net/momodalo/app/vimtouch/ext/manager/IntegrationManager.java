package net.momodalo.app.vimtouch.ext.manager;

import java.util.HashMap;
import java.util.Map;

import net.momodalo.app.vimtouch.Exec;
import net.momodalo.app.vimtouch.ext.FieldReaderException;
import net.momodalo.app.vimtouch.ext.IncomingTransfer;
import net.momodalo.app.vimtouch.ext.OutgoingTransfer;
import net.momodalo.app.vimtouch.ext.Transferable;
import android.util.Log;

public class IntegrationManager {

	private static final String TAG = "Integration";

	private IntegrationManager() {
	}
	
	private static IntegrationManager instance = null;
	
	public static IntegrationManager getInstance() {
		if (null == instance) {
			instance = new IntegrationManager();
		}
		return instance;
	}

	private Map<String, IntegrationExtension<Transferable, Transferable>> extensions = new HashMap<String, IntegrationExtension<Transferable, Transferable>>();
	private int nextEvent = 0;

	@SuppressWarnings("unchecked")
	public void addExtension(
			IntegrationExtension<? extends Transferable, ? extends Transferable> ext) {
		if (!extensions.containsValue(ext)) {
			extensions.put(ext.getType(),
					(IntegrationExtension<Transferable, Transferable>) ext);
		}
	}

	public void removeExtension(
			IntegrationExtension<Transferable, Transferable> ext) {
		extensions.remove(ext.getType());
	}

	public String process(String type, String input) {
		Transferable output = null;
		try {
			IntegrationExtension<Transferable, Transferable> ext = extensions
					.get(type);
			if (null == ext) {
				throw new IntegrationExtensionException(String.format(
						"Extension '%s' not found", type));
			}
			Transferable inp = ext.newInput();
			IncomingTransfer it = new IncomingTransfer(input);
			inp.readFrom(it);
			try {
				it.read();
			} catch (FieldReaderException e) {
				throw new IntegrationExtensionException(String.format(
						"Parsing failed: %s", e.getMessage()));
			}
			output = ext.process(inp);
		} catch (IntegrationExtensionException e) {
			Log.w(TAG, "Error processing input:", e);
			output = new IntegrationError(e.getMessage());
		}
		OutgoingTransfer ot = new OutgoingTransfer();
		ot.beginWrite();
		output.writeTo(ot);
		ot.endWrite();
		return ot.getBuffer().toString();
	}

	public int nextEvent() {
		return ++nextEvent;
	}

	public void sendEvent(int type, Transferable object) {
		OutgoingTransfer ot = new OutgoingTransfer();
		ot.beginWrite();
		object.writeTo(ot);
		ot.endWrite();
		Exec.sendAndroidEvent(type, ot.getBuffer().toString());
	}
}
