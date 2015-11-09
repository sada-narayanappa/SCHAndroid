package org.geospaces.schas.Fragments;

import android.content.Context;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;


public class GoogleMaps extends SupportMapFragment implements LocationListener {

    private GoogleMap googleMap;
    private LatLng mPosFija = new LatLng(37.878901,-4.779396);
    private Context mContext;
    LocationManager locationManager;
    LocationListener locationListener;
    Location myLocation;
    Criteria criteria;
    //set min update time to 15 seconds
    long minTime = 15*1000;
    //set min update distance to 50 meters
    float minDistance = 50;
    //list to hold LatLng values
    List<LatLng> locList = new ArrayList<>();
    PolylineOptions trackLine;
    Polyline polyLine;


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

        //googleMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker").snippet("snippet"));
        // Enable MyLocation Layer of Google Map
        //   googleMap.setMyLocationEnabled(true);

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

        // Create a LatLng object for the current location
        LatLng latLng = new LatLng(lat, lon);
        if (locList == null) Log.i("LocList", "loclist is null");
        if (latLng != null) locList.add(latLng);
        // Show the current location in Google Map
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

        // Zoom in the Google Map
        //googlMap.
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(12));
        googleMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon)).title("You are here!"));

        trackLine =new PolylineOptions()
                .add(latLng)
                .width(10)
                .color(Color.BLUE)
                .geodesic(true);
        polyLine = googleMap.addPolyline(trackLine);

        locationManager.requestLocationUpdates(provider, minTime, minDistance, this);
    }

    @Override
    public void onLocationChanged(Location location)
    {
        double newLat = myLocation == null ? 0: myLocation.getLatitude();
        double newLon = myLocation == null ? 0: myLocation.getLongitude();
        LatLng newlatLng = new LatLng(newLat, newLon);
        if (newlatLng != null) locList.add(newlatLng);

        polyLine.setPoints(locList);

        Toast.makeText(mContext, String.valueOf(newLat)+", "+String.valueOf(newLon), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider){
    }

    @Override
    public void onProviderDisabled(String provider) {
    }
}
