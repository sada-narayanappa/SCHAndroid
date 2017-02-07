package org.geospaces.schas.Services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

import org.geospaces.schas.R;
import org.geospaces.schas.StepCounterView;
import org.geospaces.schas.utils.db;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Erik on 1/11/2017.
 */

public class StepCounter extends Service{
    IBinder mBinder = new LocalBinder();
    Context mContext;
    private static Handler mUIThreadHandler = null;

    SensorManager mSensorManager;
    Sensor mStepCounter;
    SensorEventListener mStepListener;

    public static int currentNumberOfSteps;
    public static boolean viewerPageIsForeground = false;

    private static Timer timer = new Timer();


    public class LocalBinder extends Binder {
        public StepCounter getService() { return StepCounter.this; }
    }

    @Override
    public void onCreate(){
        mContext = getApplicationContext();

        Intent clickedOnIntent = new Intent(this, StepCounterView.class);
        PendingIntent clickedOnPendingIntent = PendingIntent.getActivity(this, 0, clickedOnIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new Notification.Builder(mContext)
                .setContentTitle("SCHAS Step Counter")
                //.setContentText("Location Polling Currently Enabled!")
                .setSmallIcon(R.drawable.ic_directions_walk_white_36dp)
                .setContentIntent(clickedOnPendingIntent)
                .build();

        startForeground(9, notification);

        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mStepCounter = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        mStepListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                currentNumberOfSteps = (int) event.values[0];

                if (viewerPageIsForeground) {
                    if (mUIThreadHandler == null) {
                        mUIThreadHandler = new Handler(Looper.getMainLooper());
                    }
                    mUIThreadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            StepCounterView.updateStepsCounter(currentNumberOfSteps);
                        }
                    });
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };

        if (mStepCounter != null){
            mSensorManager.registerListener(mStepListener, mStepCounter, SensorManager.SENSOR_DELAY_NORMAL);
        }
        else{
            Log.i("Step Counter", "step counter sensor not available");
        }

        //launches a recorder method for a heartbeat at the interval for inhaler cap update checks specified by the settings page
//            SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(mContext);
//            int timerInterval = SP.getInt("inhalerCapScan", 12);
        //int timerInterval = 1;
        //timer.schedule(new heartBeatRecord(), 0, (timerInterval * 3600000));
    }

    private class heartBeatRecord extends TimerTask {
        public void run(){
            String msg = db.getHeartBeat(mContext);
            try {
                db.Write(msg + "\n");
                if (LocationService.appIsRunning) {
                    db.Upload(mContext, null);
                }
            } catch (IOException e) {
                Log.e("ERROR", "Exception appending to or uploading file", e);
            }
            Log.d("Heartbeat", "Heartbeat occured");

//            SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(mContext);
//            int timerInterval = SP.getInt("inhalerCapScan", 12);
            int timerInterval = 1;
            timer.schedule(new heartBeatRecord(), 0, (timerInterval * 3600000));
        }
    }

    @Override
    public void onDestroy(){
        Log.i("StepCounter", "Step Counter Service stopped");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void startStepCounter(){
        mSensorManager.registerListener(mStepListener, mStepCounter, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void stopStepCounter() {
        mSensorManager.unregisterListener(mStepListener);
    }
}
