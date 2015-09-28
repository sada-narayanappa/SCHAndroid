package org.geospaces.schas.Broadcast_Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import org.geospaces.schas.Welcome;
import org.geospaces.schas.utils.db;

public class heartBeatReceiver extends BroadcastReceiver {
    public heartBeatReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
       // Toast.makeText(context, "I'm running", Toast.LENGTH_SHORT).show();
        Log.d("Heartbeat", "Heartbeat occured");
    }
}
