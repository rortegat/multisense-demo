package com.example.multisense_demo;

import android.util.Log;

import com.cellocator.nano.android.sdk.MultiSenseDeviceCallback;
import com.cellocator.nano.android.sdk.MultiSenseObserver;
import com.cellocator.nano.android.sdk.model.MultiSenseDevice;

public class MultiSenseDeviceCallbackImpl implements MultiSenseDeviceCallback {

    private static final String TAG = "DEVICE_CALLBACK";
    private final MultiSenseObserver multiSenseObserver;

    public MultiSenseDeviceCallbackImpl(MultiSenseObserver multiSenseObserver) {
        this.multiSenseObserver = multiSenseObserver;
    }

    @Override
    public void onError(int errorType, String message) {
        Log.e(TAG, message != null ? message : "Unknown");
    }

    @Override
    public void onChange(MultiSenseDevice multiSenseDevice) {
        // Adding found device by mac address to observer
        multiSenseObserver.addTag(multiSenseDevice.getAddress());
    }
}
