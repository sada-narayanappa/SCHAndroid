package org.geospaces.schas;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import mymodule.app2.mymodule.app2.SysStrings;




public class Test extends Activity implements SensorEventListener {
    EditText tv1;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private SensorManager mSensorManager2;
    private Sensor mAmbientLight;

    static float x;
    static float y;
    static float z;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        tv1 = (EditText)findViewById(R.id.statusText);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager2 = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAmbientLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);



        final Button button = (Button) findViewById(R.id.button1);
        if ( button != null) {
            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                   /* Intent intent = new Intent(Test.this, Welcome.class);
                    startActivity(intent);
                    //finish();*/
                    LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    tv1.setText("The Time is:  \n" + SysStrings.getTheTime() + "\n-----------------\n"  + "Your location is: \n" + SysStrings.getTheGPS(locationManager)
                    + "\n-----------------\n"  + "The Orientation is: " + SysStrings.getOrientation(getApplicationContext()) + "\n-----------------\n"  +
                    "The gravity is: \n" + SysStrings.getGravity() + "\n-----------------\n" + "The light is: \n" + SysStrings.getLight() + "\n-----------------\n"
                    );                }
            });
        }    }




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
    public final void onSensorChanged(SensorEvent event) {
        Sensor thisSensor = event.sensor;
        if (thisSensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            SysStrings.updateGravity(event);
        }
        else if (thisSensor.getType() == Sensor.TYPE_LIGHT){
            SysStrings.updateLight(event);
        }
    }
    @Override
    protected void onResume()
    {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager2.registerListener(this, mAmbientLight, SensorManager.SENSOR_DELAY_NORMAL);

    }
    @Override
    protected void onPause()
    {
        super.onPause();
        mSensorManager.unregisterListener(this);
        mSensorManager2.unregisterListener(this);

    }
}
