package org.geospaces.schas.Fragments;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;

import org.geospaces.schas.R;

public class googleMapsActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_google_maps);
        GoogleMaps newMaps = GoogleMaps.newInstance();
    }

    GoogleMaps.OnFragmentInteractionListener {

    }
}
