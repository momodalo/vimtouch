package net.momodalo.app.vimtouch.ext.manager;

import net.momodalo.app.vimtouch.Exec;

import org.kvj.vimtouch.IntegrationProvider;
import org.kvj.vimtouch.TransferableData;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

public class IntegrationProviderService extends Service {

	class IntegrationProviderImpl extends IntegrationProvider.Stub {

		@Override
		public void sendEvent(int type, TransferableData data)
				throws RemoteException {
			Exec.sendAndroidEvent(type, data.getData());
		}

		@Override
		public int nextSubscription() throws RemoteException {
			return IntegrationManager.getInstance(
					IntegrationProviderService.this).nextEvent();
		}

	}

	private IntegrationProviderImpl impl = null;

	public IntegrationProviderService() {
		impl = new IntegrationProviderImpl();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return impl;
	}

}
