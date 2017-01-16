package com.gbeatty.smsbackupandrestorepro;

import android.content.CursorLoader;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.app.LoaderManager;
import android.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.gbeatty.smsbackupandrestorepro.models.Sms;
import com.gbeatty.smsbackupandrestorepro.presenters.MainPresenter;
import com.gbeatty.smsbackupandrestorepro.views.MainView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class MainActivity extends AppCompatActivity implements MainView {

    private MainPresenter presenter;
    @BindView(R.id.progress_info)
    TextView progressInfo;
    @BindView(R.id.progress)
    MaterialProgressBar progressBar;
    private Resources res;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        presenter = new MainPresenter(this);
        res = getResources();
        ButterKnife.bind(this);
    }

    @OnClick(R.id.backupButton)
    public void backup(){
        presenter.backup();
    }

    @OnClick(R.id.restoreButton)
    public void restore(){
        presenter.restore();
    }

    @OnClick(R.id.settingsButton)
    public void settings(){
        presenter.settings();
    }

    @Override
    public void showSettings() {
        startActivity(new Intent(getApplicationContext(), PreferenceActivity.class));
    }

    public void getAllSms() {

        if(ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_SMS") == PackageManager.PERMISSION_GRANTED) {

        }else{
            final int REQUEST_CODE_ASK_PERMISSIONS = 123;
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{"android.permission.READ_SMS"}, REQUEST_CODE_ASK_PERMISSIONS);
        }
    }
}
