package com.insertcoolnamehere.showandsell;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.widget.Toast;

public class GetUpdatesService extends Service {
    private Looper mLooper;
    private UpdateHandler mHandler;

    @Override
    public void onCreate() {
        // start background thread to work on
        HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        mLooper = thread.getLooper();
        mHandler = new UpdateHandler(mLooper);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("#AintNeverGettingCuffed");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Starting update service", Toast.LENGTH_SHORT).show();

        Message msg = mHandler.obtainMessage();
        msg.arg1 = startId;
        mHandler.sendMessage(msg);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "Update service done", Toast.LENGTH_SHORT).show();
    }

    private class UpdateHandler extends Handler {
        public UpdateHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            // TODO: do work here
            while(true) {

            }
        }
    }
}
