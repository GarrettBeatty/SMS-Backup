package com.gbeatty.smsbackupandrestorepro.presenters;

import com.gbeatty.smsbackupandrestorepro.views.MainView;

public class MainPresenter {

    private MainView view;

    public MainPresenter(MainView mainView){
        this.view = mainView;
    }

    public void backup() {
        view.getAllSms();
    }

    public void restore(){

    }

    public void settings() {
        view.showSettings();
    }

}
