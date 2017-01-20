package com.gbeatty.smsbackupandrestorepro;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.gbeatty.smsbackupandrestorepro.presenter.MainPresenter;
import com.gbeatty.smsbackupandrestorepro.views.MainView;
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
import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

import static com.gbeatty.smsbackupandrestorepro.Utils.BACKUP_RESULT;
import static com.gbeatty.smsbackupandrestorepro.Utils.PREF_ACCOUNT_NAME;
import static com.gbeatty.smsbackupandrestorepro.Utils.REQUEST_AUTHORIZATION;
import static com.gbeatty.smsbackupandrestorepro.Utils.SCOPES;

public class MainActivity extends BaseActivity implements MainView {


    @BindView(R.id.progress_info)
    TextView progressInfo;
    @BindView(R.id.progress)
    MaterialProgressBar progressBar;
    private BroadcastReceiver receiver;
    @BindView(R.id.backupButton)
    Button backupButton;
    private MainPresenter presenter;
    private Intent intent;
    private PendingIntent pintent;
    private AlarmManager alarm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        presenter = new MainPresenter(this);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                presenter.handleBackupReceiver(intent);
            }
        };

        intent = new Intent(this, BackupService.class);
        pintent = PendingIntent.getService(this, 0, intent, 0);
        alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);

    }

    @Override
    public void signInResult() {
        getAllSms();
    }

    @OnClick(R.id.backupButton)
    public void backup() {
        presenter.backup();
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

    @Override
    public void updateProgressInfo(int count, int total) {
        progressInfo.setText(getResources().getString(R.string.progress_info, count, total));
    }

    @Override
    public void updateProgressBar(int percent) {
        progressBar.setProgress(percent);
    }

    @Override
    public void updateBackupButtonText(String text) {
        backupButton.setText(text);
    }

    @Override
    public void updateProgressInfo(String status) {
        progressInfo.setText(getResources().getString(R.string.progress_info_status, status));
    }

    private void startBackupService() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String i = settings.getString("backup_interval", "1");
        Long interval = Long.valueOf(i) * 3600000;

        boolean autoBackup = settings.getBoolean("auto_backup", true);
        if(autoBackup){
            alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pintent);
        }else{
            Intent serviceIntent = new Intent(getApplicationContext(), BackupService.class);
            startService(serviceIntent);
        }

        Log.d("TRYING", "TESTING");
    }

    @Override
    public void enableBackupButton(boolean enabled) {
        backupButton.setEnabled(enabled);
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
                   startBackupService();
                }
            }
        }
    }

}
