package net.momodalo.app.vimtouch.ext.manager.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.momodalo.app.vimtouch.ext.IncomingTransfer;
import net.momodalo.app.vimtouch.ext.impl.event.EventRequest;
import net.momodalo.app.vimtouch.ext.impl.event.EventResponse;
import net.momodalo.app.vimtouch.ext.impl.read.NumberFieldReader;
import net.momodalo.app.vimtouch.ext.manager.IntegrationExtension;
import net.momodalo.app.vimtouch.ext.manager.IntegrationExtensionException;
import net.momodalo.app.vimtouch.ext.manager.IntegrationManager;
import net.momodalo.app.vimtouch.ext.manager.impl.TimerExtension.TimerInput;
import net.momodalo.app.vimtouch.ext.manager.impl.TimerExtension.TimerOutput;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

public class TimerExtension implements
		IntegrationExtension<TimerInput, TimerOutput> {

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

	static class TimerOutput extends EventResponse {

		public TimerOutput(int subscription) {
			super(subscription);
		}

	}

	class TimerHandler implements Runnable {

		private int interval;
		private int subscription;

		public TimerHandler(int subscription, int interval) {
			super();
			this.interval = interval;
			this.subscription = subscription;
		}

		@Override
		public void run() {
			Log.i(TAG, "Timer: " + interval);
			IntegrationManager.getInstance().sendEvent(subscription,
					new EventResponse(subscription));
			if (interval > 0) {
				timerHandler.postDelayed(this, interval * 1000);
			} else {
				handlers.remove(subscription);
			}
		}

	}

	private static final String TYPE = "timer";
	private static final String TAG = "Timer";
	private Context context;
	private Handler timerHandler = null;

	private Map<Integer, TimerHandler> handlers = new HashMap<Integer, TimerHandler>();

	public TimerExtension(Context context) {
		this.context = context;
		timerHandler = new Handler();
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public TimerInput newInput() {
		return new TimerInput();
	}

	@Override
	public TimerOutput process(TimerInput input)
			throws IntegrationExtensionException {
		Log.i(TAG, "New timer subscription: " + input.getSubscription()
				+ ", interval: " + input.interval + ", time: " + input.time);
		if (input.interval <= 0 && input.time <= 0) {
			// No data - cancel?
			TimerHandler handler = handlers.get(input.getSubscription());
			if (null != handler) {
				// Found
				timerHandler.removeCallbacks(handler);
				handlers.remove(input.getSubscription());
			}
			Log.i(TAG, "Removed subscription");
			return new TimerOutput(input.getSubscription());
		}
		int subscription = IntegrationManager.getInstance().nextEvent();
		TimerHandler handler = new TimerHandler(subscription, input.interval);
		handlers.put(subscription, handler);
		long start = input.time;
		if (start != 0) {
			Log.i(TAG, "Will be executed at: " + (new Date(start * 1000)));
			timerHandler.postDelayed(handler,
					(start * 1000) - System.currentTimeMillis());
		} else {
			timerHandler.postDelayed(handler, input.interval * 1000);
		}
		Log.i(TAG, "Event subscription created: " + subscription);
		return new TimerOutput(subscription);
	}
}
