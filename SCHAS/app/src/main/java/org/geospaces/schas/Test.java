package org.geospaces.schas;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.geospaces.schas.Adapters.bluetoothDevicesAdapter;
import org.geospaces.schas.Services.bleService;

import java.util.ArrayList;

public class Test extends ActionBarActivity implements AdapterView.OnItemClickListener {

    private final static int REQUEST_ENABLE_BT = 1;
    private BluetoothManager btManager;
    private BluetoothAdapter btAdapter;
    private ArrayList<BluetoothDevice> btDeviceList = new ArrayList<BluetoothDevice>();
    private ListView listview;
    private ProgressBar btSpinner;
    private ArrayAdapter<BluetoothDevice> itemsAdapter;
    private bleService mBluetoothLeService;
    protected TextView mConnectionState;
    private Button searchButton;
    private Button cancelButton;
    private String mDeviceAddress;

    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            Log.d("DeviceFound", "" + device.toString());
            btDeviceList.add(device);
            Log.d("arrayListStatus", btDeviceList.toString());
            itemsAdapter.notifyDataSetChanged();
        }
    };

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(final ComponentName componentName, final IBinder service) {
            mBluetoothLeService = ((bleService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e("initalizeBT", "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(final ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BluetoothGattCallback btleGattCallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {

        }

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            // this will get called when a device connects or disconnects
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            // this will get called after the client initiates a BluetoothGatt.discoverServices() call
        }
    };

    private boolean mConnected = false;
    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device. This can be a
    // result of read or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (bleService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (bleService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
            } else if (bleService.
                    ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the
                // user interface.
                //displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (bleService.ACTION_DATA_AVAILABLE.equals(action)) {
                //displayData(intent.getStringExtra(bleService.EXTRA_DATA));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.activity_test);

        //set object default values
        itemsAdapter = new bluetoothDevicesAdapter(this.getApplicationContext(), btDeviceList);
        listview = (ListView) findViewById(R.id.btdeviceListView);
        btSpinner = (ProgressBar)findViewById(R.id.btProgressBar);
        mConnectionState = (TextView) findViewById(R.id.connection_state);

        //Set up button listeners
        searchButton = (Button) findViewById(R.id.searchBtn);
        searchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                checkBluetoothActive();

                btDeviceList.clear();
                itemsAdapter.clear();
                itemsAdapter.notifyDataSetChanged();

                btAdapter.startLeScan(leScanCallback);
                Log.d("btSearch", "Device search began");
                Toast.makeText(Test.this, "Bluetooth Scanning Began", Toast.LENGTH_SHORT).show();
                btSpinner.setVisibility(View.VISIBLE);
                cancelButton.setEnabled(true);
                searchButton.setEnabled(false);
            }
        });

        cancelButton = (Button) findViewById(R.id.cancelBtn);
        cancelButton.setEnabled(false);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                checkBluetoothActive();

                btAdapter.stopLeScan(leScanCallback);
                Log.d("btCancel", "Device search canceled");
                Toast.makeText(Test.this, "Bluetooth Scanning Ended", Toast.LENGTH_SHORT).show();
                btSpinner.setVisibility(View.GONE);
                searchButton.setEnabled(true);
                cancelButton.setEnabled(false);
            }
        });

        //Link the list view to the adapter
        listview.setAdapter(itemsAdapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent,
                                    View view, int position, long id) {
                Log.d("My POSITION", "" + position);

            }
        });

        updateConnectionState(R.string.disconnected);
    }

    // Checks if the phone has bluetooth enabled
    public void checkBluetoothActive() {
        btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);

        btAdapter = btManager.getAdapter();
        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
        }
    }
    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final int colourId;

                switch (resourceId) {
                    case R.string.connected:
                        colourId = android.R.color.holo_green_dark;
                        break;
                    case R.string.disconnected:
                        colourId = android.R.color.holo_red_dark;
                        break;
                    default:
                        colourId = android.R.color.black;
                        break;
                }

                mConnectionState.setText(resourceId);
                mConnectionState.setTextColor(getResources().getColor(colourId));
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        final BluetoothDevice device = itemsAdapter.getItem(position);
        if (device == null) return;

        else{
            mBluetoothLeService.connect(device.getAddress());
        }

        //   final Intent intent = new Intent(this, DeviceDetailsActivity.class);
        //   intent.putExtra(DeviceDetailsActivity.EXTRA_DEVICE, device);

        //   startActivity(intent);
        //   Toast.makeText(this,"OnClickWorks :)",Toast.LENGTH_SHORT);
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

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d("OnResume", "Connect request result=" + result);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
        if(btAdapter != null && btAdapter.isDiscovering()) {
            btAdapter.stopLeScan(leScanCallback);
        }


    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(bleService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(bleService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(bleService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(bleService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}