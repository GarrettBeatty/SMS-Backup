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

public class MainActivity extends AppCompatActivity implements MainView, LoaderManager.LoaderCallbacks<Cursor> {

    private MainPresenter presenter;
    @BindView(R.id.progress_info)
    TextView progressInfo;
    @BindView(R.id.progress)
    MaterialProgressBar progressBar;
    private Resources res;
    private static final int LOADER_ID = 1;
    private Uri uri = Uri.parse("content://sms/");
    private String[] projection = {
            "_id","address","read","body","date","type"
    };



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
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1)
            startActivity(new Intent(getApplicationContext(), PreferenceActivity.class));
        else
            startActivity(new Intent(getApplicationContext(), PreferenceActivityCompat.class));
    }

    public void getAllSms() {

        if(ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_SMS") == PackageManager.PERMISSION_GRANTED) {
            getLoaderManager().initLoader(LOADER_ID, null, this);
        }else{
            final int REQUEST_CODE_ASK_PERMISSIONS = 123;
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{"android.permission.READ_SMS"}, REQUEST_CODE_ASK_PERMISSIONS);
        }

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(MainActivity.this, uri, projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
        if(c != null && c.getCount() > 0){
            c.moveToFirst();
            int totalSMS = c.getCount();
            int count = 0;
            Sms objSms;
            for (int i = 0; i < totalSMS; i++) {
                objSms = new Sms();
                objSms.setId(c.getString(c.getColumnIndexOrThrow("_id")));
                objSms.setAddress(c.getString(c
                        .getColumnIndexOrThrow("address")));
                objSms.setReadState(c.getString(c.getColumnIndex("read")));
                objSms.setMsg(c.getString(c.getColumnIndexOrThrow("body")));
                objSms.setTime(c.getString(c.getColumnIndexOrThrow("date")));
                if (c.getString(c.getColumnIndexOrThrow("type")).contains("1")) {
                    objSms.setFolderName("inbox");
                } else {
                    objSms.setFolderName("sent");
                }
                count++;
                double percent = (100 * count) / totalSMS;
                progressBar.setProgress((int) percent);
                progressInfo.setText(res.getString(R.string.progress_info, count, totalSMS));
                c.moveToNext();
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
