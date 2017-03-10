package org.geospaces.schas;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.geospaces.schas.Services.StepCounter;

public class StepCounterView extends AppCompatActivity {
    public static TextView stepCounterText;
    public Button stopButton;
    public Button startButton;

    public Context mContext;
    private ShareActionProvider mActionProvider;

    StepCounter mService;
    boolean mBound;

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            StepCounter.LocalBinder binder = (StepCounter.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_step_counter_view);
        mContext = this;

        stepCounterText = (TextView) findViewById(R.id.stepsCounterText);
        stopButton = (Button) findViewById(R.id.stopButton);
        startButton = (Button) findViewById(R.id.startButton);

        stopButton.setOnClickListener(stopButtonListener);
        startButton.setOnClickListener(startButtonListener);

        stepCounterText.setText(StepCounter.currentNumberOfSteps + "\nSteps Taken In Last Hour");
    }

    @Override
    protected void onResume(){
        StepCounter.viewerPageIsForeground = true;
        super.onResume();
    }

    @Override
    protected void onPause(){
        StepCounter.viewerPageIsForeground = false;
        super.onPause();
    }

    private View.OnClickListener stopButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent stopStepCounterIntent = new Intent(mContext, StepCounter.class);
            stopService(stopStepCounterIntent);
        }
    };

    private View.OnClickListener startButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent startStepCounterIntent = new Intent(mContext, StepCounter.class);
            startService(startStepCounterIntent);
        }
    };

    public static void updateStepsCounter(int steps){
        stepCounterText.setText(steps + "\nSteps Taken");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.welcome, menu);

        MenuItem shareItem = menu.findItem(R.menu.welcome);

        // To retrieve the Action Provider
        mActionProvider = (ShareActionProvider)
                MenuItemCompat.getActionProvider(shareItem);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!HandleMenu.onOptionsItemSelected(item, this)) {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }
}
