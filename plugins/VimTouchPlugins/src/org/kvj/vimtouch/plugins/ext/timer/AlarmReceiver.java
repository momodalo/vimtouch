package org.kvj.vimtouch.plugins.ext.timer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {

	private static final String TAG = "AlarmReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(TAG, "Timer via Alarm manager!");
		Intent svcIntent = new Intent(context, TimerExtensionService.class);
		svcIntent.putExtras(intent);
		context.startService(svcIntent);
	}

}
