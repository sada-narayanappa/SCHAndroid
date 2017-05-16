package org.geospaces.schas.AsyncTasks;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Erik on 4/25/2017.
 */

public class SendFirebaseTokenToServer extends AsyncTask<Void, Void, String> {
    Context context;
    String mobileID;
    int count;
    String firebaseToken;

    @Override
    protected String doInBackground(Void... params) {
        String queryURL = "";
        if (count > 0){
            //upload an updated firebase token using db.jsp QN 53
            queryURL = "http://www.smartconnectedhealth.org/aura/webroot/db.jsp?qn=53&mobileid=" + mobileID + "&firebasetoken=" + firebaseToken;
            HttpURLConnection urlConnection = null;

            try{
                URL url = new URL(queryURL);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = new BufferedInputStream((urlConnection.getInputStream()));
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder builder = new StringBuilder(in.available());

                String currentline = reader.readLine();
                while (currentline != null){
                    builder.append(currentline);
                    currentline = reader.readLine();
                }
                String responseString = builder.toString();
                return responseString;
            }
            catch (MalformedURLException e) {
                e.printStackTrace();
                Toast.makeText(context, "The URL for getting data was malformed", Toast.LENGTH_SHORT).show();
            }
            catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(context, "There was an exception opening the url connection", Toast.LENGTH_SHORT).show();
            }
        }
        else if (count == 0){
            //upload a firebase token using db.jsp QN 52
            queryURL = "http://www.smartconnectedhealth.org/aura/webroot/db.jsp?qn=52&mobileid=" + mobileID + "&firebasetoken=" + firebaseToken;
            HttpURLConnection urlConnection = null;

            try{
                URL url = new URL(queryURL);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = new BufferedInputStream((urlConnection.getInputStream()));
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder builder = new StringBuilder(in.available());

                String currentline = reader.readLine();
                while (currentline != null){
                    builder.append(currentline);
                    currentline = reader.readLine();
                }
                String responseString = builder.toString();
                return responseString;
            }
            catch (MalformedURLException e) {
                e.printStackTrace();
                Toast.makeText(context, "The URL for getting data was malformed", Toast.LENGTH_SHORT).show();
            }
            catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(context, "There was an exception opening the url connection", Toast.LENGTH_SHORT).show();
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(String responseString){
        if (responseString == null){
            return;
        }
        else{
            Toast.makeText(context, responseString, Toast.LENGTH_SHORT).show();
        }
    }
}
