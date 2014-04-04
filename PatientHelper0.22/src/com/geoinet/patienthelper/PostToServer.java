package com.geoinet.patienthelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.os.Handler;

public class PostToServer
{
	private static HttpPost httppost;
	private static HttpClient httpclient;
	
	// posts the data to the server
	// returns the server response
	public String postResults(List<NameValuePair> nameValuePairs, String postUrl)
	{
		httpclient = new DefaultHttpClient();
		httppost = new HttpPost(postUrl);
		try
		{
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			// Execute HTTP Post Request
			HttpResponse response = httpclient.execute(httppost);

			// Get the response back from the server
			InputStream in = response.getEntity().getContent();
			BufferedReader reader = new BufferedReader(	new InputStreamReader(in));
			StringBuilder str = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null)
			{
				str.append(line + "\n");
			}
			in.close();
			try
			{
				String debugMessage = str.toString();
				
				return debugMessage;
			}
			catch (Exception e)
			{
			}

		}
		catch (ClientProtocolException e)
		{
		}
		catch (IOException e)
		{
		}
		return "Transmission Failed";
	}
}
