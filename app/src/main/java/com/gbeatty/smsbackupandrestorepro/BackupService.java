package com.gbeatty.smsbackupandrestorepro;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
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
import com.google.api.services.gmail.model.Thread;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import static com.gbeatty.smsbackupandrestorepro.Utils.BACKUP_COMPLETE;
import static com.gbeatty.smsbackupandrestorepro.Utils.BACKUP_IDLE;
import static com.gbeatty.smsbackupandrestorepro.Utils.BACKUP_MESSAGE;
import static com.gbeatty.smsbackupandrestorepro.Utils.BACKUP_RESULT;
import static com.gbeatty.smsbackupandrestorepro.Utils.BACKUP_RUNNING;
import static com.gbeatty.smsbackupandrestorepro.Utils.BACKUP_STARTING;
import static com.gbeatty.smsbackupandrestorepro.Utils.BACKUP_STOPPING;
import static com.gbeatty.smsbackupandrestorepro.Utils.PREF_ACCOUNT_NAME;
import static com.gbeatty.smsbackupandrestorepro.Utils.SCOPES;
import static com.gbeatty.smsbackupandrestorepro.Utils.createEmail;
import static com.gbeatty.smsbackupandrestorepro.Utils.createLabelIfNotExistAndGetLabelID;
import static com.gbeatty.smsbackupandrestorepro.Utils.getThreadsWithLabelsQuery;
import static com.gbeatty.smsbackupandrestorepro.Utils.insertMessage;

public class BackupService extends Service {

    public static boolean RUNNING = false;
    private Uri uri = Uri.parse("content://sms/");
    private String[] projection = {"address", "read", "body", "date", "type"
    };
    private GoogleAccountCredential credential;
    private com.google.api.services.gmail.Gmail mService = null;
    private SharedPreferences settings;
    private HttpTransport transport;
    private JsonFactory jsonFactory;
    private String account;
    private String labelName;
    private LocalBroadcastManager broadcaster;
    private String user = "me";
    private Map<String, String> contacts;
    private Long tempLastDate = Long.MIN_VALUE;
    private NotificationManager mNotificationManager = null;
    private NotificationCompat.Builder mNotifyBuilder = null;
    private int notifyID = 1;
    private String[] labelIDs;

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
        contacts = new HashMap<>(200);
        getContactName();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void handleParsing(String address, String msg, String date, String name, String folder, int count, int totalSMS) throws IOException, MessagingException {

        String subject = "SMS with " + name;
        String query = "subject:" + subject;

        String threadID = null;

        List<Thread> threads = getThreadsWithLabelsQuery(mService, user, query, labelIDs);
        if (threads.size() > 0) {
            threadID = threads.get(0).getId();
        }

        String from;
        String to;
        String personal;

        if (folder.equals("inbox")) {
            to = account;
            personal = name;
            from = address + "@g.mail";
        } else {
            to = address + "@g.mail";
            personal = "me";
            from = account;
        }

        MimeMessage email = createEmail(to, from, personal, subject, msg, new Date(Long.valueOf(date)));
        insertMessage(mService, user, email, threadID, labelIDs);

        tempLastDate = Long.valueOf(date);

        updateProgress(count, totalSMS, BACKUP_RUNNING);

    }

    private void saveValuesToSharedPrefs() {
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong("last_date", tempLastDate);
        editor.apply();
    }

    private void updateProgress(int current, int total, int status) {
        Intent intent = new Intent(BACKUP_RESULT);
        int[] message = {current, total, status};
        intent.putExtra(BACKUP_MESSAGE, message);
        broadcaster.sendBroadcast(intent);

        if (!settings.getBoolean("notifications", false)) return;

        switch (status) {
            case BACKUP_STARTING:
                updateNotification("Progress: Starting...");
                break;
            case BACKUP_RUNNING:
                updateNotification("" + current + " out of " + total + " SMS backed up");
                break;
            case BACKUP_STOPPING:
                updateNotification("Progress: Stopping...");
                break;
            case BACKUP_COMPLETE:
                updateNotification("Progress: Complete");
                break;
            case BACKUP_IDLE:
                updateNotification("Progress: Idle");
        }

    }

    private int handleBackup() throws IOException, MessagingException {

        labelIDs = new String[]{createLabelIfNotExistAndGetLabelID(mService, user, labelName)};

        RUNNING = true;
        updateProgress(0, 0, BACKUP_STARTING);
        BigInteger lastDate = BigInteger.valueOf(tempLastDate);

        ContentResolver resolver = getContentResolver();
        String query = "CAST(date as BIGINT) > " + lastDate + "";
        Cursor c = resolver.query(uri, projection, query, null, "date ASC");

        if (c != null && c.getCount() > 0) {
            c.moveToFirst();
            int totalSMS = c.getCount();
            int count = 0;
            for (int i = 0; i < totalSMS; i++) {

                if (!RUNNING) {
                    c.close();
                    return BACKUP_IDLE;
                }

                String address = c.getString(c
                        .getColumnIndexOrThrow("address"));
                String msg = c.getString(c.getColumnIndexOrThrow("body"));
                String date = c.getString(c.getColumnIndexOrThrow("date"));
                String folder;
                String name = contacts.get(address);
                if (name == null) name = address;
                if (c.getString(c.getColumnIndexOrThrow("type")).contains("1")) {
                    folder = "inbox";
                } else {
                    folder = "sent";
                }

                count++;
                handleParsing(address, msg, date, name, folder, count, totalSMS);

                c.moveToNext();
            }
            RUNNING = false;
            c.close();
            return BACKUP_COMPLETE;
        }
        return BACKUP_COMPLETE;
    }

    private void getContactName() {

        Cursor managedCursor = getContentResolver()
                .query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        new String[]{ContactsContract.CommonDataKinds.Phone._ID, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER}, null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");

        if (managedCursor != null && managedCursor.getCount() > 0) {
            managedCursor.moveToFirst();
            int total = managedCursor.getCount();
            for (int i = 0; i < total; i++) {
                String number = managedCursor.getString(managedCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                String name = managedCursor.getString(managedCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String phNo = number.replaceAll("[()\\-\\s]", "").trim();

                contacts.put(phNo, name);
                managedCursor.moveToNext();
            }
            managedCursor.close();
        }


    }

    private void stopOnError() {
        RUNNING = false;
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
        mNotificationManager.notify(
                notifyID,
                mNotifyBuilder.build());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        saveValuesToSharedPrefs();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        performOnBackgroundThread(new Runnable() {
            @Override
            public void run() {
                try {
                    int status = handleBackup();
                    saveValuesToSharedPrefs();
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
