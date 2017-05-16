package org.geospaces.schas.AsyncTasks;

import android.content.Context;
import android.os.AsyncTask;
import android.provider.Settings;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

public class ReturnMobileIdExistsOnServer extends AsyncTask<Void, Void, String> {
    public Context context;
    public String mobileID;
    public int count;
    public String firebaseToken;

    @Override
    protected String doInBackground(Void... params) {
        String jsonString = "";

        mobileID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        String queryURL = "http://www.smartconnectedhealth.org/aura/webroot/db.jsp?qn=51&mobileid=" + mobileID;
        HttpURLConnection urlConnection = null;

        try{
            URL url = new URL(queryURL);
            urlConnection = (HttpURLConnection) url.openConnection();
            InputStream in = new BufferedInputStream((urlConnection.getInputStream()));
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder builder = new StringBuilder(in.available());

            String currentLine = reader.readLine();
            while (currentLine != null){
                builder.append(currentLine);
                currentLine = reader.readLine();
            }
            jsonString = builder.toString();
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
            Toast.makeText(context, "The URL for getting data was malformed", Toast.LENGTH_SHORT).show();
        }
        catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "There was an exception opening the url connection", Toast.LENGTH_SHORT).show();
        }
        finally {
            if (urlConnection != null){
                urlConnection.disconnect();
            }
        }

        return jsonString;
    }

    @Override
    protected void onPostExecute(String jsonString){
        String[] splitJson = jsonString.split("=");
        JSONObject jsonRootObject = null;
        try {
            jsonRootObject = new JSONObject(splitJson[1]);
            JSONArray rowsArray = jsonRootObject.optJSONArray("rows");
            JSONArray nextJSONArray = rowsArray.getJSONArray(0);
            count = nextJSONArray.getInt(0);
            SendFirebaseTokenToServer sendToken = new SendFirebaseTokenToServer();
            sendToken.context = context;
            sendToken.mobileID = mobileID;
            sendToken.count = count;
            sendToken.firebaseToken = firebaseToken;
            sendToken.execute();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
