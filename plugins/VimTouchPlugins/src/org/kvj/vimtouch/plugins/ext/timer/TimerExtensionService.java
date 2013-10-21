package org.kvj.vimtouch.plugins.ext.timer;

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class TimerExtensionService extends IntentService {

	private static final String TAG = "TimerService";
	private TimerExtension extension = null;

	public TimerExtensionService() {
		super("TimerExtensionService");
	}

	@Override
	public void onCreate() {
		extension = new TimerExtension(this);
		super.onCreate();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return extension;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "Destroying Service");
		extension.stop();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		extension.onAlarm(intent);
		AlarmReceiver.completeWakefulIntent(intent);
	}

}
