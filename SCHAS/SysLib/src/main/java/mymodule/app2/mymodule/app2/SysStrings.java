package mymodule.app2.mymodule.app2;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
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
    public static void updateGravity(SensorEvent event){
        x = event.values[0];
        y = event.values[1];
        z = event.values[2];


    }
    public static String getGravity(){
        return "x: " + x  + "\ny: " + y  + "\nz: " + z ;
    }

    public static void updateLight(SensorEvent event){
        light = event.values[0];
    }
    public static String getLight(){
        return "light = " + light;
    }

}
