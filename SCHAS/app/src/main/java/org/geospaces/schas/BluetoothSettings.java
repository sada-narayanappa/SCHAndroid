package org.geospaces.schas;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.hardware.Sensor;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.List;
import java.util.Set;


public class BluetoothSettings extends Activity {
    BluetoothAdapter mBluetoothAdapter=null;
    BluetoothDevice mmDevice=null;
    CheckBox peakflow;
    CheckBox inhaler;
    TextView storage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_settings);



        findViewById(R.id.button).setOnClickListener(buttonListener1);
        peakflow = (CheckBox)findViewById(R.id.PeakFlow);
        inhaler = (CheckBox)findViewById(R.id.inhaler);
        peakflow.setEnabled(false);
        inhaler.setEnabled(false);
        storage = (TextView)findViewById(R.id.storage);
        storage.setText(this.calculateMem());
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
    private View.OnClickListener buttonListener1 = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            findBT();

        }
    };
    void findBT()
    {

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            Toast.makeText(getApplicationContext(), "Your device does not support Bluetooth, and is therefore incompatable.", Toast.LENGTH_LONG).show();
            onDestroy();
        }

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
            //Toast.makeText(getApplicationContext(), "Bluetooth Enabled", Toast.LENGTH_SHORT).show();

        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getName().contains("ASMA"))
                {
                    mmDevice = device;
                    Toast.makeText(getApplicationContext(), "Bluetooth Found", Toast.LENGTH_SHORT).show();
                    peakflow.setChecked(true);
                    break;

                }
                Toast.makeText(getApplicationContext(), "Bluetooth Device Not Found", Toast.LENGTH_SHORT).show();
                peakflow.setChecked(false);

            }
        }
    }

    public String calculateMem()
    {
        //Calculating Available
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long bytesAvailable = (long)stat.getBlockSize() * (long)stat.getAvailableBlocks();
        long megAvailable = bytesAvailable / (1024 * 1024);

        //Calculating Total
        long blockSize = stat.getBlockSize();
        long totalBlocks = stat.getBlockCount();
        long Total = (totalBlocks * blockSize) / (1024*1024);

        long megUsed = Total-megAvailable;

        return (megUsed + "/" + Total +"MB used");
    }
}
