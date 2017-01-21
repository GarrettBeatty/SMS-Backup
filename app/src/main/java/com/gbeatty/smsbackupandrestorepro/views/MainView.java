package com.gbeatty.smsbackupandrestorepro.views;

public interface MainView {
    void updateProgressInfo(int count, int total);

    void updateProgressBar(int percent);

    void updateBackupButtonText(String text);

    void updateProgressInfo(String status);

    void enableBackupButton(boolean enabled);

    void updateNotification(String text);

    void activateNotification(String title, String content);

    void createToast(String text);

    void testOAuth();

    void loginGoogle(boolean b);

    void startBackupService();
}
