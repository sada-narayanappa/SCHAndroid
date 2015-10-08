package org.geospaces.schas;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.geospaces.schas.AsyncTasks.PostToServer;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EventListener;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import mymodule.app2.mymodule.app2.schasStrings;
import android.support.v7.app.ActionBarActivity;



public class BluetoothSettings extends ActionBarActivity implements EventListener{

    UUID                uuid;
    BluetoothAdapter    mBluetoothAdapter = null;
    BluetoothDevice     mmDevice = null;
    CheckBox            peakflow;
    TextView            storage;
    //Peakflow Below
    volatile boolean    stopWorker;
    int                 readBufferPosition;
    byte[]              readBuffer;
    Thread              workerThread;
    OutputStream        mmOutputStream = null;
    InputStream         mmInputStream = null;

    BluetoothSocket     mmSocket = null;
    LocationManager     locationManager;
    private boolean     device1previousconnect = false;
    private boolean     device2previousconnect = false;
    //Inhaler Below
    boolean             stopWorker2;
    Thread              inhalerTD;
    int                 inhalerinfo = 0;
    OutputStream        mmOutputStream2 = null;
    InputStream         mmInputStream2 = null;
    BluetoothSocket     mmSocket2 = null;
    BluetoothDevice     mmDevice2 = null;
    CheckBox            inhaler;

    private static double gpsLat = -1;
    private static double gpsLon = -1;
    private static double gpsVeloc = -1;
    private static String android_id = "NA";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_settings);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        BluetoothAdapter adap = BluetoothAdapter.getDefaultAdapter();
        android_id = (adap == null) ? "NA" : adap.getAddress().replace(":", "");

        peakflow = (CheckBox) findViewById(R.id.PeakFlow);
        inhaler = (CheckBox) findViewById(R.id.inhalercb);
        storage = (TextView) findViewById(R.id.storage);
        stopWorker = false;

        peakflow.setEnabled(false);
        storage.setText(this.calculateMem());
        inhaler.setEnabled(false);

        final Button button = (Button) findViewById(R.id.homeButton);
        button.setOnClickListener(collectDataOnClickListener);

        final Button button4 = (Button) findViewById(R.id.button4);
        button4.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    TextView t = (TextView) findViewById(R.id.storage);
                    t.setText("" + inhalerinfo);

                }
            });

        final Button attemptConnection = (Button) findViewById(R.id.attemptConnection);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            attemptConnection.setEnabled(false);
        }

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        attemptConnection.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                /*if(mmSocket != null)
                {
                    try {
                        mmSocket.close();
                    }
                    catch(IOException e){

                    }
                }
                if(mmSocket2 != null)
                {
                    try {
                        mmSocket2.close();
                    }
                    catch(IOException e){

                    }
                }*/
                mmDevice = null;
                mmDevice2 = null;
                mmSocket = null;
                mmSocket2 = null;

                BlueToothHelper.on(BluetoothSettings.this);

                if (mBluetoothAdapter.isEnabled()) {
                    findBT();
                   uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
                   // uuid = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
                    //Toast.makeText(getApplicationContext(), ""+mmSocket.isConnected(), Toast.LENGTH_SHORT).show();

                    if (mmDevice != null) {
                        try {
                            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
                            mmSocket.connect();

                        } catch (IOException e) {
                            try {
                                mmSocket = (BluetoothSocket) mmDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(mmDevice, 1);
                                mmSocket.connect();
                            } catch (Exception f) {
                                Toast.makeText(getApplicationContext(), "dev 1" + f.getMessage(), Toast.LENGTH_SHORT).show();

                            }
                        }

                        if (mmSocket != null) {
                            if (mmSocket.isConnected()) {
                                peakflow.setChecked(true);
                            } else {
                                peakflow.setChecked(false);
                            }
                        }
                    }


                    findBT2();
                    if (mmDevice2 != null) {
                        try {
                            mmSocket2 = mmDevice2.createRfcommSocketToServiceRecord(uuid);
                            mmSocket2.connect();

                        } catch (IOException e) {
                            try {
                                mmSocket2 = (BluetoothSocket) mmDevice2.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(mmDevice2, 1);
                                mmSocket2.connect();
                            } catch (Exception f) {
                                Toast.makeText(getApplicationContext(), "dev2" + f.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                        if (mmSocket2 != null) {
                            if (mmSocket2.isConnected()) {
                                inhaler.setChecked(true);
                            } else {
                                inhaler.setChecked(false);
                            }
                        }
                    }


                    if (mmSocket == null && mmSocket2 == null) {
                        Toast.makeText(getApplicationContext(), "No Devices connected", Toast.LENGTH_SHORT).show();
                        Intent intentOpenBluetoothSettings = new Intent();
                        intentOpenBluetoothSettings.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                        startActivity(intentOpenBluetoothSettings);
                    }


                    if (mmSocket != null && mmSocket2 != null) {
                        if (mmSocket.isConnected() && mmSocket2.isConnected()) {
                            //Toast.makeText(getApplicationContext(), "Both Devices Connected", Toast.LENGTH_SHORT).show();
                        }
                    }
                    if (mmSocket != null) {
                        if (mmSocket.isConnected()) {
                            // peakflow.setChecked(true);
                            Toast.makeText(getApplicationContext(), "PeakFlow Connected", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        peakflow.setChecked(false);
                    }

                    if (mmSocket2 != null) {
                        if (mmSocket2.isConnected()) {
                            Toast.makeText(getApplicationContext(), "Inhaler Connected", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        inhaler.setChecked(false);
                    }
                }
                try {
                    mmSocket.close();
                    mmSocket2.close();
                } catch (Exception e) {

                }
            }
        });

        final Button inhalebutton = (Button) findViewById(R.id.graphButton);
        if (inhalebutton != null) {
            inhalebutton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    TextView t = (TextView) findViewById(R.id.storage);
                    findBT2();
                    uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
                    if (mmDevice2 == null) {
                        return;
                    }
                    try {
                        mmSocket2 = mmDevice2.createRfcommSocketToServiceRecord(uuid);
                        mmSocket2.connect();
                    } catch (IOException e) {

                    }
                    if (mmSocket2.isConnected()) {
                        inhaler.setChecked(true);
                    } else {
                        inhaler.setChecked(false);
                    }
                    t.setText("" + mmDevice2.getName() + "-" + mmSocket2.isConnected());
                    try {
                        openBT2();
                    } catch (IOException e) {

                    }
                }
            });
        }
    }

    /**
     * This is a callback function for button2.
     */
    private View.OnClickListener collectDataOnClickListener  = new View.OnClickListener() {
        public void onClick(View v) {
            final Button button = (Button) findViewById(R.id.homeButton);

            if (button.getText().equals("Collect Data")) {
                //User Information
                if (peakflow.isChecked() && inhaler.isChecked()) {
                    Toast.makeText(getApplicationContext(), "Both Devices are ready for input", Toast.LENGTH_SHORT).show();
                } else {
                    if (peakflow.isChecked()) {
                        Toast.makeText(getApplicationContext(), "PeakFlow is ready for input", Toast.LENGTH_SHORT).show();
                    } else if (inhaler.isChecked()) {
                        Toast.makeText(getApplicationContext(), "Inhaler is ready for input", Toast.LENGTH_SHORT).show();
                    }
                }
                //PeakFlow
                if (peakflow.isChecked()) {
                    try {
                        //Toast.makeText(getApplicationContext(), "tried OpenBT", Toast.LENGTH_LONG).show();
                        openBT();
                    } catch (IOException e) {
                        button.setText("Collect Data");
                        Toast.makeText(getApplicationContext(), "Connection Lost, Unable to Connect", Toast.LENGTH_SHORT).show();
                        peakflow.setChecked(false);
                        //statusBT.setText("Connection Failed.\nPlease re-attempt connection.");
                        //e.printStackTrace();
                        //Toast.makeText(getApplicationContext(), "This is not going to be easy", Toast.LENGTH_LONG).show();
                    }
                }
                if (inhaler.isChecked()) {
                    try {
                        Toast.makeText(getApplicationContext(), "tried OpenBT", Toast.LENGTH_LONG).show();
                        openBT2();
                    } catch (IOException e) {
                        button.setText("Collect Data");
                        Toast.makeText(getApplicationContext(), "Connection Lost, Unable to Connect", Toast.LENGTH_SHORT).show();
                        inhaler.setChecked(false);
                        //statusBT.setText("Connection Failed.\nPlease re-attempt connection.");
                        //e.printStackTrace();
                        //Toast.makeText(getApplicationContext(), "This is not going to be easy", Toast.LENGTH_LONG).show();
                    }
                }

            }
            //button.setText("End Collection");
            else if (button.getText().equals("End Collection")) {
                // Toast.makeText(getApplicationContext(), "" +mmSocket.isConnected(), Toast.LENGTH_LONG).show();
                onDestroy();
                button.setText("Collect Data");
                peakflow.setChecked(false);
                inhaler.setChecked(false);
               /* try {
                    mmSocket.close();
                    mmSocket2.close();
                }
                catch(IOException e){
                    Log.e("SocketClose",e.getMessage());
                }*/
            } else {
                Toast.makeText(getApplicationContext(), "No Device to Collect From", Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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

    @Override
    protected void onPause() {
        //onDestroy();
        //inhaler.setChecked(false);
        // peakflow.setChecked(false);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    void findBT2() {
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Your device does not support Bluetooth, and is therefore incompatable.", Toast.LENGTH_LONG).show();
            onDestroy();
            return;
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().contains("RNBT")) {
                    if (mmDevice2 == null) {
                        device2previousconnect = true;
                        mmDevice2 = device;
                    }
                } else {
                    device2previousconnect = false;
                }
            }
        }

    }

    public void openBT2() throws IOException {
        //Toast.makeText(getApplicationContext(), "before" + mmSocket.isConnected(), Toast.LENGTH_SHORT).show();
        mmSocket2.close();
        //Toast.makeText(getApplicationContext(), "after" + mmSocket.isConnected(), Toast.LENGTH_SHORT).show();

        Button b = (Button) findViewById(R.id.homeButton);
        uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        if (!mmSocket2.isConnected()) {
            //Toast.makeText(getApplicationContext(), "Linking", Toast.LENGTH_SHORT).show();
            mmSocket2 = mmDevice2.createRfcommSocketToServiceRecord(uuid);
            mmSocket2.connect();
        }
        //Toast.makeText(getApplicationContext(), "Past Linking", Toast.LENGTH_LONG).show();
        mmOutputStream2 = mmSocket2.getOutputStream();
        mmInputStream2 = mmSocket2.getInputStream();

        if (mmSocket2.isConnected()) {
            inhalerThread();
            b.setText("End Collection");
        }
    }

    public void inhalerThread() {
        stopWorker2 = false;
        inhalerTD = new Thread(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted() && !stopWorker2) {
                    try {
                        Log.e("Thread", "Input Stream");
                        inhalerinfo += mmInputStream2.read();


                        //Toast.makeText(getApplicationContext(), "" + inhalerinfo, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        stopWorker2 = true;
                        Log.e("Thread", "Exception");

                    }
                }
                //Toast.makeText(getApplicationContext(), "In Thread", Toast.LENGTH_SHORT).show();
            }
        });
        //Toast.makeText(getApplicationContext(), "START", Toast.LENGTH_SHORT).show();
        Log.e("Thread", "Ran");
        inhalerTD.start();
    }

    void findBT() {
        if (mBluetoothAdapter == null) {
            return;
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().contains("ASMA")) {
                    if (mmDevice == null) {
                        device1previousconnect = true;
                        mmDevice = device;
                    }
                } else {
                    device1previousconnect = false;
                }

            }
        }
    }

    //Following code was the Code eric used to pull data from peakflow meter
    public void openBT() throws IOException {
        //Toast.makeText(getApplicationContext(), "before" + mmSocket.isConnected(), Toast.LENGTH_SHORT).show();
        mmSocket.close();
        //Toast.makeText(getApplicationContext(), "after" + mmSocket.isConnected(), Toast.LENGTH_SHORT).show();

        Button b = (Button) findViewById(R.id.homeButton);
        uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        if (!mmSocket.isConnected()) {
            //Toast.makeText(getApplicationContext(), "Linking", Toast.LENGTH_SHORT).show();
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            mmSocket.connect();
        }
        //Toast.makeText(getApplicationContext(), "Past Linking", Toast.LENGTH_LONG).show();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();

        if (mmSocket.isConnected()) {
            // Toast.makeText(getApplicationContext(), "ISCON", Toast.LENGTH_LONG).show();
            b.setText("End Collection");
            beginListenForData();
        }
        // statusBT.setText("Successful Connection");//lets user know bt is open
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
                                                //   fev1.setText("FEV1: "+fev);
                                                //   pef.setText("PEF: "+pef2);
                                                //   statusBT.setText("Collection Successful");//change to print data somewhere
                                                AlertDialog.Builder tempAlertBuilder2 = new AlertDialog.Builder(BluetoothSettings.this);
                                                tempAlertBuilder2.setTitle("Submit Data")
                                                        .setMessage("Do you want to submit this data?\n" + "The FEV1 was: " + fev + " and PEF was: " + pef2 + ".")
                                                        .setNeutralButton("No", null)
                                                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                                            public void onClick(DialogInterface arg0, int arg1) {
                                                                yes = true;
                                                                try {
                                                                    final String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                                                                    final String remainingString = ", " + fev + ", " + pef2 + ", " + gpsLat + ", " + gpsLon + ", " + gpsVeloc + " \r\n";
                                                                    final double fevData = fev;
                                                                    final int pefData = pef2;
                                                                    ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                                                                    NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                                                                    if (mWifi.isConnected()) {
                                                                        Thread submitAttack = new Thread(new Runnable() {
                                                                            public void run() {
                                                                                List<NameValuePair> data = new ArrayList<NameValuePair>(3);//break remaining into better pairs
                                                                                JSONObject values = new JSONObject();
                                                                                try {
                                                                                    values.put("fev", fev);
                                                                                    values.put("pef", pef2);
                                                                                } catch (Exception e) {
                                                                                    Log.e("JSON PeakFlow", "Failed adding values");
                                                                                }
                                                                                data.add(new BasicNameValuePair("id", android_id));
                                                                                data.add(new BasicNameValuePair("api_key", "1"));
                                                                                data.add(new BasicNameValuePair("timeRecorded", date));
                                                                                data.add(new BasicNameValuePair("val", values.toString()));
                                                                                data.add(new BasicNameValuePair("htype", "peakflow"));
                                                                                data.add(new BasicNameValuePair("lat", gpsLat + ""));
                                                                                data.add(new BasicNameValuePair("lon", gpsLon + ""));
                                                                                data.add(new BasicNameValuePair("veloc", gpsVeloc + ""));
                                                                                PostToServer postMan = new PostToServer();
                                                                                final String temp = postMan.postResults(data, "http://www.geospaces.org/aura/webroot/health.jsp");

                                                                                sendFiles();
                                                                            }
                                                                        });
                                                                        submitAttack.start();
                                                                    } else {
                                                                        Toast.makeText(getApplicationContext(), "Writing", Toast.LENGTH_LONG).show();
                                                                        String tempString = date + ", " + fev + ", " + pef2 + ", " + schasStrings.getTheGPS(locationManager) + "\r\n";
                                                                        File externalMem2 = Environment.getExternalStorageDirectory();
                                                                        File directory2 = new File(externalMem2.getAbsolutePath() + "/SCHAS");
                                                                        directory2.mkdirs();
                                                                        File file2 = new File(directory2, "peakFlowData.txt");


                                                                        if (file2.exists()) {
                                                                            FileWriter fw = new FileWriter(file2, true);
                                                                            fw.write(tempString);
                                                                            fw.close();
                                                                        } else {
                                                                            FileWriter fw = new FileWriter(file2, false);
                                                                            fw.write("Year-Month-Day Hour:Minute:Second, FEV1, PEF,Lat,Lon,Velocity\r\n");
                                                                            fw.write(tempString);
                                                                            fw.close();
                                                                        }
                                                                    }
                                                                } catch (Exception e) {
                                                                    yes = false;
                                                                    Log.e("WRITING ERROR", e.toString());
                                                                } finally {
                                                                    if (yes) {
                                                                        Toast.makeText(getApplicationContext(), "Information Saved", Toast.LENGTH_SHORT).show();
                                                                    } else {
                                                                        Toast.makeText(getApplicationContext(), "Error Saving", Toast.LENGTH_SHORT).show();
                                                                    }
                                                                }
                                                            }


                                                        });
                                                AlertDialog dialog2 = tempAlertBuilder2.create();
                                                dialog2.show();
                                                //if(yes){//probably garbage
                                                //}
                                            } else if (data.contains("CPD")) {
                                                //     statusBT.setText("Device shutdown, please press end.");
                                                Toast.makeText(getApplicationContext(), "Device shutdown, please press end.", Toast.LENGTH_LONG).show();

                                            } else {
                                                //     statusBT.setText("Invalid Statement Recieved");
                                                Toast.makeText(getApplicationContext(), "Invalid Statement Recieved", Toast.LENGTH_LONG).show();
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


    private void sendFiles() {
        File externalMem = Environment.getExternalStorageDirectory();
        File directory = new File(externalMem.getAbsolutePath() + "/PatientHelper");
        directory.mkdirs();
        File file = new File(directory, "medicationData.txt");
        if (file.exists()) {
            BufferedReader br = null;
            String line;
            try {
                br = new BufferedReader(new FileReader(file));
            } catch (FileNotFoundException e) {
                //e.printStackTrace();
            }
            try {
                while ((line = br.readLine()) != null) {//use split method
                    if (!(line.substring(0, 4).equals("Year"))) {
                        String[] readData = line.split(",");
                        List<NameValuePair> filedata = new ArrayList<NameValuePair>(3);
                        JSONObject fileValues = new JSONObject();
                        try {
                            fileValues.put("medicationName", readData[1]);
                            fileValues.put("medicationDose", readData[2]);
                        } catch (Exception e) {
                            Log.e("JSON Medication Service", "Failed adding values");
                        }
                        filedata.add(new BasicNameValuePair("id", android_id));
                        filedata.add(new BasicNameValuePair("api_key", "1"));
                        filedata.add(new BasicNameValuePair("timeRecorded", readData[0]));
                        filedata.add(new BasicNameValuePair("val", fileValues.toString()));
                        filedata.add(new BasicNameValuePair("lat", readData[3]));
                        filedata.add(new BasicNameValuePair("lon", readData[4]));
                        filedata.add(new BasicNameValuePair("veloc", readData[5]));
                        filedata.add(new BasicNameValuePair("htype", "medication"));
                        PostToServer filepostMan = new PostToServer();
                        filepostMan.postResults(filedata, "http://www.geospaces.org/aura/webroot/health.jsp");
                    }
                }

            } catch (IOException e1) {
                //e1.printStackTrace();
            }


            try {
                if (br != null)
                    br.close();
            } catch (IOException e) {
                //e.printStackTrace();
            }
            file.delete();
        }
        file = new File(directory, "peakFlowData.txt");
        if (file.exists()) {
            BufferedReader br = null;
            String line;
            try {
                br = new BufferedReader(new FileReader(file));
            } catch (FileNotFoundException e) {
                //e.printStackTrace();
            }
            try {
                while ((line = br.readLine()) != null) {//use split method
                    if (!(line.substring(0, 4).equals("Year"))) {
                        String[] readData = line.split(",");
                        List<NameValuePair> filedata = new ArrayList<NameValuePair>(3);
                        JSONObject values2 = new JSONObject();
                        try {
                            values2.put("fev", readData[1]);
                            values2.put("pef", readData[2]);
                        } catch (Exception e) {
                            Log.e("JSON PeakFlow Service", "Failed adding values");
                        }
                        filedata.add(new BasicNameValuePair("id", android_id));
                        filedata.add(new BasicNameValuePair("api_key", "1"));
                        filedata.add(new BasicNameValuePair("timeRecorded", readData[0]));
                        filedata.add(new BasicNameValuePair("htype", "peakflow"));
                        filedata.add(new BasicNameValuePair("val", values2.toString()));
                        filedata.add(new BasicNameValuePair("lat", readData[3]));
                        filedata.add(new BasicNameValuePair("lon", readData[4]));
                        filedata.add(new BasicNameValuePair("veloc", readData[5]));
                        PostToServer filepostMan = new PostToServer();
                        filepostMan.postResults(filedata, "http://www.geospaces.org/aura/webroot/health.jsp");
                    }
                }

            } catch (IOException e1) {
                //e1.printStackTrace();
            }


            try {
                if (br != null)
                    br.close();
            } catch (IOException e) {
                //e.printStackTrace();
            }
            file.delete();
        }
        file = new File(directory, "attackData.txt");
        if (file.exists()) {
            BufferedReader br = null;
            String line;
            try {
                br = new BufferedReader(new FileReader(file));
            } catch (FileNotFoundException e) {
                //e.printStackTrace();
            }
            try {
                while ((line = br.readLine()) != null) {//use split method
                    if (!(line.substring(0, 4).equals("Year"))) {
                        String[] readData = line.split(",");
                        List<NameValuePair> filedata = new ArrayList<NameValuePair>(4);
                        JSONObject values2 = new JSONObject();
                        try {
                            values2.put("solution", readData[1]);
                            values2.put("severity", readData[2]);
                            values2.put("description", readData[3]);
                            for (int i = 7; i < readData.length; i++) {
                                values2.put(readData[i], "Checked");

                            }
                        } catch (Exception e) {
                            Log.e("JSON Attack Service", "Failed adding values");
                        }
                        filedata.add(new BasicNameValuePair("id", android_id));
                        filedata.add(new BasicNameValuePair("api_key", "1"));
                        filedata.add(new BasicNameValuePair("timeRecorded", readData[0]));
                        filedata.add(new BasicNameValuePair("htype", "attack"));
                        filedata.add(new BasicNameValuePair("val", values2.toString()));
                        filedata.add(new BasicNameValuePair("lat", readData[4]));
                        filedata.add(new BasicNameValuePair("lon", readData[5]));
                        filedata.add(new BasicNameValuePair("veloc", readData[6]));
                        PostToServer filepostMan = new PostToServer();
                        filepostMan.postResults(filedata, "http://www.geospaces.org/aura/webroot/health.jsp");
                    }
                }

            } catch (IOException e1) {
                //e1.printStackTrace();
            }


            try {
                if (br != null)
                    br.close();
            } catch (IOException e) {
                //e.printStackTrace();
            }
            file.delete();
        }

        file = new File(directory, "dailyData.txt");
        if (file.exists()) {
            BufferedReader br = null;
            String line;
            try {
                br = new BufferedReader(new FileReader(file));
            } catch (FileNotFoundException e) {
                //e.printStackTrace();
            }
            try {
                while ((line = br.readLine()) != null) {//use split method
                    if (!(line.substring(0, 4).equals("Year"))) {
                        String[] readData = line.split(",");
                        List<NameValuePair> filedata = new ArrayList<NameValuePair>(3);
                        JSONObject values2 = new JSONObject();
                        try {
                            for (int i = 4; i < readData.length; i++) {
                                values2.put(readData[i], "Checked");

                            }
                        } catch (Exception e) {
                            Log.e("JSON DailyData Service", "Failed adding values");
                        }
                        filedata.add(new BasicNameValuePair("id", android_id));
                        filedata.add(new BasicNameValuePair("api_key", "1"));
                        filedata.add(new BasicNameValuePair("timeRecorded", readData[0]));
                        filedata.add(new BasicNameValuePair("htype", "Daily"));
                        filedata.add(new BasicNameValuePair("val", values2.toString()));
                        filedata.add(new BasicNameValuePair("lat", readData[1]));
                        filedata.add(new BasicNameValuePair("lon", readData[2]));
                        filedata.add(new BasicNameValuePair("veloc", readData[3]));
                        Log.e("Read from txt file", readData[1] + "" + readData[2] + "" + readData[3]);
                        PostToServer filepostMan = new PostToServer();
                        filepostMan.postResults(filedata, "http://www.geospaces.org/aura/webroot/health.jsp");
                    }
                }

            } catch (IOException e1) {
                //e1.printStackTrace();
            }


            try {
                if (br != null)
                    br.close();
            } catch (IOException e) {
                //e.printStackTrace();
            }
            file.delete();
        }//here
        file = new File(directory, "inhalerDuration.txt");
        if (file.exists()) {
            BufferedReader br = null;
            String line;
            try {
                br = new BufferedReader(new FileReader(file));
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            try {
                while ((line = br.readLine()) != null) {//Year-Month-Day,Hour:Minute:Second,Latitude,Longitude,Speed
                    if (!(line.substring(0, 4).equals("Year"))) {
                        String[] readData = line.split(",");
                        List<NameValuePair> data2 = new ArrayList<NameValuePair>(4);
                        data2.add(new BasicNameValuePair("id", android_id));
                        data2.add(new BasicNameValuePair("api_key", "1"));
                        data2.add(new BasicNameValuePair("timeRecorded", readData[0]));
                        data2.add(new BasicNameValuePair("lat", readData[1]));
                        data2.add(new BasicNameValuePair("lon", readData[2]));
                        data2.add(new BasicNameValuePair("veloc", readData[3]));
                        PostToServer postMan2 = new PostToServer();
                        postMan2.postResults(data2, "http://www.geospaces.org/aura/webroot/health.jsp");
                    }
                }

            } catch (Exception e1) {
                // TODO Auto-generated catch block
                Log.e("Error Reading InhalerUse", "Error Reading Inhaler");
            }
            file.delete();

        }

    }

    public void closeBT() throws IOException {
        stopWorker = true;
        stopWorker2 = true;
        if (mmOutputStream != null)
            mmOutputStream.close();
        if (mmInputStream != null)
            mmInputStream.close();
        if (mmSocket != null)
            mmSocket.close();
        if (mmOutputStream2 != null)
            mmOutputStream2.close();
        if (mmInputStream2 != null)
            mmInputStream2.close();
        if (mmSocket2 != null)
            mmSocket2.close();
        Toast.makeText(getApplicationContext(), "Connection Closed", Toast.LENGTH_LONG).show();
    }

    public String calculateMem() {
        //Calculating Available
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long bytesAvailable = (long) stat.getBlockSize() * (long) stat.getAvailableBlocks();
        long megAvailable = bytesAvailable / (1024 * 1024);

        //Calculating Total
        long blockSize = stat.getBlockSize();
        long totalBlocks = stat.getBlockCount();
        long Total = (totalBlocks * blockSize) / (1024 * 1024);

        long megUsed = Total - megAvailable;

        return (megUsed + "/" + Total + "MB used");
    }

}