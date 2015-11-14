package org.geospaces.schas;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.commonsware.cwac.locpoll.LocationPoller;
import com.google.android.gms.maps.GoogleMap;

import org.geospaces.schas.BluetoothLE.InhalerCap;
import org.geospaces.schas.Broadcast_Receivers.GPSWakfulReciever;
import org.geospaces.schas.Fragments.GoogleMaps;
import org.geospaces.schas.utils.SCHASSettings;
import org.geospaces.schas.utils.db;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class UploadData extends ActionBarActivity{

    private int PERIOD = 1 * 1000 * 60;  // 1 min
    private PendingIntent pi = null;
    private AlarmManager mgr = null;
    private String PEF_Text;
    private String FEV_Text;
    private int intT;
    private ConnectivityManager cm;
    private ShareActionProvider mActionProvider;


    SharedPreferences SP;

    //TextView statusText;
    //TextView medText;


    //Bluetooth Variables below
    private BluetoothAdapter mBluetoothAdapter;
    private ProgressDialog mProgress;
    private HashMap<String, BluetoothDevice> mDevices;
    private BluetoothLeScanner bleScan;
    private List<ScanFilter> filters;
    private ScanSettings settings;
    private HashMap<String, InhalerCap> mCaps;
    private CapAdapter mCapAdapter;

    //Floating Action Button
    private FloatingActionButton menuButton;
    private FloatingActionButton testButton1;
    private FloatingActionButton testButton2;
    private FloatingActionButton testButton3;
    private FloatingActionButton testButton4;
    private FloatingActionButton testButton5;
    private FloatingActionButton testButton6;
    private FloatingActionButton mapButton;
    private boolean menuActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_data);

    //    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setProgressBarIndeterminate(true);
        // ^^ temp

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);



        SP = PreferenceManager.getDefaultSharedPreferences(this);
        if(!SP.getString("frequency","2").equals("0")) {
            String stringT = SP.getString("frequency", "2");
            intT = Integer.parseInt(stringT);

            PERIOD = 1000 * 60 * intT;
            //Toast("onCreate() "+PERIOD/1000/60);
        }
        else{
            autoUpdate();
        }

        SharedPreferences.OnSharedPreferenceChangeListener listener =
                new SharedPreferences.OnSharedPreferenceChangeListener() {
                    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                        if(prefs.getBoolean("CellularData",false) == true) {
                            mobiledataenable(true);
                           // Toast("mobile true");
                        }
                        else if(prefs.getBoolean("CellularData",true) == false){
                            mobiledataenable(false);
                           // Toast("mobile false");
                        }
                    }
                };
        SP.registerOnSharedPreferenceChangeListener(listener);

       testButton1 = (FloatingActionButton) findViewById(R.id.test1);
        testButton2 = (FloatingActionButton) findViewById(R.id.test2);
        testButton3 = (FloatingActionButton) findViewById(R.id.test3);
        testButton4 = (FloatingActionButton) findViewById(R.id.test4);
        testButton5 = (FloatingActionButton) findViewById(R.id.test5);
        testButton6 = (FloatingActionButton) findViewById(R.id.test6);
      //  mapButton = (FloatingActionButton) findViewById(R.id.map);
        menuButton = (FloatingActionButton) findViewById(R.id.menu_button);
        menuButton.setOnClickListener(menu_button);

        findViewById(R.id.homeButton).setOnClickListener(start_service_button);
        findViewById(R.id.test4).setOnClickListener(pfm_button);
        //   findViewById(R.id.updateStatus).setOnClickListener(updateStatusCB);
        findViewById(R.id.graphButton).setOnClickListener(uploadCB);
        findViewById(R.id.resetButton).setOnClickListener(resetCB);
        findViewById(R.id.test1).setOnClickListener(mild_attack_button);
        findViewById(R.id.test2).setOnClickListener(medium_attack_button);
        findViewById(R.id.test3).setOnClickListener(severe_attack_button);
        findViewById(R.id.test5).setOnClickListener(inhaler_button);

        //medText    = (TextView) findViewById(R.id.medText);
        //statusText = (TextView) findViewById(R.id.statusText);
        //statusText.setMovementMethod(new ScrollingMovementMethod());

        if (pi == null) {
            startStopService();
        }
        updateStatus();
    }




    /*
     TO-DO DELETE FROM HERE TO HANDLER TO REMOVE UNUSED BLE CODE
     ALL BLE CODE MOVED TO TEST ACTIVITY
     */
    public void bluetoothInit(){
        BluetoothManager blemanager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = blemanager.getAdapter();

        bleScan = mBluetoothAdapter.getBluetoothLeScanner();



        mCaps = new HashMap<String, InhalerCap>();
        /*
         *
         * Progress dialog while connection in progress
         */

        mProgress = new ProgressDialog(this);
        mProgress.setIndeterminate(true);
        mProgress.setCancelable(false);
    }


    /*public void startTheScan(){

        ScanFilter bluegiga = new ScanFilter.Builder()
                    .setServiceUuid(InhalerCap.genericAccessService).build();

        ArrayList<ScanFilter> filters = new ArrayList<ScanFilter>();
        filters.add(bluegiga);


        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

      // bleScan.startScan(filters,settings,callback);
      //  bleScan.startScan(filters,settings,callback);

    }
    private String TAG = "BLE";
    private ScanCallback callback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.d(TAG, "onScanResult");
            processResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.d(TAG, "onBatchScanResults: "+results.size()+" results");
            for (ScanResult result : results) {
                processResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.w(TAG, "LE Scan Failed: "+errorCode);
        }

        private void processResult(ScanResult result) {
            Log.i(TAG, "New LE Device: " + result.getDevice().getName() + " @ " + result.getRssi());

            /*
             * Create a new beacon from the list of obtains AD structures
             * and pass it up to the main thread
             */
      /*      InhalerCap cap = new InhalerCap(result.getScanRecord(),
                    result.getDevice().getAddress(),
                    result.getRssi());
            mHandler.sendMessage(Message.obtain(null, 0, cap));
        }
    };*/

    /*
 * We have a Handler to process scan results on the main thread,
 * add them to our list adapter, and update the view
 */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            InhalerCap cap = (InhalerCap) msg.obj;
            mCaps.put(cap.getName(), cap);

            mCapAdapter.setNotifyOnChange(false);
            mCapAdapter.clear();
            mCapAdapter.addAll(mCaps.values());
            mCapAdapter.notifyDataSetChanged();
        }
    };

    /*
     * A custom adapter implementation that displays the TemperatureBeacon
     * element data in columns, and also varies the text color of each row
     * by the temperature values of the beacon
     */
    private static class CapAdapter extends ArrayAdapter<InhalerCap> {

        public CapAdapter(Context context) {
            super(context, 0);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.item_inhaler_list, parent, false);
            }

            InhalerCap cap = getItem(position);
            //Set color based on temperature
            final int textColor = getTemperatureColor(cap.getCurrentTemp());

            TextView nameView = (TextView) convertView.findViewById(R.id.text_name);
            nameView.setText(cap.getName());
            nameView.setTextColor(textColor);

            TextView tempView = (TextView) convertView.findViewById(R.id.text_temperature);
            tempView.setText(String.format("%.1f\u00B0C", cap.getCurrentTemp()));
            tempView.setTextColor(textColor);

            TextView addressView = (TextView) convertView.findViewById(R.id.text_address);
            addressView.setText(cap.getAddress());
            addressView.setTextColor(textColor);

            TextView rssiView = (TextView) convertView.findViewById(R.id.text_rssi);
            rssiView.setText(String.format("%ddBm", cap.getSignal()));
            rssiView.setTextColor(textColor);

            return convertView;
        }

        private int getTemperatureColor(float temperature) {
            //Color range from 0 - 40 degC
            float clipped = Math.max(0f, Math.min(40f, temperature));

            float scaled = ((40f - clipped) / 40f) * 255f;
            int blue = Math.round(scaled);
            int red = 255 - blue;

            return Color.rgb(red, 0, blue);
        }
    }




    public void mobiledataenable(boolean enabled) {
        try {
            final ConnectivityManager conman = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            final Class<?> conmanClass = Class.forName(conman.getClass().getName());
            final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
            iConnectivityManagerField.setAccessible(true);
            final Object iConnectivityManager = iConnectivityManagerField.get(conman);
            final Class<?> iConnectivityManagerClass = Class.forName(iConnectivityManager.getClass().getName());
            final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
            setMobileDataEnabledMethod.setAccessible(true);
            setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //NOT IMPLEMENTED YET SIMPLY TO KEEP APP WORKING FOR NOW
    public void autoUpdate(){
        String stringT = SP.getString("frequency", "2");
        intT = Integer.parseInt(stringT);

        PERIOD = 1000 * 60 * intT;
    }

    private void Toast(String msg) {
        Context ctx = getApplicationContext();
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
    }

    protected void updateStatus() {
        StringBuffer sb = new StringBuffer(256);
        File f;
        f = db.getFile(db.FILE_NAME);



        sb.append(f.getName() + " : size=" + f.length() + "\n");
        f = db.getFile(db.FILE_READY);
        sb.append(f.getName() + " : size=" + f.length() + "\n");

        if (SCHASSettings.host == null ) {
            SCHASSettings.Initialize();
        }
        sb.append("URL: " + SCHASSettings.host + "\n");

        String []ls = db.read(db.FILE_NAME).split("\n");

        sb.append("DATA:" + ls[ls.length-1] + " ...\n");
        sb.append("SETTINGS:" + SCHASSettings.getSettings() + " ...\n");
        sb.append("WIFI:" + db.isWIFIOn(this.getApplicationContext()) + " ...\n");

        //statusText.setText(sb.toString());
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
            SCHASSettings.Initialize();
            SCHASSettings.saveSettings();
        }
    };

    private View.OnClickListener uploadCB = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Context ctx = UploadData.this.getApplicationContext();
            String str;
            if ( null == (str = db.isWIFIOn(ctx))) {
                Toast( "NO Wireless Connection! Please check back");
            }

            str = db.Upload(ctx, UploadData.this);
            if ( str != null) {
                Toast( str + " retry");
            }
            updateStatus();
        }
    };

    public void setIntent( Intent i) {
        super.setIntent(i);
        String str = "SetResult: " + i.getStringExtra("result");
        String url = "SetResult: " + i.getStringExtra("url");
        //statusText.setText(str);
        if (!str.contains("ERROR")) {
            //db.delete();
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        //Toast("On Resume");

        SP = PreferenceManager.getDefaultSharedPreferences(this);
        String stringT = SP.getString("frequency", "2");
        if(!SP.getString("frequency","2").equals("0")){
            intT = Integer.parseInt(stringT);

            PERIOD = 1000 * 60 * intT;
            //Toast("onCreate() "+PERIOD/1000/60);
        }
        else{
            autoUpdate();
        }

        //Bluetooth Portion Below
/*
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()){
            //Bluetooth is disabled

            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            finish();

        }
        //check for low energy support
        if(!getPackageManager().hasSystemFeature((PackageManager.FEATURE_BLUETOOTH_LE))){
        Toast("No LE Support.");
            finish();
        }*/


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

         //   Toast("" + PERIOD);

            mgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime(),
                    PERIOD,
                    pi);

            String str = "Location polling every " + PERIOD/1000/60 + " minutes begun";
            Toast( str);

            b.setText("Stop");
            b.setBackgroundColor(0xffff0000);

        } else {
            STOP_LOCATION_UPDATES = true;
            mgr.cancel(pi);
            pi = null;
            b.setText("Start Service");
            b.setBackgroundColor(0xff00ff00);
            Toast( "Location polling STOPPED");
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
            //Toast ("Location: " );
            Log.w("onLocationChanged", ret);
            updateStatus();
            //medText.setText(ret);
            if ( STOP_LOCATION_UPDATES ) {
                lm.removeUpdates(this);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            //Toast(provider + "'s status changed to " );
        }

        @Override
        public void onProviderEnabled(String provider) {
            //medText.setText(provider + " is Enabled");
            //Toast( "Provider " + provider );
        }

        @Override
        public void onProviderDisabled(String provider) {
            //medText.setText(provider + " is disabled");
            //Toast( "Provider " + provider + " disabled!");
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
                Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (loc == null) {
                    Toast("Can't get Location");
                    return;
                }
               String msg = db.getPeakFlow(loc,input.getText().toString(),input2.getText().toString());
                try {
                    db.Write(msg + "\n");
                } catch (IOException e) {
                    Log.e("ERROR", "Exception appending to log file", e);
                }
                updateStatus();
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

    public void AttackConfirmPopUpCreator(String label,final String severity,boolean inhal) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(label);

        if(inhal == false) {
            builder.setPositiveButton("Confirm Attack", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (loc == null) {
                        Toast("Can't get Location");
                        return;
                    }

                    String msg = db.getAttack(loc, severity);
                    try {
                        db.Write(msg + "\n");
                    } catch (IOException e) {
                        Log.e("ERROR", "Exception appending to log file", e);
                    }
                    updateStatus();
                }
            });
        }
        else{
            builder.setPositiveButton("Confirm Usage", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (loc == null) {
                        Toast("Can't get Location");
                        return;
                    }

                    String msg = db.getAttack(loc, severity);
                    try {
                        db.Write(msg + "\n");
                    } catch (IOException e) {
                        Log.e("ERROR", "Exception appending to log file", e);
                    }
                    updateStatus();
                }
            });
        }
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public void MedicineTakenPopUp (String label) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(label);
        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("Confirm Medicine Used", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String value = input.getText().toString();
                Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if ( loc == null ){
                    Toast("Can't get Location");
                    return;
                }

                String msg = db.getMedicine(loc,value);
                try {
                    db.Write(msg + "\n");
                } catch (IOException e) {
                    Log.e("ERROR", "Exception appending to log file", e);
                }
                updateStatus();
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
            AttackConfirmPopUpCreator("Confirm Mild Attack","MILD_ATTACK",false);
        }
    };

    private View.OnClickListener medium_attack_button = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            AttackConfirmPopUpCreator("Confirm Medium Attack","MEDIUM_ATTACK",false);

        }
    };

    private View.OnClickListener severe_attack_button = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            AttackConfirmPopUpCreator("Confirm Severe Attack","SEVERE_ATTACK",false);

        }
    };

    private View.OnClickListener inhaler_button = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            AttackConfirmPopUpCreator("Confirm Inhaler Used","INHALER",true);

        }
    };

    private View.OnClickListener medicine_button = new View.OnClickListener() {
        @Override
        public void onClick(View v) {


        }
    };
    /*
     *
     * controls animations for the floating action button for the dashboard screen
     */
    private View.OnClickListener menu_button = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            menuButton.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate));

            if(!menuActive) {
                menuActive = true;

                testButton1.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        testButton1.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in));
                    }
                }, 50);

                testButton2.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        testButton2.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in));
                    }
                }, 100);

                testButton3.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        testButton3.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in));
                    }
                }, 150);

                testButton4.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        testButton4.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in));
                    }
                }, 200);

                testButton5.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        menuButton.setImageResource(R.drawable.ic_x);
                        testButton5.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in));
                    }
                }, 250);


                testButton1.setEnabled(true);
                testButton2.setEnabled(true);
                testButton3.setEnabled(true);
                testButton4.setEnabled(true);
                testButton5.setEnabled(true);

            }
            else{
                menuActive = false;

                testButton1.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out));
                testButton2.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out));
                testButton3.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out));
                testButton4.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out));
                testButton5.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out));

                testButton1.setEnabled(false);
                testButton2.setEnabled(false);
                testButton3.setEnabled(false);
                testButton4.setEnabled(false);
                testButton5.setEnabled(false);

                menuButton.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate));

                menuButton.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        menuButton.setImageResource(R.drawable.ic_heart);
                    }
                }, 150);
            }

        }
    };


    public void writeFile(String s1, String s2) {
        PEF_Text = "";
        FEV_Text = "";
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.welcome, menu);

        MenuItem shareItem = menu.findItem(R.menu.welcome);

        // To retrieve the Action Provider
        mActionProvider = (ShareActionProvider)
                MenuItemCompat.getActionProvider(shareItem);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!HandleMenu.onOptionsItemSelected(item, this)) {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }
    @Override
    protected void onPause() {
        super.onPause();
        //Cancel scan in progress
        //bleScan.stopScan(callback);

    }

}
