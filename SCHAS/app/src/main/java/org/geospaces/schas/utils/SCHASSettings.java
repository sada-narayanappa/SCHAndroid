package org.geospaces.schas.utils;

import android.os.AsyncTask;
import android.provider.Settings;

import java.net.InetAddress;
import java.net.URL;

public class SCHASSettings {
    public static String host       = null;
    public static String username   = "None";
    public static String deviceID   = "ID";

    public static void Initialize(String ...args) {
        if ( args == null ) {
            String host1    = "www.geosspaces.org";
            String host2    = "10.0.0.3";

            new PickHosts().execute(host1, host2);
        } else {
            new PickHosts().execute(args);
        }
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
