package org.geospaces.schas.utils;

import android.location.Location;
import android.os.AsyncTask;
import android.provider.Settings;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class SCHASSettings {
    public static String    host            = "www.geospaces.org";
    public static String    username        = "None";
    public static String    deviceID        = "ID";
    private static String    urls            = "www.geospaces.org;10.0.0.3";
    public static String    lastLoc         = "";
    public static long      lastRecorded    = -1;
    public static Location  location        = new Location("LAST");

    public static void Initialize() {
        String[] weburls = urls.split(";");
        new PickHosts().execute(weburls);
    }
    public static String getSettings() {
        StringBuilder sb = new StringBuilder(512);

        lastLoc = location.getLongitude() + ", "  + location.getLatitude();
        sb.append(  "host="     +   host        + "\n"  +
                    "username=" +   username    + "\n"  +
                    "deviceID=" +   deviceID    + "\n"  +
                    "urls="     +   urls        + "\n"  +
                    "lastLoc="  +   lastLoc     + "\n"  +
                     ""
        );
        return sb.toString();
    }
    public static String saveSettings() {
        String s = getSettings();
        File file = db.getFile(db.FILE_SETTINGS);
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(file.getAbsolutePath()));
            out.write(s);
            out.close();
        }catch (Exception e){
            ;
        }
        return s;
    }
    public static String readSettings() {
        String s = db.read(db.FILE_SETTINGS);
        if (s.length() <=1 ) {
            s = saveSettings();
            return s;
        }
        String [] lines = s.split("\n");
        HashMap<Object,String> m = new HashMap();
        for ( String l : lines ) {
            String[] kv = l.split("=");
            if ( kv.length <= 1) {
                continue;
            }
            m.put(kv[0].trim(), kv[1].trim());
        }

        host        = m.get("host");
        username    = m.get("username");
        deviceID    = m.get("deviceID");
        //urls        = m.get("urls");
        lastLoc     = m.get("lastLoc");

        if ( lastLoc != null ) {
            String[] ll = lastLoc.split(",");
            if ( ll.length == 2) {
                Float lon = Float.parseFloat(ll[0]);
                Float lat = Float.parseFloat(ll[1]);
                location.setLatitude (lat);
                location.setLongitude(lon);
            }
        }
        Initialize();
        return s;
    }


    private static class PickHosts extends AsyncTask<String, Integer, String> {

        private boolean isReachable(String h) {

            boolean ret = false;
            try {
                ret = InetAddress.getByName(h).isReachable(2000);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return ret;
        }

        public static boolean isInternetReachable(String h){
            String host = h.startsWith("http:") ? h : "http://"+h;
            try {
                URL url = new URL(host);
                HttpURLConnection urlConnect = (HttpURLConnection)url.openConnection();
                urlConnect.setConnectTimeout(5000);
                urlConnect.setReadTimeout(5000);
                Object objData = urlConnect.getContent();
            } catch (Exception e) {
                //e.printStackTrace();
                return false;
            }
            return true;
        }
        protected String doInBackground(String... urls) {

            for ( String h: urls) {
                if(isInternetReachable(h)) {
                    host = h;
                    break;
                }
            }
            return host;
        }
        protected void onProgressUpdate(Integer... progress) {
        }
        protected void onPostExecute(String result) {
            host = (result != null) ? result: host;
        }
    }

}
