package com.gbeatty.smsbackupandrestorepro;

import android.app.IntentService;
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
import android.support.v4.content.LocalBroadcastManager;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
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

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import static com.gbeatty.smsbackupandrestorepro.Utils.BACKUP_COMPLETE;
import static com.gbeatty.smsbackupandrestorepro.Utils.BACKUP_MESSAGE;
import static com.gbeatty.smsbackupandrestorepro.Utils.BACKUP_RESULT;
import static com.gbeatty.smsbackupandrestorepro.Utils.PREF_ACCOUNT_NAME;
import static com.gbeatty.smsbackupandrestorepro.Utils.SCOPES;
import static com.gbeatty.smsbackupandrestorepro.Utils.createEmail;
import static com.gbeatty.smsbackupandrestorepro.Utils.createLabelIfNotExistAndGetLabelID;
import static com.gbeatty.smsbackupandrestorepro.Utils.getThreadsWithLabelsQuery;
import static com.gbeatty.smsbackupandrestorepro.Utils.insertMessage;

public class BackupService extends Service {

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
    private java.lang.Thread thread;

    private HashMap<String, String> threadIDs;
    private HashMap<String, String> contacts;
    public static boolean RUNNING;

    private Runnable run = new Runnable() {
        public void run() {
            try {
                handleBackup();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }
    };


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

        contacts = new HashMap<>(200);
        threadIDs = new HashMap<>(200);

        transport = AndroidHttp.newCompatibleTransport();
        jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.gmail.Gmail.Builder(
                transport, jsonFactory, credential)
                .setApplicationName("SMS Backup and Restore Pro")
                .build();

        thread = new java.lang.Thread(run);
        broadcaster = LocalBroadcastManager.getInstance(this);

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void handleParsing(String address, String msg, String date, String name, String folder, int count, int totalSMS) throws IOException, MessagingException {

        String[] labelIDs = {createLabelIfNotExistAndGetLabelID(mService, user, labelName)};
        String subject = "SMS with " + name;
        String query = "subject:" + subject;

        String threadID = null;

        if(threadIDs.containsKey(subject)){
            threadID = threadIDs.get(subject);
        }else{
            List<Thread> threads = getThreadsWithLabelsQuery(mService, user, query, labelIDs);
            if(threads.size() > 0){
                threadID = threads.get(0).getId();
                threadIDs.put(subject, threadID);
            }
        }

        String from;
        String to;
        String personal;

        if(folder.equals("inbox")){
            to = account;
            personal = name;
            from = address + "@g.mail";
        }else{
            to = address + "@g.mail";
            personal = "me";
            from = account;
        }

        MimeMessage email = createEmail(to, from, personal, subject, msg, new Date(Long.valueOf(date)));
        insertMessage(mService, user, email, threadID, labelIDs);

        updateProgress(count, totalSMS);

        SharedPreferences.Editor editor = settings.edit();
        editor.putLong("last_date", Long.valueOf(date));
        editor.apply();

    }

    private void updateProgress(int current, int total){
        Intent intent = new Intent(BACKUP_RESULT);
        int completed = (current == total) ? 1 : 0;
        int[] message = {current, total, completed};
        intent.putExtra(BACKUP_MESSAGE, message);
        broadcaster.sendBroadcast(intent);
    }

    private void handleBackup() throws IOException, MessagingException {

        //running
        RUNNING = true;

        Long l = settings.getLong("last_date", Long.MIN_VALUE);
        BigInteger lastDate = BigInteger.valueOf(l);

        ContentResolver resolver = getContentResolver();
        String query = "CAST(date as BIGINT) > " + lastDate + "";
        Cursor c = resolver.query(uri, projection, query, null, "date ASC");

        if (c != null && c.getCount() > 0) {
            c.moveToFirst();
            int totalSMS = c.getCount();
            int count = 1;
            for (int i = 0; i < 20; i++) {

                if(!RUNNING){
                    updateProgress(0,1);
                    c.close();
                    return;
                }

                String address = c.getString(c
                        .getColumnIndexOrThrow("address"));
                String msg = c.getString(c.getColumnIndexOrThrow("body"));
                String date = c.getString(c.getColumnIndexOrThrow("date"));
                String folder;
                String name = getContactName(this, address);
                if(name == null) name = address;
                if (c.getString(c.getColumnIndexOrThrow("type")).contains("1")) {
                    folder = "inbox";
                } else {
                    folder = "sent";
                }

                handleParsing(address, msg, date, name, folder, count, totalSMS);
                count++;

                c.moveToNext();
            }
            RUNNING = false;
            updateProgress(0,0);
            c.close();
        }
    }

    public String getContactName(Context context, String phoneNumber) {

        if(contacts.containsKey(phoneNumber)) return contacts.get(phoneNumber);

        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber));
        Cursor cursor = cr.query(uri,
                new String[] { ContactsContract.PhoneLookup.DISPLAY_NAME }, null, null, null);
        if (cursor == null) {
            return null;
        }
        String contactName = null;
        if (cursor.moveToFirst()) {
            contactName = cursor.getString(cursor
                    .getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        contacts.put(phoneNumber, contactName);
        return contactName;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(!thread.isAlive()){
            thread.start();
        }
        return  START_STICKY;
    }

}
