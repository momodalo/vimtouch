/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.momodalo.app.vimtouch;

import jackpal.androidterm.emulatorview.TermSession;
import net.momodalo.app.vimtouch.compat.ServiceForegroundCompat;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class VimTermService extends Service implements TermSession.FinishCallback
{
    /* Parallels the value of START_STICKY on API Level >= 5 */
    public static final String LOG_TAG = "VimTermService";
    private static final int COMPAT_START_STICKY = 1;

    private static final int RUNNING_NOTIFICATION = 1;
    private ServiceForegroundCompat compat;

    public class TSBinder extends Binder {
        VimTermService getService() {
            Log.i("VimTermService", "Activity binding to service");
            return VimTermService.this;
        }
    }
    private final IBinder mTSBinder = new TSBinder();

    @Override
    public void onStart(Intent intent, int flags) {
    }

    /* This should be @Override if building with API Level >=5 */
    public int onStartCommand(Intent intent, int flags, int startId) {
        return COMPAT_START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mTSBinder;
    }

    @Override
    public void onCreate() {
        compat = new ServiceForegroundCompat(this);

        /* Put the service in the foreground. */
		Notification notification = new Notification(
				R.drawable.ic_vim_notification,
				getText(R.string.service_notify_text),
				System.currentTimeMillis());
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        Intent notifyIntent = new Intent(this, VimTouch.class);
        notifyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notifyIntent, 0);
        notification.setLatestEventInfo(this, getText(R.string.application_vimtouch), getText(R.string.service_notify_text), pendingIntent);
        compat.startForeground(RUNNING_NOTIFICATION, notification);

        Log.d(VimTermService.LOG_TAG, "VimTermService started");
        return;
    }

    @Override
    public void onDestroy() {
        return;
    }

    public void onSessionFinish(TermSession session) {
    }
}
