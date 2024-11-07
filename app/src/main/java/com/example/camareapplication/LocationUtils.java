package com.example.camareapplication;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.os.Build;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
import java.io.IOException;
import java.util.List;

public class LocationUtils {

    private LocationManager locationManager;
    private Location lastKnownLocation;
    private static volatile LocationUtils locationUtils = null;
    private Context mContext;

    @RequiresApi(api = Build.VERSION_CODES.N)
    private LocationUtils(Activity context, LocationListener locationListener) {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        mContext = context;

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context,new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION},10000);
        }
        if (CameraPreview.INSTANCE.asServer) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,2000,10, locationListener);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locationManager.addNmeaListener(new OnNmeaMessageListener() {
                @Override
                public void onNmeaMessage(String message, long timestamp) {
                    Log.e("yyyy", "message = " + message);
                    if(isValidForNmea(message)){
                        double v = nmeaProgress(message);
                        Toast.makeText(context,"现在的海拔是"+v,Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }else {
            locationManager.addNmeaListener(new OnNmeaMessageListener() {
                @Override
                public void onNmeaMessage(String nmea, long timestamp) {
                    Log.e("yyy","nmea="+nmea);
                    if(isValidForNmea(nmea)){
                        double v = nmeaProgress(nmea);
                        Toast.makeText(context,"现在的海拔是"+v,Toast.LENGTH_SHORT).show();

                    }
                }
            });
        }
        Criteria c = new Criteria();
        c.setAccuracy(Criteria.ACCURACY_FINE);//高精度
        c.setAltitudeRequired(true);//包含高度信息
        c.setBearingRequired(true);//包含方位信息
        c.setSpeedRequired(true);//包含速度信息
        c.setCostAllowed(true);//允许付费
        c.setPowerRequirement(Criteria.POWER_HIGH);//高耗电
        String bestProvider = locationManager.getBestProvider(c, true);
//        boolean providerEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
//        if(providerEnabled){
//            OnNmeaMessageListener listener = new OnNmeaMessageListener() {
//                @Override
//                public void onNmeaMessage(String message, long timestamp) {
//                    Log.e("yyyy","message = "+message);
//                }
//            };
//            locationManager.addNmeaListener(listener);
            lastKnownLocation = locationManager.getLastKnownLocation(bestProvider);
            if (lastKnownLocation == null) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,3000,0, locationListener);
            }
//        }
    }

    private double nmeaProgress(String rawNmea){

        String[] rawNmeaSplit = rawNmea.split(",");
        Log.e("yyy","rawNmeaSplit="+rawNmeaSplit[0]);

        if (rawNmeaSplit[0].equalsIgnoreCase("$GPGGA")){
            double mLastMslAltitude = Double.parseDouble(rawNmeaSplit[9]);
            Log.e("yyy","mLastMslAltitude="+mLastMslAltitude);
            return mLastMslAltitude;
            //send GGA nmea data to handler
//            Message msg = new Message();
//            msg.obj = rawNmea;
//            mHandler.sendMessage(msg);
        }

        return 0;
    }

    private boolean isValidForNmea(String rawNmea){
        boolean valid = true;
        byte[]bytes = rawNmea.getBytes();
        int checksumIndex = rawNmea.indexOf("*");
        //NMEA星號後為checksumnumber
        byte checksumCalcValue = 0;
        int checksumValue;

        //檢查開頭是否為$
        if((rawNmea.charAt(0) != '$') ||(checksumIndex==-1)){
            valid= false;
        }
        //
        if(valid){
            String val = rawNmea.substring(checksumIndex + 1,rawNmea.length()).trim();
            checksumValue= Integer.parseInt(val, 16);
            for(int i = 1; i< checksumIndex; i++){
                checksumCalcValue= (byte) (checksumCalcValue ^ bytes[i]);
            }
            if(checksumValue != checksumCalcValue){
                valid= false;
            }
        }
        Log.e("yyy","valid="+valid);

        return valid;
    }

    public static synchronized LocationUtils getInstance(Activity context,LocationListener locationListener) {
        if(locationUtils == null){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                locationUtils = new LocationUtils(context,locationListener);
            }
        }
        return locationUtils;
    }

    public String getAddress(double latitude, double longitude) {
        String location = "";
        Geocoder geocoder = new Geocoder(mContext);
        try {
            List<Address> fromLocation = geocoder.getFromLocation(latitude, longitude, 1);
            Address address = fromLocation.get(0);
            Log.e("yyyy","address = "+address);

            location = address.getCountryName()+address.getAdminArea()+address.getLocality()+address.getFeatureName();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return location;
    }

}
