package org.geospaces.schas.utils;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.provider.Settings;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.TextView;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.geospaces.schas.PostToServer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class db {
    public final static String  DIRECTORY   = "/SCHAS";
    public final static String  FILE_NAME   = "LOC.txt";
    public final static String  FILE_READY  = "LOC_ready.txt";
    public final static int     FILE_SIZE   = 4 * 1024;

    public final static String  FILE_SETTINGS = "Settings.txt";

    public static String read(String fileName) {
        File file = getFile(fileName);

        if ( !file.exists()) {
            return "";
        }
        String str = "ERROR reading File:  " + fileName;
        char[] bytes = new char[Math.min(5 * 1024, (int) file.length())];
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

    public static String getUploadableText(Context context) throws Exception {
        File file = getFile(FILE_READY);
        if ( !file.exists()) {
            boolean b = db.rename(false);
            if ( !b ) {
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

    public static String getLocation(Location loc, Object...args) {
        if ( loc == null ) {
            return "";
        }
        StringBuffer sb = new StringBuffer(512);
        String sessionNum = (args.length > 0 ) ? ""+args[0] : "";
        String source     = (args.length > 1 ) ? ""+args[1] : "";
        source = source.substring(0, Math.min(4, source.length()));

        double speed      = (args.length > 2 ) ? ((Double)args[2]) : 0.0;

        if (loc.getSpeed() > 0) {
            speed = loc.getSpeed();
        }

        StringBuffer append = sb.append(
                "measured_at="  + (loc.getTime()/1000)  + "," +
                "lat="          + loc.getLatitude()     + "," +
                "lon="          + loc.getLongitude()    + "," +
                "alt="          + loc.getAltitude()     + "," +
                "speed="        + speed                 + "," +
                "bearing="      + loc.getBearing()      + "," +
                "accuracy="     + loc.getAccuracy()     + "," +
                "record_type="  + "GPS_"+source         + "," +
                "session_num="  + sessionNum            + ""  +
                ""
        );

        return sb.toString();
    }

    public static String getAttack(Location loc, String severity) {
        if ( loc == null ) {
            return "";
        }
        StringBuffer sb = new StringBuffer(512);
        long sessionNum = System.currentTimeMillis()/1000000 * 60;

        StringBuffer append = sb.append(
                "measured_at="  + (loc.getTime()/1000)  + "," +
                        "lat="          + loc.getLatitude()     + "," +
                        "lon="          + loc.getLongitude()    + "," +
                        "alt="          + loc.getAltitude()     + "," +
                        "speed="        + loc.getSpeed()        + "," +
                        "bearing="      + loc.getBearing()      + "," +
                        "accuracy="     + loc.getAccuracy()     + "," +
                        "record_type="  + severity              + "," +
                        "session_num="  + sessionNum            + ""  +
                        ""
        );

        return sb.toString();
    }

    public static boolean fileReady() {
        File to     = getFile(FILE_READY);
        return to.exists();
    }
    public synchronized static boolean rename(boolean force) {
        File from   = getFile(FILE_NAME);
        File to     = getFile(FILE_READY);

        if ( !from.exists() ) {
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

    public static boolean isWIFIOn(Context ctx) {
        ConnectivityManager connManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        WifiManager wifiman     = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        NetworkInfo mWifi       = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        HashMap m = new HashMap();

        if (mWifi.isConnected()) {
            int linkSpeed = wifiman.getConnectionInfo().getLinkSpeed();
            int ip = wifiman.getConnectionInfo().getIpAddress();
            String ips = Formatter.formatIpAddress(ip);
            String str = "SPEED: " + linkSpeed +"Mbps, STRENGTH: " + wifiman.getConnectionInfo().getRssi() + "dBm";
            return true;
        }
        return false;
    }

    public static synchronized boolean Upload(Context ctx, Activity act) {
        if ( !db.isWIFIOn(ctx) ) {
            Log.w("DB:Upload", "WIFI is not ready!!!");
            return false;
        }
        db.rename(false);
        if ( !db.fileReady() ) {
            String msg = "Redo:" + SCHASSettings.host + " File not ready";
            Log.w("DB:Upload:", msg);
            return false;
        }
        boolean r = db.Post( act, ctx, "/aura/webroot/loc.jsp");
        if ( !r ) {
            String msg = "Redo:" + SCHASSettings.host + " Post failed";
            Log.w("DB:post:", msg);
            return false;
        }
        Log.w("DB:post:", " Post succeeded");
        return true;
    }

    private static PostToServer POST_TO_SERVER = null;
    private static synchronized boolean Post(Activity act, Context context, String service) {

        if ( POST_TO_SERVER != null && !POST_TO_SERVER.COMPLETED) {   // Avoid race condition
            Log.e("DB", "One posting going on, please retry ....");
            return false;
        }

        String host     = SCHASSettings.host;

        if ( host == null || ! isWIFIOn(context) || host.equals("null") ) {
            SCHASSettings.Initialize(null);
            return false;
        }
        String url = "http://" + host+ service;
        List <NameValuePair> nv = new ArrayList<NameValuePair>(2);
        String msg = "";

        try {
            msg = getUploadableText(context);
        } catch (Exception e) {
            return false;
        }
        String ID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        nv.add(new BasicNameValuePair("api_key", ID));
        nv.add(new BasicNameValuePair("mobile_id", ID));
        nv.add(new BasicNameValuePair("text", msg));

        String ns = "Sending: " + url + "\n" + msg.substring(0, Math.min(msg.length() - 1, 256) );
        Log.i("",ns);

        POST_TO_SERVER = null;
        POST_TO_SERVER = new PostToServer(nv, act, true);
        POST_TO_SERVER.execute(url);

        return true;
    }
}
