package com.gbeatty.smsbackupandrestorepro;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
    private SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        presenter = new MainPresenter(this, settings);

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

    @OnClick(R.id.backupButton)
    public void backup() {
        presenter.backup(mCredential);
    }

    @OnClick(R.id.restoreButton)
    public void restore() {

    }

    @Override
    public void createToast(String text) {
        Toast.makeText(this, text,
                Toast.LENGTH_LONG).show();
    }

    @OnClick(R.id.settingsButton)
    public void settings() {
        startActivity(new Intent(this, PreferenceActivity.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver((receiver),
                new IntentFilter(BACKUP_RESULT)
        );
        presenter.resume();
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

    public void startBackupService(Long interval) {

        Intent serviceIntent = new Intent(getApplicationContext(), BackupService.class);
        startService(serviceIntent);

        boolean autoBackup = settings.getBoolean("auto_backup", true);
        if(autoBackup){
            alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + interval, interval, pintent);
        }
    }

    @Override
    public void enableBackupButton(boolean enabled) {
        backupButton.setEnabled(enabled);
    }
}
