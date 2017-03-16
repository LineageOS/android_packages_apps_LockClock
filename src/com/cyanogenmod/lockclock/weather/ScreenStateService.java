/*
 * Copyright (C) 2017 The LineageOS Project
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

package com.cyanogenmod.lockclock.weather;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;
import com.cyanogenmod.lockclock.misc.Constants;

public class ScreenStateService extends Service {

    private static final String TAG = ScreenStateService.class.getSimpleName();
    private static final boolean D = Constants.DEBUG;
    private Context mContext;
    private BroadcastReceiver mScreenStateReceiver;

    @Override
    public void onCreate() {
        mContext = getApplicationContext();

        IntentFilter screenStateFilter = new IntentFilter();
        screenStateFilter.addAction(Intent.ACTION_SCREEN_ON);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        mScreenStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                    if (D) Log.d(TAG, "onDisplayOff: Cancel pending updates");
                    WeatherUpdateService.cancelUpdates(context);
                } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                    if (D) Log.d(TAG, "onDisplayOn: Reschedule updates");
                    WeatherUpdateService.rescheduleUpdates(context);
                }
            }
        };
        mContext.registerReceiver(mScreenStateReceiver, screenStateFilter);
    }

    @Override
    public void onDestroy() {
        if (D) Log.d(TAG, "Stopping ScreenStateService");
        mContext.unregisterReceiver(mScreenStateReceiver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (D) Log.d(TAG, "Starting ScreenStateService");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
