package com.example.multisense_demo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.cellocator.nano.android.sdk.MultiSenseManager;
import com.cellocator.nano.android.sdk.MultiSenseObserver;
import com.cellocator.nano.android.sdk.MultiSenseScanner;
import com.cellocator.nano.android.sdk.model.MultiSenseConfiguration;
import com.cellocator.nano.android.sdk.model.MultiSenseEnabledSensors;
import com.cellocator.nano.android.sdk.model.MultiSenseSettings;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MULTISENSE";

    private boolean isScanning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkLocationPermissions();

        Button scan = findViewById(R.id.scan);

        MultiSenseManager multiSenseManager = new MultiSenseManager(this);
        MultiSenseScanner multiSenseScanner = multiSenseManager.createScanner();
        MultiSenseObserver multiSenseObserver = multiSenseManager.createObserver();

        //Attempt to change beacon configuration
        String macAddress = "48.1A.84.00.86.D6";
        MultiSenseConfiguration multiSenseConfiguration = setUpDeviceConfiguration();
        multiSenseObserver.saveConfiguration(macAddress, multiSenseConfiguration);
        MultiSenseSettings multiSenseSettings = setUpMultiSenseSettings();
        multiSenseObserver.saveSettings(macAddress, multiSenseSettings);

        scan.setOnClickListener(view -> {
            if (isScanning) {
                Log.i(TAG, "STOPPING SCAN");
                multiSenseScanner.stopScan();
                scan.setText("START");
            } else {
                Log.i(TAG, "STARTING SCAN");
                scan.setText("STOP");
                // Begin scanning
                multiSenseScanner.scan(new MultiSenseDeviceCallbackImpl(multiSenseObserver));
                // Begin observing
                multiSenseObserver.startObserveTags(new MultiSenseObserverCallbackImpl());
            }
            isScanning = !isScanning;
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

    private MultiSenseConfiguration setUpDeviceConfiguration() {
        MultiSenseEnabledSensors enabledSensors = new MultiSenseEnabledSensors.Builder()
                .setTemperature(true)
                .setHumidity(true)
                .setLight(true)
                .setHallEffect(true)
                .setHumidity(true)
                .setAccelerometer(true)
                .setLoggerEnabled(true)
                .setPowerDown(true)
                .setTxReason(true)
                .create();

        return new MultiSenseConfiguration.Builder()
                .setSensorMask(enabledSensors)
                //Beacon advertisement
                .setProximityTimer(60)
                //Stored data intervals
                .setRelaxedTimer(60)
                .setViolationTimer(60)
                .setAlertTimer(2)
                //THRESHOLDS
                //Temperature
                .setTempUpper(32)
                .setTempLower(-18)
                //Humidity
                .setHumidityUpper(60)
                .setHumidityLower(0)
                //Light
                .setLightThreshold(60)
                //Impact
                .setImpactThreshold(0.200f)
                .create();
    }

    private MultiSenseSettings setUpMultiSenseSettings() {
        return new MultiSenseSettings.Builder()
                .setTransmissionPowerLevel(3)
                .create();
    }
}