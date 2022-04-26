package com.example.multisense_demo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.cellocator.nano.android.sdk.MultiSenseDeviceCallback;
import com.cellocator.nano.android.sdk.MultiSenseManager;
import com.cellocator.nano.android.sdk.MultiSenseObserver;
import com.cellocator.nano.android.sdk.MultiSenseObserverCallback;
import com.cellocator.nano.android.sdk.MultiSenseReadingLoggerStatus;
import com.cellocator.nano.android.sdk.MultiSenseScanner;
import com.cellocator.nano.android.sdk.model.MultiSenseDevice;
import com.cellocator.nano.android.sdk.model.MultiSenseSensors;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class MyService extends Service {

    private static final String TAG = "MULTISENSE";

    private MultiSenseScanner multiSenseScanner;
    private MultiSenseObserver multiSenseObserver;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        MultiSenseManager multiSenseManager = new MultiSenseManager(this);
        multiSenseScanner = multiSenseManager.createScanner();
        multiSenseObserver = multiSenseManager.createObserver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        startNotification();

        startScanning();

        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        stopScanning();
        super.onDestroy();
    }

    private void startScanning() {
        Log.i(TAG, "STARTING SCAN");

        multiSenseScanner.scan(new MultiSenseDeviceCallback() {
            @Override
            public void onError(int errorType, String message) {
                Log.e(TAG, message != null ? message : "Unknown");
            }

            @Override
            public void onChange(MultiSenseDevice multiSenseDevice) {
                // Adding found device by mac address to observer
                Log.i(TAG, "ADDING " + multiSenseDevice.getAddress());
                multiSenseObserver.addTag(multiSenseDevice.getAddress());
            }
        });

        multiSenseObserver.addTag("48.1A.84.00.69.24");
        multiSenseObserver.addTag("48.1A.84.00.86.D6");
        multiSenseObserver.startForegroundMode();

        Log.i(TAG, multiSenseObserver.toString());

        // Begin observing
        multiSenseObserver.startObserveTags(new MultiSenseObserverCallback() {
            @Override
            public void onReadingLoggerStatusChange(String s, MultiSenseReadingLoggerStatus multiSenseReadingLoggerStatus) {
                Log.i(TAG, "[" + s + "] " + multiSenseReadingLoggerStatus.getStatus() + " " + multiSenseReadingLoggerStatus.getPercent());
                if (multiSenseReadingLoggerStatus.getStatus().equals(MultiSenseReadingLoggerStatus.Status.SUCCESS)) {
                    Log.i(TAG, "LOAD COMPLETED");
                }
            }

            @Override
            public void onError(int errorType, String message) {
                Log.e(TAG, message != null ? message : "");
            }

            @Override
            public void onChange(MultiSenseDevice multiSenseDevice) {
                // Reading device general info
                String mac = multiSenseDevice.getAddress();
                Log.i(TAG, mac);
                for (MultiSenseSensors sensorSample : multiSenseDevice.getSensors()) {


                    String temp = sensorSample.getTemperature() != null ? sensorSample.getTemperature().toString() : " - ";
                    String info = "[" + mac + "] " + temp;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && sensorSample.getCreateDate() != null) {
                        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(sensorSample.getCreateDate().toInstant(), ZoneId.systemDefault());
                        info = info + " @ " + zonedDateTime;
                    }

                    Log.i(TAG, info);
                }
            }
        });
    }

    private void stopScanning() {
        Log.i(TAG, "STOPPING SCAN");
        multiSenseObserver.stopObserveTags();
        multiSenseScanner.stopScan();
    }

    private void startNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel notificationChannel = new NotificationChannel("007", "MS_CHANNEL", NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setLightColor(Color.GREEN);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);

            Intent notificationIntent = new Intent(this, NotificationActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

            Notification notification = new Notification.Builder(this, notificationChannel.getId())
                    .setContentTitle("SensaPlus")
                    .setContentText("Scanning in the foreground")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentIntent(pendingIntent)
                    .setTicker("sensaplus")
                    .build();

            // Notification ID cannot be 0.
            startForeground(1, notification);
            Toast.makeText(this, "ForegroundService has started", Toast.LENGTH_LONG).show();
        } else
            Log.e(TAG, "Minimum required version is Oreo");
    }

}
