package com.google.audioworker.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.audioworker.R;
import com.google.audioworker.functions.commands.CommandHelper;
import com.google.audioworker.functions.common.WorkerFunction;
import com.google.audioworker.functions.controllers.MainController;
import com.google.audioworker.utils.Constants;

public class AudioWorkerService extends Service implements WorkerFunction.WorkerFunctionListener {
    private static final String TAG = Constants.packageTag("AudioWorkerService");
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "AudioWorkerServiceChannel";

    private final IBinder binder = new AudioWorkerBinder();

    private MainController mMainController;
    private CommandHelper.BroadcastHandler mBroadcastReceiver;

    public class AudioWorkerBinder extends Binder {
        public AudioWorkerService getService() {
            return AudioWorkerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mMainController = new MainController();
        mMainController.activate(this);

        mBroadcastReceiver =
                CommandHelper.BroadcastHandler.registerReceiver(this, this::onFunctionReceived);

        createNotificationChannel();
        Notification notification =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("AudioWorker Active")
                        .setContentText("Running audio tasks.")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        CommandHelper.BroadcastHandler.unregisterReceiver(this, mBroadcastReceiver);
        mMainController.destroy();
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "onTaskRemoved() is called");
        mMainController.destroy();
        stopSelf();
        super.onTaskRemoved(rootIntent);
    }

    public MainController getMainController() {
        return mMainController;
    }

    private void onFunctionReceived(WorkerFunction function) {
        Log.d(TAG, "onFunctionReceived via Service(" + function + ")");
        mMainController.execute(function, this);
    }

    @Override
    public void onAckReceived(WorkerFunction.Ack ack) {
        Log.d(TAG, "onAckReceived via Service(" + ack + ")");
        // This logic can also be broadcast to the active UI if needed
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel =
                new NotificationChannel(
                        CHANNEL_ID,
                        "AudioWorker Service Channel",
                        NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(serviceChannel);
        }
    }
}
