package com.gbeatty.smsbackupandrestorepro;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.gmail.model.Message;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import static com.gbeatty.smsbackupandrestorepro.Utils.BACKUP_IDLE;
import static com.gbeatty.smsbackupandrestorepro.Utils.PREF_ACCOUNT_NAME;
import static com.gbeatty.smsbackupandrestorepro.Utils.RESTORE_COMPLETE;
import static com.gbeatty.smsbackupandrestorepro.Utils.RESTORE_MESSAGE;
import static com.gbeatty.smsbackupandrestorepro.Utils.RESTORE_RESULT;
import static com.gbeatty.smsbackupandrestorepro.Utils.RESTORE_RUNNING;
import static com.gbeatty.smsbackupandrestorepro.Utils.RESTORE_STARTING;
import static com.gbeatty.smsbackupandrestorepro.Utils.RESTORE_STOPPING;
import static com.gbeatty.smsbackupandrestorepro.Utils.SCOPES;
import static com.gbeatty.smsbackupandrestorepro.Utils.createLabelIfNotExistAndGetLabelID;
import static com.gbeatty.smsbackupandrestorepro.Utils.getMessagesMatchingQuery;
import static com.gbeatty.smsbackupandrestorepro.Utils.getMimeMessage;

public class RestoreService extends Service {

    public static boolean RUNNING = false;

    private Uri uri = Uri.parse("content://sms/");
    private String[] projection = {"address", "body", "date"};
    private com.google.api.services.gmail.Gmail mService = null;
    private SharedPreferences settings;
    private String account;
    private String labelName;
    private LocalBroadcastManager broadcaster;
    private NotificationManager mNotificationManager = null;
    private NotificationCompat.Builder mNotifyBuilder = null;

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

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize credentials and service object.
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        account = settings.getString(PREF_ACCOUNT_NAME, null);

        if (account != null) {
            credential.setSelectedAccountName(account);
        }

        labelName = settings.getString("gmail_label", "sms");

        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.gmail.Gmail.Builder(
                transport, jsonFactory, credential)
                .setApplicationName("SMS Backup and Restore Pro")
                .build();

        broadcaster = LocalBroadcastManager.getInstance(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void updateProgress(int current, int total, int status) {
        Intent intent = new Intent(RESTORE_RESULT);
        int[] message = {current, total, status};
        intent.putExtra(RESTORE_MESSAGE, message);
        broadcaster.sendBroadcast(intent);

        if (!settings.getBoolean("notifications", false)) return;

        switch (status) {
            case RESTORE_STARTING:
                updateNotification("Progress: Starting...");
                break;
            case RESTORE_RUNNING:
                updateNotification("" + current + " out of " + total + " SMS restored");
                break;
            case RESTORE_STOPPING:
                updateNotification("Progress: Stopping...");
                break;
            case RESTORE_COMPLETE:
                updateNotification("Progress: Complete");
                break;
            case BACKUP_IDLE:
                updateNotification("Progress: Idle");
        }

    }

    private void handleMimeMessage(MimeMessage message, int count, int total) throws MessagingException, IOException {

        Address[] froms = message.getFrom();
        String fromEmail = froms == null ? null : ((InternetAddress) froms[0]).getAddress();

        Address[] to = message.getRecipients(javax.mail.Message.RecipientType.TO);
        String toEmail = froms == null ? null : ((InternetAddress) to[0]).getAddress();

        long date = message.getSentDate().getTime();

        String content = message.getContent().toString();

        if(fromEmail == null) return;

        if(fromEmail.equals(account)){
            ContentValues values = new ContentValues();
            String address = toEmail.split("@")[0];
            if(checkIfExists(address, content, date)){
               updateProgress(count, total, RESTORE_RUNNING);
                return;
            }
            values.put("address", address);
            values.put("body", content);
            values.put("date",date);
            getContentResolver().insert(Uri.parse("content://sms/sent"), values);
        }else{
            ContentValues values = new ContentValues();
            String address = fromEmail.split("@")[0];
            if(checkIfExists(address, content, date)){
                updateProgress(count, total, RESTORE_RUNNING);
                return;
            }
            values.put("address", address);
            values.put("body", content);
            values.put("date",date);
            getContentResolver().insert(Uri.parse("content://sms/inbox"), values);
        }

        updateProgress(count, total, RESTORE_RUNNING);
    }

    private boolean checkIfExists(String address, String body, long time){
        ContentResolver resolver = getContentResolver();
        String query = "address = ? AND body = ? AND CAST(date as BIGINT) = ?";
        BigInteger date = BigInteger.valueOf(time);
        String[] args = new String[]{address, body, String.valueOf(date)};
        Cursor c = resolver.query(uri, projection, query, args, null);
        if (c != null) {
            if(c.getCount() > 0){
                c.close();
                return true;
            }
            c.close();
        }
        return false;
    }

    private int handleRestore() throws IOException, MessagingException {

        String user = "me";
        String[] labelIDs = new String[]{createLabelIfNotExistAndGetLabelID(mService, user, labelName)};

        RUNNING = true;
        updateProgress(0, 0, RESTORE_STARTING);

        List<Message> messages = getMessagesMatchingQuery(mService, user, labelIDs);

        String amountS = settings.getString("restore_limit", "all");
        int amount;
        try{
            amount = Integer.parseInt(amountS);
        }catch (NumberFormatException e){
            amount = messages.size();
        }
        if(amount > messages.size()) amount = messages.size();

        for(int i = 0; i < amount; i++){
            if (!RUNNING) {
                return BACKUP_IDLE;
            }

            MimeMessage mimeMessage = getMimeMessage(mService, user, messages.get(i).getId());
            handleMimeMessage(mimeMessage, i, amount);
        }

        return RESTORE_COMPLETE;
    }


    private void stopOnError() {
        updateProgress(0, 0, BACKUP_IDLE);
        stopSelf();
    }

    public void updateNotification(String text) {

        if (mNotificationManager == null || mNotifyBuilder == null) {

            Intent resultIntent = new Intent(this, MainActivity.class);

            PendingIntent resultPendingIntent =
                    PendingIntent.getActivity(
                            this,
                            0,
                            resultIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );

            mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
// Sets an ID for the notification, so it can be updated
            mNotifyBuilder = new NotificationCompat.Builder(this)
                    .setContentTitle("SMS Backup and Restore Pro")
                    .setContentText("")
                    .setContentIntent(resultPendingIntent)
                    .setSmallIcon(R.mipmap.ic_launcher);
        }

        mNotifyBuilder.setContentText(text);
        // Because the ID remains unchanged, the existing notification is
        // updated.
        int notifyID = 1;
        mNotificationManager.notify(
                notifyID,
                mNotifyBuilder.build());
    }

    @Override
    public void onDestroy() {
        RUNNING = false;
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        performOnBackgroundThread(new Runnable() {
            @Override
            public void run() {
                try {
                    int status = handleRestore();
                    updateProgress(0, 0, status);
                    stopSelf();
                } catch (UserRecoverableAuthIOException e) {
                    startActivity(e.getIntent().setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    stopOnError();
                } catch (IOException e) {
                    stopOnError();
                    e.printStackTrace();
                } catch (MessagingException e) {
                    stopOnError();
                    e.printStackTrace();
                }
            }
        });
        return START_STICKY;
    }

}
