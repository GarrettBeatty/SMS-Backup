package com.gbeatty.smsbackupandrestorepro.presenter;

import android.content.Intent;

import com.gbeatty.smsbackupandrestorepro.BackupService;
import com.gbeatty.smsbackupandrestorepro.views.MainView;

import junit.framework.Assert;

import static com.gbeatty.smsbackupandrestorepro.Utils.BACKUP_COMPLETE;
import static com.gbeatty.smsbackupandrestorepro.Utils.BACKUP_MESSAGE;
import static com.gbeatty.smsbackupandrestorepro.Utils.BACKUP_STARTING;
import static com.gbeatty.smsbackupandrestorepro.Utils.BACKUP_STOPPING;

public class MainPresenter {

    private MainView view;

    public MainPresenter(MainView view){
        this.view = view;
    }

    public void handleBackupReceiver(Intent intent){
        if(BackupService.RUNNING) {
            int[] message = intent.getIntArrayExtra(BACKUP_MESSAGE);
            int count = message[0];
            int total = message[1];
            updateProgressInfo(count, total);
            int percent = 0;
            if(total != 0){
                 percent = (100 * count) / total;
            }
            updateProgressBar(percent);
            updateBackupButtonText("Stop Backup");
        }else{
            updateBackupButtonText("Backup");
            updateProgressBar(0);
            int status = intent.getIntArrayExtra(BACKUP_MESSAGE)[2];
            if(status == BACKUP_COMPLETE){
                updateProgressInfo("Complete");
            }else if(status == BACKUP_STARTING) {
                updateProgressInfo("Starting...");
            }else if(status == BACKUP_STOPPING) {
                updateProgressInfo("Stopping...");
            }else{
                updateProgressInfo("Idle");
            }
        }
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

    public void backup() {
        if(!BackupService.RUNNING){
            enableBackupButton(false);
            view.getAllSms();
        }else{
            enableBackupButton(false);
            BackupService.RUNNING = false;
        }
    }

}
