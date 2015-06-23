package org.geospaces.schas;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.SystemClock;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import android.os.Handler;

public class UploadData extends ActionBarActivity {

    private int PERIOD = 1 * 1000 * 60;  // 1 min
    private PendingIntent pi = null;
    private AlarmManager mgr = null;
    private String PEF_Text;
    private String FEV_Text;
    private int intT;
    private ConnectivityManager cm;

    SharedPreferences SP;

    TextView statusText;
    TextView medText;


    //Bluetooth Variables

    private BluetoothAdapter mBluetoothAdapter;
    private final static int REQUEST_ENABLE_BT = 1;
    private BluetoothDevice mBTdev;
    private int mBTint;
    private byte[] mBTbyte;
    private boolean mScanning;
    private Handler mHandler;
    private List<BluetoothDevice> btDeviceList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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


        setContentView(R.layout.activity_upload_data);

        findViewById(R.id.homeButton).setOnClickListener(start_service_button);
        findViewById(R.id.PFMButton).setOnClickListener(pfm_button);
        findViewById(R.id.updateStatus).setOnClickListener(updateStatusCB);
        findViewById(R.id.graphButton).setOnClickListener(uploadCB);
        findViewById(R.id.resetButton).setOnClickListener(resetCB);
        findViewById(R.id.attack1).setOnClickListener(mild_attack_button);
        findViewById(R.id.attack2).setOnClickListener(medium_attack_button);
        findViewById(R.id.attack3).setOnClickListener(severe_attack_button);
        findViewById(R.id.inhlaerButton).setOnClickListener(inhaler_button);
        findViewById(R.id.medButton).setOnClickListener(medicine_button);
        findViewById(R.id.Test123).setOnClickListener(test_button);

        medText    = (TextView) findViewById(R.id.medText);
        statusText = (TextView) findViewById(R.id.statusText);
        statusText.setMovementMethod(new ScrollingMovementMethod());

        if (pi == null) {
            startStopService();
        }
        updateStatus();


        mHandler = new Handler();
        btDeviceList = new ArrayList<BluetoothDevice>();

        BTinit();
        BleGattConnect();



    }

    //BT LE section



    private void BTinit(){
        // Use this check to determine whether BLE is supported on the device. Then
// you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth LE not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent,REQUEST_ENABLE_BT);
        }

        scanLeDevice(true);
    }


    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                    //The code here is executed on main thread
                    if(!btDeviceList.contains(device)) {
                        btDeviceList.add(device);
                        Log.e("LeScanCallback", Thread.currentThread().getName());//Prints main
                        device.connectGatt(getApplicationContext(), true, mGattCallback);
                    }

                }
            };



    private static final long SCAN_PERIOD = 20000;

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                System.out.println(gatt.getDevice() + ": Connected.. ");
                gatt.readRemoteRssi();
            }
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                System.out.println(gatt.getDevice() + ": Disconnected.. ");
            }
        }

        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
           System.out.println(gatt.getDevice() + " RSSI:" + rssi + "db ");
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            gatt.readRemoteRssi();

        }
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            System.out.println(gatt.readCharacteristic(characteristic));
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    private void BleGattConnect(){

    }



   /* private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            //The code here is executed on on new thread everytime
                            Toast("LeScan");
                            Log.e("LeScanCallback", Thread.currentThread().getName());//Prints Thread-xxxx
                        }
                    }).start();
                }
            };*/



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
        statusText.setText(str);
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

            Toast("" + PERIOD);

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
            medText.setText(ret);
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
            medText.setText(provider + " is Enabled");
            //Toast( "Provider " + provider );
        }

        @Override
        public void onProviderDisabled(String provider) {
            medText.setText(provider + " is disabled");
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
            MedicineTakenPopUp("Medicine Used");

        }
    };

    private View.OnClickListener test_button = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            btDeviceList.get(0).connectGatt(getApplicationContext(), false, mGattCallback);
            Toast("btdevice: " + btDeviceList.get(0).getName() + "\n size:" + btDeviceList.size());
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
