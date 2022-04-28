package com.example.multisense_demo;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.cellocator.nano.android.sdk.MultiSenseDeviceCallback;
import com.cellocator.nano.android.sdk.MultiSenseManager;
import com.cellocator.nano.android.sdk.MultiSenseObserver;
import com.cellocator.nano.android.sdk.MultiSenseObserverCallback;
import com.cellocator.nano.android.sdk.MultiSenseReadingLoggerStatus;
import com.cellocator.nano.android.sdk.MultiSenseScanner;
import com.cellocator.nano.android.sdk.model.MultiSenseDevice;
import com.cellocator.nano.android.sdk.model.MultiSenseSensors;
import com.example.multisense_demo.model.DeviceMeasurement;
import com.example.multisense_demo.model.MultiSenseBeacon;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.net.NetworkInterface;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MyService extends Service {

    private static final String TAG = "MULTISENSE";
    private static final String TOPIC = "android/measurement";

    private MultiSenseScanner multiSenseScanner;
    private MultiSenseObserver multiSenseObserver;
    private MqttAndroidClient client;

    private final DeviceMeasurement deviceMeasurement = new DeviceMeasurement();
    private final Gson gson = new Gson();
    private final Timer timer = new Timer();
    private FusedLocationProviderClient fusedLocationClient;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "CREATING SERVICE");

        Long deviceId = toLong(getMacAddress());
        deviceMeasurement.setDeviceId(deviceId);
        deviceMeasurement.setBattery(100f);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        MultiSenseManager multiSenseManager = new MultiSenseManager(this);
        multiSenseScanner = multiSenseManager.createScanner();
        multiSenseObserver = multiSenseManager.createObserver();

        String connectionUri = "tcp://34.211.58.239:1883";
        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this, connectionUri, clientId);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startTracking();
        startNotification();
        mqttClientConnect();
        startScanning();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        timer.cancel();
        stopScanning();
        mqttClientDisconnect();
        super.onDestroy();
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

    private void startTracking() {
        getCurrentLocation();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                getCurrentLocation();
            }
        }, 0, TimeUnit.MINUTES.toMillis(1));
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

        //multiSenseObserver.addTag("48.1A.84.00.69.24");
        //multiSenseObserver.addTag("48.1A.84.00.86.D6");
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
                //Convert mac address hex to decimal
                Long beaconId = toLong(multiSenseDevice.getAddress());
                List<MultiSenseBeacon> beacons = new ArrayList<>();

                // Reading sensors data
                for (MultiSenseSensors sensorSample : multiSenseDevice.getSensors()) {

                    //If measurement doesn't have temperature, we don't need it
                    if (sensorSample.getTemperature() != null) {

                        MultiSenseBeacon beacon = new MultiSenseBeacon();
                        beacon.setBeaconId(beaconId);
                        beacon.setTemperature(sensorSample.getTemperature());
                        System.out.println(sensorSample.getTxReason());

                        if (sensorSample.getBatteryLevel() != null)
                            beacon.setBattery(sensorSample.getBatteryLevel().floatValue());

                        if (sensorSample.getLight() != null)
                            beacon.setLight(sensorSample.getLight());

                        if (sensorSample.getHumidity() != null)
                            beacon.setHumidity(sensorSample.getHumidity());

                        if (sensorSample.isOpenPackage() != null)
                            beacon.setOpenPackage(sensorSample.isOpenPackage());

                        if (sensorSample.getAccelerometerX() != null)
                            beacon.setAccX(sensorSample.getAccelerometerX());

                        if (sensorSample.getAccelerometerY() != null)
                            beacon.setAccY(sensorSample.getAccelerometerY());

                        if (sensorSample.getAccelerometerZ() != null)
                            beacon.setAccZ(sensorSample.getAccelerometerZ());

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && sensorSample.getCreateDate() != null) {
                            ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(sensorSample.getCreateDate().toInstant(), ZoneId.systemDefault());
                            beacon.setDatetime(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(zonedDateTime));
                        }
                        beacons.add(beacon);
                    }
                }
                deviceMeasurement.setBeacons(beacons);
                mqttPublishMeasurements();
            }
        });
    }

    private void stopScanning() {
        Log.i(TAG, "STOPPING SCAN");
        multiSenseObserver.stopObserveTags();
        multiSenseScanner.stopScan();
    }

    private void mqttClientConnect() {
        try {
            client.connect()
                    .setActionCallback(new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {
                            // We are connected
                            Log.d(TAG, "MQTT connection success");
                            Toast.makeText(getApplicationContext(), "Mqtt connected!!", Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                            // Something went wrong e.g. connection timeout or firewall problems
                            Log.d(TAG, "MQTT disconnection failure" + exception.getMessage());
                            Toast.makeText(getApplicationContext(), "Mqtt connection failed", Toast.LENGTH_LONG).show();
                        }
                    });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void mqttClientDisconnect() {
        try {
            client.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void mqttPublishMeasurements() {
        try {
            client.publish(TOPIC, gson.toJson(deviceMeasurement).getBytes(), 0, false);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            fusedLocationClient.getLastLocation().addOnSuccessListener(this.getMainExecutor(), new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(android.location.Location location) {
                    if (location != null) {
                        Log.i(TAG, "LOCATION UPDATED");
                        deviceMeasurement.setLatitude(location.getLatitude());
                        deviceMeasurement.setLongitude(location.getLongitude());
                        deviceMeasurement.setDatetime(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(ZonedDateTime.now()));
                    }
                }
            });
    }

    private String getMacAddress() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(Integer.toHexString(b & 0xFF)).append(".");
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
            //handle exception
        }
        return "";
    }

    private Long toLong(String macAddress) {
        macAddress = macAddress.replace(".", "");
        return Long.parseLong(macAddress, 16);
    }

}
