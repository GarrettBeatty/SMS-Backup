package com.gbeatty.smsbackupandrestorepro;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Context;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.gbeatty.smsbackupandrestorepro.models.Sms;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.gbeatty.smsbackupandrestorepro.BaseActivity.PREF_ACCOUNT_NAME;
import static com.gbeatty.smsbackupandrestorepro.BaseActivity.REQUEST_AUTHORIZATION;
import static com.gbeatty.smsbackupandrestorepro.BaseActivity.SCOPES;
import static com.google.android.gms.internal.zzs.TAG;

public class BackupService extends IntentService  {

    private static final int LOADER_ID = 1;
    private Uri uri = Uri.parse("content://sms/");
    private String[] projection = {
            "_id", "address", "read", "body", "date", "type"
    };
    private GoogleAccountCredential credential;
    private com.google.api.services.gmail.Gmail mService = null;
    private Exception mLastError = null;
    private SharedPreferences settings;
    private HttpTransport transport;
    private JsonFactory jsonFactory;
    private String account;

    public BackupService() {
        super("BackupService");
    }

    @Override
    public void onCreate(){
        super.onCreate();

        // Initialize credentials and service object.
        credential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        account = settings.getString(PREF_ACCOUNT_NAME, null);

        if(account != null){
            credential.setSelectedAccountName(account);
        }

        transport = AndroidHttp.newCompatibleTransport();
        jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.gmail.Gmail.Builder(
                transport, jsonFactory, credential)
                .setApplicationName("SMS Backup and Restore Pro")
                .build();

    }

    private void handleBackup() throws IOException {

        ContentResolver resolver = getContentResolver();
        Cursor c = resolver.query(uri, projection, null,null,null);

        String user = "me";

//        Label label = new Label().setName("garrett").setLabelListVisibility("labelShow").setMessageListVisibility("show");
//        mService.users().labels().create(user, label).execute();

        if (c != null && c.getCount() > 0) {
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
                Log.d("count", "" + count);
                c.moveToNext();
            }
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if(intent != null){
            try {
                handleBackup();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
