/*
 * Copyright (C) 2016 The CyanogenMod Project
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

package org.lineageos.lockclock.weather;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import org.lineageos.lockclock.ClockWidgetService;
import org.lineageos.lockclock.misc.Constants;
import org.lineageos.lockclock.misc.Preferences;
import lineageos.weather.LineageWeatherManager;

public class WeatherSourceListenerService extends Service
        implements LineageWeatherManager.WeatherServiceProviderChangeListener {

    private static final String TAG = WeatherSourceListenerService.class.getSimpleName();
    private static final boolean D = Constants.DEBUG;
    private Context mContext;
    private volatile boolean mRegistered;

    @Override
    public void onWeatherServiceProviderChanged(String providerLabel) {
        if (D) Log.d(TAG, "Weather Source changed " + providerLabel);
        Preferences.setWeatherSource(mContext, providerLabel);
        Preferences.setCachedWeatherInfo(mContext, 0, null);
        //The data contained in WeatherLocation is tightly coupled to the weather provider
        //that generated that data, so we need to clear the cached weather location and let the new
        //weather provider regenerate the data if the user decides to use custom location again
        Preferences.setCustomWeatherLocationCity(mContext, null);
        Preferences.setCustomWeatherLocation(mContext, null);
        Preferences.setUseCustomWeatherLocation(mContext, false);

        //Refresh the widget
        ClockWidgetService.scheduleUpdate(mContext, ClockWidgetService.ACTION_REFRESH);


        if (providerLabel != null) {
            WeatherUpdateService.scheduleNextUpdate(mContext, true);
        }
    }

    @Override
    public void onCreate() {
        mContext = getApplicationContext();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final LineageWeatherManager weatherManager
                = LineageWeatherManager.getInstance(mContext);
        weatherManager.registerWeatherServiceProviderChangeListener(this);
        mRegistered = true;
        if (D) Log.d(TAG, "Listener registered");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mRegistered) {
            final LineageWeatherManager weatherManager = LineageWeatherManager.getInstance(mContext);
            weatherManager.unregisterWeatherServiceProviderChangeListener(this);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
