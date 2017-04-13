package org.geospaces.schas.Services;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import org.geospaces.schas.Broadcast_Receivers.MidnightAlarmReceiver;
import org.geospaces.schas.R;
import org.geospaces.schas.StepCounterView;
import org.geospaces.schas.ViewPatientInhalerData;
import org.geospaces.schas.utils.db;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Erik on 1/11/2017.
 */

public class StepCounter extends Service
                         implements SharedPreferences.OnSharedPreferenceChangeListener {
    IBinder mBinder = new LocalBinder();
    Context mContext;
    private static Handler mUIThreadHandler = null;

    SensorManager mSensorManager;
    Sensor mStepCounter;
    SensorEventListener mStepListener;

    AlarmManager alarmManager;

    SharedPreferences SP;
    SharedPreferences.OnSharedPreferenceChangeListener spListener;

    private static Timer timer;
    int timerInterval;

    public Date stepIncrementStartTime = null;
    public Date stepIncrementEndTime = null;
    public int numberOfStepsAtLastUpload = 0;
    public static int currentNumberOfSteps;
    public static boolean viewerPageIsForeground = false;

    //subtract this one from the total current number to get number for last day
    public static int previousStepAmountNotLastDay = 0;

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (Integer.valueOf(sharedPreferences.getString("inhalerCapScan", "1")) != timerInterval){
            timerInterval = Integer.valueOf(sharedPreferences.getString("inhalerCapScan", "1"));
            Log.d("inhalerCapConfig", "new timerInterval = " + timerInterval);
            launchNewTimer();
        }
    }

    public class LocalBinder extends Binder {
        public StepCounter getService() { return StepCounter.this; }
    }

    @Override
    public void onCreate(){
        mContext = getApplicationContext();

        Intent clickedOnIntent = new Intent(this, ViewPatientInhalerData.class);
        PendingIntent clickedOnPendingIntent = PendingIntent.getActivity(this, 0, clickedOnIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new Notification.Builder(mContext)
                .setContentTitle("SCHAS Step Counter")
                //.setContentText("Location Polling Currently Enabled!")
                .setSmallIcon(R.drawable.ic_directions_walk_white_36dp)
                .setContentIntent(clickedOnPendingIntent)
                .setGroup("schasGroup")
                .build();

        startForeground(9, notification);

        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mStepCounter = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        mStepListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                currentNumberOfSteps = (int) event.values[0];
                final int stepsForTheDay = currentNumberOfSteps - previousStepAmountNotLastDay;

                if (viewerPageIsForeground) {
                    if (mUIThreadHandler == null) {
                        mUIThreadHandler = new Handler(Looper.getMainLooper());
                    }
                    mUIThreadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            StepCounterView.updateStepsCounter(stepsForTheDay);
                        }
                    });
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };

        if (mStepCounter != null){
            startStepCounter();
        }
        else{
            Log.i("Step Counter", "step counter sensor not available");
        }

        //launches a recorder method for a heartbeat at the interval for inhaler cap update checks specified by the settings page
        SP = PreferenceManager.getDefaultSharedPreferences(mContext);
        timerInterval = Integer.valueOf(SP.getString("inhalerCapScan", "1"));
        Log.d("inhalerCapConfig", "new timerInterval = " + timerInterval);
        timer = new Timer();
        timer.scheduleAtFixedRate(new StepCounter.heartBeatRecord(), 0, (timerInterval * 3600000));
        SP.registerOnSharedPreferenceChangeListener(this);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 0);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(mContext, MidnightAlarmReceiver.class).setAction("TRIGGER_MIDNIGHT_ALARM"), PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
    }

    private class heartBeatRecord extends TimerTask {
        public void run(){
            //check for first heartbeat since starting
            if (stepIncrementStartTime == null) {
                stepIncrementStartTime = new Date();
            }
            //if not first heartbeat, find number of steps since last
            else {
                stepIncrementEndTime = new Date();
                int numberOfStepsToReport = currentNumberOfSteps - numberOfStepsAtLastUpload;

                String stepsMsg = db.getIncrementStepCount(stepIncrementStartTime, stepIncrementEndTime, numberOfStepsToReport);
                try {
                    db.Write(stepsMsg);
                } catch (IOException e) {
                    Log.e("ERROR", "Exception appending to or uploading file", e);
                }

                numberOfStepsAtLastUpload = currentNumberOfSteps;
                stepIncrementStartTime = stepIncrementEndTime;
            }

            String msg = db.getHeartBeat(mContext);
            try {
                db.Write(msg + "\n");
                if ((db.canUploadData(mContext) != null) && (db.isNetworkAvailable(mContext))){
                    db.Upload(mContext, null);
                }
            } catch (IOException e) {
                Log.e("ERROR", "Exception appending to or uploading file", e);
            }


            Log.d("Heartbeat", "Heartbeat occured");
        }
    }

    private void launchNewTimer(){
        timer.cancel();
        timer = new Timer();
        timer.scheduleAtFixedRate(new StepCounter.heartBeatRecord(), 0, (timerInterval * 3600000));
    }

    public static void ResetStepCounterForNextDay(){
        previousStepAmountNotLastDay = currentNumberOfSteps;
        final int stepsForTheDay = currentNumberOfSteps - previousStepAmountNotLastDay;

        if (viewerPageIsForeground) {
            if (mUIThreadHandler == null) {
                mUIThreadHandler = new Handler(Looper.getMainLooper());
            }
            mUIThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    StepCounterView.updateStepsCounter(stepsForTheDay);
                }
            });
        }
    }

    @Override
    public void onDestroy(){
        Log.i("StepCounter", "Step Counter Service stopped");
        SP.unregisterOnSharedPreferenceChangeListener(this);
        stopStepCounter();
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
        timer.cancel();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(mContext, MidnightAlarmReceiver.class).setAction("TRIGGER_MIDNIGHT_ALARM"), PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(pendingIntent);
    }
}
