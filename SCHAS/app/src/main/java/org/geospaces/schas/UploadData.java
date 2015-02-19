package org.geospaces.schas;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.SystemClock;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.commonsware.cwac.locpoll.LocationPoller;

import org.geospaces.schas.utils.SCHASSettings;
import org.geospaces.schas.utils.db;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;

public class UploadData extends Activity {

    private static final int PERIOD = 1 * 1000 * 60;  // 2 min
    private PendingIntent pi = null;
    private AlarmManager mgr = null;
    private String PEF_Text;
    private String FEV_Text;

    TextView statusText;
    TextView medText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        setContentView(R.layout.activity_upload_data);

        findViewById(R.id.homeButton).setOnClickListener(start_service_button);
        findViewById(R.id.PFMButton).setOnClickListener(pfm_button);
        findViewById(R.id.updateStatus).setOnClickListener(updateStatusCB);
        findViewById(R.id.graphButton).setOnClickListener(uploadCB);
        findViewById(R.id.resetButton).setOnClickListener(resetCB);
        findViewById(R.id.attack1).setOnClickListener(mild_attack_button);
        findViewById(R.id.attack2).setOnClickListener(medium_attack_button);
        findViewById(R.id.attack3).setOnClickListener(severe_attack_button);

        medText    = (TextView) findViewById(R.id.medText);
        statusText = (TextView) findViewById(R.id.statusText);
        statusText.setMovementMethod(new ScrollingMovementMethod());

        if (pi == null) {
            startStopService();
        }
        updateStatus();
    }

    protected void updateStatus() {
        StringBuffer sb = new StringBuffer(256);
        File f;
        f = db.getFile(db.FILE_NAME);



        sb.append(f.getName() + " : size=" + f.length() + "\n");
        f = db.getFile(db.FILE_READY);
        sb.append(f.getName() + " : size=" + f.length() + "\n");

        if (SCHASSettings.host == null ) {
            SCHASSettings.Initialize(null);
        }
        sb.append("URL: " + SCHASSettings.host + "\n");

        String []ls = db.read(db.FILE_NAME).split("\n");

        sb.append("DATA:" + ls[ls.length-1] + " ...\n");
        sb.append("SETTINGS:" + SCHASSettings.getSettings() + " ...\n");
        sb.append("WIFI:" + db.isWIFIOn(this.getApplicationContext()) + " ...\n");

        statusText.setText(sb.toString());
    }

    private View.OnClickListener updateStatusCB = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            updateStatus();
        }
    };

    private View.OnClickListener resetCB = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            SCHASSettings.host = null;
            SCHASSettings.Initialize( null);
            SCHASSettings.saveSettings();


            db.delete();
        }
    };

    private View.OnClickListener uploadCB = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Context ctx = UploadData.this.getApplicationContext();
            db.Upload(ctx, UploadData.this);
            updateStatus();
        }
    };

    public void setIntent( Intent i) {
        super.setIntent(i);
        String str = "SetResult: " + i.getStringExtra("result");
        String url = "SetResult: " + i.getStringExtra("url");
        statusText.setText(str);
        if (!str.contains("ERROR")) {
            //db.delete();
        }
    }

    private void startStopService() {
        Button b = ((Button) findViewById(R.id.homeButton));
        if (pi == null) {
            STOP_LOCATION_UPDATES = false;
            getLocationUpdates();

            mgr = (AlarmManager) getSystemService(ALARM_SERVICE);

            Intent i = new Intent(UploadData.this, LocationPoller.class);

            i.putExtra(LocationPoller.EXTRA_INTENT, new Intent(UploadData.this, GPSWakfulReciever.class));
            i.putExtra(LocationPoller.EXTRA_PROVIDER, LocationManager.GPS_PROVIDER);

            pi = PendingIntent.getBroadcast(UploadData.this, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);

            mgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime(),
                    PERIOD,
                    pi);

            String str = "Location polling every " + PERIOD/1000/60 + " minutes begun";
            Toast.makeText(UploadData.this, str , Toast.LENGTH_SHORT).show();

            b.setText("Stop");
            b.setBackgroundColor(0xffff0000);

        } else {
            STOP_LOCATION_UPDATES = true;
            mgr.cancel(pi);
            pi = null;
            b.setText("Start Service");
            b.setBackgroundColor(0xff00ff00);
            Toast.makeText(UploadData.this, "Location polling STOPPED", Toast.LENGTH_SHORT).show();
        }
    }
    LocationManager lm = null;
    private void getLocationUpdates() {
        Criteria criteria = new Criteria();
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        criteria.setCostAllowed(false);
        String provider = lm.getBestProvider(criteria, false);
        MyLocationListener mylistener = new MyLocationListener(provider);
        lm.requestLocationUpdates(provider, 5 * 60 * 1000, 200, mylistener); // every 60 seconds or 10 meter

        //lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 4000, 0, mylistener);
        if ( !provider.equals(LocationManager.GPS_PROVIDER)) {
            MyLocationListener myl1 = new MyLocationListener(LocationManager.GPS_PROVIDER);
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60000, 15, myl1);
        }

        Log.w("", "******** PROVIDER ***** " + provider);

    }

    private boolean STOP_LOCATION_UPDATES = false;
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a");
    private class MyLocationListener implements LocationListener {
        String myProvider;

        public MyLocationListener(String p) {
            myProvider = p;
        }
        @Override
        public void onLocationChanged(Location loc) {
            String ret = GPSWakfulReciever.storeLocation(loc, myProvider);
            //Toast.makeText(UploadData.this, "Location: " + ret,Toast.LENGTH_SHORT).show();
            Log.w("onLocationChanged", ret);
            updateStatus();
            medText.setText(ret);
            if ( STOP_LOCATION_UPDATES ) {
                lm.removeUpdates(this);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            //Toast.makeText(UploadData.this, provider + "'s status changed to " + status + "!", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProviderEnabled(String provider) {
            medText.setText(provider + " is Enabled");
            //Toast.makeText(UploadData.this, "Provider " + provider + " enabled!",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProviderDisabled(String provider) {
            medText.setText(provider + " is disabled");
            //Toast.makeText(UploadData.this, "Provider " + provider + " disabled!", Toast.LENGTH_SHORT).show();
            lm.removeUpdates(this);
            getLocationUpdates();
        }
    }

    private View.OnClickListener start_service_button = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            startStopService();
        }
    };

    private View.OnClickListener pfm_button = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String PEF = "";
            String FEV = "";

            InputTextPopUpCreator("Record PEF/FEV");
        }
    };

    public void InputTextPopUpCreator(String Label) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(Label);

        final LinearLayout ll = new LinearLayout(this);
        final EditText input = new EditText(this);
        final EditText input2 = new EditText(this);

        input.setHint("PEF");
        input2.setHint("FEV");

        ll.addView(input);
        ll.addView(input2);

        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input2.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        builder.setView(ll);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                PEF_Text = input.getText().toString();
                FEV_Text = input2.getText().toString();

                writeFile(PEF_Text, FEV_Text);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public void AttackConfirmPopUpCreator(String label,final String severity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(label);

        builder.setPositiveButton("Confirm Attack", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                File file = db.getFile(db.FILE_NAME);

                String msg = db.getAttack(lm.getLastKnownLocation(LocationManager.GPS_PROVIDER),severity);
                try {
                    BufferedWriter out = new BufferedWriter(new FileWriter(file.getAbsolutePath(), file.exists()));
                    out.write(msg + "\n");
                    out.close();
                } catch (IOException e) {
                    Log.e("ERROR", "Exception appending to log file", e);
                    msg = "ERROR: Exception appending to log file: " + e;
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private View.OnClickListener mild_attack_button = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            AttackConfirmPopUpCreator("Confirm Mild Attack","MILD_ATTACK");
        }
    };

    private View.OnClickListener medium_attack_button = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            AttackConfirmPopUpCreator("Confirm Medium Attack","MEDIUM_ATTACK");

        }
    };

    private View.OnClickListener severe_attack_button = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            AttackConfirmPopUpCreator("Confirm Severe Attack","SEVERE_ATTACK");

        }
    };

    /**
     * Michael:
     * Write to the same file as String ret = GPSWakfulReciever.storeLocation(loc);
     * Get the location information and set
     *  record_type = "PFM_USE"
     *  Get the lat= and lon= and
     *  Store the values in notes=
     *  So your record would look like:
     *  measured_at="time", record_type="PFM_USE",lat="letitude -lookup", lon="lon",notes"your values"
     *  That is all you need to do - the file will be uploaded whenever it gets uploaded
     *
     *  Same thing fot Inhaler USE lets use record_type="INHALER"
     *  ATTACK - record_type="ATTACK_MILD" , "ATTACK_MEDIUM" ATTACK_SEVERE"
     *
     * @param s1
     * @param s2
     */
    public void writeFile(String s1, String s2) {
        PEF_Text = "";
        FEV_Text = "";
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.welcome, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!HandleMenu.onOptionsItemSelected(item, this)) {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }
}
