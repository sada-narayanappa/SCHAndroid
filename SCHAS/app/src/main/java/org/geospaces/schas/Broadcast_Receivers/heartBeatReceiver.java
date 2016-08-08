package org.geospaces.schas.Broadcast_Receivers;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.geospaces.schas.utils.db;

import java.io.IOException;

public class heartBeatReceiver extends BroadcastReceiver {
    private Context cntx;
    private static Activity act;

    public heartBeatReceiver() {

    }

    @Override
    public void onReceive(Context context, Intent intent) {

       // Toast.makeText(context, "I'm running", Toast.LENGTH_SHORT).show();
        cntx = context;


        String msg = db.getHeartBeat(cntx);
        try {
            db.Write(msg + "\n");
            db.Upload(cntx, act);
        } catch (IOException e) {
            Log.e("ERROR", "Exception appending to or uploading file", e);
        }
        Log.d("Heartbeat", "Heartbeat occured");
    }
    public static void setAct(Activity active){
        act = active;
    }


}
