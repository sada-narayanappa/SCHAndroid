package mymodule.app2.mymodule.app2;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.location.Address;
import android.view.Surface;
import android.view.WindowManager;

import java.util.Calendar;

/**
 * Created by Michael Fleming on 11/2/2014.
 */
public class SysStrings{

    private static float x = 5;
    private static float y;
    private static float z;
    private static float light;
    private static float pressure;
    private static float temp;




    public static String getTheTime() {
        Calendar c = Calendar.getInstance();
        int seconds = c.get(Calendar.SECOND);
        int minutes = c.get(Calendar.MINUTE);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int mili = c.get(Calendar.MILLISECOND);
        int day = c.get(Calendar.DAY_OF_MONTH);
        int mon = c.get(Calendar.MONTH);
        int yr = c.get(Calendar.YEAR);


        String time = hour + ":" + minutes + ":" + seconds + ":" + mili +"\n" + mon + "/" + day + "/" + yr;

        return time;
    }

    //Requires an instance of LocationManger passed in from the Main activity used
    // LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    // Use above code to create location manager and pass in locationManager to the function for accurate results
    //Include the below in the AndroidManifest.xml File of the project to allow for acces of these features
    //<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    //<uses-permission android:name="android.permission. ACCESS_COARSE_LOCATION" />
    //<uses-permission android:name="android.permission.INTERNET" />
    public static String getTheGPS(LocationManager locationManager){
        String loc = "Coarse:\n";

        //Checks for Coarse GPS location then Fine, if fine is available that reading will be taken, else Coarse will
        if(locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) != null) {
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) != null){
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                loc = "Fine: \n";
        }
            loc += "Lat: " + location.getLatitude() + "\nLon: " + location.getLongitude() + "\nSpeed: " + location.getSpeed()
            + "\nAltitude: " + location.getAltitude() + "\nBearing: " + location.getBearing();
        }
        else{
            loc = "null";
        }


        return loc;

    }
    public static String getOrientation(Context context){

        final int rotation = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getOrientation();
        switch (rotation) {
            case Surface.ROTATION_0:
                return "portrait";
            case Surface.ROTATION_90:
                return "landscape";
            case Surface.ROTATION_180:
                return "reverse portrait";
            default:
                return "reverse landscape";
        }
    }

    //        connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    //        wifiman = (WifiManager) getSystemService(Context.WIFI_SERVICE);
    // Include the above lines in your onCreate method and pass in (connManager,wifiman)

    public static String getWIFI(ConnectivityManager cm,WifiManager wifiManager){

        NetworkInfo mWifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (mWifi.isConnected()) {
            int linkSpeed = wifiManager.getConnectionInfo().getLinkSpeed();
            return "wifi is connected at the speed of: " + linkSpeed +"Mbps" + "\nAt a strength of: " + wifiManager.getConnectionInfo().getRssi() + "dBm";
        }
        else{return "wifi is not connected";}
    }


    //
    //
    public static String getIEMI(TelephonyManager tm){
        return tm.getDeviceId();
    }


    public static void updateGravity(SensorEvent event){
        x = event.values[0];
        y = event.values[1];
        z = event.values[2];


    }
    public static String getGravity(SensorManager sm){
        if(sm.getDefaultSensor(Sensor.TYPE_GRAVITY) != null){
            return "x: " + x + "\ny: " + y + "\nz: " + z;
        }
        else{return "Phone has no gravity sensor";}
    }

    public static void updateLight(SensorEvent event){
        light = event.values[0];
    }
    public static String getLight(SensorManager sm){
        if (sm.getDefaultSensor(Sensor.TYPE_LIGHT) != null){
            return "light(lux) = " + light;

        }
        else{return ""+light;}


    }

    public static void updatePressure(SensorEvent event){
        pressure = event.values[0];
    }
    public static String getPressure(SensorManager sm){
        if(sm.getDefaultSensor(Sensor.TYPE_PRESSURE) != null) {
            return "pressure(hPa) = " + pressure;
        }
        else{return "Phone has no pressure sensor";}

    }

    public static void updateTemp(SensorEvent event){
        temp = event.values[0];
    }
    public static String getTemp(SensorManager sm){
        if(sm.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null) {
            return "Temp(C) = " + temp;
        }
        else{return "Phone has no temperature sensor";}

    }


}
