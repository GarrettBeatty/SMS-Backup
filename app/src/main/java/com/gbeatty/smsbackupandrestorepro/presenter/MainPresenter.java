package com.gbeatty.smsbackupandrestorepro.presenter;

import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.gbeatty.smsbackupandrestorepro.BackupService;
import com.gbeatty.smsbackupandrestorepro.RestoreService;
import com.gbeatty.smsbackupandrestorepro.views.MainView;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.gbeatty.smsbackupandrestorepro.Utils.BACKUP_COMPLETE;
import static com.gbeatty.smsbackupandrestorepro.Utils.BACKUP_IDLE;
import static com.gbeatty.smsbackupandrestorepro.Utils.BACKUP_MESSAGE;
import static com.gbeatty.smsbackupandrestorepro.Utils.BACKUP_RUNNING;
import static com.gbeatty.smsbackupandrestorepro.Utils.BACKUP_STARTING;
import static com.gbeatty.smsbackupandrestorepro.Utils.BACKUP_STOPPING;
import static com.gbeatty.smsbackupandrestorepro.Utils.RESTORE_COMPLETE;
import static com.gbeatty.smsbackupandrestorepro.Utils.RESTORE_MESSAGE;
import static com.gbeatty.smsbackupandrestorepro.Utils.RESTORE_RUNNING;
import static com.gbeatty.smsbackupandrestorepro.Utils.RESTORE_STARTING;
import static com.gbeatty.smsbackupandrestorepro.Utils.RESTORE_STOPPING;

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
            updateProgressInfo(count, total, status);
            updateProgressBar(percent);
            updateBackupButtonText("Stop Backup");

        } else if (status == BACKUP_COMPLETE) {
            updateBackupButtonText("Backup");
            updateProgressBar(0);
            updateProgressInfo("Backup complete");
            updateLastComplete();
        } else if (status == BACKUP_STARTING) {
            updateProgressInfo("Backup starting...");
        } else if (status == BACKUP_STOPPING) {
            updateProgressInfo("Backup stopping...");
        } else if (status == BACKUP_IDLE) {
            updateBackupButtonText("Backup");
            updateProgressBar(0);
            updateProgressInfo("Idle");
        }

        enableBackupButton(true);
    }

    private void updateProgressInfo(int count, int total, int status) {
        if(status > BACKUP_RUNNING){
            view.updateProgressInfoRestore(count, total);
        }else{
            view.updateProgressInfoBackup(count, total);
        }

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

    private void revertToOldDefaultSMS(){
        view.revertToOldDefaultSMS();
    }

    public void backup(GoogleAccountCredential mCredential) {

        if (mCredential.getSelectedAccountName() == null) {
            loginGoogle();
        }else if (!BackupService.RUNNING && !RestoreService.RUNNING) {
            startBackupService();
        } else if(BackupService.RUNNING){
            enableBackupButton(false);
            BackupService.RUNNING = false;
            createToast("Backup stopping...");
        }
    }

    public void resume() {
        updateProgressInfo("Idle");
        updateLastComplete();
    }

    private void updateLastComplete() {
        long lastComplete = settings.getLong("last_complete", Long.MIN_VALUE);
        String dateString;
        if( lastComplete != Long.MIN_VALUE){
            dateString = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss", Locale.getDefault()).format(new Date(lastComplete));
            dateString = "Last backed up: " + dateString;
        }else{
            dateString = "Not Backed up yet.";
        }
        Log.d("Date String", dateString);
        view.updateLastComplete(dateString);
    }

    private void createToast(String text) {
        view.createToast(text);
    }

    public void loginGoogle() {
        view.loginGoogle(false);
    }

    public void startBackupService() {
        createToast("Backup starting...");
        String i = settings.getString("backup_interval", "1");
        Long interval = Long.valueOf(i) * 3600000;
        enableBackupButton(false);
        view.startBackupService(interval);
    }

    public void restore(GoogleAccountCredential mCredential) {

        if (mCredential.getSelectedAccountName() == null) {
            loginGoogle();
        } else if (!RestoreService.RUNNING && !BackupService.RUNNING) {
            startRestoreService();
        } else if(RestoreService.RUNNING){
            enableRestoreButton(false);
            RestoreService.RUNNING = false;
            createToast("Restore stopping...");
        }
    }

    private void startRestoreService() {
        createToast("Restore starting...");
        view.startRestoreService();
    }

    private void enableRestoreButton(boolean b) {
        view.enableRestoreButton(b);
    }

    public void handleRestoreReceiver(Intent intent) {
        Log.d("TEST", "test");
        int[] message = intent.getIntArrayExtra(RESTORE_MESSAGE);
        int count = message[0];
        int total = message[1];
        int status = message[2];

        if (status == RESTORE_RUNNING) {
            int percent = 0;
            if (total != 0) {
                percent = (100 * count) / total;
            }
            updateProgressInfo(count, total, status);
            updateProgressBar(percent);
            updateRestoreButtonText("Stop Restore");

        } else if (status == RESTORE_COMPLETE) {
            updateRestoreButtonText("Restore");
            updateProgressBar(0);
            updateProgressInfo("Restore complete");
            revertToOldDefaultSMS();
        } else if (status == RESTORE_STARTING) {
            updateProgressInfo("Restore starting...");
        } else if (status == RESTORE_STOPPING) {
            updateProgressInfo("Restore stopping...");
        } else if (status == BACKUP_IDLE) {
            updateRestoreButtonText("Restore");
            updateProgressBar(0);
            updateProgressInfo("Idle");
        }

        enableRestoreButton(true);
    }

    private void updateRestoreButtonText(String s) {
        view.updateRestoreButtonText(s);
    }
}
