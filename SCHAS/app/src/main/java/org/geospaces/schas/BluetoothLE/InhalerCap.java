package org.geospaces.schas.BluetoothLE;

import android.annotation.TargetApi;
import android.bluetooth.le.ScanRecord;
import android.os.Build;
import android.os.Parcel;
import android.os.ParcelUuid;

/**
 * Created by Michael on 5/12/2015.
 */
public class InhalerCap {

    public static final  ParcelUuid genericAccessService = ParcelUuid.fromString("00001800-0000-1000-8000-00805f9b34fb");
    //Bluetooth UUID that defines the inhaler cap service
    public static final ParcelUuid ButtonPressedService = ParcelUuid.fromString("00431c4a-a7a4-428b-a96d-d92d43c8c7ca");

    //Short form UUID that defines the inhaler service
    private static final int UUID_SERVICE_THERMOMETER = 0x1809;

    private String mName;
    private float mButtonPresses;
    //Device metadata
    private int mSignal;
    private String mAddress;

    /* Builder for Lollipop+ */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public InhalerCap(ScanRecord record, String deviceAddress, int rssi) {
        mSignal = rssi;
        mAddress = deviceAddress;

        mName = record.getDeviceName();

        byte[] data = record.getServiceData(genericAccessService);
        if (data != null) {
            mButtonPresses = parseData(data);
        } else {
            mButtonPresses = 0f;
        }
    }

    private float parseData(byte[] serviceData) {
        /*
         * Temperature data is two bytes, and precision is 0.5degC.
         * LSB contains temperature whole number
         * MSB contains a bit flag noting if fractional part exists
         */
        float temp = (serviceData[0] & 0xFF);
        if ((serviceData[1] & 0x80) != 0) {
            temp += 0.5f;
        }

        return temp;
    }
    public String getName() {
        return mName;
    }

    public int getSignal() {
        return mSignal;
    }

    public float getCurrentTemp() {
        return mButtonPresses;
    }

    public String getAddress() {
        return mAddress;
    }

    @Override
    public String toString() {
        return String.format("%s (%ddBm): %.1fC", mName, mSignal, mButtonPresses);
    }
}
