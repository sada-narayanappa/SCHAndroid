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
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

import mymodule.app2.mymodule.app2.schasStrings;

public class Test extends Activity implements SensorEventListener {
    TextView tv1;
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
        tv1 = (TextView) findViewById(R.id.statusText);

        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

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

        findViewById(R.id.readButton).setOnClickListener(button1CB);
        findViewById(R.id.serviceButton).setOnClickListener(button2CB);
        findViewById(R.id.uploadButton).setOnClickListener(webServiceCB);
        findViewById(R.id.clearButton).setOnClickListener(clearCB);
    }

    private View.OnClickListener clearCB = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String url = "http://10.0.0.223:8080/aura/webroot/loc.jsp";

            List <NameValuePair> nv = new ArrayList<NameValuePair>(2);
            String msg = GPSWakfulReciever.read(GPSWakfulReciever.FILE_NAME);

            nv.add(new BasicNameValuePair("api_key", "123"));
            nv.add(new BasicNameValuePair("text", msg));

            tv1.setText("Sending: " + url + "\n" + msg.substring(0, 256));

            PostToServer ps = new PostToServer(nv, tv1);
            ps.execute(url);
        }
    };

    private View.OnClickListener button1CB = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        StringBuilder sb = new StringBuilder(256);

        sb.append(schasStrings.getTheTime() + "\n" +
                    schasStrings.getTheGPS(locationManager) + "\n" +
                    schasStrings.getOrientation(getApplicationContext()) + "\n" +
                    schasStrings.getGravity(mSensorManager) + "\n" +
                    schasStrings.getLight(mSensorManager) + "\n" +
                    schasStrings.getPressure(mSensorManager) + "\n" +
                    schasStrings.getTemp(mSensorManager) + "\n" +
                    schasStrings.getDeviceID(Test.this) + "\n" +
                    schasStrings.getWIFI(connManager, wifiman) + "\n" +
                    schasStrings.getBluetoothDevices() + "\n" +
                    schasStrings.getBatteryStatus(getApplicationContext())

        );
        String list = "";
        List<Sensor> deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for (int i = 0; i < deviceSensors.size(); i++) {
            String name = deviceSensors.get(i).getName();
            if (name.startsWith("placeholder"))
                continue;
            list += name + "\n";
        }
        tv1.setText(sb.toString() + "\n" + list);
        }
    };
    //-------------------------------------------------------------------------
    // Testing bluetooth Stuff
    private final class UIHandler extends Handler {
        public static final int DISPLAY_UI_TOAST = 0;
        public static final int DISPLAY_UI_DIALOG = 1;

        public UIHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UIHandler.DISPLAY_UI_TOAST: {
                    Context context = getApplicationContext();
                    Toast t = Toast.makeText(context, (String) msg.obj, Toast.LENGTH_LONG);
                    t.show();
                }
                case UIHandler.DISPLAY_UI_DIALOG:
                    //TBD
                default:
                    break;
            }
        }
    }

    private View.OnClickListener button2CB = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (BlueToothHelper.threadRunnimg) {
                Toast.makeText(getApplicationContext(), "Running", Toast.LENGTH_LONG).show();
                return;
            }
            HandlerThread uiThread = new HandlerThread("UIHandler");
            uiThread.start();
            UIHandler uiHandler = new UIHandler(uiThread.getLooper());
            BlueToothHelper.start("RNBT", Test.this, uiHandler);
        }
    };
    //-------------------------------------------------------------------------

    private View.OnClickListener webServiceCB = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String str = GPSWakfulReciever.read(GPSWakfulReciever.FILE_NAME);
            tv1.setText(str);
/*
            String list = "";
            String url = "http://10.0.0.223:8080/aura/webroot/index.jsp?cmd=test&a=b";

            List<NameValuePair> nv = new ArrayList<NameValuePair>(2);
            nv.add(new BasicNameValuePair("test1", "A"));

            PostToServer ps = new PostToServer(nv, tv1);
            ps.execute(url);
*/
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
        if (!HandleMenu.onOptionsItemSelected(item, this)) {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    //The following 2 functions must be used to implement the SensorEventListener to access accelerometer data
    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    //Check for type of sensor being used then update accordingly
    public final void onSensorChanged(SensorEvent event) {
        Sensor thisSensor = event.sensor;
        if (thisSensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            schasStrings.updateGravity(event);
        } else if (thisSensor.getType() == Sensor.TYPE_LIGHT) {
            schasStrings.updateLight(event);
        } else if (thisSensor.getType() == Sensor.TYPE_PRESSURE) {
            schasStrings.updatePressure(event);
        } else if (thisSensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            schasStrings.updateTemp(event);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager2.registerListener(this, mAmbientLight, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager3.registerListener(this, mPressure, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager4.registerListener(this, mPressure, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        mSensorManager2.unregisterListener(this);
        mSensorManager3.unregisterListener(this);
        mSensorManager4.unregisterListener(this);
    }
}
