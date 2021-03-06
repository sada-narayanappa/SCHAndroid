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
import android.widget.Toast;

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

        Intent startLocationService = new Intent(mContext, LocationService.class);
        mContext.startService(startLocationService);

        //starts the pedometer service
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

        try {
            locList = db.ReadFromLocationsFile();
        } catch (IOException e) {
            Log.i("plotting primary", "could not read from primary text file");
            e.printStackTrace();
            //Toast.makeText(mContext, "could not read from loc.txt", Toast.LENGTH_SHORT).show();
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

        // Enable MyLocation Layer of Google Map
        googleMap.setMyLocationEnabled(true);

        //set map type
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        double lat = lastLocation == null ? 47.6205333 : lastLocation.getLatitude();
        double lon = lastLocation == null ? -122.19293 : lastLocation.getLongitude();

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

        polyLine = googleMap.addPolyline(trackLine);
        secondaryPolyline = googleMap.addPolyline(secondaryTrackline);

        googleMap.setOnMarkerClickListener(this);

        LocationService.appIsRunning = true;

        if (db.isNetworkAvailable(mContext)){
            Toast.makeText(mContext, "Retrieving locations from server", Toast.LENGTH_SHORT).show();
            new DownloadPrevLocations().execute(mContext);
        }
        else{
            Toast.makeText(mContext, "Could not download last 24 hours of locations \nno network connection available", Toast.LENGTH_SHORT).show();
        }

        //retain the fragment across orientation changes
        setRetainInstance(true);
    }

    @Override
    public boolean onMarkerClick(final Marker marker){
        DatabaseLocationObject dlo = (DatabaseLocationObject) marker.getTag();
        if (dlo.isUploadedPreviously == false) {
            if (dlo.GetValidity()) {
                dlo.isValid = false;
                //change marker color here
                marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ballred16));
                Log.i("marker clicked", String.valueOf(dlo.GetValidity()));
            } else {
                dlo.isValid = true;
                //change marker color here
                marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ballblue16));
                Log.i("marker clicked", String.valueOf(dlo.GetValidity()));
            }
        }

        return false;
    }

    @Override
    public void onPause() {
        db.PrepareTextFile();
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
        super.onDetach();
    }

    public static void plotNewPoint(DatabaseLocationObject dlo) {
        double newLat = dlo.lat;
        double newLon = dlo.lon;
        LatLng newLatLng = new LatLng(newLat, newLon);

        //move the camera to the new location
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newLatLng, 15));

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

        //if there are more than 50 points, get them into the text file
        if (markers.size() >= 50){
            db.PrepareTextFile();
            markers = new ArrayList<>();
        }
    }

    public static void startPoll() {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, locListener);
    }

    public static void stopPoll() {
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
                speedLevel = 1;
                setMinTime();
                startPoll();
            }
        } else if (speed >= 10.0 && speed < 20.0) {
            //do stuff for running
            if (speedLevel != 2) {
                speedLevel = 2;
                setMinTime();
                startPoll();
            }
        } else if (speed > 20.0) {
            //do stuff for driving
            if (speedLevel != 3) {
                speedLevel = 3;
                setMinTime();
                startPoll();
            }
        }
    }

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
        //googleMap.clear();

        for (Marker marker : markers){
            DatabaseLocationObject nextDLO = (DatabaseLocationObject) marker.getTag();
            LatLng nextLoc = new LatLng(nextDLO.lat, nextDLO.lon);

            DatabaseLocationObject newDLO = new DatabaseLocationObject(
                    String.valueOf(System.currentTimeMillis() / 1000),
                    (float) nextLoc.latitude,
                    (float) nextLoc.longitude,
                    "null",
                    "null",
                    "null",
                    "null",
                    "null",
                    "null",
                    true);
            newDLO.setPreviouslyUploaded();

            Marker nextMarker = googleMap.addMarker(new MarkerOptions()
                    .flat(true)
                    .position(nextLoc)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ballorange16))
                    .anchor(.5f, .5f));
            nextMarker.setTag(newDLO);

            secondaryTrackline.add(nextLoc);
        }

        try {
            db.plotSecondaryTxtPoints();
        } catch (IOException e) {
            Log.i("error", "error reading from secondary text file");
        }

        for (LatLng nextLoc : secondaryLocList) {
            Log.i("RefreshLocPlot", nextLoc.toString());
            DatabaseLocationObject newDLO = new DatabaseLocationObject(
                    String.valueOf(System.currentTimeMillis() / 1000),
                    (float) nextLoc.latitude,
                    (float) nextLoc.longitude,
                    "null",
                    "null",
                    "null",
                    "null",
                    "null",
                    "null",
                    true);
            newDLO.setPreviouslyUploaded();
            Marker nextMarker = googleMap.addMarker(new MarkerOptions()
                    .flat(true)
                    .position(nextLoc)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ballorange16))
                    .anchor(.5f, .5f));
            nextMarker.setTag(newDLO);

            secondaryTrackline.add(nextLoc);
        }

        locList = new ArrayList<>();
        markers = new ArrayList<>();

        trackLine = new PolylineOptions()
                .width(5)
                .color(Color.BLUE)
                .geodesic(true);
        polyLine = googleMap.addPolyline(trackLine);

        secondaryTrackline = new PolylineOptions()
                .width(5)
                .color(Color.RED)
                .geodesic(true);
        secondaryPolyline = googleMap.addPolyline(secondaryTrackline);
    }

    public static List<DatabaseLocationObject> GetDLOList(){
        locList = new ArrayList<>();
        for (Marker marker: markers) {
            DatabaseLocationObject markerTag = (DatabaseLocationObject) marker.getTag();
            if (!markerTag.isWrittenIntoFile){
                locList.add(markerTag);
            }
        }
        return locList;
    }

    public static void buildLast24HoursData(String jsonString){
        try{
            String[] splitJson = jsonString.split("=");
            JSONObject jsonRootObject = new JSONObject(splitJson[1]);

            JSONArray rowsArray = jsonRootObject.optJSONArray("rows");
            JSONArray nextJSONArray = rowsArray.getJSONArray(0);
            int i = 1;
            while (nextJSONArray != null){
                double nextLat = nextJSONArray.getDouble(11);
                double nextLon = nextJSONArray.getDouble(12);

                secondaryLocList.add(new LatLng(nextLat, nextLon));

                nextJSONArray = rowsArray.optJSONArray(i);
                i++;
            }
        }
        catch (JSONException e) {
            Log.i("JSON Parser", e.getMessage());
        }

        try {
            db.plotSecondaryTxtPoints();
        } catch (IOException e) {
            Log.i("error", "error reading from secondary text file");
        }

        for (LatLng nextLoc : secondaryLocList) {
            Log.i("RefreshLocPlot", nextLoc.toString());
            DatabaseLocationObject newDLO = new DatabaseLocationObject(
                    String.valueOf(System.currentTimeMillis() / 1000),
                    (float) nextLoc.latitude,
                    (float) nextLoc.longitude,
                    "null",
                    "null",
                    "null",
                    "null",
                    "null",
                    "null",
                    true);
            newDLO.setPreviouslyUploaded();
            Marker nextMarker = googleMap.addMarker(new MarkerOptions()
                    .flat(true)
                    .position(nextLoc)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ballorange16))
                    .anchor(.5f, .5f));
            nextMarker.setTag(newDLO);

            secondaryTrackline.add(nextLoc);
        }

        Log.i("secondaryloc", secondaryLocList.toString());
        secondaryPolyline = googleMap.addPolyline(secondaryTrackline);
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
