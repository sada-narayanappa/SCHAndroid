package org.geospaces.schas;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.geospaces.schas.utils.SCHASApplication;
import org.geospaces.schas.utils.db;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;



public class Welcome extends ActionBarActivity {

    private boolean firstTime = true;
    private PendingIntent pendingIntent;
    private AlarmManager manager;
    private Context context;
    boolean waitForEmail = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Creates layout via XML file 'activity_welcome'
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        context = this;

        String filename = "DEBUG_STACKTRACE.txt";

        File file = db.getFile(filename);

        if(firstTime && file.exists())
        {
            Uri uri = Uri.fromFile(file);

            //send to email here
            Intent mailIntent = new Intent();
            mailIntent.setAction(Intent.ACTION_SENDTO);
            mailIntent.setData(Uri.parse("mailto:"));
            mailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {"schas.debug@gmail.com"});
            mailIntent.putExtra(Intent.EXTRA_SUBJECT, "Application has Crashed!");
            mailIntent.putExtra(Intent.EXTRA_TEXT, "The SCHAS application has crashed.  Attached to this email is" +
                    " a file which will be sent to our developers to work to solve this issue.  Please" +
                    " consider sending this email to help us solve any and all issues.\n\n" +
                    "Thank you for your time and help,\n" +
                    "\t- SCHAS app developers");
            mailIntent.putExtra(Intent.EXTRA_STREAM, uri);

            try
            {
                startActivityForResult(Intent.createChooser(mailIntent, "Your SCHAS Application has recently crashed.\nPlease choose a mail client to send an error report."), 1);
            }
            catch (android.content.ActivityNotFoundException exception)
            {
                Toast.makeText(this, "No email clients installed.", Toast.LENGTH_SHORT).show();
            }
            waitForEmail = true;
        }


//        //Creates an intent that will launch the heartBeatReceiver Class
//        Intent alarmIntent = new Intent(getApplicationContext(),heartBeatReceiver.class);
//        pendingIntent = PendingIntent.getBroadcast(this,0,alarmIntent,0);
//        context = getApplicationContext();

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

        //If this is the first time the app is launched it will set up/wait 5 seconds and then move from splash screen to UploadData activity
//        if (!waitForEmail) {
//            new Timer().schedule(new TimerTask() {
//                 @Override
//                 public void run() {
//                     Intent intent = new Intent(Welcome.this, UploadData.class);
//                     startActivity(intent);
//                     this.cancel();
//                 }
//             }, 5000
//            );
//
//            //startAlarm();
//
//            firstTime = false;
//        }

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

//        if (db.isNetworkAvailable(context)){
//            new GetCurrentVersionCode().execute(context);
//        }
//        else{
//            Toast.makeText(context, "Could not download current version code\nno network connection available", Toast.LENGTH_SHORT).show();
//
//        }

        if (!waitForEmail) {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    Intent intent = new Intent(Welcome.this, UploadData.class);
                    startActivity(intent);
                    this.cancel();
                }
            }, 5000 );
            //startAlarm();

            firstTime = false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == 1) {
            // Make sure the request was successful
            String filename = "DEBUG_STACKTRACE.txt";
            File file = db.getFile(filename);
            if(file.exists())
                file.delete();

            new Timer().schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    Intent intent = new Intent(Welcome.this, UploadData.class);
                    startActivity(intent);
                    this.cancel();
                }
            }, 5000);
        }
    }

    //Set's alarm manager to trigger each hour for a 'heartbeat' of the device
//    public void startAlarm(){
//        manager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
//
//        Calendar calendar = Calendar.getInstance();
//
//        //Time in ms 60*60*1000 = 1 hour
//        int hourHeartBeat = 3600000;
//        int testHeartBeat = 5000;
//
//        manager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_HOUR, pendingIntent);
//    }

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

    public void goToHomePage(View view) {
        Intent homeIntent = new Intent(this, UploadData.class);
        startActivity(homeIntent);
    }

    private class GetCurrentVersionCode extends AsyncTask<Context, String, String> {
        private boolean shouldStartIntentTimer = true;

        @Override
        protected String doInBackground(Context... params) {
            String jsonStringFromQuery = "";
            String queryUrl = "http://www.smartconnectedhealth.org/aura/webroot/db.jsp?q=SELECT%20version%20FROM%20loc%20WHERE%20record_type=%27active%27%20AND%20version%20LIKE%20%27%25-%25%27%20ORDER%20BY%20version%20DESC%20LIMIT%201";
            HttpURLConnection urlConnection = null;

            try {
                URL url = new URL(queryUrl);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder builder = new StringBuilder(in.available());

                String currentLine = reader.readLine();
                while(currentLine != null){
                    builder.append(currentLine);
                    currentLine = reader.readLine();
                }
                jsonStringFromQuery = builder.toString();
            }
            catch (java.net.MalformedURLException exception){
                Toast.makeText(params[0], "The URL for getting data was malformed", Toast.LENGTH_SHORT).show();
            }
            catch (IOException e){
                Toast.makeText(params[0], "There was an exception opening the url connection", Toast.LENGTH_SHORT).show();
            }
            finally {
                if (urlConnection != null){
                    urlConnection.disconnect();
                }
            }

            return jsonStringFromQuery;
        }

        @Override
        protected void onPostExecute(String jsonString) {
            int currentAppVersion = 0;
            String newestAppVersionCode = "";
            try {
                currentAppVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            try {
                String[] splitJson = jsonString.split("=");
                JSONObject jsonRootObject = new JSONObject(splitJson[1]);

                JSONArray rowsArray = jsonRootObject.optJSONArray("rows");
                JSONArray nextJSONArray = rowsArray.getJSONArray(0);

                String versionComboString = nextJSONArray.getString(0);
                String splitVersions[] = versionComboString.split("-");
                newestAppVersionCode = splitVersions[1];
            }
            catch(JSONException e){
                e.printStackTrace();
            }

            Log.i("currentAppVersion", String.valueOf(currentAppVersion));
            Log.i("newestAppVersion", newestAppVersionCode);

            if ((currentAppVersion != 0) && (!newestAppVersionCode.equals("")) && (!newestAppVersionCode.contains("ALPHA"))){
                if (currentAppVersion < Integer.valueOf(newestAppVersionCode)){
                    final String appPackageName = getPackageName();
                    shouldStartIntentTimer = false;
                    try{
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                    }
                    catch (android.content.ActivityNotFoundException exception) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                    }
                }
            }
            if (shouldStartIntentTimer){
                if (!waitForEmail) {
                    new Timer().schedule(new TimerTask() {
                         @Override
                         public void run() {
                             Intent intent = new Intent(Welcome.this, UploadData.class);
                             startActivity(intent);
                             this.cancel();
                         }
                    }, 5000 );
                    //startAlarm();

                    firstTime = false;
                }
            }
        }
    }
}