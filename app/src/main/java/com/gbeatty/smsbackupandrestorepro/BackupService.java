package com.gbeatty.smsbackupandrestorepro;

import android.app.Service;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.widget.Toast;

import com.gbeatty.smsbackupandrestorepro.models.Sms;

public class BackupService extends Service implements Loader.OnLoadCompleteListener<Cursor> {

    private static final int LOADER_ID = 1;
    private Uri uri = Uri.parse("content://sms/");
    private String[] projection = {
            "_id","address","read","body","date","type"
    };
    private CursorLoader mCursorLoader;

    public BackupService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
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
                double percent = (100 * count) / totalSMS;
//                progressBar.setProgress((int) percent);
//                progressInfo.setText(res.getString(R.string.progress_info, count, totalSMS));
                c.moveToNext();
            }
        }
    }
}
