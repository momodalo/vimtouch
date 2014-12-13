package org.kvj.vimtouch.ext.manager.impl;

import org.kvj.bravo7.ipc.RemoteServiceConnector;
import org.kvj.vimtouch.BasePlugin;
import org.kvj.vimtouch.IntegrationProvider;
import org.kvj.vimtouch.TransferableData;
import org.kvj.vimtouch.ext.FieldReaderException;
import org.kvj.vimtouch.ext.IncomingTransfer;
import org.kvj.vimtouch.ext.OutgoingTransfer;
import org.kvj.vimtouch.ext.Transferable;
import org.kvj.vimtouch.ext.manager.IntegrationExtension;
import org.kvj.vimtouch.ext.manager.IntegrationExtensionException;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public abstract class SimplePlugin<I extends Transferable, O extends Transferable>
		extends BasePlugin.Stub implements IntegrationExtension<I, O> {

	protected static String TAG = "SimplePlugin";
	private RemoteServiceConnector<IntegrationProvider> serviceConnector = null;

	public SimplePlugin(Context ctx) {
		serviceConnector = new RemoteServiceConnector<IntegrationProvider>(ctx,
				IntegrationExtension.PROVIDER_ACTION, null) {

			@Override
			public IntegrationProvider castAIDL(IBinder binder) {
				return IntegrationProvider.Stub.asInterface(binder);
			}
		};
	}
	
	protected IntegrationProvider getProvider() {
		return serviceConnector.getRemote();
	}

	@Override
	public final TransferableData process(TransferableData data)
			throws RemoteException {
		try {
			I inp = newInput();
			IncomingTransfer it = new IncomingTransfer(data.getData());
			inp.readFrom(it);
			try {
				it.read();
			} catch (FieldReaderException e) {
				throw new IntegrationExtensionException(String.format(
						"Parsing failed: %s", e.getMessage()));
			}
			O output = process(inp);
			OutgoingTransfer ot = new OutgoingTransfer();
			ot.beginWrite();
			output.writeTo(ot);
			ot.endWrite();
			return new TransferableData(ot.getBuffer().toString());
		} catch (IntegrationExtensionException e) {
			Log.w(TAG, "Error processing input:", e);
			throw new RemoteException();
		}
	}

	@Override
	public final String getName() throws RemoteException {
		return getType();
	}

	protected boolean sendEvent(int subscription, Transferable data) {
		IntegrationProvider provider = getProvider();
		if (null == provider) {
			Log.w(TAG, "Provider is not available now");
			return false;
		}
		OutgoingTransfer ot = new OutgoingTransfer();
		ot.beginWrite();
		data.writeTo(ot);
		ot.endWrite();
		try {
			provider.sendEvent(subscription, new TransferableData(ot
					.getBuffer().toString()));
			return true;
		} catch (RemoteException e) {
			Log.e(TAG, "Error sending event:", e);
		}
		return false;
	}

	public void stop() {
		serviceConnector.stop();
	}

}
