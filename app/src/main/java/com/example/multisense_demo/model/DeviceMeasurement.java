package com.example.multisense_demo.model;

import java.util.List;

public class DeviceMeasurement {
    private Long deviceId;
    private String datetime;
    private Float battery;
    private Double latitude;
    private Double longitude;
    private List<MultiSenseBeacon> beacons;

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getDatetime() {
        return datetime;
    }

    public void setDatetime(String datetime) {
        this.datetime = datetime;
    }

    public Float getBattery() {
        return battery;
    }

    public void setBattery(Float battery) {
        this.battery = battery;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public List<MultiSenseBeacon> getBeacons() {
        return beacons;
    }

    public void setBeacons(List<MultiSenseBeacon> beacons) {
        this.beacons = beacons;
    }
}
