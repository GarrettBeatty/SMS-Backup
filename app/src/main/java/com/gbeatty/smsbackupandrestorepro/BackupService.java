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
import android.provider.ContactsContract;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.gbeatty.smsbackupandrestorepro.models.Sms;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.Thread;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import static android.R.attr.name;
import static android.icu.lang.UCharacter.GraphemeClusterBreak.L;
import static com.gbeatty.smsbackupandrestorepro.Utils.PREF_ACCOUNT_NAME;
import static com.gbeatty.smsbackupandrestorepro.Utils.SCOPES;
import static com.gbeatty.smsbackupandrestorepro.Utils.createEmail;
import static com.gbeatty.smsbackupandrestorepro.Utils.createLabelIfNotExistAndGetLabelID;
import static com.gbeatty.smsbackupandrestorepro.Utils.getThreadsWithLabelsQuery;
import static com.gbeatty.smsbackupandrestorepro.Utils.insertMessage;

public class BackupService extends IntentService  {

    private Uri uri = Uri.parse("content://sms/");
    private String[] projection = {
            "_id", "address", "read", "body", "date", "type"
    };
    private GoogleAccountCredential credential;
    private com.google.api.services.gmail.Gmail mService = null;
    private SharedPreferences settings;
    private HttpTransport transport;
    private JsonFactory jsonFactory;
    private String account;
    private String labelName;
    private String user = "me";

    private HashMap<String, String> contacts;

    public BackupService() {
        super("BackupService");
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

        contacts = new HashMap<>(200);

        transport = AndroidHttp.newCompatibleTransport();
        jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.gmail.Gmail.Builder(
                transport, jsonFactory, credential)
                .setApplicationName("SMS Backup and Restore Pro")
                .build();
    }

    private void handleBackup() throws IOException, MessagingException {

        ContentResolver resolver = getContentResolver();
        Cursor c = resolver.query(uri, projection, null,null,null);

        String[] labelIDs = {createLabelIfNotExistAndGetLabelID(mService, user, labelName)};

        if (c != null && c.getCount() > 0) {
            c.moveToFirst();
            int totalSMS = c.getCount();
            int count = 0;
            for (int i = 0; i < totalSMS; i++) {
                String id = c.getString(c.getColumnIndexOrThrow("_id"));
                String address = c.getString(c
                        .getColumnIndexOrThrow("address"));
                String msg = c.getString(c.getColumnIndexOrThrow("body"));
                String date = c.getString(c.getColumnIndexOrThrow("date"));
                String folder = "";
                String name = getContactName(this, address);
                if (c.getString(c.getColumnIndexOrThrow("type")).contains("1")) {
                    folder = "inbox";
                } else {
                    folder = "sent";
                }
                if(name == null) name = address;

                String subject = "SMS with " + name;
                String query = "subject:" + subject;

                List<Thread> threads = getThreadsWithLabelsQuery(mService, user, query, labelIDs);

                Thread thread = null;
                if(threads.size() > 0){
                    thread = threads.get(0);
                }


                String from;
                String to;
                String personal;

                if(folder.equals("inbox")){
                    to = account;
                    personal = name;
                    from = address + "@unknown.email";
                }else{
                    to = address + "@unknown.email";
                    personal = "me";
                    from = account;
                }

                MimeMessage email = createEmail(to, from, personal, subject, msg, new Date(Long.valueOf(date)));
                if(thread != null) {
                    insertMessage(mService, user, email, thread.getId(), labelIDs);
                }else{
                    insertMessage(mService, user, email, null, labelIDs);
                }

                count++;
                c.moveToNext();
            }
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
    protected void onHandleIntent(Intent intent) {

        if(intent != null){
            try {
                handleBackup();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }
    }

}
