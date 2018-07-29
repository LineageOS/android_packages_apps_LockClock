/*
 * Copyright (C) 2012-2015 The CyanogenMod Project (DvTonder)
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

package org.lineageos.lockclock;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;
import android.util.Log;

import org.lineageos.lockclock.misc.Constants;
import org.lineageos.lockclock.misc.Preferences;
import org.lineageos.lockclock.misc.WidgetUtils;
import org.lineageos.lockclock.weather.ForecastActivity;
import org.lineageos.lockclock.weather.Utils;
import org.lineageos.lockclock.weather.WeatherSourceListenerService;
import org.lineageos.lockclock.weather.WeatherUpdateService;

import static android.app.job.JobInfo.NETWORK_TYPE_ANY;

public class ClockWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "ClockWidgetProvider";
    private static boolean D = Constants.DEBUG;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Default handling, triggered via the super class
        if (D) Log.v(TAG, "Updating widgets, default handling.");
        updateWidgets(context, false, false);
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        // Deal with received broadcasts that force a refresh
        String action = intent.getAction();
        if (D) Log.v(TAG, "Received intent " + intent);

        // Boot completed, schedule next weather update
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            //Since we're using elapsed time since boot, we can't use the timestamp from the
            //previous boot so we need to reset the timer
            Preferences.setLastWeatherUpdateTimestamp(context, 0);
        // A widget has been deleted, prevent our handling and ask the super class handle it
        } else if (AppWidgetManager.ACTION_APPWIDGET_DELETED.equals(action)
                || AppWidgetManager.ACTION_APPWIDGET_DISABLED.equals(action)) {
            super.onReceive(context, intent);

        // Calendar, Time or a settings change, force a calendar refresh
        } else if (Intent.ACTION_PROVIDER_CHANGED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_DATE_CHANGED.equals(action)
                || Intent.ACTION_LOCALE_CHANGED.equals(action)
                || "android.app.action.NEXT_ALARM_CLOCK_CHANGED".equals(action)
                || ClockWidgetService.ACTION_REFRESH_CALENDAR.equals(action)) {
            updateWidgets(context, true, false);

        // There are no events to show in the Calendar panel, hide it explicitly
        } else if (ClockWidgetService.ACTION_HIDE_CALENDAR.equals(action)) {
            updateWidgets(context, false, true);

        // The intent is to launch the modal pop-up forecast dialog
        } else if (Constants.ACTION_SHOW_FORECAST.equals(action)) {
            Intent i = new Intent(context, ForecastActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);

        // Something we did not handle, let the super class deal with it.
        // This includes the REFRESH_CLOCK intent from Clock settings
        } else {
            if (D) Log.v(TAG, "We did not handle the intent, trigger normal handling");
            super.onReceive(context, intent);
            updateWidgets(context, false, false);
        }
    }

    /**
     *  Update the widget via the service.
     */
    private void updateWidgets(Context context, boolean refreshCalendar, boolean hideCalendar) {
        // Build the intent and pass on the weather and calendar refresh triggers
        ComponentName serviceComponent = new ComponentName(context, ClockWidgetService.class);
        JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
        PersistableBundle args = new PersistableBundle();
        if (refreshCalendar) {
            args.putString("action", ClockWidgetService.ACTION_REFRESH_CALENDAR);
        } else if (hideCalendar) {
            args.putString("action", ClockWidgetService.ACTION_HIDE_CALENDAR);
        } else {
            args.putString("action", ClockWidgetService.ACTION_REFRESH);
        }
        builder.setExtras(args);
        builder.setOverrideDeadline(500);

        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        jobScheduler.schedule(builder.build());



        // Start the service. The service itself will take care of scheduling refreshes if needed
    }

    @Override
    public void onEnabled(Context context) {
        if (D) Log.d(TAG, "Scheduling next weather update");
        if (Utils.isWeatherServiceAvailable(context)) {
            WeatherUpdateService.scheduleNextUpdate(context, true);
        }

        // Start the broadcast receiver (API 16 devices)
        // This will schedule a repeating alarm every minute to handle the clock refresh
        // Once triggered, the receiver will unregister itself when its work is done
        if (!WidgetUtils.isTextClockAvailable()) {
            final WidgetApplication app = (WidgetApplication) context.getApplicationContext();
            app.startTickReceiver();
        }
    }

    @Override
    public void onDisabled(Context context) {
        if (D) Log.d(TAG, "Cleaning up: Clearing all pending alarms");
        if (Utils.isWeatherServiceAvailable(context)) {
            ClockWidgetService.cancelUpdates(context);
            WeatherUpdateService.cancelUpdates(context);
        }

        // Stop the clock update event (API 16 devices)
        if (!WidgetUtils.isTextClockAvailable()) {
            WidgetApplication.cancelClockRefresh(context);
        }
    }
}
