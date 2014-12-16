package org.geospaces.schas;

import android.os.AsyncTask;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PostToServer extends AsyncTask<String, Integer, String>{

    List<NameValuePair> nameValuePairs = null;
    String              result = "";
    TextView            tv;


    public PostToServer() {
    }
    public PostToServer(List<NameValuePair> nv, TextView tv1) {
        tv = tv1;
        nameValuePairs =nv;
    }

    public String postResults(List<NameValuePair> nameValuePairs, String postUrl) {
        HttpClient  httpclient = new DefaultHttpClient();
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

        } catch (Exception e) {
           ret = "Transmission Failed " + e;
        }
        return ret;
    }

    @Override
    protected String doInBackground(String... urls) {
        String url = urls[0];

        if ( nameValuePairs == null) {
            nameValuePairs = new ArrayList<NameValuePair>(0);
        }
        String ret = postResults(nameValuePairs, url);
        return ret;
    }

    protected void onPostExecute(String ret) {
        result = ret;
        if (tv !=null) {
            tv.setText(ret);
        }
    }


    /**
     * Sample Usage of the API
     */
    public static void post() {
        String url = "http://10.0.0.223:8080/aura/webroot/index.jsp?cmd=test&a=b";

        List <NameValuePair> nv = new ArrayList<NameValuePair>(2);
        nv.add(new BasicNameValuePair("test1", "A"));

        PostToServer ps = new PostToServer(nv, null);
        ps.execute(url);
    }
}
