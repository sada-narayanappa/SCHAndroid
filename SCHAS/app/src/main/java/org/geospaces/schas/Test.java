package org.geospaces.schas;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import java.util.List;

import mymodule.app2.mymodule.app2.SysStrings;

public class Test extends Activity implements SensorEventListener {
    EditText tv1;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private SensorManager mSensorManager2;
    private Sensor mAmbientLight;
    private SensorManager mSensorManager3;
    private Sensor mPressure;
    private SensorManager mSensorManager4;
    private Sensor mTemp;
    private TelephonyManager telephonyManager;
    private ConnectivityManager connManager;
    private WifiManager wifiman;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        setContentView(R.layout.activity_test);
        tv1 = (EditText)findViewById(R.id.statusText);

        telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);

        connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        wifiman = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager2 = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAmbientLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mSensorManager3 = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mSensorManager4 = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mTemp = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);

        findViewById(R.id.button1).setOnClickListener(button1CB);
        findViewById(R.id.button2).setOnClickListener(button2CB);

        findViewById(R.id.buttonClear).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    tv1.setText("");
                }
            });
    }

    private View.OnClickListener button1CB = new View.OnClickListener() {
        @Override
        public void onClick(View v){
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            StringBuilder sb = new StringBuilder(256);


            sb.append(  SysStrings.getTheTime() + "\n" +
                        SysStrings.getTheGPS(locationManager) + "\n" +
                        SysStrings.getOrientation(getApplicationContext()) + "\n" +
                        SysStrings.getGravity(mSensorManager) + "\n" +
                        SysStrings.getLight(mSensorManager) + "\n" +
                        SysStrings.getPressure(mSensorManager) + "\n" +
                        SysStrings.getTemp(mSensorManager) + "\n" +
                        SysStrings.getDeviceID(Test.this) + "\n" +
                        SysStrings.getWIFI(connManager,wifiman) + "\n" +
                        SysStrings.getBluetoothDevices() + "\n" +
                        SysStrings.getBatteryStatus(getApplicationContext())


            );
            tv1.setText(sb.toString());
        }
    };
    private View.OnClickListener button2CB = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String list = "";
            List<Sensor> deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
            for (int i = 0; i < deviceSensors.size(); i++) {
                String name = deviceSensors.get(i).getName();
                if (name.startsWith("placeholder"))
                    continue;
                list += name + "\n";
            }
            tv1.setText(list);
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.welcome, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if ( !HandleMenu.onOptionsItemSelected(item, this)) {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }
    //The following 2 functions must be used to implement the SensorEventListener to access accelerometer data
    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy)
    {
        // Do something here if sensor accuracy changes.
    }

    //Check for type of sensor being used then update accordingly
    public final void onSensorChanged(SensorEvent event) {
        Sensor thisSensor = event.sensor;
        if (thisSensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            SysStrings.updateGravity(event);
        }
        else if (thisSensor.getType() == Sensor.TYPE_LIGHT){
            SysStrings.updateLight(event);
        }
        else if (thisSensor.getType() == Sensor.TYPE_PRESSURE){
            SysStrings.updatePressure(event);
        }
        else if (thisSensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE){
            SysStrings.updateTemp(event);
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager2.registerListener(this, mAmbientLight, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager3.registerListener(this, mPressure, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager4.registerListener(this, mPressure, SensorManager.SENSOR_DELAY_NORMAL);


    }
    @Override
    protected void onPause()
    {
        super.onPause();
        mSensorManager.unregisterListener(this);
        mSensorManager2.unregisterListener(this);
        mSensorManager3.unregisterListener(this);
        mSensorManager4.unregisterListener(this);


    }
}
