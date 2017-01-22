package com.gbeatty.smsbackupandrestorepro.presenter;

import android.content.Intent;
import android.content.SharedPreferences;

import com.gbeatty.smsbackupandrestorepro.BackupService;
import com.gbeatty.smsbackupandrestorepro.views.MainView;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

import static com.gbeatty.smsbackupandrestorepro.Utils.BACKUP_COMPLETE;
import static com.gbeatty.smsbackupandrestorepro.Utils.BACKUP_IDLE;
import static com.gbeatty.smsbackupandrestorepro.Utils.BACKUP_MESSAGE;
import static com.gbeatty.smsbackupandrestorepro.Utils.BACKUP_RUNNING;
import static com.gbeatty.smsbackupandrestorepro.Utils.BACKUP_STARTING;
import static com.gbeatty.smsbackupandrestorepro.Utils.BACKUP_STOPPING;

public class MainPresenter {

    private MainView view;
    private SharedPreferences settings;

    public MainPresenter(MainView view, SharedPreferences settings) {
        this.view = view;
        this.settings = settings;
    }

    public void handleBackupReceiver(Intent intent) {
        int[] message = intent.getIntArrayExtra(BACKUP_MESSAGE);
        int count = message[0];
        int total = message[1];
        int status = message[2];

        if (status == BACKUP_RUNNING) {
            int percent = 0;
            if (total != 0) {
                percent = (100 * count) / total;
            }
            updateProgressInfo(count, total);
            updateProgressBar(percent);
            updateBackupButtonText("Stop Backup");

        } else if (status == BACKUP_COMPLETE) {
            updateBackupButtonText("Backup");
            updateProgressBar(0);
            updateProgressInfo("Complete");
        } else if (status == BACKUP_STARTING) {
            updateProgressInfo("Starting...");
        } else if (status == BACKUP_STOPPING) {
            updateProgressInfo("Stopping...");
        } else if (status == BACKUP_IDLE) {
            updateBackupButtonText("Backup");
            updateProgressBar(0);
            updateProgressInfo("Idle");
        }

        enableBackupButton(true);
    }

    private void updateProgressInfo(int count, int total) {
        view.updateProgressInfo(count, total);
    }

    private void updateProgressInfo(String status) {
        view.updateProgressInfo(status);
    }

    private void updateProgressBar(int percent) {
        view.updateProgressBar(percent);
    }

    private void updateBackupButtonText(String text) {
        view.updateBackupButtonText(text);
    }

    private void enableBackupButton(boolean enabled) {
        view.enableBackupButton(enabled);
    }

    public void oauth(GoogleAccountCredential mCredential) {

        if (mCredential.getSelectedAccountName() == null) {
            loginGoogle();
        } else {
            backup();
        }
    }

    public void backup() {
        if (!BackupService.RUNNING) {
            startBackupService();
        } else {
            enableBackupButton(false);
            BackupService.RUNNING = false;
            createToast("Stopping backup...");
        }
    }

    public void resume() {
        updateProgressInfo("Idle");
    }

    private void createToast(String text) {
        view.createToast(text);
    }

    public void loginGoogle() {
        view.loginGoogle(false);
    }

    public void startBackupService() {
        createToast("Starting backup...");
        String i = settings.getString("backup_interval", "1");
        Long interval = Long.valueOf(i) * 3600000;
        enableBackupButton(false);
        view.startBackupService(interval);
    }
}
