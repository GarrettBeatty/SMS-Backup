package com.gbeatty.smsbackupandrestorepro.views;

import com.gbeatty.smsbackupandrestorepro.models.Sms;

import java.util.List;

public interface MainView {


    void showSettings();
    List<Sms> getAllSms();
}
