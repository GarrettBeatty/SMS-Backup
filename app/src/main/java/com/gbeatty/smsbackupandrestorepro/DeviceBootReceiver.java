package com.gbeatty.smsbackupandrestorepro;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;


public class DeviceBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
            boolean autoBackup = settings.getBoolean("auto_backup", true);
            if (!autoBackup) return;

            /* Setting the alarm here */
            Intent alarmIntent = new Intent(context, BackupService.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);

            AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            String i = settings.getString("backup_interval", "1");
            Long interval = Long.valueOf(i) * 3600000;
            manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pendingIntent);
        }
    }
}