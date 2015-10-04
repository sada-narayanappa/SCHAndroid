package org.geospaces.schas.Broadcast_Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;
import android.widget.Toast;

import org.geospaces.schas.Welcome;
import org.geospaces.schas.utils.SCHASSettings;
import org.geospaces.schas.utils.db;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

public class heartBeatReceiver extends BroadcastReceiver {
    private Context cntx;
    private float batLevel;

    public heartBeatReceiver() {

    }

    @Override
    public void onReceive(Context context, Intent intent) {
       // Toast.makeText(context, "I'm running", Toast.LENGTH_SHORT).show();
        cntx = context;
        batLevel = getBatteryLevel();

        String msg = getHeartBeat();
        try {
            db.Write(msg + "\n");
        } catch (IOException e) {
            Log.e("ERROR", "Exception appending to log file", e);
        }
        Log.d("Heartbeat", "Heartbeat occured");
        updateStatus();
    }

    public float getBatteryLevel() {
        Intent batteryIntent = cntx.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        // Error checking that probably isn't needed but I added just in case.
        if(level == -1 || scale == -1) {
            return 50.0f;
        }

        return ((float)level / (float)scale) * 100.0f;
    }

    protected void updateStatus() {
        StringBuffer sb = new StringBuffer(256);
        File f;
        f = db.getFile(db.FILE_NAME);



        sb.append(f.getName() + " : size=" + f.length() + "\n");
        f = db.getFile(db.FILE_READY);
        sb.append(f.getName() + " : size=" + f.length() + "\n");

        if (SCHASSettings.host == null ) {
            SCHASSettings.Initialize(null);
        }
        sb.append("URL: " + SCHASSettings.host + "\n");

        String []ls = db.read(db.FILE_NAME).split("\n");

        sb.append("DATA:" + ls[ls.length-1] + " ...\n");
        sb.append("SETTINGS:" + SCHASSettings.getSettings() + " ...\n");
        sb.append("WIFI:" + db.isWIFIOn(cntx) + " ...\n");
    }

    public String getHeartBeat() {

        StringBuffer sb = new StringBuffer(512);
        long sessionNum = System.currentTimeMillis()/1000000 * 60;
        Calendar c = Calendar.getInstance();
        int seconds = c.get(Calendar.SECOND);


        StringBuffer append = sb.append(
                "measured_at="  + (seconds/1000)      + "," +
                        "system1="  + ("active")      + "," +
                        "battery_level=" + batLevel   + "," +
                        "session_num="  + sessionNum  + ""

        );

        return sb.toString();
    }
}
