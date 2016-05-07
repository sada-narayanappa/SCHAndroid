package org.geospaces.schas.Services;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.geospaces.schas.Fragments.GoogleMaps;
import org.geospaces.schas.R;
import org.geospaces.schas.UploadData;
import org.geospaces.schas.utils.db;

import java.util.List;

/**
 * Created by Erik on 3/14/2016.
 */
public class LocationService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    IBinder mBinder = new LocalBinder();

    private Context mContext;
    GoogleApiClient client;
    LocationRequest locReq;
    //LocationManager locationManager;
    Location myLocation;
    Location prevLocation = null;
    Criteria criteria;
    //set min update time to 60 seconds
    long minTime = 60000;
    //set min update distance to 30 meters
    float minDistance =25;
    //list to hold LatLng values
    //public static List<LatLng> locList;
     LocationListener locListener;
    //boolean isFirstPoint = true;
    float speed;
    int speedLevel;
    //String provider;

    SensorManager mSensorManager;
    Sensor mSigMotion;
    TriggerEventListener mListener;

    float newLocDist;

    public static boolean appIsRunning = false;

    public class LocalBinder extends Binder {
            public LocationService getService() {
                return LocationService.this;
            }
    }

    @Override
    public void onCreate() {

        mContext = getApplicationContext();

        Notification notification = new Notification.Builder(mContext)
                .setContentTitle("SCHAS Location Service")
                .setContentText("Thank you for sharing your locations to help us help you!")
                .setSmallIcon(R.drawable.ic_location_on_black)
                .build();

        startForeground(7, notification);

        // Create a criteria object to retrieve provider
        criteria = new Criteria();

        //instantiate the managers for getting locations and using the sigmotionsensor
        //locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        mSensorManager = (SensorManager)mContext.getSystemService(Context.SENSOR_SERVICE);

        //set up the signmotion sensor and link with the sensor manager
        mSigMotion = mSensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);

        // Get the name of the best provider
        //provider = locationManager.getBestProvider(criteria, true);

        //build a GoogleApiClient object that has access to the location API
        client = new GoogleApiClient.Builder(mContext)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        client.connect();

        //build a LocationRequest object with the given parameters
        locReq = new LocationRequest();
        locReq.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locReq.setInterval(minTime);
        locReq.setSmallestDisplacement(minDistance);

        //set up the tigger event for the sigmotionsensor to start updates
        mListener = new TriggerEventListener() {
            @Override
            public void onTrigger(TriggerEvent event) {
                //Toast.makeText(mContext, "sig motion triggered", Toast.LENGTH_SHORT).show();
                startPoll();
            }
        };

        //create the location listener and begin location collection logic
        locListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                //add location lat and lon to LatLng and add to list
                double newLat = location == null ? 0: location.getLatitude();
                double newLon = location == null ? 0: location.getLongitude();
                LatLng newlatLng = new LatLng(newLat, newLon);

                if (location != null) {
                    if (prevLocation != null) {
                        newLocDist = location.distanceTo(prevLocation);
                    } else {
                        newLocDist = 25;
                    }

                    //if the new location is more than 25 meters away (for accuracy purposes)
                    if (newLocDist >= 25) {
                        if (appIsRunning) {
                            GoogleMaps.plotNewPoint(newlatLng);
                        }

                        db.getLocationData(location, location.getProvider());

                        prevLocation = location;

                        //get the extra info generated by the locationManager
                        speed = location.getSpeed();

                        Log.i("speed", String.valueOf(speed));

                        //calculate the new minTime for the location updates if needed
                        speedCalc();
                    }
                }
                //   Toast.makeText(mContext, String.valueOf(newLat)+", "+String.valueOf(newLon), Toast.LENGTH_SHORT).show();
                //   Log.d("OnLocationChanged: ", String.valueOf(newLat) + ", " + String.valueOf(newLon));
            }
        };
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
        client.disconnect();
        super.onDestroy();
    }

    //call this function before making a call to request location updates
    public void setMinTime ()
    {
        //if walking, set minTime to 60 seconds
        if (speedLevel == 1) {
            minTime = 60000;
        }
        //if running, set minTime to 30 seconds
        if (speedLevel == 2) {
            minTime = 30000;
        }
        //if driving, set minTime to 10 seconds
        if (speedLevel == 3) {
            minTime = 10000;
        }
        //Toast.makeText(mContext, "setMinTime called", Toast.LENGTH_SHORT).show();
    }

    public void speedCalc() {
        //do things based on the calculated speed
        if (speed < .5 ) {
            //do stuff for not moving
            if (speedLevel !=0) {
                stopPoll();
                mSensorManager.requestTriggerSensor(mListener, mSigMotion);
                speedLevel = 0;
                //Toast.makeText(mContext, "not moving", Toast.LENGTH_SHORT).show();
            }
        }
        else if (speed >= .5 && speed < 6.0) {
            //do stuff for walking
            if (speedLevel !=1) {
                //locationManager.removeUpdates(locListener);
                speedLevel = 1;
                setMinTime();
                locReq.setInterval(minTime);
                //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, 30, locListener);
                startPoll();
                //Toast.makeText(mContext, "walking", Toast.LENGTH_SHORT).show();
            }
        }
        else if (speed >= 10.0 && speed < 20.0){
            //do stuff for running
            if (speedLevel != 2) {
                //locationManager.removeUpdates(locListener);
                speedLevel = 2;
                setMinTime();
                locReq.setInterval(minTime);
                //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, 30, locListener);
                startPoll();
                //Toast.makeText(mContext, "running", Toast.LENGTH_SHORT).show();
            }
        }
        else if (speed > 20.0) {
            //do stuff for driving
            if (speedLevel != 3) {
                //locationManager.removeUpdates(locListener);
                speedLevel = 3;
                setMinTime();
                locReq.setInterval(minTime);
                //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, 30, locListener);
                startPoll();
                //Toast.makeText(mContext, "driving", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        startPoll();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        stopPoll();
    }

    @Override
    public void onConnectionSuspended(int result) {
        stopPoll();
    }

    public void startPoll() {
        if (client.isConnected()) {
            LocationServices.FusedLocationApi.requestLocationUpdates(client, locReq, locListener);
            UploadData.startStop.setText("Stop");
            UploadData.startStop.setBackgroundColor(Color.RED);
        }
        if (!client.isConnected()) {
            client.connect();
        }
    }

    public void stopPoll() {
        LocationServices.FusedLocationApi.removeLocationUpdates(client, locListener);
        UploadData.startStop.setText("Start");
        UploadData.startStop.setBackgroundColor(Color.GREEN);
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