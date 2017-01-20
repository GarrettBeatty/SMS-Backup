package com.gbeatty.smsbackupandrestorepro.views;

public interface MainView {
    void updateProgressInfo(int count, int total);

    void updateProgressBar(int percent);

    void updateBackupButtonText(String text);

    void updateProgressInfo(String status);

    void enableBackupButton(boolean enabled);

    void getAllSms();
}
