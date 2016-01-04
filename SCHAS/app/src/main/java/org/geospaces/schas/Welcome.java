package org.geospaces.schas;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.geospaces.schas.Broadcast_Receivers.heartBeatReceiver;
import org.geospaces.schas.utils.SCHASApplication;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;



public class Welcome extends ActionBarActivity {

    static boolean firstTime = true;
    private PendingIntent pendingIntent;
    private AlarmManager manager;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        Intent alarmIntent = new Intent(this,heartBeatReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(this,0,alarmIntent,0);
        context = getApplicationContext();

        SCHASApplication.getInstance();
        //Creates a SCHAS directory on the external storage portion of the Device to keep data files
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            Log.d("SCHAS", "No SDCARD");
            String msg = "No External Storage";
            Toast.makeText(Welcome.this, msg, Toast.LENGTH_SHORT).show();

        } else {
            File directory = new File(Environment.getExternalStorageDirectory()+File.separator+"SCHAS");
            directory.mkdirs();
        }

        if ( firstTime ) {
            new Timer().schedule(new TimerTask() {
                                     @Override
                                     public void run() {
                                         Intent intent = new Intent(Welcome.this, UploadData.class);
                                         startActivity(intent);
                                         this.cancel();
                                     }
                                 }, 5000
            );


            startAlarm();

            firstTime = false;
        }
        try {
            PackageInfo pInfo = getApplicationContext().getPackageManager().getPackageInfo(getPackageName(), 0);
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a");
            String ID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

            SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(this);
            String stringUName = SP.getString("username", "NA");

            String version = pInfo.versionName + " Code:" + pInfo.versionCode + "\n"+
                    sdf.format( pInfo.lastUpdateTime) + "\nUser ID: " + ID + "\nUsername: " + stringUName;
            ((TextView)findViewById(R.id.version)).setText(version);
        } catch(Exception e){
            Log.e("Welcome", e.toString());
        }
    }

    public void startAlarm(){
        manager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);

        //Time in ms 60*60*1000 = 1 hour
        int hourHeartBeat = 3600000;
        int testHeartBeat = 5000;

        manager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), hourHeartBeat, pendingIntent);
    }
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
}
