package org.geospaces.schas.Broadcast_Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.geospaces.schas.Services.StepCounter;

/**
 * Created by Erik on 3/7/2017.
 */

public class MidnightAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        StepCounter.ResetStepCounterForNextDay();
    }
}
