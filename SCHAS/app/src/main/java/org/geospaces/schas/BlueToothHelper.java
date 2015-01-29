package org.geospaces.schas;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class BlueToothHelper {

    /**
     * Get all Blue tooth devices Connected -
     * This is identical to the function in schasStrings.getBluetoothDevices
     * @return
     */
    public static String getBluetoothDevices() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        String BTlist = "Bluetooth Devices: \n";
        if ( adapter != null && adapter.isEnabled()){
            Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    if (device.getName() != null) {
                        BTlist += device.getName() + "  " + device.getAddress() + "\n";
                    }
                }
            }
            BTlist += "End of Bluetooth Devices";
        }
        else{
            BTlist = "Bluetooth not connected";
        }
        return BTlist;
    }

    /**
     * Turn on Bluetooth if available;
     * @return
     */
    public static boolean on(Activity a) {

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            Toast.makeText(a.getApplicationContext(), "Bluetooth Not available",
                                                                Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!adapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            a.startActivityForResult(enableBluetooth, 0);
        }
        return adapter.isEnabled();
    }

    /**
     * Parameter:
     *      "RNBT"  for Inhaler
     *      "ASMA"  for Peakflow
     */
    public static BluetoothDevice findBT(String name) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        if ( adapter == null) {
            return null;
        }

        Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().contains(name)) {
                    return device;
                }
           }
        }
        return null;
    }


    public static Boolean  threadRunnimg  = false;
    public static Thread   thread  = null;

    public static void start(String name, final Activity a, final Handler h) {

        if (threadRunnimg ) {
            Toast("Thread running: " + name, h);
            return;
        }
        BluetoothDevice device = findBT(name);
        if (device == null) {
            if ( a != null)
                Toast("Could not find device: " + name, h);
            return;
        }

        BluetoothSocket socket  = null;
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        try {
            try {
                socket = device.createRfcommSocketToServiceRecord(uuid);
                socket.connect();
            } catch (Exception e1) {
                socket = null;
            }
            if (socket == null) {
                try {
                    socket = (BluetoothSocket) device.getClass().getMethod("createRfcommSocket",
                                                    new Class[]{int.class}).invoke(device, 1);
                    socket.connect();
                } catch (Exception f) {
                    Toast("Exception : " + f, h);
                    socket = null;
                }
            }
            if ( socket == null ) {
                Toast( "Socket is Null", h);
                return;
            }
            final InputStream inps1 = socket.getInputStream();


            thread = new Thread(new Runnable() {
                public void run() {
                    threadRunnimg = true;
                    while (!Thread.currentThread().isInterrupted() && threadRunnimg ) {
                        try {
                            Toast("Starting ...", h);
                            String data = "" + inps1.read();
                            Toast("Data: " + data, h);
                        }
                        catch (Exception e) {
                            Toast("In Read Thread : " + e, h);
                            threadRunnimg = false;
                        }
                    }
                    threadRunnimg = false;
                }
            });
            thread.start();
        } catch (Exception e) {
            Toast("Exception : " + e, h);
        }
    }

    public static void Toast(Object o, Handler h) {
        Message msg = h.obtainMessage(0);
        msg.obj = ""+ o;
        h.sendMessage(msg);
    }


}
