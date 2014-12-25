package kvj.app.vimtouch.ext.manager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.momodalo.app.vimtouch.Exec;

import org.kvj.bravo7.ipc.RemoteServicesCollector;
import org.kvj.vimtouch.BasePlugin;
import org.kvj.vimtouch.TransferableData;
import org.kvj.vimtouch.ext.FieldReaderException;
import org.kvj.vimtouch.ext.IncomingTransfer;
import org.kvj.vimtouch.ext.OutgoingTransfer;
import org.kvj.vimtouch.ext.Transferable;
import org.kvj.vimtouch.ext.manager.IntegrationError;
import org.kvj.vimtouch.ext.manager.IntegrationExtension;
import org.kvj.vimtouch.ext.manager.IntegrationExtensionException;

import android.content.Context;
import android.os.IBinder;
import android.util.Log;

public class IntegrationManager {

	private static final String TAG = "Integration";

	private Context ctx = null;

	private IntegrationManager(Context ctx) {
		this.ctx = ctx;
		pluginCollector = new RemoteServicesCollector<BasePlugin>(ctx,
				IntegrationExtension.PLUGIN_ACTION) {

			@Override
			public BasePlugin castAIDL(IBinder binder) {
				return BasePlugin.Stub.asInterface(binder);
			}

		};
	}
	
	private static IntegrationManager instance = null;
	
	public static IntegrationManager getInstance(Context ctx) {
		if (null == instance) {
			instance = new IntegrationManager(ctx);
		}
		return instance;
	}

	private RemoteServicesCollector<BasePlugin> pluginCollector = null;

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
				return invokeRemoteExtension(type, input);
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

	private String invokeRemoteExtension(String type, String input)
			throws IntegrationExtensionException {
		try {
			List<BasePlugin> plugins = pluginCollector.getPlugins();
			for (BasePlugin plugin : plugins) {
				if (plugin.getName().equals(type)) {
					// Found
					TransferableData out = plugin.process(new TransferableData(
							input));
					return out.getData();
				}
			}
			throw new IntegrationExtensionException(String.format(
					"Extension '%s' not found", type));
		} catch (Exception e) {
			Log.w(TAG,
					String.format("Error invoking remote plugin '%s':", type),
					e);
			throw new IntegrationExtensionException(e.getMessage());
		}
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

	public void stop() {
		pluginCollector.stop();
	}
}
