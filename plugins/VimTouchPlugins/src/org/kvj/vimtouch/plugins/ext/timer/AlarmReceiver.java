package org.kvj.vimtouch.plugins.ext.timer;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

public class AlarmReceiver extends WakefulBroadcastReceiver {

	private static final String TAG = "AlarmReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(TAG, "Timer via Alarm manager!");
		Intent svcIntent = new Intent(context, TimerExtensionService.class);
		svcIntent.putExtras(intent);
		startWakefulService(context, svcIntent);
	}

}
