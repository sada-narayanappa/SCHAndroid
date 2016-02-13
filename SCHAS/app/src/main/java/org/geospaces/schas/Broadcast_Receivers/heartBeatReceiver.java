package org.geospaces.schas.Broadcast_Receivers;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;
import android.widget.Toast;

import org.geospaces.schas.UploadData;
import org.geospaces.schas.Welcome;
import org.geospaces.schas.utils.SCHASSettings;
import org.geospaces.schas.utils.db;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

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
        } catch (IOException e) {
            Log.e("ERROR", "Exception appending to log file", e);
        }
        Log.d("Heartbeat", "Heartbeat occured");
        db.Upload(cntx,act);

    }
    public static void setAct(Activity active){
        act = active;
    }


}
