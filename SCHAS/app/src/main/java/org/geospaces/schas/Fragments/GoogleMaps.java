package org.geospaces.schas.Fragments;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.geospaces.schas.R;
import org.geospaces.schas.utils.db;


import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.abs;
import static java.lang.Math.hypot;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;


public class GoogleMaps extends SupportMapFragment {

    private GoogleMap googleMap;
    private LatLng mPosFija = new LatLng(37.878901,-4.779396);
    private Context mContext;
    LocationManager locationManager;
    Location myLocation;
    Location prevLocation = null;
    Criteria criteria;
    //set min update time to 60 seconds
    long minTime = 60000;
    //set min update distance to 30 meters
    float minDistance =30;
    //list to hold LatLng values
    List<LatLng> locList = new ArrayList<>();
    PolylineOptions trackLine;
    Polyline polyLine;
    LocationListener locListener;
    boolean isFirstPoint = true;

    float speed;
    int speedLevel;

    SensorManager mSensorManager;
    Sensor mSigMotion;
    TriggerEventListener mListener;

    double newLocDist;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = super.onCreateView(inflater, container, savedInstanceState);

        googleMap = getMap();

        setUpMap();

        return v;
    }

    private void setUpMap() {

        mContext = getActivity().getApplicationContext();

        //instantiate the managers for getting locations and using the sigmotionsensor
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        mSensorManager = (SensorManager)mContext.getSystemService(Context.SENSOR_SERVICE);

        //set up the signmotion sensor and link with the sensor manager
        mSigMotion = mSensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);

        //set up the tigger event for the sigmotionsensor to start updates
        mListener = new TriggerEventListener() {
            @Override
            public void onTrigger(TriggerEvent event) {
                //Toast.makeText(mContext, "sig motion triggered", Toast.LENGTH_SHORT).show();
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, 30, locListener);
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
                locList.add(newlatLng);

                //float accuracy = location.getAccuracy();

                //Log.i("speed", String.valueOf(speed));
                //Log.i("new loc dist", String.valueOf(newLocDist));

               // polyLine.setPoints(locList);
                /*for (int z = 0; z < locList.size(); z++) {
                    LatLng point = locList.get(z);
                    trackLine.add(point);
                }*/

                if (isFirstPoint) {

                    //move the camera to the new location
                    googleMap.moveCamera(CameraUpdateFactory.newLatLng(newlatLng));

                    //add a marker at the new location
                    googleMap.addMarker(new MarkerOptions()
                            .flat(true)
                            .position(newlatLng)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.mapmarker))
                            .anchor(.5f, .5f));

                    //add new location to the trackline
                    trackLine.add(newlatLng);

                    polyLine = googleMap.addPolyline(trackLine);

                    //db.getLocationData(location);

                    prevLocation = location;

                    //get the extra info generated by the locationManager
                    speed = location.getSpeed();

                    //calculate the new minTime for the location updates if needed
                    speedCalc();

                    isFirstPoint = false;
                }

                else if (!isFirstPoint){

                    newLocDist = location.distanceTo(prevLocation);

                    //if the new location is more than 25 meters away (for accuracy purposes)
                    if (newLocDist >= 25)
                    {
                        //move the camera to the new location
                        googleMap.moveCamera(CameraUpdateFactory.newLatLng(newlatLng));

                        //add a marker at the new location
                        googleMap.addMarker(new MarkerOptions()
                                .flat(true)
                                .position(newlatLng)
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.mapmarker))
                                .anchor(.5f, .5f));

                        //add new location to the trackline
                        trackLine.add(newlatLng);

                        polyLine = googleMap.addPolyline(trackLine);

                        //db.getLocationData(location);

                        prevLocation = location;

                        //get the extra info generated by the locationManager
                        speed = location.getSpeed();

                        //calculate the new minTime for the location updates if needed
                        speedCalc();
                    }
                }

                //   Toast.makeText(mContext, String.valueOf(newLat)+", "+String.valueOf(newLon), Toast.LENGTH_SHORT).show();
                //   Log.d("OnLocationChanged: ", String.valueOf(newLat) + ", " + String.valueOf(newLon));
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        //googleMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker").snippet("snippet"));
        // Enable MyLocation Layer of Google Map
        googleMap.setMyLocationEnabled(true);

        // Create a criteria object to retrieve provider
        criteria = new Criteria();

        // Get the name of the best provider
        String provider = locationManager.getBestProvider(criteria, true);

        //create the trackline and add it to the map as a polyline
        trackLine =new PolylineOptions()
                .width(5)
                .color(Color.BLUE)
                .geodesic(true);
        polyLine = googleMap.addPolyline(trackLine);

        //TODO implement requestSingleUpdate here instead of last known location
        //Get Current Location
        //myLocation = locationManager.getLastKnownLocation(provider);
        locationManager.requestSingleUpdate(provider, locListener, null);

        //set map type
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        /*
        double lat = myLocation == null ? 47.6205333: myLocation.getLatitude();
        double lon = myLocation == null ? -122.19293: myLocation.getLongitude();

        prevLocation = myLocation;

        // Create a LatLng object for the current location
        LatLng latLng = new LatLng(lat, lon);

        if (latLng != null) locList.add(latLng);

        googleMap.addMarker(new MarkerOptions()
                .flat(true)
                .position(latLng)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.mapmarker))
                .anchor(.5f, .5f));

        // Show the current location in Google Map
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

        // Zoom in the Google Map
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(14));

     //   googleMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon)).title("You are here!"));

        */

        /*
        //create the trackline and add it to the map as a polyline
        trackLine =new PolylineOptions()
                .add(latLng)
                .width(5)
                .color(Color.BLUE)
                .geodesic(true);
        polyLine = googleMap.addPolyline(trackLine);
        */

        //mSensorManager.requestTriggerSensor(mListener, mSigMotion);

        //request location updates at the given minTime or 30 meters
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, 30, locListener);

        //retain the fragment across orientation changes
        setRetainInstance(true);
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
                locationManager.removeUpdates(locListener);
                mSensorManager.requestTriggerSensor(mListener, mSigMotion);
                speedLevel = 0;
                //Toast.makeText(mContext, "not moving", Toast.LENGTH_SHORT).show();
            }
        }
        else if (speed >= .5 && speed < 6.0) {
            //do stuff for walking
            if (speedLevel !=1) {
                locationManager.removeUpdates(locListener);
                speedLevel = 1;
                setMinTime();
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, 30, locListener);
                //Toast.makeText(mContext, "walking", Toast.LENGTH_SHORT).show();
            }
        }
        else if (speed >= 10.0 && speed < 20.0){
            //do stuff for running
            if (speedLevel != 2) {
                locationManager.removeUpdates(locListener);
                speedLevel = 2;
                setMinTime();
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, 30, locListener);
                //Toast.makeText(mContext, "running", Toast.LENGTH_SHORT).show();
            }
        }
        else if (speed > 20.0) {
            //do stuff for driving
            if (speedLevel != 3) {
                locationManager.removeUpdates(locListener);
                speedLevel = 3;
                setMinTime();
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, 30, locListener);
                //Toast.makeText(mContext, "driving", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }

    @Override
    public void onStop()
    {
        super.onStop();
    }

    @Override
    public void onDetach()
    {
        //remove the location listener when the app during onDetach
        locationManager.removeUpdates(locListener);
        //mSensorManager.unregisterListener(this);
        super.onDetach();
    }
}

