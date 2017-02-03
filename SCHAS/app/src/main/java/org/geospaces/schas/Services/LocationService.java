package org.geospaces.schas.Services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;

import org.geospaces.schas.Fragments.GoogleMaps;
import org.geospaces.schas.R;
import org.geospaces.schas.UploadData;
import org.geospaces.schas.UtilityObjectClasses.DatabaseLocationObject;
import org.geospaces.schas.utils.db;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Erik on 3/14/2016.
 */
public class LocationService extends Service {

    IBinder mBinder = new LocalBinder();

    private Context mContext;
    GoogleApiClient client;
    LocationRequest locReq;
    private static LocationManager locationManager;
    Location myLocation;
    Location prevLocation = null;
    Criteria criteria;
    //set min update time to 60 seconds
    private static long minTime = 60000;
    //set min update distance to 30 meters
    private static float minDistance =25;
    //list to hold LatLng values
    //public static List<LatLng> locList;
    private LocationListener locListener = new MyLocationListener();
    //boolean isFirstPoint = true;
    float speed;
    int speedLevel;
    //String provider;

    SensorManager mSensorManager;
    Sensor mSigMotion;
    TriggerEventListener mListener;

    float newLocDist;

    public static boolean appIsRunning = false;

    private static Timer timer = new Timer();

    private Thread locationThread = new LocationThread();
    private static Looper threadLooper = null;

    private static Handler mUIThreadHandler = null;

    public class LocalBinder extends Binder {
            public LocationService getService() {
                return LocationService.this;
            }
    }

    @Override
    public void onCreate() {

        mContext = getApplicationContext();

        Intent clickedOnIntent = new Intent(this, UploadData.class);
        PendingIntent clickedOnPendingIntent = PendingIntent.getActivity(this, 0, clickedOnIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new Notification.Builder(mContext)
                .setContentTitle("SCHAS Location Polling")
                //.setContentText("Location Polling Currently Enabled!")
                .setSmallIcon(R.drawable.ic_place_white_36dp)
                .setContentIntent(clickedOnPendingIntent)
                .build();

        startForeground(7, notification);

        //start new code to create a new thread for location reading
        locationThread.start();

        // Create a criteria object to retrieve provider
        //criteria = new Criteria();

//        //instantiate the managers for getting locations and using the sigmotionsensor
//        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
//        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
//
//        //set up the signmotion sensor and link with the sensor manager
//        mSigMotion = mSensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);

        // Get the name of the best provider
        //provider = locationManager.getBestProvider(criteria, true);

//        //build a GoogleApiClient object that has access to the location API
//        client = new GoogleApiClient.Builder(mContext)
//                .addApi(LocationServices.API)
//                .addConnectionCallbacks(this)
//                .addOnConnectionFailedListener(this)
//                .build();
//        client.connect();
//
//        //build a LocationRequest object with the given parameters
//        locReq = new LocationRequest();
//        locReq.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//        locReq.setInterval(minTime);
//        locReq.setSmallestDisplacement(minDistance);

//        //set up the tigger event for the sigmotionsensor to start updates
//        mListener = new TriggerEventListener() {
//            @Override
//            public void onTrigger(TriggerEvent event) {
//                //Toast.makeText(mContext, "sig motion triggered", Toast.LENGTH_SHORT).show();
//                startPoll();
//            }
//        };

        //create the location listener and begin location collection logic

//        locListener = new LocationListener() {
//            @Override
//            public void onLocationChanged(Location location) {
//                if (location != null) {
//                    if (prevLocation != null) {
//                        newLocDist = location.distanceTo(prevLocation);
//                    } else {
//                        newLocDist = 25;
//                    }
//
//                    //if the new location is more than 25 meters away (for accuracy purposes)
//                    if (newLocDist >= 25 && newLocDist < 500) {
//
//                        int numberOfTextLocations = 0;
//                        try{
//                            numberOfTextLocations = db.GetNumberOfLocations();
//                        }
//                        catch (IOException e){
//                            e.printStackTrace();
//                        }
//                        Log.v("locations amounts", String.valueOf(GoogleMaps.markers.size() + numberOfTextLocations));
//                        if (appIsRunning) {
//                            if (db.canUploadData(mContext) != null) {
//                                if (GoogleMaps.markers.size() + numberOfTextLocations >= 50) {
//
//                                    try {
//                                        db.Upload(mContext, null);
//                                    } catch (IOException e) {
//                                        e.printStackTrace();
//                                    }
//                                }
//                            }
//
//                            long sessionNum = System.currentTimeMillis() / 1000000 * 60;
//
//                            GoogleMaps.plotNewPoint(new DatabaseLocationObject(
//                                    String.valueOf(System.currentTimeMillis() / 1000),
//                                    (float) location.getLatitude(),
//                                    (float) location.getLongitude(),
//                                    String.valueOf(location.getAltitude()),
//                                    String.valueOf(location.getSpeed()),
//                                    String.valueOf(location.getBearing()),
//                                    String.valueOf(location.getAccuracy()),
//                                    String.valueOf(location.getProvider()),
//                                    String.valueOf(sessionNum),
//                                    true
//                            ));
//                            db.lastLocation = location;
//                        }
//                        else{
//                            db.getLocationData(location, location.getProvider());
//                        }
//
//                        prevLocation = location;
//
//                        //get the extra info generated by the locationManager
//                        speed = location.getSpeed();
//                        //Toast.makeText(mContext, "speed is currently: " + String.valueOf(speed), Toast.LENGTH_SHORT).show();
//
//                        Log.i("speed", String.valueOf(speed));
//                        //Toast.makeText(mContext, "speed is "+String.valueOf(speed), Toast.LENGTH_SHORT).show();
//
//                        //calculate the new minTime for the location updates if needed
//                        speedCalc();
//                    }
//                }
//                //   Toast.makeText(mContext, String.valueOf(newLat)+", "+String.valueOf(newLon), Toast.LENGTH_SHORT).show();
//                //   Log.d("OnLocationChanged: ", String.valueOf(newLat) + ", " + String.valueOf(newLon));
//            }
//
//            @Override
//            public void onStatusChanged(String provider, int status, Bundle extras) {
//                if (provider == LocationManager.GPS_PROVIDER) {
//                    if (status == LocationProvider.AVAILABLE) {
//                        startPoll();
//                    } else {
//                        stopPoll();
//                    }
//                }
//            }
//
//            @Override
//            public void onProviderEnabled(String provider) {
//                startPoll();
//            }
//
//            @Override
//            public void onProviderDisabled(String provider) {
//                stopPoll();
//            }
//        };

        //launches a recorder method for a heartbeat once per hour 3600000
        //timer.scheduleAtFixedRate(new heartBeatRecord(), 0, 3600000);

//        startPoll();
    }

    private class heartBeatRecord extends TimerTask{
        public void run(){
            String msg = db.getHeartBeat(mContext);
            try {
                db.Write(msg + "\n");
                if (appIsRunning) {
                    db.Upload(mContext, null);
                }
            } catch (IOException e) {
                Log.e("ERROR", "Exception appending to or uploading file", e);
            }
            Log.d("Heartbeat", "Heartbeat occured");

//            SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(mContext);
//            int timerInterval = SP.getInt("inhalerCapScan", 12);
            int timerInterval = 1;
            timer.schedule(new heartBeatRecord(), 0, (timerInterval * 3600000));
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //TODO not sure what yet...

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        Log.i("LocationService", "Location Service stopped");
        stopPoll();
        //client.disconnect();
        threadLooper.quit();
        super.onDestroy();
    }

    //call this function before making a call to request location updates
    public void setMinTime ()
    {
        //if walking, set minTime to 30 seconds
        if (speedLevel == 1) {
            minTime = 30000;
        }
        //if running, set minTime to 25 seconds
        if (speedLevel == 2) {
            minTime = 25000;
        }
        //if driving, set minTime to 15 seconds
        if (speedLevel == 3) {
            minTime = 15000;
        }
        //Toast.makeText(mContext, "setMinTime called", Toast.LENGTH_SHORT).show();
    }

    public void speedCalc() {
        //do things based on the calculated speed
        if (speed < .5) {
            //do stuff for not moving
            if (speedLevel != 0) {
                stopPoll();
                mSensorManager.requestTriggerSensor(mListener, mSigMotion);
                speedLevel = 0;
            }
        } else if (speed >= .5 && speed < 6.0) {
            //do stuff for walking
            if (speedLevel != 1) {
                //locationManager.removeUpdates(locListener);
                speedLevel = 1;
                setMinTime();
                //locReq.setInterval(minTime);
                //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, 30, locListener);
                startPoll();
            }
        } else if (speed >= 10.0 && speed < 20.0) {
            //do stuff for running
            if (speedLevel != 2) {
                //locationManager.removeUpdates(locListener);
                speedLevel = 2;
                setMinTime();
                //locReq.setInterval(minTime);
                //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, 30, locListener);
                startPoll();
            }
        } else if (speed > 20.0) {
            //do stuff for driving
            if (speedLevel != 3) {
                //locationManager.removeUpdates(locListener);
                speedLevel = 3;
                setMinTime();
                //locReq.setInterval(minTime);
                //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, 30, locListener);
                startPoll();
            }
        }
    }

    public void startPoll() {
        //LocationServices.FusedLocationApi.requestLocationUpdates(client, locReq, locListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, locListener);
    }

    public void stopPoll() {
        //LocationServices.FusedLocationApi.removeLocationUpdates(client, locListener);
        locationManager.removeUpdates(locListener);
    }

    private class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location newLocation) {
            final Location location = newLocation;

            if (location != null) {
                if (prevLocation != null) {
                    newLocDist = location.distanceTo(prevLocation);
                } else {
                    newLocDist = 25;
                }

                //if the new location is more than 25 meters away (for accuracy purposes)
                if (newLocDist >= 25 && newLocDist < 500) {

                    int numberOfTextLocations = 0;
                    try{
                        numberOfTextLocations = db.GetNumberOfLocations();
                    }
                    catch (IOException e){
                        e.printStackTrace();
                    }
                    Log.v("locations amounts", String.valueOf(GoogleMaps.markers.size() + numberOfTextLocations));
                    if (appIsRunning) {
                        if (db.canUploadData(mContext) != null) {
                            if (GoogleMaps.markers.size() + numberOfTextLocations >= 50) {
                                try {
                                    db.Upload(mContext, null);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        final long sessionNum = System.currentTimeMillis() / 1000000 * 60;

                        if (mUIThreadHandler == null){
                            mUIThreadHandler = new Handler(Looper.getMainLooper());
                        }
                        mUIThreadHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                GoogleMaps.plotNewPoint(new DatabaseLocationObject(
                                        String.valueOf(System.currentTimeMillis() / 1000),
                                        (float) location.getLatitude(),
                                        (float) location.getLongitude(),
                                        String.valueOf(location.getAltitude()),
                                        String.valueOf(location.getSpeed()),
                                        String.valueOf(location.getBearing()),
                                        String.valueOf(location.getAccuracy()),
                                        String.valueOf(location.getProvider()),
                                        String.valueOf(sessionNum),
                                        true
                                ));
                            }
                        });

                        db.lastLocation = location;
                    }
                    else{
                        db.getLocationData(location, location.getProvider());
                    }

                    prevLocation = location;

                    //get the extra info generated by the locationManager
                    speed = location.getSpeed();
                    //Toast.makeText(mContext, "speed is currently: " + String.valueOf(speed), Toast.LENGTH_SHORT).show();

                    Log.i("speed", String.valueOf(speed));
                    //Toast.makeText(mContext, "speed is "+String.valueOf(speed), Toast.LENGTH_SHORT).show();

                    //calculate the new minTime for the location updates if needed
                    speedCalc();
                }
            }
            //   Toast.makeText(mContext, String.valueOf(newLat)+", "+String.valueOf(newLon), Toast.LENGTH_SHORT).show();
            //   Log.d("OnLocationChanged: ", String.valueOf(newLat) + ", " + String.valueOf(newLon));
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            if (provider == LocationManager.GPS_PROVIDER) {
                if (status == LocationProvider.AVAILABLE) {
                    startPoll();
                } else {
                    stopPoll();
                }
            }
        }

        @Override
        public void onProviderEnabled(String provider) {
            startPoll();
        }

        @Override
        public void onProviderDisabled(String provider) {
            stopPoll();
        }
    }

    private class LocationThread extends Thread {
        @Override
        public void run() {
            Looper.prepare();

            //instantiate the managers for getting locations and using the sigmotionsensor
            locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
            mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);

            //set up the signmotion sensor and link with the sensor manager
            mSigMotion = mSensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);

            //set up the tigger event for the sigmotionsensor to start updates
            mListener = new TriggerEventListener() {
                @Override
                public void onTrigger(TriggerEvent event) {
                    //Toast.makeText(mContext, "sig motion triggered", Toast.LENGTH_SHORT).show();
                    startPoll();
                }
            };

            //launches a recorder method for a heartbeat at the interval for inhaler cap update checks specified by the settings page
//            SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(mContext);
//            int timerInterval = SP.getInt("inhalerCapScan", 12);
            int timerInterval = 1;
            timer.schedule(new heartBeatRecord(), 0, (timerInterval * 3600000));

            startPoll();

            threadLooper = Looper.myLooper();
            Looper.loop();

            stopPoll();
        }
    }
}


//Archived code, saved for those just-in-case-we-need-it moments

//locList.add(newlatLng);

//float accuracy = location.getAccuracy();

//Log.i("speed", String.valueOf(speed));
//Log.i("new loc dist", String.valueOf(newLocDist));

// polyLine.setPoints(locList);
/*for (int z = 0; z < locList.size(); z++) {
                    LatLng point = locList.get(z);
                    trackLine.add(point);
                }*/

                /*if (isFirstPoint) {
                    if(appIsRunning) {
                        GoogleMaps.plotNewPoint(newlatLng);
                    }

                    db.getLocationData(location, provider);

                    prevLocation = location;

                    //get the extra info generated by the locationManager
                    speed = location.getSpeed();
                    Log.i("speed", String.valueOf(speed));

                    //calculate the new minTime for the location updates if needed
                    speedCalc();

                    isFirstPoint = false;
                }*/

//originally came from the google maps activity, services do not support onDetach()
    /*@Override
    public void onDetach()
    {
        //remove the location listener when the app during onDetach
        //locationManager.removeUpdates(locListener);
        stopPoll();
        client.disconnect();
        //mSensorManager.unregisterListener(this);
        super.onDetach();
    }*/