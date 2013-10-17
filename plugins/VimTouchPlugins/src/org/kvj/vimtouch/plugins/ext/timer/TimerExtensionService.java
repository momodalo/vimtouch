package org.kvj.vimtouch.plugins.ext.timer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class TimerExtensionService extends Service {

	private static final String TAG = "TimerService";
	private TimerExtension extension = null;

	public TimerExtensionService() {
		super();
	}

	@Override
	public void onCreate() {
		extension = new TimerExtension(this);
		super.onCreate();
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return extension;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		extension.onAlarm(intent);
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "Destroying Service");
		extension.stop();
	}

}
