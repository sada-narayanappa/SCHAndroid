package org.geospaces.schas;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.commonsware.cwac.locpoll.LocationPoller;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import mymodule.app2.mymodule.app2.schasStrings;

public class GPSWakfulReciever extends BroadcastReceiver {
    public final static String DIRECTORY = "/SCHAS";
    public final static String FILE_NAME = "LOC.txt";
    public final static String FILE_READY = "LOC_ready.txt";
    public final static int FILE_SIZE = 4 * 1024;

    @Override
    public void onReceive(Context context, Intent intent) {
        File externalMem2 = Environment.getExternalStorageDirectory();
        File directory2 = new File(externalMem2.getAbsolutePath() + DIRECTORY);
        directory2.mkdirs();
        File log = new File(directory2, FILE_NAME);

        String ID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        if (log.length() > FILE_SIZE) {
            if (!rename(false)) {
                return;                 // File is full and we can't do much now
            }
        }
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(log.getAbsolutePath(), log.exists()));

            Bundle b = intent.getExtras();
            Location loc = (Location) b.get(LocationPoller.EXTRA_LOCATION);
            String msg;

            if (loc == null) {
                loc = (Location) b.get(LocationPoller.EXTRA_LASTKNOWN);
                if (loc == null) {
                    msg = intent.getStringExtra(LocationPoller.EXTRA_ERROR);
                } else {
                    msg = "TIMEOUT, lastKnown=" + loc.toString();
                }
            } else {
                //msg = loc.toString();
                msg = getLocation(loc);
            }
            if (msg == null) {
                msg = "Invalid broadcast received!";
            }
            String wmsg = "id=" + ID + "& " + msg + "\n";
            out.write(wmsg);
            out.close();
            //Toast.makeText(context, "GPSWakfulReceiveer:" + wmsg, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(getClass().getName(), "Exception appending to log file", e);
        }
    }

    public static String read(String fileName) {
        File externalMem2 = Environment.getExternalStorageDirectory();
        File directory2 = new File(externalMem2.getAbsolutePath() + DIRECTORY);
        directory2.mkdirs();
        fileName = (fileName == null) ? FILE_READY : fileName;
        File file = new File(directory2, fileName);

        String str = "ERROR reading File:  " + fileName;
        char[] bytes = new char[Math.min(5 * 1024, (int) file.length())];
        try {
            FileReader in = new FileReader(file);
            in.read(bytes);
            str = new String(bytes);
        } catch (Exception e) {
            Log.e("ERR", "" + e);
        }
        return str;
    }

    public static String getLocation(Location loc) {
        StringBuffer sb = new StringBuffer(512);
        StringBuffer append = sb.append(
                "time=" + loc.getTime() + "," +
                        "lat=" + loc.getLatitude() + "," +
                        "lon=" + loc.getLongitude() + "," +
                        "alt=" + loc.getAltitude() + "," +
                        "speed=" + loc.getSpeed() + "," +
                        "bearing=" + loc.getBearing() + "," +
                        "accuracy=" + loc.getAccuracy() + "" +
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
        File from = new File(directory2, FILE_NAME);
        File to = new File(directory2, FILE_READY);
        from.delete();
        to.delete();
    }

    /**
     * Sample Usage of the API
     */
    public static void post() {
        String url = "http://10.0.0.223:8080/aura/webroot/loc.jsp";

        List<NameValuePair> nv = new ArrayList<NameValuePair>(2);
        nv.add(new BasicNameValuePair("test1", "A"));

        PostToServer ps = new PostToServer(nv, null);
        ps.execute(url);
    }

    public static boolean isWIFIOn(Context ctx) {
        ConnectivityManager connManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        WifiManager         wifiman     = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        NetworkInfo         mWifi       = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

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
    public static void Post(TextView tv) {
        String url = "http://10.0.0.223:8080/aura/webroot/loc.jsp";

        List <NameValuePair> nv = new ArrayList<NameValuePair>(2);
        String msg = GPSWakfulReciever.read(GPSWakfulReciever.FILE_NAME);

        nv.add(new BasicNameValuePair("api_key", "123"));
        nv.add(new BasicNameValuePair("text", msg));

        if ( tv != null ) {
            tv.setText("Sending: " + url + "\n" + msg.substring(0, 256));
        }

        PostToServer ps = new PostToServer(nv, tv);
        ps.execute(url);
    }
}

