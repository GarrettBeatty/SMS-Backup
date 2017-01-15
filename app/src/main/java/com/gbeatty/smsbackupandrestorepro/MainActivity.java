package com.gbeatty.smsbackupandrestorepro;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.gbeatty.smsbackupandrestorepro.presenters.MainPresenter;
import com.gbeatty.smsbackupandrestorepro.views.MainView;

public class MainActivity extends AppCompatActivity implements MainView {

    MainPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        presenter = new MainPresenter(this);
    }

    public void backup(View v){
        presenter.backup();
    }

    public void restore(View v){
        presenter.restore();
    }

    public void backupSettings(View v){
        presenter.backupSettings();
    }

    public void restoreSettings(View v){
        presenter.restoreSettings();
    }

}
