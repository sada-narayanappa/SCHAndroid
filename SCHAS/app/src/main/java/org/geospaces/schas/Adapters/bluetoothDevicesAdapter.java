package org.geospaces.schas.Adapters;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.geospaces.schas.R;

import java.util.ArrayList;

public class bluetoothDevicesAdapter  extends ArrayAdapter<BluetoothDevice> {

    public bluetoothDevicesAdapter(Context context, ArrayList<BluetoothDevice> device) {
        super(context, 0, device);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        BluetoothDevice device = getItem(position);

        // Checks if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.bt_device_list_item, parent, false);
        }

        // Lookup view bluetooth device name
        TextView btDeviceText = (TextView) convertView.findViewById(R.id.btDeviceName);

        // Populate the data into the template view using the data object
        btDeviceText.setText(device.getName());

        // Return the completed view to render on screen
        return convertView;
    }
}
