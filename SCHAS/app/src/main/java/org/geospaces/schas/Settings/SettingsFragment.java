package org.geospaces.schas.Settings;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import org.geospaces.schas.R;

/**
 * Created by Student on 3/29/2015.
 */
public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }
}
