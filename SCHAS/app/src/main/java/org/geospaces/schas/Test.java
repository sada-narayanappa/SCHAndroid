package org.geospaces.schas;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.geospaces.schas.Adapters.bluetoothDevicesAdapter;

import java.util.ArrayList;

public class Test extends ActionBarActivity {

    private final static int REQUEST_ENABLE_BT = 1;
    private BluetoothManager btManager;
    private BluetoothAdapter btAdapter;
    private ArrayList<BluetoothDevice> btDeviceList = new ArrayList<BluetoothDevice>();
    private ListView listview;
    private ProgressBar btSpinner;
    private ArrayAdapter<BluetoothDevice> itemsAdapter;

    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            Log.d("DeviceFound", "" + device.toString());
            btDeviceList.add(device);
            Log.d("arrayListStatus", btDeviceList.toString());
            itemsAdapter.notifyDataSetChanged();
        }
    };

    private final BluetoothGattCallback btleGattCallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            // this will get called anytime you perform a read or write characteristic operation
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.activity_test);

        //set object default values
        itemsAdapter = new bluetoothDevicesAdapter(this.getApplicationContext(), btDeviceList);
        listview = (ListView) findViewById(R.id.btdeviceListView);
        btSpinner = (ProgressBar)findViewById(R.id.btProgressBar);

        //Set up button listeners
        final Button searchButton = (Button) findViewById(R.id.searchBtn);
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
            }
        });

        final Button cancelButton = (Button) findViewById(R.id.cancelBtn);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                checkBluetoothActive();

                btAdapter.stopLeScan(leScanCallback);
                Log.d("btCancel", "Device search canceled");
                Toast.makeText(Test.this, "Bluetooth Scanning Ended", Toast.LENGTH_SHORT).show();
                btSpinner.setVisibility(View.GONE);
            }
        });

        //Link the list view to the adapter
        listview.setAdapter(itemsAdapter);


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

    }

    @Override
    protected void onPause() {
        super.onPause();
        btAdapter.stopLeScan(leScanCallback);

    }
}