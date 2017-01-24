package com.gbeatty.smsbackupandrestorepro.views;

public interface MainView {

    void updateProgressBar(int percent);

    void updateBackupButtonText(String text);

    void updateProgressInfo(String status);

    void enableBackupButton(boolean enabled);

    void createToast(String text);

    void loginGoogle(boolean b);

    void startBackupService(Long interval);

    void updateLastComplete(String dateString);

    void startRestoreService();

    void updateProgressInfoRestore(int count, int total);

    void updateProgressInfoBackup(int count, int total);

    void updateRestoreButtonText(String s);

    void enableRestoreButton(boolean b);
}
