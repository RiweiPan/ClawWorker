package com.example.clawworker;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class ClawWorkerService extends Service {
    private LocalSocketThread socketThread;

    @Override
    public void onCreate() {
        super.onCreate();
        CommandHistoryStore.getInstance().init(getApplicationContext());
        if (socketThread == null) {
            socketThread = new LocalSocketThread(this);
            socketThread.start();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (socketThread != null) {
            socketThread.shutdown();
            socketThread = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
