package com.gbeatty.smsbackupandrestorepro;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.gbeatty.smsbackupandrestorepro.presenter.MainPresenter;
import com.gbeatty.smsbackupandrestorepro.views.MainView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

import static com.gbeatty.smsbackupandrestorepro.Utils.BACKUP_RESULT;
import static com.gbeatty.smsbackupandrestorepro.Utils.DEFAULT_SMS_REQUEST;
import static com.gbeatty.smsbackupandrestorepro.Utils.RESTORE_RESULT;

public class MainActivity extends BaseActivity implements MainView {

    @BindView(R.id.progress_info)
    TextView progressInfo;
    @BindView(R.id.progress)
    MaterialProgressBar progressBar;
    @BindView(R.id.backupButton)
    Button backupButton;
    @BindView(R.id.last_complete) TextView lastComplete;
    @BindView(R.id.restoreButton) Button restoreButton;
    private BroadcastReceiver backupReceiver;
    private BroadcastReceiver restoreReceiver;
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

        backupReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                presenter.handleBackupReceiver(intent);
            }
        };

        restoreReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                presenter.handleRestoreReceiver(intent);
            }
        };

        intent = new Intent(this, BackupService.class);
        pintent = PendingIntent.getService(this, 0, intent, 0);
        alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

    }

    @OnClick(R.id.backupButton)
    public void backup() {
        presenter.backup(mCredential);
    }

    @OnClick(R.id.restoreButton)
    public void restore() {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (! Telephony.Sms.getDefaultSmsPackage(this).equals(getPackageName()) ) {

                  AlertDialog.Builder  builder = new AlertDialog.Builder(this);
                    builder.setMessage("This app needs to be temporarily set as the default SMS app to restore SMS.")
                            .setCancelable(false)
                            .setTitle("Alert!")
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {


                                }
                            })
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @TargetApi(19)
                                public void onClick(DialogInterface dialog, int id) {

                                    Intent intent =
                                            new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);

                                    intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME,
                                            getPackageName());

                                    startActivityForResult(intent, DEFAULT_SMS_REQUEST);
                                }
                            });
                    builder.show();

                }else{
                    presenter.restore(mCredential);
                }
            }else{
                presenter.restore(mCredential);
            }
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
        LocalBroadcastManager.getInstance(this).registerReceiver((backupReceiver),
                new IntentFilter(BACKUP_RESULT)
        );
        LocalBroadcastManager.getInstance(this).registerReceiver((restoreReceiver),
                new IntentFilter(RESTORE_RESULT)
        );
        presenter.resume();
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(backupReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(restoreReceiver);
        super.onStop();
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
        if (autoBackup) {
            alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + interval, interval, pintent);
        }
    }

    @Override
    public void updateLastComplete(String text) {
        lastComplete.setText(text);
    }

    @Override
    public void startRestoreService() {
        Intent serviceIntent = new Intent(getApplicationContext(), RestoreService.class);
        startService(serviceIntent);
        Log.d("fjdafasd", "Fdasfas");
    }

    @Override
    public void updateProgressInfoRestore(int count, int total) {
        progressInfo.setText(getResources().getString(R.string.progress_info_restore, count, total));
    }

    @Override
    public void updateProgressInfoBackup(int count, int total) {
        progressInfo.setText(getResources().getString(R.string.progress_info_backup, count, total));
    }

    @Override
    public void updateRestoreButtonText(String s) {
        restoreButton.setText(s);
    }

    @Override
    public void enableRestoreButton(boolean b) {
        restoreButton.setEnabled(b);
    }

    @Override
    public void revertToOldDefaultSMS() {
        AlertDialog.Builder  builder = new AlertDialog.Builder(this);
        builder.setMessage("You can chnage back to the old default SMS now")
                .setCancelable(false)
                .setTitle("Alert!")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @TargetApi(19)
                    public void onClick(DialogInterface dialog, int id) {

                        Intent intent =
                                new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);

                        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME,
                                "defaultSmsApp");

                        startActivityForResult(intent, DEFAULT_SMS_REQUEST);
                    }
                });
        builder.show();
    }

    @Override
    public void enableBackupButton(boolean enabled) {
        backupButton.setEnabled(enabled);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case DEFAULT_SMS_REQUEST:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    if(Telephony.Sms.getDefaultSmsPackage(this).equals(getPackageName())){
                        presenter.restore(mCredential);
                    }
                }

        }
    }
}
