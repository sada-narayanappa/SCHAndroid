package org.geospaces.schas.Settings;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.TextView;

import com.google.android.gms.common.GoogleApiAvailability;

import org.geospaces.schas.R;

import static com.google.android.gms.common.GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo;

public class LegalNotices extends ActionBarActivity {


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.legal_notices);

        Context mContext = getApplicationContext();

        TextView text = (TextView) findViewById(R.id.legal);
        String legalText = getOpenSourceSoftwareLicenseInfo(mContext);
        text.setText(legalText);
    }
}
