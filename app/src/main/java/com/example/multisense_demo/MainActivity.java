package com.example.multisense_demo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.cellocator.nano.android.sdk.MultiSenseDeviceCallback;
import com.cellocator.nano.android.sdk.MultiSenseManager;
import com.cellocator.nano.android.sdk.MultiSenseObserver;
import com.cellocator.nano.android.sdk.MultiSenseObserverCallback;
import com.cellocator.nano.android.sdk.MultiSenseReadingLoggerStatus;
import com.cellocator.nano.android.sdk.MultiSenseScanner;
import com.cellocator.nano.android.sdk.model.MultiSenseDevice;
import com.cellocator.nano.android.sdk.model.MultiSenseSensors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "RIOT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkLocationPermissions();

        Button foreground = findViewById(R.id.foreground);
        Button scan = findViewById(R.id.scan);

        MultiSenseManager multiSenseManager = new MultiSenseManager(this);
        MultiSenseScanner multiSenseScanner = multiSenseManager.createScanner();
        MultiSenseObserver multiSenseObserver = multiSenseManager.createObserver();

        foreground.setOnClickListener(v -> {
            Log.i(TAG, "OBSERVING: " + multiSenseObserver.isObserving());
            if (multiSenseObserver.isObserving()) {
                multiSenseObserver.startForegroundMode();
                Toast.makeText(this, "ForegroundService has started", Toast.LENGTH_LONG).show();
            }
        });

        scan.setOnClickListener(view -> {
            Log.i(TAG, "SCANNING: " + multiSenseScanner.isScanning());
            if (multiSenseScanner.isScanning()) {
                Log.i(TAG, "STOPPING SCAN");
                multiSenseScanner.stopScan();
                scan.setText("Start");
            } else {
                // Begin scanning
                Log.i(TAG, "STARTING SCAN");
                multiSenseScanner.scan(new MultiSenseDeviceCallback() {
                    @Override
                    public void onError(int errorType, String message) {
                        Log.e(TAG, message != null ? message : "Unknown");
                    }

                    @Override
                    public void onChange(MultiSenseDevice multiSenseDevice) {
                        // Adding found device by mac address to observer
                        multiSenseObserver.addTag(multiSenseDevice.getAddress());
                    }
                });

                // Begin observing
                multiSenseObserver.startObserveTags(new MultiSenseObserverCallback() {
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
                        for (MultiSenseSensors sensorSample : multiSenseDevice.getSensors()) {
                            String temp = sensorSample.getTemperature() != null ? sensorSample.getTemperature().toString() : " - ";
                            Log.i(TAG, mac + ": " + temp);

                        }
                    }
                });
                scan.setText("Stop");
            }
        });
    }

    public void checkLocationPermissions() {
        //if the user hasn't granted access location permissions yet
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //Request for required permissions
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }

    }
}