package org.geospaces.schas.utils;

import android.os.AsyncTask;
import android.provider.Settings;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.InetAddress;
import java.net.URL;

public class SCHASSettings {
    public static String host       = null;
    public static String username   = "None";
    public static String deviceID   = "ID";
    public static String urls       = "www.geosspaces.org;10.0.0.3";

    public static void Initialize(String ...args) {
        if ( args == null ) {
            String[] weburls = urls.split(";");
            new PickHosts().execute(weburls);
        } else {
            new PickHosts().execute(args);
        }
    }
    public static String getSettings() {
        StringBuilder sb = new StringBuilder(512);

        sb.append(  "host="     +   host        + "\n"  +
                    "username=" +   username    + "\n"  +
                    "deviceID=" +   deviceID    + "\n"  +
                    "urls="     +   urls        + "\n"  +
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
        return s;
    }


    private static class PickHosts extends AsyncTask<String, Integer, String> {
        protected String doInBackground(String... urls) {
            String host = null;

            for ( String h: urls) {
                try {
                    boolean r;
                    r = InetAddress.getByName(h).isReachable(1000);
                    if (r) {
                        host = h;
                        break;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return host;
        }
        protected void onProgressUpdate(Integer... progress) {
        }
        protected void onPostExecute(String result) {
            host = result;
        }
    }

}
