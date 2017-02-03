package org.geospaces.schas.Fragments;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.TriggerEventListener;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.geospaces.schas.AsyncTasks.DownloadPrevLocations;
import org.geospaces.schas.R;
import org.geospaces.schas.Services.LocationService;
import org.geospaces.schas.Services.StepCounter;
import org.geospaces.schas.UtilityObjectClasses.DatabaseLocationObject;
import org.geospaces.schas.utils.CustomExceptionHandler;
import org.geospaces.schas.utils.db;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//import com.google.android.gms.location.LocationListener;


public class GoogleMaps extends SupportMapFragment implements GoogleMap.OnMarkerClickListener, OnMapReadyCallback {

    LocationService mService;
    boolean mBound;

    private static GoogleMap googleMap;
    private LatLng mPosFija = new LatLng(37.878901, -4.779396);
    private static Context mContext;
    static GoogleApiClient client;
    static LocationRequest locReq;
    static LocationManager locationManager;
    Location lastLocation;
    Location prevLocation = null;
    Criteria criteria;
    //    //set min update time to 60 seconds
    static long minTime = 20000;
    //    //set min update distance to 30 meters
    static float minDistance = 25;
    //    //list to hold LatLng values
    public static List<DatabaseLocationObject> locList;
    public static List<LatLng> secondaryLocList;
    static PolylineOptions trackLine;
    static PolylineOptions secondaryTrackline;
    static Polyline polyLine;
    static Polyline secondaryPolyline;
    static LocationListener locListener;
    //    boolean isFirstPoint = true;
    public static List<Marker> markers;
    public static List<Marker> secondaryMarkers;
    public static int lineCount = 0;
    //
    String provider;
    //
    float speed;
    int speedLevel;
    //
    SensorManager mSensorManager;
    Sensor mSigMotion;
    TriggerEventListener mListener;
    //
    float newLocDist;

    Handler mHandler = new Handler (Looper.getMainLooper()) {
        /*
         * handleMessage() defines the operations to perform when
         * the Handler receives a new Message to process.
         */
        @Override
        public void handleMessage(Message inputMessage) {

        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocationService.LocalBinder binder = (LocationService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler());

        View v = super.onCreateView(inflater, container, savedInstanceState);

        getMapAsync(this);

        //setUpMap();

        return v;
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        //LocationService.appIsRunning = true;

        mContext = getActivity().getApplicationContext();

        // Create a criteria object to retrieve provider
        criteria = new Criteria();

        //instantiate the managers for getting locations and using the sigMotionSensor
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);

        //check to see if GPS is enabled for the device, and if not prompt the user to enable
        //GpsStatusCheck();

        //set up the sigmotion sensor and link with the sensor manager
        mSigMotion = mSensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);

        // Get the name of the best provider
        provider = locationManager.getBestProvider(criteria, true);

        //Get Current Location
        lastLocation = locationManager.getLastKnownLocation(provider);
        db.lastLocation = lastLocation;

//        //build a GoogleApiClient object that has access to the location API
//        client = new GoogleApiClient.Builder(mContext)
//                .addApi(LocationServices.API)
//                .addConnectionCallbacks(this)
//                .addOnConnectionFailedListener(this)
//                .build();
//        client.connect();

//        //build a LocationRequest object with the given parameters
//        locReq = new LocationRequest();
//        locReq.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//        locReq.setInterval(minTime);
//        locReq.setSmallestDisplacement(minDistance);

        //set up the tigger event for the sigmotionsensor to start updates
//        mListener = new TriggerEventListener() {
//            @Override
//            public void onTrigger(TriggerEvent event) {
//                //Toast.makeText(mContext, "sig motion triggered", Toast.LENGTH_SHORT).show();
//                startPoll();
//            }
//        };

        /*locListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                //add location lat and lon to LatLng and add to list
                double newLat = location == null ? 0 : location.getLatitude();
                double newLon = location == null ? 0 : location.getLongitude();
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
                            plotNewPoint(newlatLng);
                        }

                        db.getLocationData(location, location.getProvider());

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
            }

            @Override
            public void onProviderEnabled(String provider) {
                startPoll();
            }

            @Override
            public void onProviderDisabled(String provider) {
                stopPoll();
            }
        };*/

        Intent startLocationService = new Intent(mContext, LocationService.class);
        mContext.startService(startLocationService);

        Intent startStepCounterService = new Intent(mContext, StepCounter.class);
        mContext.startService(startStepCounterService);

        //create the trackline
        trackLine = new PolylineOptions()
                .width(5)
                .color(Color.BLUE)
                .geodesic(true);

        secondaryTrackline = new PolylineOptions()
                .width(5)
                .color(Color.RED)
                .geodesic(true);

        locList = new ArrayList<>();
        markers = new ArrayList<>();
        secondaryLocList = new ArrayList<>();
        secondaryMarkers = new ArrayList<>();

        try {
            locList = db.ReadFromLocationsFile();
        } catch (IOException e) {
            Log.i("plotting primary", "could not read from primary text file");
            e.printStackTrace();
            //Toast.makeText(mContext, "could not read from loc.txt", Toast.LENGTH_SHORT).show();
        }
        try {
            db.plotSecondaryTxtPoints();
        } catch (IOException e) {
            Log.i("plotting secondary", "could not read from secondary text file");
            e.printStackTrace();
        }

        for (DatabaseLocationObject dlo : locList) {
            LatLng nextLoc = new LatLng(dlo.lat, dlo.lon);
            Log.i("StartingLocPlot", nextLoc.toString());

            if (dlo.GetValidity()){
                Marker nextMarker = googleMap.addMarker(new MarkerOptions()
                        .flat(true)
                        .position(nextLoc)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ballblue16))
                        .anchor(.5f, .5f));
                nextMarker.setTag(dlo);
                trackLine.add(nextLoc);
                markers.add(nextMarker);
            }
            else{
                Marker nextMarker = googleMap.addMarker(new MarkerOptions()
                        .flat(true)
                        .position(nextLoc)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ballred16))
                        .anchor(.5f, .5f));
                nextMarker.setTag(dlo);
                trackLine.add(nextLoc);
                markers.add(nextMarker);
            }

        }

        for (LatLng nextLoc: secondaryLocList) {
            Log.i("startingSecondaryPlot", nextLoc.toString());
            Marker nextMarker = googleMap.addMarker(new MarkerOptions()
                    .flat(true)
                    .position(nextLoc)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ballorange16))
                    .anchor(.5f, .5f));
            //nextMarker.setTag(new CustomMarker(null, false));
            secondaryTrackline.add(nextLoc);
            secondaryMarkers.add(nextMarker);
        }

        //Log.i("locList", locList.toString());
        //Log.i("secondaryLocList", secondaryLocList.toString());

        // Enable MyLocation Layer of Google Map
        googleMap.setMyLocationEnabled(true);

        // Zoom in the Google Map
        //googleMap.animateCamera(CameraUpdateFactory.zoomTo(14));

        //create the trackline and add it to the map as a polyline
        /*trackLine =new PolylineOptions()
                .width(5)
                .color(Color.BLUE)
                .geodesic(true);*/
        //polyLine = googleMap.addPolyline(trackLine);

        //locationManager.requestSingleUpdate(provider, locListener, null);

        //set map type
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        double lat = lastLocation == null ? 47.6205333 : lastLocation.getLatitude();
        double lon = lastLocation == null ? -122.19293 : lastLocation.getLongitude();

        //prevLocation = myLocation;

        // Create a LatLng object for the current location
        LatLng latLng = new LatLng(lat, lon);

        trackLine.add(latLng);
        //if (latLng != null) locList.add(latLng);

        Marker firstMarker = googleMap.addMarker(new MarkerOptions()
                .flat(true)
                .position(latLng)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ballblue16))
                .anchor(.5f, .5f));
        firstMarker.setTag(new DatabaseLocationObject(
                String.valueOf(System.currentTimeMillis() / 1000),
                (float) latLng.latitude,
                (float) latLng.longitude,
                "null",
                "null",
                "null",
                "null",
                "null",
                "null",
                true
        ));
        markers.add(firstMarker);

        // Show the current location in Google Map
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

        // Zoom in the Google Map
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(14));

        //   googleMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon)).title("You are here!"));


        //create the trackline and add it to the map as a polyline
//        trackLine =new PolylineOptions()
//                .add(latLng)
//                .width(5)
//                .color(Color.BLUE)
//                .geodesic(true);
//        polyLine = googleMap.addPolyline(trackLine);

        polyLine = googleMap.addPolyline(trackLine);
        secondaryPolyline = googleMap.addPolyline(secondaryTrackline);

        //mSensorManager.requestTriggerSensor(mListener, mSigMotion);

        //request location updates at the given minTime or 30 meters
        //LocationServices.FusedLocationApi.requestLocationUpdates(client, locReq, locListener);
        //startPoll();

        googleMap.setOnMarkerClickListener(this);

        LocationService.appIsRunning = true;

        new DownloadPrevLocations().execute(mContext);

        //retain the fragment across orientation changes
        setRetainInstance(true);
    }

    @Override
    public boolean onMarkerClick(final Marker marker){
        DatabaseLocationObject dlo = (DatabaseLocationObject) marker.getTag();

        if (dlo.GetValidity()) {
            dlo.isValid = false;
            //change marker color here
            marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ballred16));
            Log.i("marker clicked", String.valueOf(dlo.GetValidity()));
        }
        else{
            dlo.isValid = true;
            //change marker color here
            marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ballblue16));
            Log.i("marker clicked", String.valueOf(dlo.GetValidity()));
        }

        return false;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDetach() {
        LocationService.appIsRunning = false;
        db.PrepareTextFile();
        super.onDetach();
        //remove the location listener when the app during onDetach
        //locationManager.removeUpdates(locListener);
        //stopPoll();
        //client.disconnect();
        //mSensorManager.unregisterListener(this);
    }

    public static void removeMarkers() {
        googleMap.clear();
    }

    public static void plotNewPoint(DatabaseLocationObject dlo) {
        //locList.add(dlo);

        double newLat = dlo.lat;
        double newLon = dlo.lon;
        LatLng newLatLng = new LatLng(newLat, newLon);

        //move the camera to the new location
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(newLatLng));

        //add a marker at the new location
        Marker newMarker = googleMap.addMarker(new MarkerOptions()
                .flat(true)
                .position(newLatLng)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ballblue16))
                .anchor(.5f, .5f));
        newMarker.setTag(dlo);
        markers.add(newMarker);

        //add new location to the trackline
        trackLine.add(newLatLng);

        polyLine = googleMap.addPolyline(trackLine);
    }

    public static void startPoll() {
        //LocationServices.FusedLocationApi.requestLocationUpdates(client, locReq, locListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, locListener);
    }

    public static void stopPoll() {
        //LocationServices.FusedLocationApi.removeLocationUpdates(client, locListener);
        locationManager.removeUpdates(locListener);
    }

    public void setMinTime() {
        //if walking, set minTime to 60 seconds
        if (speedLevel == 1) {
            minTime = 20000;
        }
        //if running, set minTime to 30 seconds
        if (speedLevel == 2) {
            minTime = 15000;
        }
        //if driving, set minTime to 10 seconds
        if (speedLevel == 3) {
            minTime = 5000;
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

//    public static void AddToLocList(LatLng nextLatLng) {
//        locList.add(nextLatLng);
//    }

    public static void AddToSecondaryLocList(LatLng nextLatlng) {
        secondaryLocList.add(nextLatlng);
    }

    public static boolean PlotAttackOnMap(String severity, Location location) {
        switch (severity) {
            case "MILD_ATTACK":
                googleMap.addMarker(new MarkerOptions()
                        .flat(true)
                        .position(new LatLng(location.getLatitude(), location.getLongitude()))
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.mildattack))
                        .anchor(.5f, .5f));
                break;
            case "MEDIUM_ATTACK":
                googleMap.addMarker(new MarkerOptions()
                        .flat(true)
                        .position(new LatLng(location.getLatitude(), location.getLongitude()))
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.mediumattack))
                        .anchor(.5f, .5f));
                break;
            case "SEVERE_ATTACK":
                googleMap.addMarker(new MarkerOptions()
                        .flat(true)
                        .position(new LatLng(location.getLatitude(), location.getLongitude()))
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.severeattack))
                        .anchor(.5f, .5f));
                break;
            default:
                break;
        }

        return true;
    }

    public static boolean PlotInhalerOnMap(Location location) {
        googleMap.addMarker(new MarkerOptions()
                .flat(true)
                .position(new LatLng(location.getLatitude(), location.getLongitude()))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.inhaler))
                .anchor(.5f, .5f));

        return true;
    }

    public static void RefreshMapAfterUpload() throws IOException {
        googleMap.clear();

        new DownloadPrevLocations().execute(mContext);

        locList = new ArrayList<>();
        markers = new ArrayList<>();

        trackLine = new PolylineOptions()
                .width(5)
                .color(Color.BLUE)
                .geodesic(true);
        polyLine = googleMap.addPolyline(trackLine);

        secondaryLocList = new ArrayList<>();
        secondaryMarkers = new ArrayList<>();

        try {
            db.plotSecondaryTxtPoints();
        } catch (IOException e) {
            Log.i("error", "error reading from secondary text file");
            throw new IOException("could not read from secondary text file" + e);
        }

        secondaryTrackline = new PolylineOptions()
                .width(5)
                .color(Color.RED)
                .geodesic(true);

        //Toast.makeText(mContext, "inside secondary_loc.txt read", Toast.LENGTH_SHORT).show();

        for (LatLng nextLoc : secondaryLocList) {
            Log.i("RefreshLocPlot", nextLoc.toString());
            Marker nextMarker = googleMap.addMarker(new MarkerOptions()
                    .flat(true)
                    .position(nextLoc)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ballorange16))
                    .anchor(.5f, .5f));
            secondaryTrackline.add(nextLoc);
            secondaryMarkers.add(nextMarker);
        }

        Log.i("secondaryloc", secondaryLocList.toString());
        secondaryPolyline = googleMap.addPolyline(secondaryTrackline);
    }

    public static List<DatabaseLocationObject> GetDLOList(){
        locList = new ArrayList<>();
        for (Marker marker: markers) {
            DatabaseLocationObject markerTag = (DatabaseLocationObject) marker.getTag();
            locList.add(markerTag);
        }
        return locList;
    }

    public static void buildLast24HoursData(String jsonString){
        secondaryTrackline = new PolylineOptions()
                .width(5)
                .color(Color.RED)
                .geodesic(true);
        secondaryMarkers = new ArrayList<>();

        try{
            String[] splitJson = jsonString.split("=");
            JSONObject jsonRootObject = new JSONObject(splitJson[1]);

            JSONArray rowsArray = jsonRootObject.optJSONArray("rows");
            JSONArray nextJSONArray = rowsArray.getJSONArray(0);
            int i = 1;
            while (nextJSONArray != null){
                double nextLat = nextJSONArray.getDouble(11);
                double nextLon = nextJSONArray.getDouble(12);

                Marker newMarker = googleMap.addMarker(new MarkerOptions()
                    .flat(true)
                    .position(new LatLng(nextLat, nextLon))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ballorange16))
                    .anchor(.5f, .5f));

                secondaryTrackline.add(new LatLng(nextLat, nextLon));
                secondaryMarkers.add(newMarker);

                nextJSONArray = rowsArray.optJSONArray(i);
                i++;
            }
        }
        catch (JSONException e) {
            Log.i("JSON Parser", e.getMessage());
        }
    }
}


//Archived code, saved for those just-in-case-we-need-it moments

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
//
//        //create the location listener and begin location collection logic
//        locListener = new LocationListener() {
//            @Override
//            public void onLocationChanged(Location location) {
//                //add location lat and lon to LatLng and add to list
//                double newLat = location == null ? 0: location.getLatitude();
//                double newLon = location == null ? 0: location.getLongitude();
//                LatLng newlatLng = new LatLng(newLat, newLon);
//
//                //locList.add(newlatLng);
//
//                //float accuracy = location.getAccuracy();
//
//                //Log.i("speed", String.valueOf(speed));
//                //Log.i("new loc dist", String.valueOf(newLocDist));
//
//               // polyLine.setPoints(locList);
//                /*for (int z = 0; z < locList.size(); z++) {
//                    LatLng point = locList.get(z);
//                    trackLine.add(point);
//                }*/
//
//                if (isFirstPoint) {
//
//                    //move the camera to the new location
//                    googleMap.moveCamera(CameraUpdateFactory.newLatLng(newlatLng));
//
//                    // Zoom in the Google Map
//                    googleMap.animateCamera(CameraUpdateFactory.zoomTo(14));
//
//                    //add a marker at the new location
//                    Marker newMarker = googleMap.addMarker(new MarkerOptions()
//                            .flat(true)
//                            .position(newlatLng)
//                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.mapmarker))
//                            .anchor(.5f, .5f));
//                    markers.add(newMarker);
//
//                    //add new location to the trackline
//                    trackLine.add(newlatLng);
//
//                    polyLine = googleMap.addPolyline(trackLine);
//
//                    db.getLocationData(location, provider);
//
//                    prevLocation = location;
//
//                    //get the extra info generated by the locationManager
//                    speed = location.getSpeed();
//
//                    //calculate the new minTime for the location updates if needed
//                    speedCalc();
//
//                    isFirstPoint = false;
//                }
//
//                else if (!isFirstPoint){
//
//                    newLocDist = location.distanceTo(prevLocation);
//
//                    //if the new location is more than 25 meters away (for accuracy purposes)
//                    if (newLocDist >= 25)
//                    {
//
//                        // Zoom in the Google Map
//                        //googleMap.animateCamera(CameraUpdateFactory.zoomTo(14));
//
//                        //move the camera to the new location
//                        googleMap.moveCamera(CameraUpdateFactory.newLatLng(newlatLng));
//
//                        //add a marker at the new location
//                        Marker newMarker = googleMap.addMarker(new MarkerOptions()
//                                .flat(true)
//                                .position(newlatLng)
//                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.mapmarker))
//                                .anchor(.5f, .5f));
//                        markers.add(newMarker);
//
//                        //add new location to the trackline
//                        trackLine.add(newlatLng);
//
//                        polyLine = googleMap.addPolyline(trackLine);
//
//                        db.getLocationData(location, provider);
//
//                        prevLocation = location;
//
//                        //get the extra info generated by the locationManager
//                        speed = location.getSpeed();
//
//                        //calculate the new minTime for the location updates if needed
//                        speedCalc();
//                    }
//                }
//
//                //   Toast.makeText(mContext, String.valueOf(newLat)+", "+String.valueOf(newLon), Toast.LENGTH_SHORT).show();
//                //   Log.d("OnLocationChanged: ", String.valueOf(newLat) + ", " + String.valueOf(newLon));
//            }
//        };

//googleMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker").snippet("snippet"));

//
//    public static void startPoll() {
//        if (client.isConnected()) {
//            LocationServices.FusedLocationApi.requestLocationUpdates(client, locReq, locListener);
//            UploadData.startStop.setText("Stop");
//            UploadData.startStop.setBackgroundColor(Color.RED);
//        }
//        if (!client.isConnected()) {
//            client.connect();
//        }
//    }
//
//    public static void stopPoll() {
//        LocationServices.FusedLocationApi.removeLocationUpdates(client, locListener);
//        UploadData.startStop.setText("Start");
//        UploadData.startStop.setBackgroundColor(Color.GREEN);
//    }
