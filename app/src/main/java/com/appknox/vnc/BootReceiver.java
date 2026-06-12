package com.appknox.vnc;

import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import androidx.preference.PreferenceManager;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "knoxvnc-bootreceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            Defaults defaults = new Defaults(context);

            if (Build.VERSION.SDK_INT >= 30 && !InputService.isConnected()) {
                Log.w(TAG, "onReceive: InputService is not set-up");
            }

            Intent vncIntent = new Intent(context.getApplicationContext(), VNCService.class);
            vncIntent.setAction(VNCService.ACTION_START);
            vncIntent.putExtra(VNCService.EXTRA_PORT, prefs.getInt(Constants.PREFS_KEY_SETTINGS_PORT, defaults.getPort()));
            vncIntent.putExtra(VNCService.EXTRA_PASSWORD, prefs.getString(Constants.PREFS_KEY_SETTINGS_PASSWORD, defaults.getPassword()));
            vncIntent.putExtra(VNCService.EXTRA_FILE_TRANSFER, prefs.getBoolean(Constants.PREFS_KEY_SETTINGS_FILE_TRANSFER, defaults.getFileTransfer()));
            vncIntent.putExtra(VNCService.EXTRA_VIEW_ONLY, prefs.getBoolean(Constants.PREFS_KEY_SETTINGS_VIEW_ONLY, defaults.getViewOnly()));
            vncIntent.putExtra(VNCService.EXTRA_SCALING, prefs.getFloat(Constants.PREFS_KEY_SETTINGS_SCALING, defaults.getScaling()));
            // check whether user set PROJECT_MEDIA app op to allow in order to get around the
            // MediaProjection permission dialog
            AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            int mediaProjectionAppOpsMode = appOpsManager.checkOpNoThrow(
                    "android:project_media",
                    android.os.Process.myUid(),
                    context.getPackageName()
            );
            // if not, set fallback screen capture
            Log.i(TAG, "onReceive: PROJECT_MEDIA app op is " + mediaProjectionAppOpsMode);
            intent.putExtra(VNCService.EXTRA_FALLBACK_SCREEN_CAPTURE, mediaProjectionAppOpsMode != AppOpsManager.MODE_ALLOWED);

            long delayMillis = 1000L * prefs.getInt(Constants.PREFS_KEY_SETTINGS_START_ON_BOOT_DELAY, defaults.getStartOnBootDelay());
            if (delayMillis > 0) {
                Log.i(TAG, "onReceive: configured to start delayed by " + delayMillis / 1000 + "s");
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                PendingIntent pendingIntent;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    pendingIntent = PendingIntent.getForegroundService(context.getApplicationContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE);
                } else {
                    pendingIntent = PendingIntent.getService(context.getApplicationContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE);
                }
                alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delayMillis, pendingIntent);
            } else {
                Log.i(TAG, "onReceive: configured to start immediately");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.getApplicationContext().startForegroundService(intent);
                } else {
                    context.getApplicationContext().startService(intent);
                }
            }
        }
    }
}
