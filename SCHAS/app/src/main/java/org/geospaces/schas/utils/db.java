package org.geospaces.schas.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.format.Formatter;
import android.util.Log;
import android.util.Pair;

import com.google.android.gms.maps.model.LatLng;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.geospaces.schas.AsyncTasks.PostToServer;
import org.geospaces.schas.Fragments.GoogleMaps;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.Provider;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class db {
    private static float batLevel;
    public final static String DIRECTORY = "/SCHAS";
    public final static String FILE_NAME = "LOC.txt";
    public final static String FILE_READY = "LOC_ready.txt";
    public final static int FILE_SIZE = 8 * 1024;


    public static Location lastLocation;
    public final static String FILE_SETTINGS = "Settings.txt";


    public static String read(String fileName) {
        File file = getFile(fileName);

        if (!file.exists()) {
            return "";
        }
        String str = "ERROR reading File:  " + fileName;
        char[] bytes = new char[Math.min(FILE_SIZE, (int) file.length())];
        try {
            FileReader in = new FileReader(file);
            in.read(bytes);
            str = new String(bytes);
            in.close();
        } catch (Exception e) {
            Log.e("ERR", "" + e);
        }
        return str;
    }

    public static void Write(String msg) throws IOException {
        File file = db.getFile(db.FILE_NAME);
        BufferedWriter out = new BufferedWriter(new FileWriter(file.getAbsolutePath(), file.exists()));

        if (msg.endsWith("\n"))
            out.write(msg);
        else
            out.write(msg + "\n");

        out.close();
    }

    public static String getUploadableText(Context context) throws Exception {
        File file = getFile(FILE_READY);
        if (!file.exists()) {
            boolean b = db.rename(false);
            if (!b) {
                throw new Exception("File not found: " + FILE_READY);
            }
        }
        String str = db.read(FILE_READY);
        return str;
    }

    public static File getFile(String fileName) {
        File externalMem2 = Environment.getExternalStorageDirectory();
        File directory2 = new File(externalMem2.getAbsolutePath() + DIRECTORY);
        directory2.mkdirs();
        fileName = (fileName == null) ? FILE_READY : fileName;
        File file = new File(directory2, fileName);

        return file;
    }

    public static String getAttack(String severity) {
        if (lastLocation == null) {
            return "";
        }
        StringBuffer sb = new StringBuffer(512);
        long sessionNum = System.currentTimeMillis() / 1000000 * 60;

        StringBuffer append = sb.append(
                "measured_at=" + (lastLocation.getTime() / 1000) + "," +
                        "lat=" + lastLocation.getLatitude() + "," +
                        "lon=" + lastLocation.getLongitude() + "," +
                        "alt=" + lastLocation.getAltitude() + "," +
                        "speed=" + lastLocation.getSpeed() + "," +
                        "bearing=" + lastLocation.getBearing() + "," +
                        "accuracy=" + lastLocation.getAccuracy() + "," +
                        "record_type=" + severity + "," +
                        "session_num=" + sessionNum + "" +
                        ""
        );

        return sb.toString();
    }

    public static String getMedicine(String medicineUsed) {
        if (lastLocation == null) {
            return "";
        }
        StringBuffer sb = new StringBuffer(512);
        long sessionNum = System.currentTimeMillis() / 1000000 * 60;

        StringBuffer append = sb.append(
                "measured_at=" + (lastLocation.getTime() / 1000) + "," +
                        "lat=" + lastLocation.getLatitude() + "," +
                        "lon=" + lastLocation.getLongitude() + "," +
                        "alt=" + lastLocation.getAltitude() + "," +
                        "speed=" + lastLocation.getSpeed() + "," +
                        "bearing=" + lastLocation.getBearing() + "," +
                        "accuracy=" + lastLocation.getAccuracy() + "," +
                        "medicine_Used=" + medicineUsed + "," +
                        "session_num=" + sessionNum + "" +
                        ""
        );

        return sb.toString();
    }

    public static String getPeakFlow(String pef, String fev) {
        if (lastLocation == null) {
            return "";
        }
        StringBuffer sb = new StringBuffer(512);
        long sessionNum = System.currentTimeMillis() / 1000000 * 60;

        StringBuffer append = sb.append(
                "measured_at=" + (lastLocation.getTime() / 1000) + "," +
                        "lat=" + lastLocation.getLatitude() + "," +
                        "lon=" + lastLocation.getLongitude() + "," +
                        "alt=" + lastLocation.getAltitude() + "," +
                        "speed=" + lastLocation.getSpeed() + "," +
                        "bearing=" + lastLocation.getBearing() + "," +
                        "accuracy=" + lastLocation.getAccuracy() + "," +
                        "PEF=" + pef + "," +
                        "FEV=" + fev + "," +
                        "session_num=" + sessionNum + "" +
                        ""
        );

        String writeString = sb.toString();

        return writeString;
    }

    public static String getHeartBeat(Context cntx) {

        batLevel = getBatteryLevel(cntx);

        StringBuffer sb = new StringBuffer(512);
        long sessionNum = System.currentTimeMillis() / 1000000 * 60;
        long milliseconds = System.currentTimeMillis();
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(cntx);
        String stringUName = SP.getString("username", "NA");
        PackageInfo pInfo = null;
        String versionStuff = null;

        try {
            pInfo = cntx.getPackageManager().getPackageInfo(cntx.getPackageName(), 0);
            versionStuff = "version=" + pInfo.versionName + "," + pInfo.versionCode;
        }
        catch(Exception e) {
            Log.i("package", "could not get package info");
        }

        StringBuffer append = sb.append(
                "measured_at=" + (milliseconds/1000) + "," +
                        "record_type=" + ("active") + "," +
                        "battery_level=" + batLevel + "," +
                        "session_num=" + sessionNum + "," +
                        versionStuff + "," +
                        "user=" + stringUName + "\n"
        );

        return sb.toString();
    }

    public static float getBatteryLevel(Context cntx) {
        Intent batteryIntent = cntx.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        // Error checking that probably isn't needed but I added just in case.
        if (level == -1 || scale == -1) {
            return 50.0f;
        }

        return ((float) level / (float) scale) * 100.0f;
    }

    public static String getInhalerData(int buttonPresses) {

        StringBuffer sb = new StringBuffer(512);
        long sessionNum = System.currentTimeMillis() / 1000000 * 60;

        StringBuffer append = sb.append(
                "inhaler_count=" + buttonPresses
        );

        return sb.toString();
    }

    public static boolean fileReady() {
        File to = getFile(FILE_READY);
        return to.exists();
    }

    public synchronized static boolean rename(boolean force) {
        File from = getFile(FILE_NAME);
        File to = getFile(FILE_READY);

        if (!from.exists()) {
            return false;
        }
        if (to.exists() && !force) {
            return false;
        }
        from.renameTo(to);
        return true;
    }

    public static void delete() {
        File externalMem2 = Environment.getExternalStorageDirectory();
        File directory2 = new File(externalMem2.getAbsolutePath() + DIRECTORY);
        directory2.mkdirs();
        File to = new File(directory2, FILE_READY);
        to.delete();
        //File from = new File(directory2, FILE_NAME);
        //from.delete();
    }

    public static String isWIFIOn(Context ctx) {
        ConnectivityManager connManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        WifiManager wifiman = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        HashMap m = new HashMap();

        if (mWifi.isConnected()) {
            int linkSpeed = wifiman.getConnectionInfo().getLinkSpeed();
            int ip = wifiman.getConnectionInfo().getIpAddress();
            String ips = Formatter.formatIpAddress(ip);
            String str = "SPEED: " + linkSpeed + "Mbps, STRENGTH: " + wifiman.getConnectionInfo().getRssi() + "dBm";
            return str;
        }
        return null;
    }

    public static synchronized String Upload(Context ctx, Activity act) {
        String str;
        if (null == (str = db.isWIFIOn(ctx))) {
            return "NO Wireless Connection! Please check back";
        }
        str = db.Post(act, ctx, "/aura/webroot/loc.jsp");
        if (null != str) {
            return str;
        }
        Log.w("DB:post:", " Post succeeded");
        return null;
    }

    private static PostToServer POST_TO_SERVER = null;

    private static synchronized String Post(Activity act, Context context, String service) {

        if (POST_TO_SERVER != null && !POST_TO_SERVER.COMPLETED) {   // Avoid race condition
            return "Message: One upload in progress, please wait ...";
        }

        String host = SCHASSettings.host;

        if (host == null || null == isWIFIOn(context) || host.equals("null")) {
            SCHASSettings.Initialize();
            return "Warning: Cannot find host: ";
        }
        db.rename(false);
        if (!db.fileReady()) {
            return "Message: " + SCHASSettings.host + " No files to upload!!";
        }

        String url = "http://" + host + service;
        List<NameValuePair> nv = new ArrayList<NameValuePair>(2);
        String msg = "";

        try {
            msg = getUploadableText(context);
        } catch (Exception e) {
            return "ERROR: exception while reading input file " + e;
        }
        String ID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        nv.add(new BasicNameValuePair("api_key", ID));
        nv.add(new BasicNameValuePair("mobile_id", ID));
        nv.add(new BasicNameValuePair("text", msg));

        String ns = "Sending: " + url + "\n" + msg.substring(0, Math.min(msg.length() - 1, 256));
        Log.i("", ns);

        POST_TO_SERVER = null;
        POST_TO_SERVER = new PostToServer(nv, act, true);
        POST_TO_SERVER.execute(url);

        return null;
    }

    //retrives a location from the maps fragment when called there, and pushes in a location
    //to be written to the data file
    public static void getLocationData(Location location, String provider) {
        lastLocation = location;
        StringBuffer sb = new StringBuffer(512);
        long sessionNum = System.currentTimeMillis() / 1000000 * 60;
        boolean isValid = true;

        sb.append(
                "measured_at=" + (location.getTime() / 1000) + "," +
                        "lat=" + location.getLatitude() + "," +
                        "lon=" + location.getLongitude() + "," +
                        "alt=" + location.getAltitude() + "," +
                        "speed=" + location.getSpeed() + "," +
                        "bearing=" + location.getBearing() + "," +
                        "accuracy=" + location.getAccuracy() + "," +
                        "record_type=" + provider + "," +
                        "session_num=" + sessionNum +
                        "is_valid=" + isValid
        );

        String writeString = sb.toString();

        try {
            Write(writeString + "\n");
        } catch (IOException e) {
            Log.e("ERROR", "Exception appending to log file", e);
        }
    }

    //use a readbuffer to read in from the txt and plot the points
    public static void plotTxtPoints() throws IOException
    {
        //open the file that has all of the location values stored within it
        File file = db.getFile(db.FILE_NAME);
        BufferedReader in = new BufferedReader(new FileReader(file));
        String nextLine = in.readLine();

        //while the next line to be read does not return null (empty line, or EOF)
        while (nextLine != null) {
            //check to see if this line is a location or a heartbeat
            if (nextLine.contains("lat=")) {
                //get the indices of the known substrings
                int indexLat = nextLine.indexOf("lat=");
                int indexLon = nextLine.indexOf("lon=");
                int indexAlt = nextLine.indexOf("alt=");
                //create substrings for the values of the lat and lon floats between known indices
                String latString = nextLine.substring(indexLat + 4, indexLon-1);
                String lonString = nextLine.substring(indexLon+4, indexAlt-1);

                //cast the strings to floats and put into the static array in GoogleMaps for plotting
                float nextLat = Float.valueOf(latString);
                float nextLon = Float.valueOf(lonString);
                LatLng nextlatLng = new LatLng(nextLat, nextLon);
                GoogleMaps.locList.add(nextlatLng);
            }
            nextLine = in.readLine();
        }

        in.close();
    }
}
