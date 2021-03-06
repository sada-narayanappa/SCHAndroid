package org.geospaces.schas;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
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
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.geospaces.schas.Services.LocationService;
import org.geospaces.schas.Services.MyFirebaseInstanceIDService;
import org.geospaces.schas.utils.CustomExceptionHandler;
import org.geospaces.schas.utils.SCHASSettings;
import org.geospaces.schas.utils.db;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class UploadData extends AppCompatActivity{

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

    //More Bluetooth Variables for Inhlaer Cap
    private BluetoothLeScanner mLEScanner;
    BluetoothGatt mGatt;
    List<BluetoothGatt> mGatts = new ArrayList<BluetoothGatt>();
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private boolean deviceFound = false;
    private int currentTimestamp = 0;
    private Date[] inhalerCapPresses = new Date[64];
    private int[] buttonDurations = new int[64];
    private Calendar now;
    private boolean currentlyConnected = false;



    //Floating Action Button
    private FloatingActionButton menuButton;
    private FloatingActionButton mildAttackButton;
    private FloatingActionButton mediumAttackButton;
    private FloatingActionButton severeAttackButton;
    private FloatingActionButton PFMConnectButton;
    private FloatingActionButton inhalerButton;
    private FloatingActionButton manualPFButton;
    private boolean menuActive = false;

    public static Button startStop;
    public static Button uploadButton;

    public Button googleLocsButton;

    private static Context mContext;
    private static Activity mActivity;

    public static String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler());

        setContentView(R.layout.activity_upload_data);

        setProgressBarIndeterminate(true);

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        BluetoothManager BTMan = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = BTMan.getAdapter();
        mHandler = new Handler();
        mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
        settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        filters = new ArrayList<ScanFilter>();

        SP = PreferenceManager.getDefaultSharedPreferences(this);
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
        manualPFButton = (FloatingActionButton) findViewById(R.id.manualPeakflow);
        menuButton = (FloatingActionButton) findViewById(R.id.menu_button);
        menuButton.setOnClickListener(menu_button);


        findViewById(R.id.PFMConnectButton).setOnClickListener(pfm_BT_connect);
        startStop = (Button) findViewById(R.id.homeButton);
        startStop.setOnClickListener(startStopButton);
        uploadButton = (Button) findViewById(R.id.graphButton);
        uploadButton.setOnClickListener(uploadCB);
        findViewById(R.id.resetButton).setOnClickListener(resetCB);
        findViewById(R.id.mildAttackButton).setOnClickListener(mild_attack_button);
        findViewById(R.id.mediumAttackButton).setOnClickListener(medium_attack_button);
        findViewById(R.id.severeAttackButton).setOnClickListener(severe_attack_button);
        findViewById(R.id.inhalerButton).setOnClickListener(inhaler_button);
        findViewById(R.id.manualPeakflow).setOnClickListener(manual_PF_enter);
        //googleLocsButton = (Button) findViewById(R.id.googleLocationsButton);
        //googleLocsButton.setOnClickListener(getGoogleData);

        //heartBeatReceiver.setAct(UploadData.this);

        mContext = this;
        mActivity = this;

        GpsStatusCheck(mContext);
    }

    public static Context GetContext(){
        return mContext;
    }

    public static Activity GetActivity() {
        return mActivity;
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

    private void Toast(String msg) {
        Context ctx = getApplicationContext();
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
    }

    private View.OnClickListener manual_PF_enter = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //create a runnable that will launch a menu for entering the PF values
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle("Manual PF Enter");

            LayoutInflater inflater = (LayoutInflater) UploadData.this.getSystemService(LAYOUT_INFLATER_SERVICE);
            final View inflateView = inflater.inflate(R.layout.manual_pf_entry, null);
            builder.setView(inflateView);

            //final EditText pefText = (EditText) inflateView.findViewById(R.id.pef);
            //final EditText fevText = (EditText) inflateView.findViewById(R.id.fev);

            builder.setPositiveButton("Enter Values", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    //String msg = db.getPeakFlow(pefText.getText().toString(), fevText.getText().toString());
                    Log.v("peakflow", "positive button");

                    Dialog castDialog = (Dialog) dialog;
                    EditText pefText = (EditText) castDialog.findViewById(R.id.pef);
                    EditText fevText = (EditText) castDialog.findViewById(R.id.fev);

                    String msg = db.getPeakFlow(pefText.getText().toString(), fevText.getText().toString());
                    Log.v("peakflow2", msg);
                    try
                    {
                        db.Write(msg + "\n");
                    } catch (IOException e) {
                        Log.e("ERROR", "Exception appending to log file", e);
                    }
                }
            });

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
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
            if ( null == (str = db.canUploadData(ctx))) {
                Toast( "NO Wireless Connection! Please check back");
            }

            try {
                str = db.Upload(ctx, UploadData.this);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if ( str != null) {
                Toast( str + " retry");
            }
        }
    };

    private View.OnClickListener startStopButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Context ctx = getApplicationContext();
            Intent stopLocationService = new Intent(ctx, LocationService.class);
            String buttonText = startStop.getText().toString();
            if (buttonText.equals("Start")) {
                startStop.setText("Stop");
                startStop.setBackgroundColor(Color.RED);
                startService(stopLocationService);
                //GoogleMaps.startPoll();
                Toast.makeText(UploadData.this, "Starting Location Polling", Toast.LENGTH_SHORT).show();
            }
            else if (buttonText.equals("Stop")) {
                startStop.setText("Start");
                startStop.setBackgroundColor(Color.GREEN);
                stopService(stopLocationService);
                //GoogleMaps.stopPoll();
                Toast.makeText(UploadData.this, "Stopping Location Polling", Toast.LENGTH_SHORT).show();
            }

            //throw new RuntimeException("Crash!");
        }
    };

//    private View.OnClickListener getGoogleData = new View.OnClickListener() {
//        @Override
//        public void onClick(View v) {
//            if (db.canUploadData(mContext) != null){
//                String url = buildGoogleLocationURL();
//                new DownloadXmlTask().execute(url);
//            }
//            else {
//                Toast.makeText(mContext, "Please Connect Wi-Fi and Retry Download\nof Google Locations",
//                        Toast.LENGTH_SHORT).show();
//            }
//        }
//    };

    //example url from google help forum                        year   M   D       year   M   D
    //https://www.google.com/maps/timeline/kml?=0&pb=!1m8!1m3!1i2015!2i7!3i1!2m3!1i2015!2i7!3i8
//    private String buildGoogleLocationURL(){
//        //get current data in a string format
//        Calendar now = Calendar.getInstance();
//        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
//        String formattedDate = df.format(now.getTime());
//        String[] date = formattedDate.split("-");
//
//        //format the date fields for use in the url
//        int year = Integer.valueOf(date[2]);
//        String yearMinus1 = String.valueOf(year - 1);
//
//        if(date[1].charAt(0) == '0'){
//            date[1] = date[1].substring(1);
//        }
//
//        if(date[0].charAt(0) == '0'){
//            date[0] = date[0].substring(1);
//        }
//
//
//
//        url = "https://www.google.com/maps/timeline/kml?authuser=gundy.goo@gmail.com&pb=!1m8!1m3!1i"
//                + date[2] + "!2i"
//                + date[1] + "!3i"
//                + date[0] + "!2m3!1i"
//                + yearMinus1 + "!2i"
//                + date[1] + "!3i"
//                + date[0];
//
//        return url;
//    }

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
    }

    public void AttackConfirmPopUpCreator(String label,final String severity,boolean inhal) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(label);

        if(inhal == false) {
            builder.setPositiveButton("Confirm Attack", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String msg = db.getAttack(severity);
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
                    String msg = db.getAttack(severity);
                    try {
                        db.Write(msg + "\n");
                        db.Upload(mContext, UploadData.this);
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
            if(mBluetoothAdapter != null){
                if (mBluetoothAdapter.isEnabled()){
                    beginScanForInhaler();
                }
                else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setMessage("Your Bluetooth is disabled, and we cannot access inhaler cap data without it.\nWould you like to enable Bluetooth?")
                            .setCancelable(false)
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                public void onClick(final DialogInterface dialog, final int id) {
                                    mBluetoothAdapter.enable();
                                    beginScanForInhaler();
                                }
                            })
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                public void onClick(final DialogInterface dialog, final int id) {
                                    dialog.cancel();
                                }
                            });
                    AlertDialog alert = builder.create();
                    alert.show();
                }
            }
        }
    };

    private void beginScanForInhaler(){
        Toast.makeText(UploadData.this, "Scanning for and attempting to pair with inhaler cap", Toast.LENGTH_SHORT).show();
        if (Build.VERSION.SDK_INT >= 21) {
            mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
            settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            filters = new ArrayList<ScanFilter>();
        }

        scanLeDevice(true);
    }

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

                manualPFButton.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        manualPFButton.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in));
                    }
                }, 250);

                inhalerButton.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        menuButton.setImageResource(R.drawable.ic_x);
                        inhalerButton.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in));
                    }
                }, 300);


                mildAttackButton.setEnabled(true);
                mediumAttackButton.setEnabled(true);
                severeAttackButton.setEnabled(true);
                PFMConnectButton.setEnabled(true);
                inhalerButton.setEnabled(true);
                manualPFButton.setEnabled(true);

            }
            else{
                menuActive = false;

                mildAttackButton.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out));
                mediumAttackButton.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out));
                severeAttackButton.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out));
                PFMConnectButton.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out));
                inhalerButton.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out));
                manualPFButton.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out));

                mildAttackButton.setEnabled(false);
                mediumAttackButton.setEnabled(false);
                severeAttackButton.setEnabled(false);
                PFMConnectButton.setEnabled(false);
                inhalerButton.setEnabled(false);
                manualPFButton.setEnabled(false);

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

            Toast.makeText(UploadData.this, "Attempting bluetooth peakflow pairing", Toast.LENGTH_SHORT).show();

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
                                intentOpenBluetoothSettings.setAction(Settings.ACTION_BLUETOOTH_SETTINGS);
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
                                                                String msg = db.getPeakFlow(String.valueOf(pef2),String.valueOf(fev));
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


    private void scanLeDevice(final boolean enable) {
        if (enable) {
            if (mBluetoothAdapter.getScanMode() !=
                    BluetoothAdapter.SCAN_MODE_CONNECTABLE) {
                int x = mBluetoothAdapter.getScanMode();
                Intent discoverableIntent =
                        new Intent(
                                BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(
                        BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
                        300); // You are able to set how long it is discoverable.
                startActivity(discoverableIntent);
            }
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT < 21) {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    } else {
                        mLEScanner.stopScan(mScanCallback);

                    }
                }
            }, SCAN_PERIOD);
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            } else {
                mLEScanner.startScan(filters, settings, mScanCallback);
            }
        } else {
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            } else {
                mLEScanner.stopScan(mScanCallback);
            }
        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            BluetoothDevice btDevice = result.getDevice();
            String deviceName = btDevice.getName();

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
            String familiarMAC = sharedPref.getString("Familiar_MAC_Address", "");

            if(!currentlyConnected && deviceName != null && deviceName.startsWith("Inhaler Cap"))
            {
                if(familiarMAC.equals(""))
                {
                    deviceFound = true;

                    currentlyConnected = true;

                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("Familiar_MAC_Address", btDevice.getAddress());
                    editor.commit();

                    connectToDevice(btDevice);

                    verifyDevice();
                }
                else if(familiarMAC.equals(btDevice.getAddress()))
                {
                    currentlyConnected = true;
                    connectToDevice(btDevice);
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results)
            {
                Log.i("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };

    public void connectToDevice(BluetoothDevice device) {
        mGatts.add(device.connectGatt(this, false, gattCallback));
        scanLeDevice(false);// will stop after first device detection
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    if(deviceFound)
                    {
                        deviceFound = false;
                        gatt.close();
                        currentlyConnected = false;
                    }
                    else {
                        gatt.discoverServices();
                    }
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    currentlyConnected = false;
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status)
        {
            BluetoothGattCharacteristic characteristic = gatt.getServices().get(3).getCharacteristics().get(1);
            characteristic.setValue("1");

            if(gatt.writeCharacteristic(characteristic)) // SET COMMAND
            {
                now = Calendar.getInstance();

            }
            else
            {
                Log.i("BLE", "Unable to initiate write command");
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
            if(characteristic.getUuid() == gatt.getServices().get(3).getCharacteristics().get(1).getUuid()) // SET COMMAND
            {
                if(status == BluetoothGatt.GATT_SUCCESS)
                {
                    gatt.readCharacteristic(gatt.getServices().get(3).getCharacteristics().get(0));
                }
                else
                {
                    Log.i("BLE", "Set command unsuccessful.");
                }
            }
            else if(characteristic.getUuid() == gatt.getServices().get(2).getCharacteristics().get(1).getUuid()) // CLEAR TIMESTAMP LIST COMMAND
            {
                if(status == BluetoothGatt.GATT_SUCCESS)
                {
                    gatt.readCharacteristic(gatt.getServices().get(4).getCharacteristics().get(0));
                }
                else
                {
                    Log.i("BLE", "Clear command unsuccessful.");
                }
            }
            else if(characteristic.getUuid() == gatt.getServices().get(4).getCharacteristics().get(1).getUuid()) // CLEAR DURATION LIST COMMAND
            {
                if(status == BluetoothGatt.GATT_SUCCESS)
                {
                    gatt.close();

                    currentlyConnected = false;

                    deviceFound = false;

                    inhalerCapToTextFile();
                }
                else
                {
                    Log.i("BLE", "Clear command unsuccessful.");
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
            if(characteristic.getUuid() == gatt.getServices().get(3).getCharacteristics().get(0).getUuid()) // read current time
            {
                currentTimestamp = characteristic.getValue()[0] & 0xFF;
                currentTimestamp <<= 8;
                currentTimestamp |= characteristic.getValue()[1] & 0xFF;
                currentTimestamp <<= 8;
                currentTimestamp |= characteristic.getValue()[2] & 0xFF;
                currentTimestamp <<= 8;
                currentTimestamp |= characteristic.getValue()[3] & 0xFF;


                if(!gatt.readCharacteristic(gatt.getServices().get(2).getCharacteristics().get(0)))
                {
                    Log.i("BLE", "Unable to read timestamp list");
                }
            }
            else if(characteristic.getUuid() == gatt.getServices().get(2).getCharacteristics().get(0).getUuid()) // read timestamp list
            {
                byte[] values = characteristic.getValue();

                calculateTimestamps(values);

                BluetoothGattCharacteristic wCh = gatt.getServices().get(2).getCharacteristics().get(1);
                wCh.setValue("1");
                if(!gatt.writeCharacteristic(wCh))
                {
                    Log.i("BLE", "Unable to execute write command");
                }
            }
            else if(characteristic.getUuid() == gatt.getServices().get(4).getCharacteristics().get(0).getUuid())
            {
                byte[] values = characteristic.getValue();

                calculateDurations(values);

                BluetoothGattCharacteristic wCh = gatt.getServices().get(4).getCharacteristics().get(1);
                wCh.setValue("1");
                if(!gatt.writeCharacteristic(wCh))
                {
                    Log.i("BLE", "Unable to execute write command");
                }
            }
        }
    };

    private void verifyDevice()
    {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_NEGATIVE:
                        // Do Nothing
                        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.remove("Familiar_MAC_Address");
                        editor.commit();
                        break;

                    case DialogInterface.BUTTON_POSITIVE:
                        // Clear the shared preference
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage("Did the green light on the device turn off?").setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

    private void calculateTimestamps(byte[] values)
    {
        int[] timestamps = new int[64];
        for(int i = 0; i < 64; i++)
        {
            int val = 0;
            val = (int)values[i*4] & 0xFF;
            val <<= 8;
            val |= (int)values[i*4 + 1] & 0xFF;
            val <<= 8;
            val |= (int)values[i*4 + 2] & 0xFF;
            val <<= 8;
            val |= (int)values[i*4 + 3] & 0xFF;

            timestamps[i] = val;
        }

        for(int i = 0; timestamps[i] != 0 && i < 64; i++)
        {
            now.add(Calendar.SECOND, timestamps[i] - currentTimestamp);
            inhalerCapPresses[i] = now.getTime();
            now.add(Calendar.SECOND, currentTimestamp - timestamps[i]);
        }
    }

    private void calculateDurations(byte[] values)
    {
        for(int i = 0; i < 64; i++)
        {
            int val = 0;
            val = (int)values[i*2] & 0xFF;
            val <<= 8;
            val |= (int)values[i*2 + 1] & 0xFF;
            buttonDurations[i] = val;
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i("onLeScan", device.toString());
                            connectToDevice(device);
                        }
                    });
                }
            };

    private void inhalerCapToTextFile(){
        int i = 0;

        while (inhalerCapPresses[i] != null){
            db.writeInhalerDataToFile(buttonDurations[i], inhalerCapPresses[i]);
            i++;
        }

        buttonDurations = new int[64];
        inhalerCapPresses = new Date[64];

        try {
            db.Upload(mContext, UploadData.this);
        } catch (IOException e) {
            e.printStackTrace();
            Log.i("Upload Error", "Error uploading after inhaler cap write");
        }
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

    public void GpsStatusCheck(Context context){
        LocationManager locationManager = (LocationManager)context.getSystemService((Context.LOCATION_SERVICE));
        if ( !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
        }
    }

    private void buildAlertMessageNoGps() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage("Your GPS is disabled, and SCHAS needs your locations to run.\nWould you like to enable GPS?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

//    private class DownloadXmlTask extends AsyncTask<String, Void, String> {
//
//        @Override
//        protected String doInBackground(String... params) {
//            try {
//                loadXmlFromNetwork(params[0]);
//                return "Finished Downloading Google Points Successfully";
//            } catch (IOException e){
//                return "Error: Connection with URL";
//            } catch (XmlPullParserException e) {
//                return "Error: Parsing XML";
//            }
//        }
//
//        @Override
//        protected void onPostExecute(String result) {
//            Toast.makeText(mContext, result, Toast.LENGTH_LONG).show();
//
//        }
//    }
//
//    private void loadXmlFromNetwork(String urlString) throws XmlPullParserException, IOException {
//        InputStream in = null;
//
//        GetGoogleLocations parser = new GetGoogleLocations();
//        List entries = null;
//
//        try {
//            in = downloadUrl(urlString);
//            entries = parser.parse(in);
//        } finally {
//            if (in != null){
//                in.close();
//            }
//        }
//
//        //do stuff with db class text file creation here
//        db.CreateGoogleLocationFile(entries);
//        db.Upload(mContext, UploadData.this);
//    }
//
//    private InputStream downloadUrl(String urlString) throws IOException {
//        URL url = new URL(urlString);
//        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//        conn.setReadTimeout(10000 /* milliseconds */);
//        conn.setConnectTimeout(15000 /* milliseconds */);
//        conn.setRequestMethod("GET");
//        conn.setDoInput(true);
//        // Starts the query
//        conn.connect();
//        return conn.getInputStream();
//    }
}
