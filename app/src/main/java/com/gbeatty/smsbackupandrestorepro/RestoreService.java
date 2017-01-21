package com.gbeatty.smsbackupandrestorepro;


import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;

import java.io.IOException;
import java.util.Arrays;

import javax.mail.MessagingException;

import static com.gbeatty.smsbackupandrestorepro.Utils.BACKUP_MESSAGE;
import static com.gbeatty.smsbackupandrestorepro.Utils.PREF_ACCOUNT_NAME;
import static com.gbeatty.smsbackupandrestorepro.Utils.RESTORE_IDLE;
import static com.gbeatty.smsbackupandrestorepro.Utils.RESTORE_RESULT;
import static com.gbeatty.smsbackupandrestorepro.Utils.SCOPES;

public class RestoreService extends Service {

    private GoogleAccountCredential credential;
    private com.google.api.services.gmail.Gmail mService = null;
    private SharedPreferences settings;
    private HttpTransport transport;
    private JsonFactory jsonFactory;
    private String account;
    private String labelName;
    private LocalBroadcastManager broadcaster;
    private String user = "me";
    private Uri uriSent = Uri.parse("content://sms/sent");
    private Uri uriInbox = Uri.parse("content://sms/inbox");


    public static boolean RUNNING;



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize credentials and service object.
        credential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        account = settings.getString(PREF_ACCOUNT_NAME, null);

        if (account != null) {
            credential.setSelectedAccountName(account);
        }

        labelName = settings.getString("gmail_label", "sms");

        transport = AndroidHttp.newCompatibleTransport();
        jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.gmail.Gmail.Builder(
                transport, jsonFactory, credential)
                .setApplicationName("SMS Backup and Restore Pro")
                .build();

        broadcaster = LocalBroadcastManager.getInstance(this);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        performOnBackgroundThread(new Runnable() {
            @Override
            public void run() {
                try {
                    handleRestore();
                } catch (IOException e) {
                    RUNNING = false;
                    updateProgress(0,0,RESTORE_IDLE);
                    e.printStackTrace();
                } catch (MessagingException e) {
                    RUNNING = false;
                    updateProgress(0,0,RESTORE_IDLE);
                    e.printStackTrace();
                }
            }
        });
        return  START_STICKY;
    }

    private void updateProgress(int current, int total, int status) {
        Intent intent = new Intent(RESTORE_RESULT);
        int[] message = {current, total, status};
        intent.putExtra(BACKUP_MESSAGE, message);
        broadcaster.sendBroadcast(intent);
    }

    private void handleRestore() throws IOException, MessagingException {

    }

    public static java.lang.Thread performOnBackgroundThread(final Runnable runnable) {
        final java.lang.Thread t = new java.lang.Thread() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } finally {

                }
            }
        };
        t.start();
        return t;
    }

}
