package com.gbeatty.smsbackupandrestorepro;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.gbeatty.smsbackupandrestorepro.models.Sms;
import com.gbeatty.smsbackupandrestorepro.presenters.MainPresenter;
import com.gbeatty.smsbackupandrestorepro.views.MainView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements MainView {

    private MainPresenter presenter;
    @BindView(R.id.progress_info)
    TextView progressInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        presenter = new MainPresenter(this);
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

    public List<Sms> getAllSms() {
        List<Sms> lstSms = new ArrayList<>();

        if(ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_SMS") == PackageManager.PERMISSION_GRANTED) {
            Sms objSms;
            Uri message = Uri.parse("content://sms/");
            ContentResolver cr = getContentResolver();

            Cursor c = cr.query(message, null, null, null, null);
            startManagingCursor(c);
            int totalSMS = c.getCount();
            Resources res = getResources();

            progressInfo.setText(res.getString(R.string.progress_info, 0 , totalSMS));

            if (c.moveToFirst()) {
                for (int i = 0; i < totalSMS; i++) {

                    objSms = new Sms();
                    objSms.setId(c.getString(c.getColumnIndexOrThrow("_id")));
                    objSms.setAddress(c.getString(c
                            .getColumnIndexOrThrow("address")));
                    objSms.setMsg(c.getString(c.getColumnIndexOrThrow("body")));
                    objSms.setReadState(c.getString(c.getColumnIndex("read")));
                    objSms.setTime(c.getString(c.getColumnIndexOrThrow("date")));
                    if (c.getString(c.getColumnIndexOrThrow("type")).contains("1")) {
                        objSms.setFolderName("inbox");
                    } else {
                        objSms.setFolderName("sent");
                    }

                    Log.d("sms", objSms.getMsg());

                    lstSms.add(objSms);
                    c.moveToNext();
                }
            }
            // else {
            // throw new RuntimeException("You have no SMS");
            // }
            c.close();

        }else{
            final int REQUEST_CODE_ASK_PERMISSIONS = 123;
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{"android.permission.READ_SMS"}, REQUEST_CODE_ASK_PERMISSIONS);
        }


        return lstSms;
    }

}
