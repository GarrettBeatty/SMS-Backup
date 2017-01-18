package com.gbeatty.smsbackupandrestorepro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.TextView;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;

import java.io.IOException;
import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

import static com.gbeatty.smsbackupandrestorepro.Utils.BACKUP_MESSAGE;
import static com.gbeatty.smsbackupandrestorepro.Utils.BACKUP_RESULT;
import static com.gbeatty.smsbackupandrestorepro.Utils.PREF_ACCOUNT_NAME;
import static com.gbeatty.smsbackupandrestorepro.Utils.REQUEST_AUTHORIZATION;
import static com.gbeatty.smsbackupandrestorepro.Utils.SCOPES;

public class MainActivity extends BaseActivity {


    @BindView(R.id.progress_info)
    TextView progressInfo;
    @BindView(R.id.progress)
    MaterialProgressBar progressBar;
    private BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int[] message = intent.getIntArrayExtra(BACKUP_MESSAGE);
                int count = message[0];
                int total = message[1];
                progressInfo.setText(getResources().getString(R.string.progress_info, count, total));
                int percent = (100 * count) / total;
                progressBar.setProgress(percent);
            }
        };
    }

    @Override
    public void signInResult() {
        getAllSms();
    }

    @OnClick(R.id.backupButton)
    public void backup() {
        getAllSms();
    }

    @OnClick(R.id.restoreButton)
    public void restore() {

    }

    @OnClick(R.id.settingsButton)
    public void settings() {
        startActivity(new Intent(this, PreferenceActivity.class));
    }

    public void getAllSms() {

        if (mCredential.getSelectedAccountName() == null) {
            getGoogleAccount(false);
        } else {
            oAuthTest();
        }
    }

    private void oAuthTest(){
        // Initialize credentials and service object.
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String account = settings.getString(PREF_ACCOUNT_NAME, null);
        credential.setSelectedAccountName(account);
        new TestOAuth(credential).execute();

    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver((receiver),
                new IntentFilter(BACKUP_RESULT)
        );
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onStop();
    }

    //really dirty way of prompting oauth screen
    private class TestOAuth extends AsyncTask<Void, Void, Void> {
        private com.google.api.services.gmail.Gmail mService = null;
        private Exception mLastError = null;

        TestOAuth(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.gmail.Gmail.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Gmail API Android Quickstart")
                    .build();
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
            }
            return null;

        }

        private void getDataFromApi() throws IOException {
            // Get the labels in the user's account.
            String user = "me";
            mService.users().labels().get(user, "abc").execute();

        }

        @Override
        protected void onCancelled() {

            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            REQUEST_AUTHORIZATION);
                }
                else{
                    //user is authenticated start the service
                    Intent serviceIntent = new Intent(getApplicationContext(), BackupService.class);
                    startService(serviceIntent);
                }
            }
        }
    }
}
