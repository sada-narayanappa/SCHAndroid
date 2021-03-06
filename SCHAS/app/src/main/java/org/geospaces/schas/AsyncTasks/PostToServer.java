package org.geospaces.schas.AsyncTasks;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.geospaces.schas.utils.SCHASSettings;
import org.geospaces.schas.utils.db;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class PostToServer extends AsyncTask<String, Integer, String>{
    List<NameValuePair> nameValuePairs = null;
    String              result = "";
    Activity            act;
    String              url;
    boolean             callDBDelete = false;

    public boolean      COMPLETED = false;

    public PostToServer() {
    }
    public PostToServer(List<NameValuePair> nv, Activity a, boolean dbDelete) {
        act = a;
        nameValuePairs =nv;
        callDBDelete = dbDelete;
    }

    public String postResultsChunks(List<NameValuePair> nameValuePairs, String postUrl) {
        NameValuePair nm = nameValuePairs.get(2);
        String text = nm.getValue();

        String[] lines = text.split("\n");

        int idx = 0;
        StringBuffer sb = new StringBuffer(512 * 1024);
        int remains = lines.length;

        int chunkSize = 2000;
        int curIndex  = 0;
        String ret = "";

        for (int i =0; i < lines.length ; ) {
            sb.setLength(0);
            for (int j = 0; j < chunkSize; j++) {
                if ( (j+curIndex) > lines.length) {
                    break;
                }
                sb.append(lines[j+curIndex] + "\n");
            }
            curIndex += chunkSize;
            NameValuePair nm1 = new BasicNameValuePair("text", sb.toString());
            nameValuePairs.set(2,nm1);
            ret = postResults( nameValuePairs, postUrl);
        }

        return ret;
    }

    public String postResults(List<NameValuePair> nameValuePairs, String postUrl) {
        final HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 100000);

        HttpClient  httpclient = new DefaultHttpClient(httpParams);
        HttpPost    httppost = new HttpPost(postUrl);
        String ret;

        try {
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpclient.execute(httppost);

            // Get the response back from the server
            InputStream in = response.getEntity().getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder str = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                str.append(line + "\n");
            }
            in.close();
            ret = str.toString();
            if ( response.getStatusLine().getStatusCode() == 200 && callDBDelete  ) {
                db.delete();
            }

        } catch (Exception e) {
            ret = "ERROR: Transmission Failed " + e;
            Log.e("PostToServer", e.toString());
            Log.e("PostToServer", ret);
            SCHASSettings.host = null;
        }
        ret = ret.replaceAll("(?m)^[ \t]*\r?\n", "");
        return ret;
    }

    @Override
    protected String doInBackground(String... urls) {
        //url = whichHost(urls);
        url = urls[0];

        if ( nameValuePairs == null) {
            nameValuePairs = new ArrayList<NameValuePair>(0);
        }
        String ret = postResults(nameValuePairs, url);
        return ret;
    }


    protected void onPostExecute(String ret) {
        result = ret;
        if (act !=null) {
            Intent i = act.getIntent();
            ret = ret.replaceAll("(?m)^[ \t]*\r?\n", "");
            i.putExtra("result", ret );
            i.putExtra("url", url);
            act.setIntent(i);
        }
        COMPLETED = true;
        db.ContinueUpload(act);
    }
    /**
     * Sample Usage of the API
     */
    public static void post() {
        String url = "http://10.0.0.223:8080/aura/webroot/loc.jsp?cmd=test&a=b";

        List <NameValuePair> nv = new ArrayList<NameValuePair>(2);
        nv.add(new BasicNameValuePair("test1", "A"));

        PostToServer ps = new PostToServer(nv, null, false);
        ps.execute(url);
    }
}