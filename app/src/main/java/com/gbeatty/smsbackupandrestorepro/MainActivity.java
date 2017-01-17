package com.gbeatty.smsbackupandrestorepro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class MainActivity extends BaseActivity{


    @BindView(R.id.progress_info)
    TextView progressInfo;
    @BindView(R.id.progress)
    MaterialProgressBar progressBar;
    private BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int[] data = intent.getIntArrayExtra(BackupService.BACKUP_DATA);
                int count = data[0];
                int totalSMS = data[1];
                double percent = (100 * count) / totalSMS;
                progressBar.setProgress((int) percent);
                progressInfo.setText(getResources().getString(R.string.progress_info, count, totalSMS));
            }
        };
        ButterKnife.bind(this);
    }

    @Override
    public void signInResult() {
        getAllSms();
    }

    @OnClick(R.id.backupButton)
    public void backup(){
        getAllSms();
    }

    @OnClick(R.id.restoreButton)
    public void restore(){

    }

    @OnClick(R.id.settingsButton)
    public void settings(){
        startActivity(new Intent(this, PreferenceActivity.class));
    }


    public void getAllSms() {

            if(mCredential.getSelectedAccountName() == null){
                getGoogleAccount(false);
            }else{
                Intent serviceIntent = new Intent(this, BackupService.class);
                startService(serviceIntent);
            }
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver((receiver), new IntentFilter(BackupService.BACKUP_RESULT));
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onStop();
    }
}
