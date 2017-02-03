package org.geospaces.schas.AsyncTasks;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import org.geospaces.schas.Fragments.GoogleMaps;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Erik on 1/18/2017.
 * downloads the previous 24 hours of locations from the server to plot on the google map in orange
 */

public class DownloadPrevLocations extends AsyncTask<Context, Void, String> {

    @Override
    protected String doInBackground(Context... params) {
        String jsonStringFromQuery = "";

        String mobileId = Settings.Secure.getString(params[0].getContentResolver(), Settings.Secure.ANDROID_ID);
        String queryUrl = "http://www.smartconnectedhealth.org/aura/webroot/db.jsp?qn=50&mobile_id=" + mobileId + "&time=24%20hours";
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
    protected void onPostExecute(String jsonString){
        Handler mUIThreadHandler = new Handler(Looper.getMainLooper());
        final String finalJsonString = jsonString;
        mUIThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                GoogleMaps.buildLast24HoursData(finalJsonString);
            }
        });

    }
}
