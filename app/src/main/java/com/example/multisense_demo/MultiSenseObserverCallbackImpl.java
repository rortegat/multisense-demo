package com.example.multisense_demo;

import android.util.Log;

import com.cellocator.nano.android.sdk.MultiSenseObserverCallback;
import com.cellocator.nano.android.sdk.MultiSenseReadingLoggerStatus;
import com.cellocator.nano.android.sdk.model.MultiSenseDevice;
import com.cellocator.nano.android.sdk.model.MultiSenseSensors;

import java.util.Date;

public class MultiSenseObserverCallbackImpl implements MultiSenseObserverCallback {

    private static final String TAG = "OBSERVER_CALLBACK";

    @Override
    public void onReadingLoggerStatusChange(String s, MultiSenseReadingLoggerStatus multiSenseReadingLoggerStatus) {
        Log.i(TAG, "STATUS: " + multiSenseReadingLoggerStatus.getStatus() + " (" + s + "): " + multiSenseReadingLoggerStatus.getPercent());
        if (multiSenseReadingLoggerStatus.getStatus().equals(MultiSenseReadingLoggerStatus.Status.SUCCESS)) {
            Log.i(TAG, "COMPLETED");
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
        Log.i(TAG, multiSenseDevice.getConfiguration().toString());
        Log.i(TAG, multiSenseDevice.getSettings().toString());
        for (MultiSenseSensors sensorSample : multiSenseDevice.getSensors()) {
            String temp = sensorSample.getTemperature() != null ? sensorSample.getTemperature().toString() : " - ";
            Date date = sensorSample.getCreateDate();
            Log.i(TAG, mac + ": " + temp + " @ " + date);
        }
    }
}
