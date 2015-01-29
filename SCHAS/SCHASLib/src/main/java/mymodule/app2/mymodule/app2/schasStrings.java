package mymodule.app2.mymodule.app2;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.ArrayAdapter;

import java.util.Set;

public class schasStrings {

    private static float x = 5;
    private static float y;
    private static float z;
    private static float light;
    private static float pressure;
    private static float temp;

    private static android.text.format.DateFormat df = new android.text.format.DateFormat();

    public static String getTheTime() {
        String time = "Time: " + df.format("yyyy-MM-dd hh:mm:ss", new java.util.Date());
        return time;
    }

    //Requires an instance of LocationManger passed in from the Main activity used
    // LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    // Use above code to create location manager and pass in locationManager to the function for accurate results
    //Include the below in the AndroidManifest.xml File of the project to allow for acces of these features
    //<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    //<uses-permission android:name="android.permission. ACCESS_COARSE_LOCATION" />
    //<uses-permission android:name="android.permission.INTERNET" />
    public static String getTheGPS(LocationManager locationManager){
        String loc = "";

        //Checks for Coarse GPS location then Fine, if fine is available that reading will be taken, else Coarse will
        if(locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) != null) {
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) != null){
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                loc = "Fine: \n";
        }
            /*loc += "Lat: " + location.getLatitude() + "\nLon: " + location.getLongitude() +
                    "\nSpeed: " + location.getSpeed() + "\nAltitude: " + location.getAltitude() +
                    "\nBearing: " + location.getBearing() +
                    "\nAccuracy:" + location.getAccuracy();*/
            loc = "" + location.getLatitude() +", " + location.getLongitude() + ", " + location.getSpeed();

        }
        else{
            loc = "NA,NA,NA";
        }
        return loc;
    }
    public static String getOrientation(Context context){

        final int rotation = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getOrientation();
        switch (rotation) {
            case Surface.ROTATION_0:
                return "Orientation: portrait";
            case Surface.ROTATION_90:
                return "Orientation: landscape";
            case Surface.ROTATION_180:
                return "Orientation: reverse portrait";
            default:
                return "Orientation: reverse landscape";
        }
    }

    //        connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    //        wifiman = (WifiManager) getSystemService(Context.WIFI_SERVICE);
    // Include the above lines in your onCreate method and pass in (connManager,wifiman)

    public static String getWIFI(ConnectivityManager cm,WifiManager wifiManager){

        NetworkInfo mWifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (mWifi.isConnected()) {
            int linkSpeed = wifiManager.getConnectionInfo().getLinkSpeed();
            return "WIFI_Mbps: " + linkSpeed +"Mbps\n" +
                    "WIFI_strength: " + wifiManager.getConnectionInfo().getRssi() + "dBm";
        }
        else{return "wifi is not connected";}
    }


    //
    //
    public static String getIEMI(TelephonyManager tm){
        return "deviceId: " + tm.getDeviceId();
    }

    //import android.provider.Settings.Secure;


    public static String getDeviceID(Activity a) {
        String id = Settings.Secure.getString(a.getContentResolver(), Settings.Secure.ANDROID_ID);
        return "DeviceId: "  + id;
    }

    public static void updateGravity(SensorEvent event){
        x = event.values[0];
        y = event.values[1];
        z = event.values[2];


    }
    public static String getGravity(SensorManager sm){
        if(sm.getDefaultSensor(Sensor.TYPE_GRAVITY) != null){
            return "gravity_x: " + x + "\ngravity_y: " + y + "\ngravity_z: " + z;
        }
        else{return "Phone has no gravity sensor";}
    }

    public static void updateLight(SensorEvent event){
        light = event.values[0];
    }
    public static String getLight(SensorManager sm){
        if (sm.getDefaultSensor(Sensor.TYPE_LIGHT) != null){
            return "light(lux): " + light;

        }
        else{return ""+light;}


    }

    public static void updatePressure(SensorEvent event){
        pressure = event.values[0];
    }
    public static String getPressure(SensorManager sm){

        if(sm.getDefaultSensor(Sensor.TYPE_PRESSURE) != null) {
            return "pressure(hPa): " + pressure;
        }
        else{return "pressure(hPa): NA";}

    }

    public static void updateTemp(SensorEvent event){
        temp = event.values[0];
    }
    public static String getTemp(SensorManager sm){
        if(sm.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null) {
            return "Temp_centigrade: " + temp;
        }
        else{return "Temp_centigrade: NA";}

    }

    public static String getBluetoothDevices() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        String BTlist = "Bluetooth Devices: \n";
        if ( mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()){
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    if (device.getName() != null) {
                        BTlist += device.getName() + "  " + device.getAddress() + "\n";
                    }
                }
            }
            BTlist += "End of Bluetooth Devices";
        }
        else{BTlist = "Bluetooth not connected";}

        return BTlist;
    }

    public static String getBatteryStatus(Context context){
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float batteryPct = 100 * (level / (float)scale);

        return "Battery level: " + batteryPct + "%";

    }
}
