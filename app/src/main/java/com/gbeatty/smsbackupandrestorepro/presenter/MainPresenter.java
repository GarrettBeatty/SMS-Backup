package com.gbeatty.smsbackupandrestorepro.presenter;

import android.content.Intent;
import android.content.SharedPreferences;

import com.gbeatty.smsbackupandrestorepro.BackupService;
import com.gbeatty.smsbackupandrestorepro.views.MainView;

import static com.gbeatty.smsbackupandrestorepro.Utils.BACKUP_COMPLETE;
import static com.gbeatty.smsbackupandrestorepro.Utils.BACKUP_IDLE;
import static com.gbeatty.smsbackupandrestorepro.Utils.BACKUP_MESSAGE;
import static com.gbeatty.smsbackupandrestorepro.Utils.BACKUP_RUNNING;
import static com.gbeatty.smsbackupandrestorepro.Utils.BACKUP_STARTING;
import static com.gbeatty.smsbackupandrestorepro.Utils.BACKUP_STOPPING;

public class MainPresenter {

    private MainView view;
    private SharedPreferences settings;

    public MainPresenter(MainView view, SharedPreferences settings){
        this.view = view;
        this.settings = settings;
    }

    public void handleBackupReceiver(Intent intent){
        int[] message = intent.getIntArrayExtra(BACKUP_MESSAGE);
        int count = message[0];
        int total = message[1];
        int status = message[2];

        if(status == BACKUP_RUNNING){
            int percent = 0;
            if(total != 0){
                percent = (100 * count) / total;
            }
            updateProgressInfo(count, total);
            updateProgressBar(percent);
            updateBackupButtonText("Stop Backup");

        }
        else if(status == BACKUP_COMPLETE){
            updateBackupButtonText("Backup");
            updateProgressBar(0);
            updateProgressInfo("Complete");
        }else if(status == BACKUP_STARTING) {
            updateProgressInfo("Starting...");
        }else if(status == BACKUP_STOPPING) {
            updateProgressInfo("Stopping...");
        }else if(status == BACKUP_IDLE){
            updateBackupButtonText("Backup");
            updateProgressBar(0);
            updateProgressInfo("Idle");
        }

        if(settings.getBoolean("notifications", false)) updateNotification(count, total, status);
        enableBackupButton(true);
    }

    private void updateProgressInfo(int count, int total){
        view.updateProgressInfo(count, total);
    }

    private void updateProgressInfo(String status){
        view.updateProgressInfo(status);
    }

    private void updateProgressBar(int percent){
        view.updateProgressBar(percent);
    }

    private void updateBackupButtonText(String text){
        view.updateBackupButtonText(text);
    }

    private void enableBackupButton(boolean enabled){
        view.enableBackupButton(enabled);
    }

    private void updateNotification(int count, int total, int status){
        switch (status){
            case BACKUP_STARTING:
                view.updateNotification("Progress: Starting...");
                break;
            case BACKUP_RUNNING:
                view.updateNotification("" + count + " out of " + total + " SMS backed up");
                break;
            case BACKUP_STOPPING:
                view.updateNotification("Progress: Stopping...");
                break;
            case BACKUP_COMPLETE:
                view.updateNotification("Progress: Complete");
                break;
            case BACKUP_IDLE:
                view.updateNotification("Progress: Idle");
        }
    }

    public void backup() {
        if(!BackupService.RUNNING){
            enableBackupButton(false);
            view.getAllSms();
            if(settings.getBoolean("notifications", false)){
                view.activateNotification("SMS Backup", "SMS Backup");
            }
        }else{
            enableBackupButton(false);
            BackupService.RUNNING = false;
        }
    }

}
