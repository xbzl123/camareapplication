package com.example.camareapplication;


import android.content.Context;
import android.hardware.SensorManager;

public class SensorUtils {
    public static SensorUtils sensorUtils;
    private SensorManager service;

    public SensorUtils(Context context) {
        service = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        service.getDefaultSensor(SensorManager.SENSOR_TEMPERATURE);
    }

    public static SensorUtils getSensorUtils(Context context) {
        if(sensorUtils == null){
            sensorUtils = new SensorUtils(context);
        }
        return sensorUtils;
    }

    public SensorManager getService() {
        return service;
    }
}
