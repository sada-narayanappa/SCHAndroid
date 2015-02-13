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

//        file = getFile(FILE_READY);
//        String str = null;
//        char[] bytes = new char[ (int) file.length() ];
//        StringBuilder sb = new StringBuilder();
//
//        BufferedReader in = new BufferedReader(new FileReader(file.getAbsolutePath()));
//        String ID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
//        String line;
//        while ( null != (line = in.readLine()) ) {
//            String nl = "mobile_id=" + ID + ", api_key=" + ID + "," + line +"\n";
//            sb.append(nl);
//        }
//
//        return sb.toString();
    }


    public static File getFile(String fileName) {
        File externalMem2 = Environment.getExternalStorageDirectory();
        File directory2 = new File(externalMem2.getAbsolutePath() + DIRECTORY);
        directory2.mkdirs();
        fileName = (fileName == null) ? FILE_READY : fileName;
        File file = new File(directory2, fileName);

        return file;
    }

    public static String getLocation(Location loc) {
        StringBuffer sb = new StringBuffer(512);
        StringBuffer append = sb.append(
                "measured_at="  + (loc.getTime()/1000)  + "," +
                "lat="          + loc.getLatitude()     + "," +
                "lon="          + loc.getLongitude()    + "," +
                "alt="          + loc.getAltitude()     + "," +
                "speed="        + loc.getSpeed()        + "," +
                "bearing="      + loc.getBearing()      + "," +
                "accuracy="     + loc.getAccuracy()     + ""  +
                ""
        );

        return sb.toString();
    }

    public static boolean rename(boolean force) {
        File externalMem2 = Environment.getExternalStorageDirectory();
        File directory2 = new File(externalMem2.getAbsolutePath() + DIRECTORY);
        directory2.mkdirs();
        File from = new File(directory2, FILE_NAME);
        File to = new File(directory2, FILE_READY);

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

    public static boolean Post(Activity act, Context context, String service) {
        String host     = SCHASSettings.host;

        if ( host == null || ! isWIFIOn(context) ) {
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

        PostToServer ps = new PostToServer(nv, act);
        ps.execute(url);

        return true;
    }
}
