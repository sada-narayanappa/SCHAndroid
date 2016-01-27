package org.geospaces.schas;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.Button;

import android.widget.Toast;

import com.commonsware.cwac.locpoll.LocationPoller;

import org.geospaces.schas.Broadcast_Receivers.GPSWakfulReciever;
import org.geospaces.schas.utils.SCHASSettings;
import org.geospaces.schas.utils.db;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Set;
import java.util.UUID;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class UploadData extends ActionBarActivity{

    private int PERIOD = 1 * 1000 * 60;  // 1 min
    private PendingIntent pi = null;
    private AlarmManager mgr = null;
    private int intT;
    private ShareActionProvider mActionProvider;


    SharedPreferences SP;


    //Bluetooth Variables below
    private BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice     mmDevice = null;
    BluetoothSocket mmSocket = null;
    UUID uuid;
    OutputStream mmOutputStream = null;
    InputStream mmInputStream = null;
    volatile boolean    stopWorker;
    int                 readBufferPosition;
    byte[]              readBuffer;
    Thread              workerThread;


    //Floating Action Button
    private FloatingActionButton menuButton;
    private FloatingActionButton mildAttackButton;
    private FloatingActionButton mediumAttackButton;
    private FloatingActionButton severeAttackButton;
    private FloatingActionButton PFMConnectButton;
    private FloatingActionButton inhalerButton;
    private boolean menuActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_data);

        setProgressBarIndeterminate(true);

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        SP = PreferenceManager.getDefaultSharedPreferences(this);
        if(!SP.getString("frequency","2").equals("0")) {
            String stringT = SP.getString("frequency", "2");
            intT = Integer.parseInt(stringT);

            PERIOD = 1000 * 60 * intT;
        }
        else{
            autoUpdate();
        }

        SharedPreferences.OnSharedPreferenceChangeListener listener =
                new SharedPreferences.OnSharedPreferenceChangeListener() {
                    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                        if(prefs.getBoolean("CellularData",false) == true) {
                            mobiledataenable(true);
                        }
                        else if(prefs.getBoolean("CellularData",true) == false){
                            mobiledataenable(false);
                        }
                    }
                };
        SP.registerOnSharedPreferenceChangeListener(listener);

        //Sets button variable to xml object
        mildAttackButton = (FloatingActionButton) findViewById(R.id.mildAttackButton);
        mediumAttackButton = (FloatingActionButton) findViewById(R.id.mediumAttackButton);
        severeAttackButton = (FloatingActionButton) findViewById(R.id.severeAttackButton);
        PFMConnectButton = (FloatingActionButton) findViewById(R.id.PFMConnectButton);
        inhalerButton = (FloatingActionButton) findViewById(R.id.inhalerButton);
        menuButton = (FloatingActionButton) findViewById(R.id.menu_button);
        menuButton.setOnClickListener(menu_button);

        findViewById(R.id.homeButton).setOnClickListener(start_service_button);
        findViewById(R.id.PFMConnectButton).setOnClickListener(pfm_BT_connect);
        findViewById(R.id.graphButton).setOnClickListener(uploadCB);
        findViewById(R.id.resetButton).setOnClickListener(resetCB);
        findViewById(R.id.mildAttackButton).setOnClickListener(mild_attack_button);
        findViewById(R.id.mediumAttackButton).setOnClickListener(medium_attack_button);
        findViewById(R.id.severeAttackButton).setOnClickListener(severe_attack_button);
        findViewById(R.id.inhalerButton).setOnClickListener(inhaler_button);

        if (pi == null) {
            startStopService();
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


    public void autoUpdate(){
        String stringT = SP.getString("frequency", "2");
        intT = Integer.parseInt(stringT);

        PERIOD = 1000 * 60 * intT;
    }

    private void Toast(String msg) {
        Context ctx = getApplicationContext();
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
    }

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
        }
    };

    public void setIntent( Intent i) {
        super.setIntent(i);
        String str = "SetResult: " + i.getStringExtra("result");
        String url = "SetResult: " + i.getStringExtra("url");
        if (!str.contains("ERROR")) {
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        SP = PreferenceManager.getDefaultSharedPreferences(this);
        String stringT = SP.getString("frequency", "2");
        if(!SP.getString("frequency","2").equals("0")){
            intT = Integer.parseInt(stringT);

            PERIOD = 1000 * 60 * intT;
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

            mgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime(),
                    PERIOD,
                    pi);

            String str = "Location polling every " + PERIOD/1000/60 + " minutes begun";
            Toast(str);

            b.setText("Stop");
            b.setBackgroundColor(0xffff0000);

        } else {
            STOP_LOCATION_UPDATES = true;
            mgr.cancel(pi);
            pi = null;
            b.setText("Start Service");
            b.setBackgroundColor(0xff00ff00);
            Toast("Location polling STOPPED");
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

        if ( !provider.equals(LocationManager.GPS_PROVIDER)) {
            MyLocationListener myl1 = new MyLocationListener(LocationManager.GPS_PROVIDER);
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60000, 15, myl1);
        }
        Log.w("", "******** PROVIDER ***** " + provider);
    }

    private boolean STOP_LOCATION_UPDATES = false;
    private class MyLocationListener implements LocationListener {
        String myProvider;

        public MyLocationListener(String p) {
            myProvider = p;
        }
        @Override
        public void onLocationChanged(Location loc) {
            String ret = GPSWakfulReciever.storeLocation(loc, myProvider);
            Log.w("onLocationChanged", ret);
            if ( STOP_LOCATION_UPDATES ) {
                lm.removeUpdates(this);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
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


    /*
     * Controls animations for the floating action button for the dashboard screen
     */
    private View.OnClickListener menu_button = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            menuButton.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate));

            if(!menuActive) {
                menuActive = true;

                mildAttackButton.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mildAttackButton.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in));
                    }
                }, 50);

                mediumAttackButton.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mediumAttackButton.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in));
                    }
                }, 100);

                severeAttackButton.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        severeAttackButton.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in));
                    }
                }, 150);

                PFMConnectButton.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        PFMConnectButton.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in));
                    }
                }, 200);

                inhalerButton.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        menuButton.setImageResource(R.drawable.ic_x);
                        inhalerButton.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in));
                    }
                }, 250);


                mildAttackButton.setEnabled(true);
                mediumAttackButton.setEnabled(true);
                severeAttackButton.setEnabled(true);
                PFMConnectButton.setEnabled(true);
                inhalerButton.setEnabled(true);

            }
            else{
                menuActive = false;

                mildAttackButton.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out));
                mediumAttackButton.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out));
                severeAttackButton.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out));
                PFMConnectButton.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out));
                inhalerButton.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out));

                mildAttackButton.setEnabled(false);
                mediumAttackButton.setEnabled(false);
                severeAttackButton.setEnabled(false);
                PFMConnectButton.setEnabled(false);
                inhalerButton.setEnabled(false);

                menuButton.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate));

                menuButton.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        menuButton.setImageResource(R.drawable.ic_heart_white);
                    }
                }, 150);
            }

        }
    };

    private View.OnClickListener pfm_BT_connect = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            PFMConnectButton.setEnabled(false);

            Toast("Attempting peakflow pairing");

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    mmDevice = null;
                    mmSocket = null;

                    BlueToothHelper.on(UploadData.this);

                    if(mBluetoothAdapter != null) {
                        if (mBluetoothAdapter.isEnabled()) {
                            findBT();
                            uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
                            // uuid = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
                            //Toast.makeText(getApplicationContext(), ""+mmSocket.isConnected(), Toast.LENGTH_SHORT).show();

                            if (mmDevice != null) {
                                try {
                                    mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
                                    mBluetoothAdapter.cancelDiscovery();
                                    mmSocket.connect();

                                } catch (IOException e) {
                                    try {
                                        mmSocket = (BluetoothSocket) mmDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(mmDevice, 1);
                                        mmSocket.connect();
                                    } catch (Exception f) {

                                    }
                                }
                            }
                            if (mmSocket == null) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getApplicationContext(), "No Devices connected", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                Intent intentOpenBluetoothSettings = new Intent();
                                intentOpenBluetoothSettings.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                                startActivity(intentOpenBluetoothSettings);
                            }
                        }
                        try {
                            mmSocket.close();
                        } catch (Exception e) {

                        }
                        try {
                            openBT();
                        } catch (IOException e) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), "Unable to Connect", Toast.LENGTH_SHORT).show();
                                    PFMConnectButton.setImageResource(R.drawable.ic_peakflow_red);
                                    PFMConnectButton.setEnabled(true);
                                }
                            });

                        }
                    }
                }
            };
            Thread myThread = new Thread(runnable);
            myThread.start();
        }

    };

    void findBT() {
        if (mBluetoothAdapter == null) {
            return;
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().contains("ASMA")) {
                    if (mmDevice == null) {
                        mmDevice = device;
                    }
                }

            }
        }
    }
    public void openBT() throws IOException {
        try {
            mmSocket.close();
        }
        catch (Exception e){
           // Toast("Open BT Fails");
        }

        uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        if(mmSocket != null) {
            if (!mmSocket.isConnected()) {
                mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
                mmSocket.connect();
            }
            try {
                mmOutputStream = mmSocket.getOutputStream();
                mmInputStream = mmSocket.getInputStream();
            } catch (Exception e) {
                Log.d("BTstream", "failed");
            }


            if (mmSocket.isConnected()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        PFMConnectButton.setImageResource(R.drawable.ic_peakflow_green);
                        Toast.makeText(getApplicationContext(), "Peakflow meter connected", Toast.LENGTH_LONG).show();
                    }
                });
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        beginListenForData();
                    }
                });
            }
        }
    }

    public void beginListenForData() {
        //Toast.makeText(getApplicationContext(), "Step1", Toast.LENGTH_LONG).show();

        final Handler handler = new Handler();
        final byte delimiter = 3; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        int count = 0;
        workerThread = new Thread(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                    try {
                        int bytesAvailable = mmInputStream.available();
                        if (bytesAvailable > 0) {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                if (b == delimiter) {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    handler.post(new Runnable() {
                                        boolean yes = false;

                                        public void run() {
                                            final double fev;
                                            final int pef2;

                                            if (data.length() >= 20) {
                                                int temp = data.indexOf('D');
                                                fev = (double) Integer.parseInt(data.substring(temp + 11, temp + 14)) / 100;
                                                pef2 = Integer.parseInt(data.substring(temp + 14, temp + 17));
                                                AlertDialog.Builder tempAlertBuilder2 = new AlertDialog.Builder(UploadData.this);
                                                tempAlertBuilder2.setTitle("Submit Data")
                                                        .setMessage("Do you want to submit this data?\n" + "The FEV1 was: " + fev + " and PEF was: " + pef2 + ".")
                                                        .setNeutralButton("No", null)
                                                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                                            public void onClick(DialogInterface arg0, int arg1) {
                                                                Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                                                if (loc == null) {
                                                                    Toast("Can't get Location");
                                                                    return;
                                                                }
                                                                String msg = db.getPeakFlow(loc,String.valueOf(pef2),String.valueOf(fev));
                                                                try {
                                                                    db.Write(msg + "\n");
                                                                } catch (IOException e) {
                                                                    Log.e("ERROR", "Exception appending to log file", e);
                                                                }
                                                            }
                                                        });
                                                AlertDialog dialog2 = tempAlertBuilder2.create();
                                                dialog2.show();
                                            } else if (data.contains("CPD")) {
                                                //     statusBT.setText("Device shutdown, please press end.");
                                                Toast.makeText(getApplicationContext(), "Device shutdown.", Toast.LENGTH_LONG).show();
                                                PFMConnectButton.setImageResource(R.drawable.ic_peakflow_red);
                                                PFMConnectButton.setEnabled(true);


                                            } else {
                                              //  Toast.makeText(getApplicationContext(), "Invalid Statement Recieved", Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    });

                                } else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    } catch (IOException ex) {
                        stopWorker = true;
                    }
                }
            }
        });
        workerThread.start();
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

    }

}
