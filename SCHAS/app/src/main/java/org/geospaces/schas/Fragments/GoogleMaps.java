package org.geospaces.schas.Fragments;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.app.Fragment;
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

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.abs;
import static java.lang.Math.hypot;


public class GoogleMaps extends SupportMapFragment {

    private GoogleMap googleMap;
    private LatLng mPosFija = new LatLng(37.878901,-4.779396);
    private Context mContext;
    LocationManager locationManager;
    Location myLocation;
    Location prevLocation = null;
    Criteria criteria;
    //set min update time to 15 seconds
    long minTime = 15*1000;
    //set min update distance to 50 meters
    float minDistance = 50;
    //list to hold LatLng values
    List<LatLng> locList = new ArrayList<>();
    PolylineOptions trackLine;
    Polyline polyLine;
    LocationListener locListener;

    //values for checking distance with
    double prevLat;
    double prevLong;
    double changeInX;
    double changeInY;
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

        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);


        locListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                double newLat = location == null ? 0: location.getLatitude();
                double newLon = location == null ? 0: location.getLongitude();
                LatLng newlatLng = new LatLng(newLat, newLon);
                locList.add(newlatLng);

                trackLine.add(newlatLng);
               // polyLine.setPoints(locList);

                /*for (int z = 0; z < locList.size(); z++) {
                    LatLng point = locList.get(z);
                    trackLine.add(point);
                }*/



                polyLine = googleMap.addPolyline(trackLine);

                prevLat = prevLocation.getLatitude();
                prevLong = prevLocation.getLongitude();

                changeInX = abs(prevLat + newLat);
                changeInY = abs(prevLong + newLon);

                newLocDist = hypot(changeInX, changeInY);

                if (newLocDist >= 25)
                {
                    googleMap.addMarker(new MarkerOptions()
                            .flat(true)
                            .position(newlatLng)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.mapmarker))
                            .anchor(.5f, .5f));
                }

                /*else if (newLocDist < 25)
                {
                    Toast.makeText(mContext, "same location, no marker added", Toast.LENGTH_LONG).show();
                    Log.i("same location", "location not changed");
                }*/

                googleMap.moveCamera(CameraUpdateFactory.newLatLng(newlatLng));

                prevLocation = location;

                //   Toast.makeText(mContext, String.valueOf(newLat)+", "+String.valueOf(newLon), Toast.LENGTH_SHORT).show();
                //Log.d("OnLocationChanged: ", String.valueOf(newLat) + ", " + String.valueOf(newLon));
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

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60000, 30, locListener);
        //locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60000, 30, locListener);


        //googleMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker").snippet("snippet"));
        // Enable MyLocation Layer of Google Map
        googleMap.setMyLocationEnabled(true);

        // Create a criteria object to retrieve provider
        criteria = new Criteria();

        // Get the name of the best provider
        String provider = locationManager.getBestProvider(criteria, true);

        // Get Current Location
        myLocation = locationManager.getLastKnownLocation(provider);

        //set map type
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

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

        trackLine =new PolylineOptions()
                .add(latLng)
                .width(5)
                .color(Color.BLUE)
                .geodesic(true);
        polyLine = googleMap.addPolyline(trackLine);

        setRetainInstance(true);
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
        locationManager.removeUpdates(locListener);
        super.onDetach();
    }
}