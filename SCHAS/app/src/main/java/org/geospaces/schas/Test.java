package org.geospaces.schas;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

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



        final Button button = (Button) findViewById(R.id.button1);
        if ( button != null) {
            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                   /* Intent intent = new Intent(Test.this, Welcome.class);
                    startActivity(intent);
                    //finish();*/
                    LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    tv1.setText("The Time is:  \n" + SysStrings.getTheTime() + "\n-----------------\n" + "Your location is: \n" + SysStrings.getTheGPS(locationManager)
                                    + "\n-----------------\n" + "The Orientation is: " + SysStrings.getOrientation(getApplicationContext()) + "\n-----------------\n" +
                                    "The gravity is: \n" + SysStrings.getGravity(mSensorManager) + "\n-----------------\n" + SysStrings.getLight(mSensorManager) + "\n-----------------\n"
                                    + SysStrings.getPressure(mSensorManager) + "\n-----------------\n" + SysStrings.getTemp(mSensorManager) + "\n-----------------\n" + "The IMEI Number is: " + SysStrings.getIEMI(telephonyManager) + "\n-----------------\n"
                                    +  SysStrings.getWIFI(connManager,wifiman) + "\n-----------------\n"
                    );
                }
            });
        }

    final Button button2 = (Button) findViewById(R.id.button2);
        if ( button2 != null) {
            button2.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    String list = "";
                    List<Sensor> deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
                    for(int i =0;i<deviceSensors.size();i++) {
                        list +=deviceSensors.get(i).getName() + "\n\n";

                    }
                    tv1.setText(list);

                }
            });

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
