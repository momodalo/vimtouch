package org.kvj.vimtouch.plugins.ext.timer;

import java.util.Date;

import org.kvj.vimtouch.ext.IncomingTransfer;
import org.kvj.vimtouch.ext.impl.event.EventRequest;
import org.kvj.vimtouch.ext.impl.event.EventResponse;
import org.kvj.vimtouch.ext.impl.read.NumberFieldReader;
import org.kvj.vimtouch.ext.manager.IntegrationExtensionException;
import org.kvj.vimtouch.ext.manager.impl.SimplePlugin;
import org.kvj.vimtouch.plugins.ext.timer.TimerExtension.TimerInput;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

public class TimerExtension extends SimplePlugin<TimerInput, EventResponse> {

	private Context ctx = null;

	public TimerExtension(Context ctx) {
		super(ctx);
		this.ctx = ctx;
	}

	private PendingIntent createIntent(int subscription, Bundle extras) {
		Intent intent = new Intent(ctx, AlarmReceiver.class);
		if (null != extras) {
			intent.putExtras(extras);
		}
		intent.setData(Uri.fromParts("subscription", "code",
				String.format("%d", subscription)));
		return PendingIntent.getBroadcast(ctx, 0, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);
	}

	private void runAtTime(long when, long repeat, int subscription) {
		AlarmManager alarmManager = (AlarmManager) ctx
				.getSystemService(Context.ALARM_SERVICE);
		Bundle b = new Bundle();
		b.putLong("repeat", repeat);
		b.putInt("subscription", subscription);
		PendingIntent intent = createIntent(subscription, b);
		Log.d(TAG, "Schedule timer: " + (new Date(when)) + ", " + subscription);
		alarmManager.set(AlarmManager.RTC_WAKEUP, when, intent);
	}

	private void cancelRunAtTime(int subscription) {
		AlarmManager alarmManager = (AlarmManager) ctx
				.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(createIntent(subscription, null));
	}

	static class TimerInput extends EventRequest {

		int interval = 0;

		long time = 0;

		@Override
		public void readFrom(IncomingTransfer t) {
			super.readFrom(t);
			t.readAs("interval", new NumberFieldReader() {

				@Override
				public void set(Number value) {
					interval = value.intValue();
				}
			});
			t.readAs("time", new NumberFieldReader() {

				@Override
				public void set(Number value) {
					time = value.longValue();
				}
			});
		}
	}

	private static final String TYPE = "timer";

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public TimerInput newInput() {
		return new TimerInput();
	}

	@Override
	public EventResponse process(TimerInput input)
			throws IntegrationExtensionException {
		Log.d(TAG, "Timer extension: " + input.interval + ", " + input.time);
		if (input.interval == 0 && input.time == 0) {
			// Cancel
			Log.d(TAG, "Removing subscription: " + input.getSubscription());
			cancelRunAtTime(input.getSubscription());
			return new EventResponse(0);
		}
		int subscription;
		try {
			subscription = getProvider().nextSubscription();
		} catch (RemoteException e) {
			throw new IntegrationExtensionException(
					"Failed to create subscription");
		}
		long time = input.time * 1000;
		if (0 == time) {
			time = System.currentTimeMillis() + input.interval * 1000;
		}
		runAtTime(time, input.interval * 1000, subscription);
		return new EventResponse(subscription);
	}

	public void onAlarm(Intent intent) {
		// When Alarm service call it
		int subscription = intent.getIntExtra("subscription", 0);
		long repeat = intent.getLongExtra("repeat", 0);
		Log.i(TAG, "Sending timer event back to vimtouch: " + subscription);
		sendEvent(subscription, new EventResponse(subscription));
		if (repeat > 0) {
			runAtTime(System.currentTimeMillis() + repeat, repeat, subscription);
		}
	}

}
