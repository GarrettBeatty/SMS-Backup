package com.gbeatty.smsbackupandrestorepro;

import android.app.Service;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.gbeatty.smsbackupandrestorepro.models.Sms;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.gmail.GmailScopes;

import java.util.Arrays;

import static com.gbeatty.smsbackupandrestorepro.BaseActivity.PREF_ACCOUNT_NAME;

public class BackupService extends Service implements Loader.OnLoadCompleteListener<Cursor> {

    private static final int LOADER_ID = 1;
    public static final String BACKUP_RESULT = "com.gbeatty.smsbackupandrestorepro.BackupService.REQUEST_PROCESSED";
    private Uri uri = Uri.parse("content://sms/");
    public static final String BACKUP_DATA = "com.gbeatty.smsbackupandrestorepro.BackupService.BACKUP_DATA";
    private String[] projection = {
            "_id","address","read","body","date","type"
    };
    private LocalBroadcastManager broadcaster;
    private CursorLoader mCursorLoader;

    public BackupService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        broadcaster = LocalBroadcastManager.getInstance(this);
        mCursorLoader = new CursorLoader(this, uri, projection, null,null,null);
        mCursorLoader.registerListener(LOADER_ID, this);
        mCursorLoader.startLoading();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {

        // Stop the cursor loader
        if (mCursorLoader != null) {
            mCursorLoader.unregisterListener(this);
            mCursorLoader.cancelLoad();
            mCursorLoader.stopLoading();
        }
    }

    @Override
    public void onLoadComplete(Loader<Cursor> loader, Cursor c) {
        if(c != null && c.getCount() > 0){
            Intent intent = new Intent(BACKUP_RESULT);
            c.moveToFirst();
            int totalSMS = c.getCount();
            int count = 0;
            Sms objSms;
            for (int i = 0; i < totalSMS; i++) {
                objSms = new Sms();
                objSms.setId(c.getString(c.getColumnIndexOrThrow("_id")));
                objSms.setAddress(c.getString(c
                        .getColumnIndexOrThrow("address")));
                objSms.setReadState(c.getString(c.getColumnIndex("read")));
                objSms.setMsg(c.getString(c.getColumnIndexOrThrow("body")));
                objSms.setTime(c.getString(c.getColumnIndexOrThrow("date")));
                if (c.getString(c.getColumnIndexOrThrow("type")).contains("1")) {
                    objSms.setFolderName("inbox");
                } else {
                    objSms.setFolderName("sent");
                }
                count++;
                intent.putExtra(BACKUP_DATA, new int[]{count, totalSMS});
                broadcaster.sendBroadcast(intent);
                c.moveToNext();
            }
        }
    }
}
